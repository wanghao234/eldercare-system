package com.wanghao.eldercare.eldercaresystem.workflow;

import java.util.List;

public class WfTaskListResponse {
    private List<WfTaskDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<WfTaskDTO> getContent() { return content; }
    public void setContent(List<WfTaskDTO> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
