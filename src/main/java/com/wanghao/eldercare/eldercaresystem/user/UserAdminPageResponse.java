package com.wanghao.eldercare.eldercaresystem.user;

import java.util.List;

public class UserAdminPageResponse {
    private List<UserAdminDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<UserAdminDTO> getContent() {
        return content;
    }

    public void setContent(List<UserAdminDTO> content) {
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
