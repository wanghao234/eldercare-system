package com.wanghao.eldercare.eldercaresystem.service.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanDraftDTO;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.AiCarePlanGenerateRequest;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.ai.OpenAiCompatibleAiClient;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiCarePlanServiceImpl implements AiCarePlanService {

    private static final Logger log = LoggerFactory.getLogger(AiCarePlanServiceImpl.class);

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final PermissionService permissionService;
    private final OpenAiCompatibleAiClient aiClient;
    private final ObjectMapper objectMapper;

    public AiCarePlanServiceImpl(UserRepository userRepository,
                                 ElderProfileRepository elderProfileRepository,
                                 PermissionService permissionService,
                                 OpenAiCompatibleAiClient aiClient,
                                 ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.permissionService = permissionService;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AiCarePlanDraftDTO generateDraft(CurrentUser currentUser, AiCarePlanGenerateRequest request) {
        validateRequest(request);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        User elder = userRepository.findByUserIdAndDeletedAtIsNull(request.getElderId())
                .orElseThrow(() -> new NotFoundException("老人不存在"));
        ElderProfileEntity profile = elderProfileRepository.findById(request.getElderId()).orElse(null);

        try {
            String aiContent = aiClient.chat(buildSystemPrompt(), buildUserPrompt(elder, profile, request));
            String cleaned = stripCodeFence(aiContent);
            return normalizeDraft(parseDraft(cleaned), profile);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("AI护理计划草稿解析失败: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回结果解析失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("AI护理计划草稿生成被中断: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI护理计划生成失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            log.warn("AI护理计划草稿生成失败: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI护理计划生成失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRequest(AiCarePlanGenerateRequest request) {
        if (request.getStartDate() == null && request.getEndDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择护理计划开始日期和结束日期", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择护理计划开始日期", HttpStatus.BAD_REQUEST);
        }
        if (request.getEndDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择护理计划结束日期", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "护理计划结束日期不能早于开始日期", HttpStatus.BAD_REQUEST);
        }
    }

    private String buildSystemPrompt() {
        return "你是养老院护理计划辅助生成助手。"
                + "你的任务是根据老人综合档案信息，生成结构化护理计划草稿。"
                + "只生成护理照护建议，不做疾病诊断，不提供治疗方案，不开具药物。"
                + "必须返回纯 JSON 对象，不要 Markdown，不要解释文字，不要代码块。"
                + "返回字段必须且只能包含："
                + "careLevel、healthAssessment、nursingProblem、riskTags、nursingGoal、dailyCare、dietPlan、medicationCare、healthMonitoring、rehabilitationActivity、psychologicalCare、safetyPrecaution、executionFrequency、evaluation、aiGenerated。"
                + "字段要求："
                + "1. careLevel：优先使用档案中的护理等级；"
                + "2. healthAssessment：总结当前健康与照护关注点；"
                + "3. nursingProblem：概括护理问题；"
                + "4. riskTags：多个风险标签用逗号分隔；"
                + "5. nursingGoal：描述护理目标；"
                + "6. dailyCare：生成生活护理建议；"
                + "7. dietPlan：结合饮食禁忌生成饮食护理建议；"
                + "8. medicationCare：第一版不要读取真实用药计划，只生成通用提醒文字；"
                + "9. healthMonitoring：生成监测建议；"
                + "10. rehabilitationActivity：生成康复或活动建议；"
                + "11. psychologicalCare：生成心理关怀建议；"
                + "12. safetyPrecaution：生成安全防护建议；"
                + "13. executionFrequency：给出执行频率，例如“每日两次”“每周评估一次”；"
                + "14. evaluation：给出阶段性评价建议；"
                + "15. aiGenerated：固定返回 true。"
                + "如果某项信息不足，可结合养老机构通用护理场景给出保守且合理的护理建议，但不要编造具体疾病诊断、检查结果、药品名称或医生结论。";
    }

    private String buildUserPrompt(User elder, ElderProfileEntity profile, AiCarePlanGenerateRequest request) throws IOException {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("currentDate", LocalDate.now().toString());
        input.put("planStartDate", request.getStartDate() == null ? "" : request.getStartDate().toString());
        input.put("planEndDate", request.getEndDate() == null ? "" : request.getEndDate().toString());
        input.put("elderId", elder.getUserId());
        input.put("elderName", safe(elder.getRealName(), elder.getUsername()));
        input.put("gender", profile == null ? "" : safe(profile.getGender()));
        input.put("age", resolveAge(profile));
        input.put("careLevel", profile == null ? "" : safe(profile.getCareLevel()));
        input.put("chronicConditions", profile == null ? "" : safe(profile.getChronicConditions()));
        input.put("allergies", profile == null ? "" : safe(profile.getAllergies()));
        input.put("dietTaboo", profile == null ? "" : safe(profile.getDietTaboo()));
        input.put("medicalNotes", profile == null ? "" : safe(profile.getNotes()));
        input.put("requirement", "请基于以上信息生成护理计划草稿 JSON。");
        return objectMapper.writeValueAsString(input);
    }

    private Integer resolveAge(ElderProfileEntity profile) {
        if (profile == null || profile.getBirthday() == null) {
            return null;
        }
        return Period.between(profile.getBirthday(), LocalDate.now()).getYears();
    }

    private AiCarePlanDraftDTO parseDraft(String cleaned) throws IOException {
        JsonNode root = objectMapper.readTree(cleaned);
        if (root == null || !root.isObject()) {
            throw new IOException("AI 返回的护理计划不是 JSON 对象");
        }
        return objectMapper.treeToValue(root, AiCarePlanDraftDTO.class);
    }

    private AiCarePlanDraftDTO normalizeDraft(AiCarePlanDraftDTO draft, ElderProfileEntity profile) {
        AiCarePlanDraftDTO normalized = draft == null ? new AiCarePlanDraftDTO() : draft;
        normalized.setCareLevel(firstNonBlank(normalized.getCareLevel(), profile == null ? null : profile.getCareLevel()));
        normalized.setHealthAssessment(safe(normalized.getHealthAssessment()));
        normalized.setNursingProblem(safe(normalized.getNursingProblem()));
        normalized.setRiskTags(safe(normalized.getRiskTags()));
        normalized.setNursingGoal(safe(normalized.getNursingGoal()));
        normalized.setDailyCare(safe(normalized.getDailyCare()));
        normalized.setDietPlan(safe(normalized.getDietPlan()));
        normalized.setMedicationCare(safe(normalized.getMedicationCare()));
        normalized.setHealthMonitoring(safe(normalized.getHealthMonitoring()));
        normalized.setRehabilitationActivity(safe(normalized.getRehabilitationActivity()));
        normalized.setPsychologicalCare(safe(normalized.getPsychologicalCare()));
        normalized.setSafetyPrecaution(safe(normalized.getSafetyPrecaution()));
        normalized.setExecutionFrequency(safe(normalized.getExecutionFrequency()));
        normalized.setEvaluation(safe(normalized.getEvaluation()));
        normalized.setAiGenerated(Boolean.TRUE);
        return normalized;
    }

    private String stripCodeFence(String content) {
        String cleaned = content == null ? "" : content.trim();
        if (cleaned.startsWith("```")) {
            int firstLineEnd = cleaned.indexOf('\n');
            if (firstLineEnd >= 0) {
                cleaned = cleaned.substring(firstLineEnd + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : safe(fallback);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : safe(fallback);
    }
}
