package com.wanghao.eldercare.eldercaresystem.qc;

public class CreateQcAuditRequest {
    private Long elderId;
    private String title;

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
