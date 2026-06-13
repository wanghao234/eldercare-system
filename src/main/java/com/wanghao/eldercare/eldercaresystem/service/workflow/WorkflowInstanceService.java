package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.CreateWfInstanceRequest;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.CreateWfInstanceResponse;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfInstance;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfInstanceRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowInstanceService {

    private final RuntimeService runtimeService;
    private final WfInstanceRepository wfInstanceRepository;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final WorkflowTaskSyncService workflowTaskSyncService;
    private final WorkflowDeployService workflowDeployService;

    public WorkflowInstanceService(RuntimeService runtimeService,
                                   WfInstanceRepository wfInstanceRepository,
                                   AdmissionRecordRepository admissionRecordRepository,
                                   WorkflowTaskSyncService workflowTaskSyncService,
                                   WorkflowDeployService workflowDeployService) {
        this.runtimeService = runtimeService;
        this.wfInstanceRepository = wfInstanceRepository;
        this.admissionRecordRepository = admissionRecordRepository;
        this.workflowTaskSyncService = workflowTaskSyncService;
        this.workflowDeployService = workflowDeployService;
    }

    @Transactional
    public CreateWfInstanceResponse start(CurrentUser currentUser, CreateWfInstanceRequest request) {
        return new CreateWfInstanceResponse(start(
                request.getProcessKey(),
                request.getBizType(),
                request.getBizId(),
                currentUser.getUserId(),
                buildStartVariables(request)
        ).getInstanceId());
    }

    private Map<String, Object> buildStartVariables(CreateWfInstanceRequest request) {
        Map<String, Object> variables = new HashMap<>();
        putIfPresent(variables, "elderId", request.getElderId());
        putIfPresent(variables, "familyUserId", request.getFamilyUserId());
        putIfPresent(variables, "nurseUserId", request.getNurseUserId());
        putIfPresent(variables, "doctorUserId", request.getDoctorUserId());
        if ("admission".equalsIgnoreCase(request.getBizType()) && request.getElderId() == null) {
            admissionRecordRepository.findById(request.getBizId())
                    .map(AdmissionRecord::getElderId)
                    .ifPresent(elderId -> variables.put("elderId", elderId));
        }
        variables.putIfAbsent("familyUserId", null);
        return variables;
    }

    private void putIfPresent(Map<String, Object> variables, String key, Object value) {
        if (value != null) {
            variables.put(key, value);
        }
    }

    @Transactional
    public WfInstance start(String processKey,
                            String bizType,
                            Long bizId,
                            Long startedBy,
                            Map<String, Object> variables) {
        Map<String, Object> processVariables = new HashMap<>();
        if (variables != null) {
            processVariables.putAll(variables);
        }
        processVariables.put("bizType", bizType);
        processVariables.put("bizId", bizId);
        processVariables.put("startedBy", startedBy);

        String businessKey = bizType + ":" + bizId;
        ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(processKey, businessKey, processVariables);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "启动 Flowable 流程失败: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        WfInstance instance = new WfInstance();
        instance.setProcessKey(processKey);
        instance.setBizType(bizType);
        instance.setBizId(bizId);
        instance.setStatus("running");
        instance.setStartedBy(startedBy);
        instance.setStartedAt(now);
        instance.setCreatedAt(now);
        instance.setEngineType("flowable");
        instance.setExternalInstanceId(processInstance.getProcessInstanceId());
        WfInstance saved = wfInstanceRepository.save(instance);

        workflowDeployService.syncLatestDefinition(processKey);
        workflowTaskSyncService.syncOpenTasks(saved, startedBy);
        return saved;
    }
}
