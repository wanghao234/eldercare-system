package com.wanghao.eldercare.eldercaresystem.careplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.ai.OpenAiCompatibleAiClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiCarePlanModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private OpenAiCompatibleAiClient openAiCompatibleAiClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void generate_accepts_json_wrapped_by_markdown_and_explanation_text() throws Exception {
        User admin = createUser("adminAiPlan", "admin");
        User elder = createUser("elderAiPlan", "elder");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        when(openAiCompatibleAiClient.chat(anyString(), anyString())).thenReturn("""
                下面是护理计划草稿：
                ```json
                {
                  "careLevel": "二级护理",
                  "healthAssessment": "生命体征总体平稳，需关注慢病管理。",
                  "nursingProblem": "存在慢病照护与跌倒风险。",
                  "riskTags": "跌倒,慢病",
                  "nursingGoal": "维持稳定状态并降低风险。",
                  "dailyCare": "协助起居，观察睡眠与排便。",
                  "dietPlan": "低盐低脂，少量多餐。",
                  "medicationCare": "按时提醒服药，观察不适反应。",
                  "healthMonitoring": "每日监测血压、脉搏。",
                  "rehabilitationActivity": "鼓励轻度步行训练。",
                  "psychologicalCare": "每日沟通，缓解焦虑。",
                  "safetyPrecaution": "加强防滑与夜间巡视。",
                  "executionFrequency": "每日两次",
                  "evaluation": "每周评估一次执行效果。",
                  "aiGenerated": true
                }
                ```
                """);

        mockMvc.perform(post("/api/ai/care-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-06-01"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.careLevel").value("二级护理"))
                .andExpect(jsonPath("$.data.healthMonitoring").value("每日监测血压、脉搏。"))
                .andExpect(jsonPath("$.data.aiGenerated").value(true));
    }

    @Test
    void generate_accepts_json_nested_in_wrapper_object() throws Exception {
        User admin = createUser("adminAiPlan2", "admin");
        User elder = createUser("elderAiPlan2", "elder");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        when(openAiCompatibleAiClient.chat(anyString(), anyString())).thenReturn("""
                {
                  "result": {
                    "careLevel": "三级护理",
                    "healthAssessment": "需要协助完成部分日常活动。",
                    "nursingProblem": "存在活动能力下降。",
                    "riskTags": "活动受限",
                    "nursingGoal": "提升舒适度与日常支持。",
                    "dailyCare": "协助洗漱、翻身与巡视。",
                    "dietPlan": "清淡饮食，注意补水。",
                    "medicationCare": "常规提醒按时服药。",
                    "healthMonitoring": "每日巡查体温和精神状态。",
                    "rehabilitationActivity": "开展被动活动训练。",
                    "psychologicalCare": "耐心陪伴与情绪安抚。",
                    "safetyPrecaution": "预防坠床和跌倒。",
                    "executionFrequency": "每日",
                    "evaluation": "每周复盘照护效果。",
                    "aiGenerated": true
                  }
                }
                """);

        mockMvc.perform(post("/api/ai/care-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-06-01"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.careLevel").value("三级护理"))
                .andExpect(jsonPath("$.data.dailyCare").value("协助洗漱、翻身与巡视。"))
                .andExpect(jsonPath("$.data.aiGenerated").value(true));
    }

    @Test
    void generate_falls_back_to_plain_text_when_ai_does_not_return_json() throws Exception {
        User admin = createUser("adminAiPlan3", "admin");
        User elder = createUser("elderAiPlan3", "elder");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        when(openAiCompatibleAiClient.chat(anyString(), anyString())).thenReturn("""
                护理等级：二级护理
                健康评估：老人近期状态基本平稳，但需持续关注血压波动与睡眠情况。
                生活护理：协助晨晚间洗漱，观察饮食和排泄情况。
                饮食计划：清淡饮食，注意补水，避免刺激性食物。
                健康监测：每日监测血压、心率和精神状态。
                安全防护：加强防跌倒提醒，夜间起身时做好陪护。
                执行频率：每日
                护理评价：每周复盘护理执行情况。
                """);

        mockMvc.perform(post("/api/ai/care-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-06-01"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.careLevel").value("二级护理"))
                .andExpect(jsonPath("$.data.healthAssessment").value("老人近期状态基本平稳，但需持续关注血压波动与睡眠情况。"))
                .andExpect(jsonPath("$.data.dailyCare").value("协助晨晚间洗漱，观察饮食和排泄情况。"))
                .andExpect(jsonPath("$.data.healthMonitoring").value("每日监测血压、心率和精神状态。"))
                .andExpect(jsonPath("$.data.aiGenerated").value(true));
    }

    @Test
    void generate_returns_default_draft_when_ai_request_throws_io_exception() throws Exception {
        User admin = createUser("adminAiPlan4", "admin");
        User elder = createUser("elderAiPlan4", "elder");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        when(openAiCompatibleAiClient.chat(anyString(), anyString())).thenThrow(new IOException("connect timeout"));

        mockMvc.perform(post("/api/ai/care-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-06-01"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.dailyCare").isNotEmpty())
                .andExpect(jsonPath("$.data.healthMonitoring").isNotEmpty())
                .andExpect(jsonPath("$.data.aiGenerated").value(true));
    }

    @Test
    void generate_returns_default_draft_when_ai_request_throws_runtime_exception() throws Exception {
        User admin = createUser("adminAiPlan5", "admin");
        User elder = createUser("elderAiPlan5", "elder");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        when(openAiCompatibleAiClient.chat(anyString(), anyString())).thenThrow(new IllegalStateException("AI HTTP 500"));

        mockMvc.perform(post("/api/ai/care-plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-06-01"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.nursingGoal").isNotEmpty())
                .andExpect(jsonPath("$.data.evaluation").isNotEmpty())
                .andExpect(jsonPath("$.data.aiGenerated").value(true));
    }

    private User createUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus("active");
        user.setRealName(username);
        user.setPhone("13800000000");
        user.setEmail(username + "@test.local");
        user.setAvatarUrl("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("token").asText();
    }
}
