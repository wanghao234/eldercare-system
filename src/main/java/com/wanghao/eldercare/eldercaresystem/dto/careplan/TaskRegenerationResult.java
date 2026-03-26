package com.wanghao.eldercare.eldercaresystem.dto.careplan;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;

public class TaskRegenerationResult {
    private final int deletedCount;
    private final int generatedCount;

    public TaskRegenerationResult(int deletedCount, int generatedCount) {
        this.deletedCount = deletedCount;
        this.generatedCount = generatedCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }
}
