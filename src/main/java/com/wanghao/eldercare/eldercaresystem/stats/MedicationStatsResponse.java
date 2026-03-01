package com.wanghao.eldercare.eldercaresystem.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class MedicationStatsResponse {
    private long totalRecords;
    private double missedRate;
    private double refusedRate;
    private Map<String, Long> byStatus = new LinkedHashMap<>();

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public double getMissedRate() {
        return missedRate;
    }

    public void setMissedRate(double missedRate) {
        this.missedRate = missedRate;
    }

    public double getRefusedRate() {
        return refusedRate;
    }

    public void setRefusedRate(double refusedRate) {
        this.refusedRate = refusedRate;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus;
    }
}

