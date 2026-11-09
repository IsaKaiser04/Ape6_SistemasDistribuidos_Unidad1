/*Servicio de Alertas: consumidor de mensajes de las colas de temperatura y de combustible
 Posterior envio a una cola centralizada */
package com.fleet.processor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.processor.config.RabbitMQConfig;
import com.fleet.processor.model.AlertEvent;
import com.fleet.processor.model.ProcessorStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Lee las colas de alertas (temperatura y combustible), normaliza la alerta y la
 * (1) envia a storage-service y (2) reenvia a cola.notificaciones.
 */
@Component
public class AlertProcessor {

    private static final Logger log = LoggerFactory.getLogger(AlertProcessor.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;
    private final ProcessorStats stats;

    public AlertProcessor(RabbitTemplate rabbitTemplate, ObjectMapper mapper, ProcessorStats stats) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.stats = stats;
    }

    @RabbitListener(queues = RabbitMQConfig.TEMP_ALERT_QUEUE)
    public void onTempAlert(Message message) {
        if (procesar(message, "ALERTA TEMPERATURA", "temperature", "TEMP_ALERT", "Temperatura excedida")) {
            stats.tempAlert();
        }
    }

    @RabbitListener(queues = RabbitMQConfig.FUEL_QUEUE)
    public void onFuelAlert(Message message) {
        if (procesar(message, "ALERTA COMBUSTIBLE", "fuel_level", "FUEL_LOW", "Combustible bajo")) {
            stats.fuelAlert();
        }
    }

    private boolean procesar(Message message, String etiqueta, String campoValor,
                             String tipoPorDefecto, String mensajePorDefecto) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode json = mapper.readTree(body);

            String vehicleId = json.path("vehicle_id").asText("");
            if (vehicleId.isBlank()) {
                throw new IllegalArgumentException("vehicle_id vacio");
            }
            Double valor = json.hasNonNull(campoValor) ? json.get(campoValor).asDouble() : null;

            AlertEvent event = new AlertEvent(
                    vehicleId,
                    json.path("type").asText(tipoPorDefecto),
                    json.path("message").asText(mensajePorDefecto),
                    valor,
                    json.path("timestamp").asText(""),
                    Instant.now().toString());

            log.warn("{}: {} = {}", etiqueta, event.vehicleId(), event.value());

            // 1) a storage-service (por el exchange de la flota)
            rabbitTemplate.convertAndSend(RabbitMQConfig.FLEET_EXCHANGE,
                    RabbitMQConfig.RK_STORAGE_ALERT, event);
            // 2) a notificaciones (exchange por defecto, routing key = nombre de la cola)
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, event);
            return true;
        } catch (Exception e) {
            stats.discard();
            log.error("Alerta descartada: {}", e.getMessage());
            return false;
        }
    }
}
