package com.wanghao.eldercare.eldercaresystem.service.careplan;

import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanDraftDTO;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanGenerateRequest;

public interface AiCarePlanService {

    AiCarePlanDraftDTO generateDraft(CurrentUser currentUser, AiCarePlanGenerateRequest request);
}
