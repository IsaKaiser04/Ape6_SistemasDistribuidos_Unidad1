package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.AlertRecord;
import com.fleet.monitor.repository.AlertRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final AlertRecordRepository repository;
    private final ObjectMapper mapper;

    public AlertConsumer(RabbitTemplate rabbitTemplate, AlertRecordRepository repository,
                         ObjectMapper mapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.repository = repository;
        this.mapper = mapper;
    }

    @RabbitListener(queues = RabbitMQConfig.TEMP_ALERT_QUEUE)
    public void consumeTempAlert(Message message) {
        procesar(message, "ALERTA TEMPERATURA", "temperature");
    }

    @RabbitListener(queues = RabbitMQConfig.FUEL_QUEUE)
    public void consumeFuelAlert(Message message) {
        procesar(message, "ALERTA COMBUSTIBLE", "fuel_level");
    }

    private void procesar(Message message, String etiqueta, String campoValor) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.warn("{}: {}", etiqueta, body);
            JsonNode json = mapper.readTree(body);
            repository.save(new AlertRecord(
                    json.path("vehicle_id").asText(),
                    json.path("type").asText(),
                    json.path("message").asText(),
                    json.has(campoValor) ? json.get(campoValor).asDouble() : null,
                    json.path("timestamp").asText()));
            // Reenvio a la cola de notificaciones (exchange por defecto, routing key = nombre de la cola)
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, body);
        } catch (Exception e) {
            log.error("Error procesando alerta: {}", e.getMessage());
        }
    }
}
