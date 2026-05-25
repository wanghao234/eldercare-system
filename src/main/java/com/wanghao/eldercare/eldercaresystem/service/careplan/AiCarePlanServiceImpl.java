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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiCarePlanServiceImpl implements AiCarePlanService {

    private static final Logger log = LoggerFactory.getLogger(AiCarePlanServiceImpl.class);
    private static final Set<String> DRAFT_FIELD_NAMES = Set.of(
            "careLevel",
            "healthAssessment",
            "nursingProblem",
            "riskTags",
            "nursingGoal",
            "dailyCare",
            "dietPlan",
            "medicationCare",
            "healthMonitoring",
            "rehabilitationActivity",
            "psychologicalCare",
            "safetyPrecaution",
            "executionFrequency",
            "evaluation",
            "aiGenerated");
    private static final List<String> WRAPPER_FIELD_NAMES = List.of(
            "draft",
            "carePlan",
            "carePlanDraft",
            "plan",
            "result",
            "data",
            "output",
            "content",
            "json");

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
            try {
                return normalizeDraft(parseDraft(cleaned), profile);
            } catch (IOException ex) {
                log.warn("AI护理计划草稿解析失败，已切换文本兜底: {}", ex.getMessage());
                return normalizeDraft(buildFallbackDraftFromText(cleaned, profile), profile);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("AI护理计划草稿生成被中断: {}", ex.getMessage());
            return normalizeDraft(buildFallbackDraftFromText("", profile), profile);
        } catch (IOException ex) {
            log.warn("AI护理计划草稿生成失败，已切换默认草稿: {}", ex.getMessage());
            return normalizeDraft(buildFallbackDraftFromText("", profile), profile);
        } catch (Exception ex) {
            log.warn("AI护理计划草稿生成失败，已切换默认草稿: {}", ex.getMessage());
            return normalizeDraft(buildFallbackDraftFromText("", profile), profile);
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
        JsonNode root = extractDraftJsonNode(cleaned);
        if (root == null || !root.isObject()) {
            throw new IOException("AI 返回的护理计划不是 JSON 对象");
        }
        return objectMapper.treeToValue(root, AiCarePlanDraftDTO.class);
    }

    private JsonNode extractDraftJsonNode(String content) throws IOException {
        String normalized = content == null ? "" : content.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IOException("AI 返回内容为空");
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);
        String extractedJson = extractFirstJsonSegment(normalized);
        if (StringUtils.hasText(extractedJson) && !normalized.equals(extractedJson)) {
            candidates.add(extractedJson);
        }

        IOException lastException = null;
        for (String candidate : new LinkedHashSet<>(candidates)) {
            try {
                JsonNode parsed = objectMapper.readTree(candidate);
                JsonNode draftNode = unwrapDraftNode(parsed);
                if (draftNode != null && draftNode.isObject()) {
                    return draftNode;
                }
                lastException = new IOException("AI 返回的护理计划缺少可识别的 JSON 对象");
            } catch (IOException ex) {
                lastException = ex;
            }
        }
        throw lastException == null ? new IOException("AI 返回结果解析失败") : lastException;
    }

    private JsonNode unwrapDraftNode(JsonNode node) throws IOException {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            String text = stripCodeFence(node.asText());
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return extractDraftJsonNode(text);
        }
        if (node.isObject()) {
            if (looksLikeDraftObject(node)) {
                return node;
            }
            for (String fieldName : WRAPPER_FIELD_NAMES) {
                JsonNode child = node.get(fieldName);
                JsonNode unwrapped = unwrapDraftNode(child);
                if (unwrapped != null) {
                    return unwrapped;
                }
            }
        }
        if (node.isContainerNode()) {
            Deque<JsonNode> queue = new ArrayDeque<>();
            queue.add(node);
            while (!queue.isEmpty()) {
                JsonNode current = queue.removeFirst();
                if (current == null || current.isNull() || current.isMissingNode()) {
                    continue;
                }
                if (current != node && current.isObject() && looksLikeDraftObject(current)) {
                    return current;
                }
                if (current.isTextual()) {
                    JsonNode unwrapped = unwrapDraftNode(current);
                    if (unwrapped != null) {
                        return unwrapped;
                    }
                    continue;
                }
                if (current.isContainerNode()) {
                    current.elements().forEachRemaining(queue::addLast);
                }
            }
        }
        return null;
    }

    private boolean looksLikeDraftObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        int matchedFieldCount = 0;
        for (String fieldName : DRAFT_FIELD_NAMES) {
            if (node.has(fieldName)) {
                matchedFieldCount += 1;
            }
        }
        return matchedFieldCount >= 2;
    }

    private String extractFirstJsonSegment(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        int objectStart = content.indexOf('{');
        int arrayStart = content.indexOf('[');
        int start;
        char openChar;
        char closeChar;
        if (objectStart < 0 && arrayStart < 0) {
            return null;
        }
        if (objectStart >= 0 && (arrayStart < 0 || objectStart < arrayStart)) {
            start = objectStart;
            openChar = '{';
            closeChar = '}';
        } else {
            start = arrayStart;
            openChar = '[';
            closeChar = ']';
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char current = content.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == openChar) {
                depth += 1;
            } else if (current == closeChar) {
                depth -= 1;
                if (depth == 0) {
                    return content.substring(start, i + 1).trim();
                }
            }
        }
        return null;
    }

    private AiCarePlanDraftDTO normalizeDraft(AiCarePlanDraftDTO draft, ElderProfileEntity profile) {
        AiCarePlanDraftDTO normalized = draft == null ? new AiCarePlanDraftDTO() : draft;
        normalized.setCareLevel(firstNonBlank(normalized.getCareLevel(), profile == null ? null : profile.getCareLevel()));
        normalized.setHealthAssessment(firstNonBlank(normalized.getHealthAssessment(), buildDefaultHealthAssessment(profile)));
        normalized.setNursingProblem(firstNonBlank(normalized.getNursingProblem(), "需持续关注老人日常照护需求、慢病管理与安全风险。"));
        normalized.setRiskTags(firstNonBlank(normalized.getRiskTags(), buildDefaultRiskTags(profile)));
        normalized.setNursingGoal(firstNonBlank(normalized.getNursingGoal(), "维持老人当前健康稳定状态，提升日常生活舒适度与安全性。"));
        normalized.setDailyCare(firstNonBlank(normalized.getDailyCare(), "协助完成日常起居照护，观察睡眠、饮食、排泄与精神状态变化。"));
        normalized.setDietPlan(firstNonBlank(normalized.getDietPlan(), buildDefaultDietPlan(profile)));
        normalized.setMedicationCare(firstNonBlank(normalized.getMedicationCare(), "按时提醒并协助用药，观察用药后不适反应并及时反馈。"));
        normalized.setHealthMonitoring(firstNonBlank(normalized.getHealthMonitoring(), "每日关注生命体征、精神状态及基础健康指标变化。"));
        normalized.setRehabilitationActivity(firstNonBlank(normalized.getRehabilitationActivity(), "根据老人耐受情况安排轻度活动或康复训练，循序渐进。"));
        normalized.setPsychologicalCare(firstNonBlank(normalized.getPsychologicalCare(), "加强沟通陪伴，关注情绪变化并给予安抚支持。"));
        normalized.setSafetyPrecaution(firstNonBlank(normalized.getSafetyPrecaution(), "加强防跌倒、防滑与夜间巡视，及时排查环境安全隐患。"));
        normalized.setExecutionFrequency(firstNonBlank(normalized.getExecutionFrequency(), "每日"));
        normalized.setEvaluation(firstNonBlank(normalized.getEvaluation(), "每周评估一次护理执行效果，并根据老人状态及时调整。"));
        normalized.setAiGenerated(Boolean.TRUE);
        return normalized;
    }

    private AiCarePlanDraftDTO buildFallbackDraftFromText(String content, ElderProfileEntity profile) {
        String normalizedText = normalizeLooseText(content);
        Map<String, String> sections = extractLooseSections(normalizedText);
        AiCarePlanDraftDTO draft = new AiCarePlanDraftDTO();
        draft.setCareLevel(firstNonBlank(sections.get("careLevel"), profile == null ? null : profile.getCareLevel()));
        draft.setHealthAssessment(firstNonBlank(sections.get("healthAssessment"), summarizeText(normalizedText)));
        draft.setNursingProblem(sections.get("nursingProblem"));
        draft.setRiskTags(sections.get("riskTags"));
        draft.setNursingGoal(sections.get("nursingGoal"));
        draft.setDailyCare(sections.get("dailyCare"));
        draft.setDietPlan(firstNonBlank(sections.get("dietPlan"), profile == null ? null : profile.getDietTaboo()));
        draft.setMedicationCare(sections.get("medicationCare"));
        draft.setHealthMonitoring(sections.get("healthMonitoring"));
        draft.setRehabilitationActivity(sections.get("rehabilitationActivity"));
        draft.setPsychologicalCare(sections.get("psychologicalCare"));
        draft.setSafetyPrecaution(sections.get("safetyPrecaution"));
        draft.setExecutionFrequency(sections.get("executionFrequency"));
        draft.setEvaluation(sections.get("evaluation"));
        draft.setAiGenerated(Boolean.TRUE);
        return draft;
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

    private Map<String, String> extractLooseSections(String content) {
        Map<String, String> sections = new HashMap<>();
        if (!StringUtils.hasText(content)) {
            return sections;
        }
        for (String line : content.split("\\R+")) {
            String normalizedLine = safe(line);
            if (!StringUtils.hasText(normalizedLine)) {
                continue;
            }
            int separatorIndex = findSectionSeparator(normalizedLine);
            if (separatorIndex < 0) {
                continue;
            }
            String label = normalizedLine.substring(0, separatorIndex).trim();
            String value = normalizedLine.substring(separatorIndex + 1).trim();
            if (!StringUtils.hasText(label) || !StringUtils.hasText(value)) {
                continue;
            }
            String fieldName = mapLooseLabelToField(label);
            if (fieldName != null && !sections.containsKey(fieldName)) {
                sections.put(fieldName, value);
            }
        }
        return sections;
    }

    private int findSectionSeparator(String line) {
        int colonIndex = line.indexOf(':');
        int chineseColonIndex = line.indexOf('：');
        if (colonIndex < 0) {
            return chineseColonIndex;
        }
        if (chineseColonIndex < 0) {
            return colonIndex;
        }
        return Math.min(colonIndex, chineseColonIndex);
    }

    private String mapLooseLabelToField(String label) {
        String normalized = label.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
        return switch (normalized) {
            case "carelevel", "护理等级" -> "careLevel";
            case "healthassessment", "健康评估" -> "healthAssessment";
            case "nursingproblem", "护理问题" -> "nursingProblem";
            case "risktags", "风险标签", "风险提示" -> "riskTags";
            case "nursinggoal", "护理目标" -> "nursingGoal";
            case "dailycare", "生活护理" -> "dailyCare";
            case "dietplan", "饮食计划", "饮食护理" -> "dietPlan";
            case "medicationcare", "用药护理", "用药提醒" -> "medicationCare";
            case "healthmonitoring", "健康监测" -> "healthMonitoring";
            case "rehabilitationactivity", "康复活动", "康复训练" -> "rehabilitationActivity";
            case "psychologicalcare", "心理关怀" -> "psychologicalCare";
            case "safetyprecaution", "安全防护", "安全措施" -> "safetyPrecaution";
            case "executionfrequency", "执行频率" -> "executionFrequency";
            case "evaluation", "护理评价", "评估建议" -> "evaluation";
            default -> null;
        };
    }

    private String normalizeLooseText(String content) {
        return safe(content)
                .replace("\\n", "\n")
                .replace('\r', '\n')
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private String summarizeText(String content) {
        String normalized = safe(content).replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120).trim();
    }

    private String buildDefaultHealthAssessment(ElderProfileEntity profile) {
        if (profile == null) {
            return "需结合老人当前身体状况、日常生活能力与照护风险持续开展综合健康评估。";
        }
        String conditions = safe(profile.getChronicConditions());
        if (StringUtils.hasText(conditions)) {
            return "老人存在%s，需持续关注慢病管理、功能状态与日常照护风险。".formatted(conditions);
        }
        return "需结合老人当前身体状况、日常生活能力与照护风险持续开展综合健康评估。";
    }

    private String buildDefaultRiskTags(ElderProfileEntity profile) {
        if (profile == null) {
            return "日常照护,安全风险";
        }
        String conditions = safe(profile.getChronicConditions());
        if (StringUtils.hasText(conditions)) {
            return "慢病管理,日常照护,安全风险";
        }
        return "日常照护,安全风险";
    }

    private String buildDefaultDietPlan(ElderProfileEntity profile) {
        if (profile == null || !StringUtils.hasText(profile.getDietTaboo())) {
            return "保持清淡均衡饮食，注意补水，根据老人进食情况少量多餐。";
        }
        return "结合饮食禁忌“%s”安排清淡均衡饮食，注意观察进食与消化情况。".formatted(safe(profile.getDietTaboo()));
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
