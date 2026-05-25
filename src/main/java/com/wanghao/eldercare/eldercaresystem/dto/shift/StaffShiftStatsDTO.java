package com.wanghao.eldercare.eldercaresystem.dto.shift;

public class StaffShiftStatsDTO {
    private long onDutyCount;
    private long morningCount;
    private long afternoonCount;
    private long nightCount;
    private long fullDayCount;
    private long cancelledCount;
    private long conflictCount;

    public long getOnDutyCount() {
        return onDutyCount;
    }

    public void setOnDutyCount(long onDutyCount) {
        this.onDutyCount = onDutyCount;
    }

    public long getMorningCount() {
        return morningCount;
    }

    public void setMorningCount(long morningCount) {
        this.morningCount = morningCount;
    }

    public long getAfternoonCount() {
        return afternoonCount;
    }

    public void setAfternoonCount(long afternoonCount) {
        this.afternoonCount = afternoonCount;
    }

    public long getNightCount() {
        return nightCount;
    }

    public void setNightCount(long nightCount) {
        this.nightCount = nightCount;
    }

    public long getFullDayCount() {
        return fullDayCount;
    }

    public void setFullDayCount(long fullDayCount) {
        this.fullDayCount = fullDayCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public long getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(long conflictCount) {
        this.conflictCount = conflictCount;
    }
}
