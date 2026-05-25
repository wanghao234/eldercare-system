package com.wanghao.eldercare.eldercaresystem.service.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.dto.activity.AiActivityFormVO;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeepSeekActivityParseServiceImpl implements DeepSeekActivityParseService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekActivityParseServiceImpl.class);

    private final DeepSeekProperties deepSeekProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekActivityParseServiceImpl(DeepSeekProperties deepSeekProperties, ObjectMapper objectMapper) {
        this.deepSeekProperties = deepSeekProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<AiActivityFormVO> parseActivityForms(String originalText) {
        if (!StringUtils.hasText(originalText)) {
            return List.of();
        }
        if (deepSeekProperties.isMockEnabled()) {
            return mockActivityForms();
        }
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }

        try {
            String content = callDeepSeek(originalText);
            String cleaned = stripCodeFence(content);
            return parseActivityFormsFromJson(cleaned);
        } catch (Exception ex) {
            log.warn("DeepSeek 活动解析失败: {}", ex.getMessage());
            throw new IllegalStateException("DeepSeek 活动解析失败", ex);
        }
    }

    private String callDeepSeek(String originalText) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", deepSeekProperties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", originalText)
        ));
        body.put("temperature", 0.1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(deepSeekProperties.getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DeepSeek HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new IllegalStateException("DeepSeek 返回内容为空");
        }
        return contentNode.asText();
    }

    private String buildSystemPrompt() {
        return "你是养老院活动信息录入助手。"
                + "请从护理员或管理员输入的文字中提取活动表单信息，并返回 JSON 数组。"
                + "只返回 JSON 数组，不要返回解释。"
                + "如果一句话中包含多个活动，请拆分成多条活动记录。"
                + "每条活动必须包含以下字段：activityName、activityTime、activityLocation、activityDescription。"
                + "公共时间继承规则：如果一句话中先出现一个时间，后面通过“并且、同时、另外、还、以及、然后、接着、之后、；、，”连接多个活动，且后面的活动没有单独说明时间，则后面的活动继承最近一次出现的时间。"
                + "公共日期继承规则：如果前面说了“今天、明天、昨天”等日期，后面多个活动没有重复日期，则都继承这个日期。"
                + "并列活动拆分规则：遇到“并且、同时、另外、还、以及、然后、接着、之后、；”等连接词时，如果前后都包含地点或活动动作，应拆分成多条活动。"
                + "地点分别归属规则：如果一句话中出现多个地点，每个地点后面对应一个活动，则每条活动使用自己对应的地点，不要把多个地点塞进同一条描述。"
                + "活动名称补全规则：如果只说“组织活动”，activityName 设置为“集体活动”；如果说“做书法活动”“参加书法活动”“组织书法”，统一识别为“书法活动”。"
                + "时间转换规则："
                + "“下午五点”默认转换为今天 17:00:00；"
                + "“上午九点”转换为今天 09:00:00；"
                + "“下午两点”转换为今天 14:00:00；"
                + "“晚上七点”转换为今天 19:00:00；"
                + "如果后面出现新的时间，使用新的时间；如果后面没有新的时间，继承前面最近一次出现的时间。"
                + "字段要求："
                + "1. activityName：提取活动名称，例如康复训练、书法活动、散步、健康讲座。"
                + "2. activityTime：提取活动时间。如果用户说“今天上午九点”，请结合当前日期(" + LocalDate.now() + ")转换为 yyyy-MM-dd HH:mm:ss 格式。"
                + "3. activityLocation：活动地点。如果文字中没有明确地点，返回空字符串。"
                + "4. activityDescription：整理成适合保存到活动描述的完整描述，可以包含老人姓名、参与情况、状态备注等。"
                + "5. 不要编造不存在的信息。"
                + "6. 如果某个活动没有明确时间，activityTime 返回空字符串。"
                + "7. 如果某个活动没有明确地点，activityLocation 返回空字符串。"
                + "8. 如果无法识别活动名称，不要生成该条活动。";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.deepseek.com";
        }
        return baseUrl.endsWith("/") ? baseUrl + "v1" : baseUrl + "/v1";
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

    private List<AiActivityFormVO> parseActivityFormsFromJson(String cleaned) throws IOException {
        JsonNode root = objectMapper.readTree(cleaned);
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isArray()) {
            List<AiActivityFormVO> results = new java.util.ArrayList<>();
            for (JsonNode node : root) {
                AiActivityFormVO form = normalizeActivityForm(objectMapper.treeToValue(node, AiActivityFormVO.class));
                if (StringUtils.hasText(form.getActivityName())) {
                    results.add(form);
                }
            }
            return inheritMissingActivityTime(results);
        }
        if (root.isObject()) {
            AiActivityFormVO form = normalizeActivityForm(objectMapper.treeToValue(root, AiActivityFormVO.class));
            return StringUtils.hasText(form.getActivityName()) ? List.of(form) : List.of();
        }
        return List.of();
    }

    private List<AiActivityFormVO> inheritMissingActivityTime(List<AiActivityFormVO> activityForms) {
        if (activityForms == null || activityForms.isEmpty()) {
            return List.of();
        }
        String latestActivityTime = "";
        for (AiActivityFormVO form : activityForms) {
            if (form == null) {
                continue;
            }
            if (StringUtils.hasText(form.getActivityTime())) {
                latestActivityTime = form.getActivityTime();
            } else if (StringUtils.hasText(latestActivityTime)) {
                form.setActivityTime(latestActivityTime);
            }
        }
        return activityForms;
    }

    private AiActivityFormVO normalizeActivityForm(AiActivityFormVO activityForm) {
        if (activityForm == null) {
            return emptyForm();
        }
        if (activityForm.getActivityName() == null) {
            activityForm.setActivityName("");
        }
        if (activityForm.getActivityTime() == null) {
            activityForm.setActivityTime("");
        }
        if (activityForm.getActivityLocation() == null) {
            activityForm.setActivityLocation("");
        }
        if (activityForm.getActivityDescription() == null) {
            activityForm.setActivityDescription("");
        }
        return activityForm;
    }

    private List<AiActivityFormVO> mockActivityForms() {
        AiActivityFormVO rehab = new AiActivityFormVO();
        rehab.setActivityName("康复训练");
        rehab.setActivityTime(LocalDate.now() + " 09:00:00");
        rehab.setActivityLocation("康复训练室");
        rehab.setActivityDescription("张三老人参加康复训练，状态良好。");

        AiActivityFormVO calligraphy = new AiActivityFormVO();
        calligraphy.setActivityName("书法活动");
        calligraphy.setActivityTime(LocalDate.now() + " 14:00:00");
        calligraphy.setActivityLocation("活动室");
        calligraphy.setActivityDescription("组织老人参加书法活动，大家参与积极。");

        AiActivityFormVO lecture = new AiActivityFormVO();
        lecture.setActivityName("健康讲座");
        lecture.setActivityTime(LocalDate.now() + " 19:00:00");
        lecture.setActivityLocation("多功能厅");
        lecture.setActivityDescription("安排健康讲座。");
        return List.of(rehab, calligraphy, lecture);
    }

    private AiActivityFormVO emptyForm() {
        AiActivityFormVO empty = new AiActivityFormVO();
        empty.setActivityName("");
        empty.setActivityTime("");
        empty.setActivityLocation("");
        empty.setActivityDescription("");
        return empty;
    }
}
