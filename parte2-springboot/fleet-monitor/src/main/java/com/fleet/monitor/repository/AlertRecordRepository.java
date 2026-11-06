package com.fleet.monitor.repository;

import com.fleet.monitor.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findTop20ByOrderByIdDesc();
}
