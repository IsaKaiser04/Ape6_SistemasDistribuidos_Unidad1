package com.fleet.monitor.repository;

import com.fleet.monitor.model.GpsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GpsRecordRepository extends JpaRepository<GpsRecord, Long> {

    List<GpsRecord> findTop20ByVehicleIdOrderByIdDesc(String vehicleId);

    @Query("select count(distinct g.vehicleId) from GpsRecord g")
    long countDistinctVehicles();
}
