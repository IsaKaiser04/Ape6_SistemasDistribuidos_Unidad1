package com.fleet.processor.model;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Contadores en memoria para la demo (se consultan en /api/processor/stats). */
@Component
public class ProcessorStats {

    private final AtomicLong gpsProcessed = new AtomicLong();
    private final AtomicLong tempAlerts = new AtomicLong();
    private final AtomicLong fuelAlerts = new AtomicLong();
    private final AtomicLong discarded = new AtomicLong();

    public void gps() { gpsProcessed.incrementAndGet(); }
    public void tempAlert() { tempAlerts.incrementAndGet(); }
    public void fuelAlert() { fuelAlerts.incrementAndGet(); }
    public void discard() { discarded.incrementAndGet(); }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gpsProcessed", gpsProcessed.get());
        m.put("tempAlerts", tempAlerts.get());
        m.put("fuelAlerts", fuelAlerts.get());
        m.put("discarded", discarded.get());
        return m;
    }
}
