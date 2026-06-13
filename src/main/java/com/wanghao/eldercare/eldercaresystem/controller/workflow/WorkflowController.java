package com.wanghao.eldercare.eldercaresystem.controller.workflow;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_DOCTOR,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final CurrentUserUtils currentUserUtils;

    public WorkflowController(WorkflowService workflowService, CurrentUserUtils currentUserUtils) {
        this.workflowService = workflowService;
        this.currentUserUtils = currentUserUtils;
    }

    @PostMapping("/instances")
    public ApiResponse<CreateWfInstanceResponse> createInstance(@Valid @RequestBody CreateWfInstanceRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.createInstance(currentUser, request));
    }

    @PostMapping("/instances/start")
    public ApiResponse<CreateWfInstanceResponse> startInstance(@Valid @RequestBody CreateWfInstanceRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.createInstance(currentUser, request));
    }

    @GetMapping("/instances")
    public ApiResponse<WfInstanceDetailDTO> getInstance(@RequestParam String bizType,
                                                         @RequestParam Long bizId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.getInstanceByBiz(currentUser, bizType, bizId));
    }

    @GetMapping("/instances/{instanceId}")
    public ApiResponse<WfInstanceDetailDTO> getInstanceById(@PathVariable Long instanceId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.getInstanceById(currentUser, instanceId));
    }

    @GetMapping("/instances/{instanceId}/tasks")
    public ApiResponse<List<WfTaskDTO>> getInstanceTasks(@PathVariable Long instanceId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.listInstanceTasks(currentUser, instanceId));
    }

    @GetMapping("/instances/{instanceId}/actions")
    public ApiResponse<List<WfTaskActionDTO>> getInstanceActions(@PathVariable Long instanceId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.listInstanceActions(currentUser, instanceId));
    }

    @GetMapping("/instances/{instanceId}/diagram")
    public ApiResponse<WfInstanceDiagramDTO> getInstanceDiagram(@PathVariable Long instanceId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.getInstanceDiagram(currentUser, instanceId));
    }

    @GetMapping("/tasks/my")
    public ApiResponse<WfTaskListResponse> myTasks(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        List<String> statuses = StringUtils.hasText(status)
                ? Arrays.stream(status.split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : null;
        return ApiResponse.ok(workflowService.listMyTasks(currentUser, statuses, page, size));
    }

    @PostMapping("/tasks/{wfTaskId}/claim")
    @Audited(action = AuditAction.CLAIM, entityType = "wf_tasks", entityIdArg = "wfTaskId")
    public ApiResponse<WfTaskDTO> claim(@PathVariable Long wfTaskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.claim(currentUser, wfTaskId));
    }

    @PostMapping("/tasks/{wfTaskId}/complete")
    @Audited(action = AuditAction.COMPLETE, entityType = "wf_tasks", entityIdArg = "wfTaskId")
    public ApiResponse<WfTaskDTO> complete(@PathVariable Long wfTaskId,
                                           @Valid @RequestBody CompleteWfTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.complete(currentUser, wfTaskId, request));
    }

    @PostMapping(value = "/tasks/{wfTaskId}/contract-template",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> downloadContractTemplate(@PathVariable Long wfTaskId,
                                                           @Valid @RequestBody(required = false) CompleteWfTaskRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        byte[] content = workflowService.downloadContractTemplate(
                currentUser,
                wfTaskId,
                request == null ? new CompleteWfTaskRequest() : request
        );
        String fileName = workflowService.buildContractTemplateFileName(wfTaskId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @PostMapping(value = "/contracts/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportAdmissionContractResponse> importAdmissionContract(@RequestPart("file") MultipartFile file,
                                                                                @RequestPart(value = "admissionId", required = false) Long admissionId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.importAdmissionContract(currentUser, file, admissionId));
    }

    @PostMapping(value = "/tasks/{wfTaskId}/contract-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportAdmissionContractResponse> importAdmissionContractByTask(@PathVariable Long wfTaskId,
                                                                                      @RequestPart("file") MultipartFile file) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(workflowService.importAdmissionContractByTask(currentUser, wfTaskId, file));
    }

    @GetMapping(value = "/tasks/{wfTaskId}/contract-file",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> downloadImportedContractByTask(@PathVariable Long wfTaskId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        byte[] content = workflowService.downloadImportedContractByTask(currentUser, wfTaskId);
        String fileName = workflowService.buildContractTemplateFileName(wfTaskId).replace(".docx", "(已上传).docx");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping("/tasks/{wfTaskId}/attachments/download")
    public ResponseEntity<byte[]> downloadTaskAttachment(@PathVariable Long wfTaskId,
                                                         @RequestParam("url") String url) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        byte[] content = workflowService.downloadTaskAttachment(currentUser, wfTaskId, url);
        String fileName = extractFileName(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private String extractFileName(String url) {
        if (!StringUtils.hasText(url)) {
            return "attachment.bin";
        }
        String value = url.trim();
        int queryPos = value.indexOf('?');
        if (queryPos >= 0) {
            value = value.substring(0, queryPos);
        }
        int slashPos = value.lastIndexOf('/');
        if (slashPos < 0 || slashPos == value.length() - 1) {
            return "attachment.bin";
        }
        return value.substring(slashPos + 1);
    }
}
