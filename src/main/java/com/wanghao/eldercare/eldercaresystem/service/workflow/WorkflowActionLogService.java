package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTask;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTaskAction;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskActionRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WorkflowActionLogService {

    private final WfTaskActionRepository wfTaskActionRepository;
    private final ObjectMapper objectMapper;

    public WorkflowActionLogService(WfTaskActionRepository wfTaskActionRepository, ObjectMapper objectMapper) {
        this.wfTaskActionRepository = wfTaskActionRepository;
        this.objectMapper = objectMapper;
    }

    public void log(WfTask task,
                    String action,
                    Long actorId,
                    String comment,
                    String formDataJson,
                    String attachmentsJson,
                    LocalDateTime actionTime) {
        WfTaskAction wfTaskAction = new WfTaskAction();
        wfTaskAction.setWfTaskId(task.getWfTaskId());
        wfTaskAction.setInstanceId(task.getInstanceId());
        wfTaskAction.setAction(action);
        wfTaskAction.setActorId(actorId);
        wfTaskAction.setActionTime(actionTime);
        wfTaskAction.setComment(comment);
        wfTaskAction.setExtraJson(toActionExtraJson(formDataJson, attachmentsJson));
        wfTaskActionRepository.save(wfTaskAction);
    }

    private String toActionExtraJson(String formDataJson, String attachmentsJson) {
        if (formDataJson == null && attachmentsJson == null) {
            return null;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        if (formDataJson != null) {
            extra.put("formDataJson", formDataJson);
        }
        if (attachmentsJson != null) {
            extra.put("attachmentsJson", attachmentsJson);
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON 序列化失败", HttpStatus.BAD_REQUEST);
        }
    }
}
