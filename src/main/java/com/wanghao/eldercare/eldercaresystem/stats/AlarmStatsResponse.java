package com.wanghao.eldercare.eldercaresystem.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class AlarmStatsResponse {
    private long total;
    private Map<String, Long> bySeverity = new LinkedHashMap<>();
    private double avgAcceptSeconds;
    private double avgArriveSeconds;
    private long overtimeAcceptCount;
    private long overtimeArriveCount;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Map<String, Long> getBySeverity() {
        return bySeverity;
    }

    public void setBySeverity(Map<String, Long> bySeverity) {
        this.bySeverity = bySeverity;
    }

    public double getAvgAcceptSeconds() {
        return avgAcceptSeconds;
    }

    public void setAvgAcceptSeconds(double avgAcceptSeconds) {
        this.avgAcceptSeconds = avgAcceptSeconds;
    }

    public double getAvgArriveSeconds() {
        return avgArriveSeconds;
    }

    public void setAvgArriveSeconds(double avgArriveSeconds) {
        this.avgArriveSeconds = avgArriveSeconds;
    }

    public long getOvertimeAcceptCount() {
        return overtimeAcceptCount;
    }

    public void setOvertimeAcceptCount(long overtimeAcceptCount) {
        this.overtimeAcceptCount = overtimeAcceptCount;
    }

    public long getOvertimeArriveCount() {
        return overtimeArriveCount;
    }

    public void setOvertimeArriveCount(long overtimeArriveCount) {
        this.overtimeArriveCount = overtimeArriveCount;
    }
}

