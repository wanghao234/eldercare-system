package com.wanghao.eldercare.eldercaresystem.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskStatsResponse {
    private long total;
    private double completedRate;
    private double overdueRate;
    private Map<String, Long> byType = new LinkedHashMap<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public double getCompletedRate() {
        return completedRate;
    }

    public void setCompletedRate(double completedRate) {
        this.completedRate = completedRate;
    }

    public double getOverdueRate() {
        return overdueRate;
    }

    public void setOverdueRate(double overdueRate) {
        this.overdueRate = overdueRate;
    }

    public Map<String, Long> getByType() {
        return byType;
    }

    public void setByType(Map<String, Long> byType) {
        this.byType = byType;
    }
}

