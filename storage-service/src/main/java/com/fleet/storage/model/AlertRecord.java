package com.fleet.storage.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_record")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleId;
    @Column(name = "alert_type")
    private String alertType;
    @Column(name = "alert_message")
    private String alertMessage;
    @Column(name = "reading_value")
    private Double readingValue;
    @Column(name = "recorded_at")
    private String recordedAt;
    @Column(name = "processed_at")
    private String processedAt;

    protected AlertRecord() { }

    public AlertRecord(String vehicleId, String alertType, String alertMessage,
                       Double readingValue, String recordedAt, String processedAt) {
        this.vehicleId = vehicleId;
        this.alertType = alertType;
        this.alertMessage = alertMessage;
        this.readingValue = readingValue;
        this.recordedAt = recordedAt;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public String getAlertType() { return alertType; }
    public String getAlertMessage() { return alertMessage; }
    public Double getReadingValue() { return readingValue; }
    public String getRecordedAt() { return recordedAt; }
    public String getProcessedAt() { return processedAt; }
}
