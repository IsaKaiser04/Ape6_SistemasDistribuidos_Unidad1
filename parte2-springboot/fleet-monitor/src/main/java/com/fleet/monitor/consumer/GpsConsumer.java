package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.GpsMessage;
import com.fleet.monitor.model.GpsRecord;
import com.fleet.monitor.repository.GpsRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class GpsConsumer {

    private static final Logger log = LoggerFactory.getLogger(GpsConsumer.class);

    private final GpsRecordRepository repository;
    private final ObjectMapper mapper;

    public GpsConsumer(GpsRecordRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    // Se recibe el Message completo porque el puente en Python publica sin content-type;
    // asi se lee el cuerpo como texto UTF-8 sin depender del conversor.
    @RabbitListener(queues = RabbitMQConfig.GPS_QUEUE)
    public void consumeGps(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("GPS recibido: {}", body);
            GpsMessage gps = mapper.readValue(body, GpsMessage.class);
            repository.save(new GpsRecord(gps.vehicleId(), gps.lat(), gps.lng(),
                    gps.speed(), gps.timestamp()));
        } catch (Exception e) {
            // Se captura para que un mensaje malformado no se reencole en bucle
            log.error("Error procesando GPS: {}", e.getMessage());
        }
    }
}
