package com.wanghao.eldercare.eldercaresystem.admission;

import java.util.List;

public class DischargeListResponse {
    private List<DischargeListItemDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<DischargeListItemDTO> getContent() {
        return content;
    }

    public void setContent(List<DischargeListItemDTO> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
