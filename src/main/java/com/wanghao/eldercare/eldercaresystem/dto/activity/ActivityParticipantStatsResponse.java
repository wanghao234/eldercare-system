package com.wanghao.eldercare.eldercaresystem.dto.activity;

public class ActivityParticipantStatsResponse {

    private long participantCount;
    private long signedCount;
    private long checkedInCount;
    private long cancelledCount;
    private long totalCount;

    public long getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(long participantCount) {
        this.participantCount = participantCount;
    }

    public long getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(long signedCount) {
        this.signedCount = signedCount;
    }

    public long getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(long checkedInCount) {
        this.checkedInCount = checkedInCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}

