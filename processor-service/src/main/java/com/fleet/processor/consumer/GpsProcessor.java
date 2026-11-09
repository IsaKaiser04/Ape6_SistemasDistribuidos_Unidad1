/** Servicio Telemetria: consumidor RabbitMQ que procese los mensajes de GPS y los almacene. */
package com.fleet.processor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.processor.config.RabbitMQConfig;
import com.fleet.processor.model.GpsEvent;
import com.fleet.processor.model.GpsMessage;
import com.fleet.processor.model.ProcessorStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Lee cola.gps.telemetria, valida, normaliza y envia el evento a storage-service. */
@Component
public class GpsProcessor {

    private static final Logger log = LoggerFactory.getLogger(GpsProcessor.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;
    private final ProcessorStats stats;

    public GpsProcessor(RabbitTemplate rabbitTemplate, ObjectMapper mapper, ProcessorStats stats) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.stats = stats;
    }

    // Se recibe el Message completo para leer el cuerpo como UTF-8 sin depender del tipo.
    @RabbitListener(queues = RabbitMQConfig.GPS_QUEUE)
    public void onGps(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            GpsMessage in = mapper.readValue(body, GpsMessage.class);
            validar(in);
            GpsEvent event = new GpsEvent(in.vehicleId(), in.timestamp(),
                    in.lat(), in.lng(), in.speed(), Instant.now().toString());
            rabbitTemplate.convertAndSend(RabbitMQConfig.FLEET_EXCHANGE,
                    RabbitMQConfig.RK_STORAGE_GPS, event);
            stats.gps();
            log.info("GPS procesado: {} ({}, {}) {} km/h", event.vehicleId(), event.lat(), event.lng(), event.speed());
        } catch (Exception e) {
            // Mensaje malformado: se descarta (no se reencola en bucle)
            stats.discard();
            log.error("GPS descartado: {}", e.getMessage());
        }
    }

    private void validar(GpsMessage m) {
        if (m.vehicleId() == null || m.vehicleId().isBlank()) {
            throw new IllegalArgumentException("vehicle_id vacio");
        }
        if (m.lat() == null || m.lng() == null || m.speed() == null) {
            throw new IllegalArgumentException("faltan lat/lng/speed");
        }
        if (m.lat() < -90 || m.lat() > 90 || m.lng() < -180 || m.lng() > 180) {
            throw new IllegalArgumentException("coordenadas fuera de rango");
        }
        if (m.speed() < 0) {
            throw new IllegalArgumentException("velocidad negativa");
        }
    }
}
