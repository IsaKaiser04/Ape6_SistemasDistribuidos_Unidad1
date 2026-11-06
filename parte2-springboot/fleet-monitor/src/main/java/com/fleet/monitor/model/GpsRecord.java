package com.fleet.monitor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gps_record")
public class GpsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleId;
    private double lat;
    private double lng;
    private double speed;
    @Column(name = "recorded_at")
    private String recordedAt;

    protected GpsRecord() { }

    public GpsRecord(String vehicleId, double lat, double lng, double speed, String recordedAt) {
        this.vehicleId = vehicleId;
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getSpeed() { return speed; }
    public String getRecordedAt() { return recordedAt; }
}
