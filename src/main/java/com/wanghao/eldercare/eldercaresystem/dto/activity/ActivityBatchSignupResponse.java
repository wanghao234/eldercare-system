package com.wanghao.eldercare.eldercaresystem.dto.activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityBatchSignupResponse {

    private int successCount;
    private int failCount;
    private List<ActivityBatchSignupResultItem> items = new ArrayList<>();

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public List<ActivityBatchSignupResultItem> getItems() {
        return items;
    }

    public void setItems(List<ActivityBatchSignupResultItem> items) {
        this.items = items;
    }

    public static class ActivityBatchSignupResultItem {
        private Long elderId;
        private boolean success;
        private String message;
        private ActivityParticipantDTO participant;

        public Long getElderId() {
            return elderId;
        }

        public void setElderId(Long elderId) {
            this.elderId = elderId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ActivityParticipantDTO getParticipant() {
            return participant;
        }

        public void setParticipant(ActivityParticipantDTO participant) {
            this.participant = participant;
        }
    }
}

