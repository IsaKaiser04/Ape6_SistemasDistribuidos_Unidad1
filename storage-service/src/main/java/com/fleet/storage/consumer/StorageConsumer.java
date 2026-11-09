package com.fleet.storage.consumer;

import com.fleet.storage.config.RabbitMQConfig;
import com.fleet.storage.model.AlertEvent;
import com.fleet.storage.model.AlertRecord;
import com.fleet.storage.model.GpsEvent;
import com.fleet.storage.model.GpsRecord;
import com.fleet.storage.repository.AlertRecordRepository;
import com.fleet.storage.repository.GpsRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consume los eventos que dejo el processor y los guarda en la base de datos. */
@Component
public class StorageConsumer {

    private static final Logger log = LoggerFactory.getLogger(StorageConsumer.class);

    private final GpsRecordRepository gpsRepository;
    private final AlertRecordRepository alertRepository;

    public StorageConsumer(GpsRecordRepository gpsRepository, AlertRecordRepository alertRepository) {
        this.gpsRepository = gpsRepository;
        this.alertRepository = alertRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.STORAGE_GPS_QUEUE)
    public void saveGps(GpsEvent e) {
        gpsRepository.save(new GpsRecord(e.vehicleId(), e.lat(), e.lng(), e.speed(),
                e.timestamp(), e.processedAt()));
        log.info("GPS guardado: {}", e.vehicleId());
    }

    @RabbitListener(queues = RabbitMQConfig.STORAGE_ALERT_QUEUE)
    public void saveAlert(AlertEvent e) {
        alertRepository.save(new AlertRecord(e.vehicleId(), e.type(), e.message(), e.value(),
                e.timestamp(), e.processedAt()));
        log.warn("Alerta guardada: {} {}", e.vehicleId(), e.type());
    }
}
