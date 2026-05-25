package com.wanghao.eldercare.eldercaresystem.controller.activity;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.dto.activity.ActivityAiConfirmBatchRequest;
import com.wanghao.eldercare.eldercaresystem.dto.activity.ActivityAiConfirmBatchResponse;
import com.wanghao.eldercare.eldercaresystem.dto.activity.AiActivityFormVO;
import com.wanghao.eldercare.eldercaresystem.dto.activity.ActivityAiUploadResponse;
import com.wanghao.eldercare.eldercaresystem.service.activity.ActivityService;
import com.wanghao.eldercare.eldercaresystem.service.activity.DeepSeekActivityParseService;
import com.wanghao.eldercare.eldercaresystem.service.speech.SpeechRecognitionService;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/activities/ai")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
        + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
public class ActivityAiController {

    private static final Logger log = LoggerFactory.getLogger(ActivityAiController.class);
    private final DeepSeekActivityParseService deepSeekActivityParseService;
    private final ActivityService activityService;
    private final SpeechRecognitionService speechRecognitionService;

    public ActivityAiController(DeepSeekActivityParseService deepSeekActivityParseService,
                                ActivityService activityService,
                                SpeechRecognitionService speechRecognitionService) {
        this.deepSeekActivityParseService = deepSeekActivityParseService;
        this.activityService = activityService;
        this.speechRecognitionService = speechRecognitionService;
    }

    @PostMapping(value = "/upload-voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ActivityAiUploadResponse> uploadVoice(@RequestPart(value = "file", required = false) MultipartFile file,
                                                             @RequestParam(value = "operatorId", required = false) Integer operatorId,
                                                             @RequestParam(value = "operatorRole", required = false) String operatorRole) {
        validateFile(file);
        Integer effectiveOperatorId = resolveOperatorId(operatorId);
        String effectiveOperatorRole = normalizeOperatorRole(operatorRole);

        log.info("AI语音录入上传音频文件名: {}", file.getOriginalFilename());
        log.info("AI语音录入上传音频文件大小: {} bytes", file.getSize());
        log.info("AI语音录入上传操作人ID: {}", effectiveOperatorId);
        log.info("AI语音录入上传操作人角色: {}", effectiveOperatorRole);

        String originalText = speechRecognitionService.recognize(file);
        List<AiActivityFormVO> activityForms = List.of();
        String message = "AI解析成功";
        try {
            activityForms = deepSeekActivityParseService.parseActivityForms(originalText);
        } catch (Exception ex) {
            log.warn("AI语音录入活动解析失败: {}", ex.getMessage());
            message = "语音识别成功，AI解析失败";
        }

        ActivityAiUploadResponse response = new ActivityAiUploadResponse();
        response.setOriginalText(originalText);
        response.setOperatorId(effectiveOperatorId);
        response.setOperatorRole(effectiveOperatorRole);
        response.setFileName(file.getOriginalFilename());
        response.setFileSize(file.getSize());
        response.setActivityForms(activityForms);
        return ApiResponse.success(message, response);
    }

    @PostMapping("/confirm-batch")
    public ApiResponse<ActivityAiConfirmBatchResponse> confirmBatch(@RequestBody ActivityAiConfirmBatchRequest request) {
        Integer operatorId = resolveOperatorId(request.getOperatorId());
        String operatorRole = normalizeOperatorRole(request.getOperatorRole());
        validateActivityForms(request.getActivityForms());

        int successCount = activityService.confirmBatchActivities(operatorId, operatorRole, request.getActivityForms());
        ActivityAiConfirmBatchResponse response = new ActivityAiConfirmBatchResponse();
        response.setSuccessCount(successCount);
        return ApiResponse.success("批量保存成功", response);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "file 不能为空", HttpStatus.BAD_REQUEST);
        }
    }

    private Integer resolveOperatorId(Integer operatorId) {
        if (operatorId != null) {
            return operatorId;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "operatorId 不能为空", HttpStatus.BAD_REQUEST);
    }

    private String normalizeOperatorRole(String operatorRole) {
        if (operatorRole == null || operatorRole.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "operatorRole 不能为空", HttpStatus.BAD_REQUEST);
        }

        String normalizedRole = operatorRole.trim().toLowerCase(Locale.ROOT);
        if (!"admin".equals(normalizedRole) && !"nurse".equals(normalizedRole)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前角色不支持 AI 语音录入", HttpStatus.BAD_REQUEST);
        }
        return normalizedRole;
    }

    private void validateActivityForms(List<AiActivityFormVO> activityForms) {
        if (activityForms == null || activityForms.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activityForms 不能为空", HttpStatus.BAD_REQUEST);
        }
    }
}
