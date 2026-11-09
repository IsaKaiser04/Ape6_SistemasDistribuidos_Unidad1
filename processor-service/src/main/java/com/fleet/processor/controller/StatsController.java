package com.fleet.processor.controller;

import com.fleet.processor.model.ProcessorStats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/processor")
public class StatsController {

    private final ProcessorStats stats;

    public StatsController(ProcessorStats stats) {
        this.stats = stats;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return stats.snapshot();
    }
}
