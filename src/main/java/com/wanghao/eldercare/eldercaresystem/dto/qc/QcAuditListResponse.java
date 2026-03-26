package com.wanghao.eldercare.eldercaresystem.dto.qc;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.qc.*;
import com.wanghao.eldercare.eldercaresystem.mapper.qc.*;
import com.wanghao.eldercare.eldercaresystem.service.qc.*;
import java.util.List;

public class QcAuditListResponse {
    private List<QcAuditDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<QcAuditDTO> getContent() { return content; }
    public void setContent(List<QcAuditDTO> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
