package com.wanghao.eldercare.eldercaresystem.dto.careteam;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careteam.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.*;
import com.wanghao.eldercare.eldercaresystem.service.careteam.*;
import java.util.List;

public class CareTeamAssignmentPageResponse {
    private List<CareTeamAssignmentDTO> content;
    private long totalElements;
    private int page;
    private int size;

    public List<CareTeamAssignmentDTO> getContent() {
        return content;
    }

    public void setContent(List<CareTeamAssignmentDTO> content) {
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
