package com.wanghao.eldercare.eldercaresystem.careplan;

import java.util.List;

public class CarePlanChangeListResponse {
    private List<CarePlanChangeDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<CarePlanChangeDTO> getContent() {
        return content;
    }

    public void setContent(List<CarePlanChangeDTO> content) {
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
