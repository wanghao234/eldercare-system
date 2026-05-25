package com.wanghao.eldercare.eldercaresystem.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.activity.*;
import com.wanghao.eldercare.eldercaresystem.dto.activity.*;
import com.wanghao.eldercare.eldercaresystem.entity.activity.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.activity.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.activity.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;
    @Autowired
    private ActivityParticipantRepository participantRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        activityRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nurse_signup_checkIn_cancel_success() throws Exception {
        createUser("admin_activity_1", "admin");
        User nurse = createUser("nurse_activity_1", "nurse");
        User elder = createUser("elder_activity_1", "elder");
        bindNurse(elder.getUserId(), nurse.getUserId());

        String adminToken = loginAndGetToken("admin_activity_1", "123456");
        String nurseToken = loginAndGetToken("nurse_activity_1", "123456");

        Long activityId = createActivity(adminToken, "太极课程");

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("signed"));

        mockMvc.perform(post("/api/activities/{id}/check-in", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("checked_in"));

        mockMvc.perform(post("/api/activities/{id}/cancel", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        ActivityParticipant saved = participantRepository.findByActivityIdAndElderId(activityId, elder.getUserId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("cancelled");
    }

    @Test
    void duplicate_signup_should_return_40001() throws Exception {
        createUser("admin_activity_2", "admin");
        User nurse = createUser("nurse_activity_2", "nurse");
        User elder = createUser("elder_activity_2", "elder");
        bindNurse(elder.getUserId(), nurse.getUserId());

        String adminToken = loginAndGetToken("admin_activity_2", "123456");
        String nurseToken = loginAndGetToken("nurse_activity_2", "123456");
        Long activityId = createActivity(adminToken, "手工课");

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_signup_invisible_elder_should_return_40301() throws Exception {
        createUser("admin_activity_3", "admin");
        createUser("nurse_activity_3", "nurse");
        User elder = createUser("elder_activity_3", "elder");

        String adminToken = loginAndGetToken("admin_activity_3", "123456");
        String nurseToken = loginAndGetToken("nurse_activity_3", "123456");
        Long activityId = createActivity(adminToken, "音乐疗愈");

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder.getUserId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void nurse_signup_batch_without_elderIds_should_signup_all_bound_elders() throws Exception {
        createUser("admin_activity_batch_1", "admin");
        User nurse = createUser("nurse_activity_batch_1", "nurse");
        User elder1 = createUser("elder_activity_batch_1a", "elder");
        User elder2 = createUser("elder_activity_batch_1b", "elder");
        bindNurse(elder1.getUserId(), nurse.getUserId());
        bindNurse(elder2.getUserId(), nurse.getUserId());

        String adminToken = loginAndGetToken("admin_activity_batch_1", "123456");
        String nurseToken = loginAndGetToken("nurse_activity_batch_1", "123456");
        Long activityId = createActivity(adminToken, "合唱活动");

        mockMvc.perform(post("/api/activities/{id}/signup-batch", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failCount").value(0));

        assertThat(participantRepository.findByActivityIdAndElderId(activityId, elder1.getUserId())).isPresent();
        assertThat(participantRepository.findByActivityIdAndElderId(activityId, elder2.getUserId())).isPresent();
    }

    @Test
    void admin_signup_batch_with_elderIds_should_signup_multiple_elders() throws Exception {
        createUser("admin_activity_batch_2", "admin");
        User elder1 = createUser("elder_activity_batch_2a", "elder");
        User elder2 = createUser("elder_activity_batch_2b", "elder");

        String adminToken = loginAndGetToken("admin_activity_batch_2", "123456");
        Long activityId = createActivity(adminToken, "象棋比赛");

        mockMvc.perform(post("/api/activities/{id}/signup-batch", activityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderIds\":[" + elder1.getUserId() + "," + elder2.getUserId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failCount").value(0));

        assertThat(participantRepository.findByActivityIdAndElderId(activityId, elder1.getUserId())).isPresent();
        assertThat(participantRepository.findByActivityIdAndElderId(activityId, elder2.getUserId())).isPresent();
    }

    @Test
    void participant_stats_should_return_counts() throws Exception {
        createUser("admin_activity_stats_1", "admin");
        User nurse = createUser("nurse_activity_stats_1", "nurse");
        User elder1 = createUser("elder_activity_stats_1a", "elder");
        User elder2 = createUser("elder_activity_stats_1b", "elder");
        bindNurse(elder1.getUserId(), nurse.getUserId());
        bindNurse(elder2.getUserId(), nurse.getUserId());

        String adminToken = loginAndGetToken("admin_activity_stats_1", "123456");
        String nurseToken = loginAndGetToken("nurse_activity_stats_1", "123456");
        Long activityId = createActivity(adminToken, "活动统计");

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder1.getUserId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{id}/signup", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder2.getUserId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{id}/check-in", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder1.getUserId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{id}/cancel", activityId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elder2.getUserId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/activities/{id}/participant-stats", activityId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.signedCount").value(0))
                .andExpect(jsonPath("$.data.checkedInCount").value(1))
                .andExpect(jsonPath("$.data.cancelledCount").value(1))
                .andExpect(jsonPath("$.data.participantCount").value(1));
    }

    @Test
    void list_activities_should_support_space_datetime_filter() throws Exception {
        createUser("admin_activity_list_1", "admin");
        String adminToken = loginAndGetToken("admin_activity_list_1", "123456");
        createActivity(adminToken, "过滤测试1");

        mockMvc.perform(get("/api/activities")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("from", "2026-05-01 00:00:00")
                        .param("to", "2026-05-02 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void admin_create_activity_success() throws Exception {
        createUser("admin_activity_4", "admin");
        String adminToken = loginAndGetToken("admin_activity_4", "123456");

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"书法活动",
                                  "description":"  老年书法体验  ",
                                  "activityTime":"2026-04-01T09:30:00",
                                  "location":"  二楼活动室  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.activityId").isNumber())
                .andExpect(jsonPath("$.data.title").value("书法活动"))
                .andExpect(jsonPath("$.data.description").value("老年书法体验"))
                .andExpect(jsonPath("$.data.activityTime").value("2026-04-01T09:30:00"))
                .andExpect(jsonPath("$.data.location").value("二楼活动室"));
    }

    @Test
    void nurse_should_not_create_activity() throws Exception {
        createUser("nurse_activity_4", "nurse");
        String nurseToken = loginAndGetToken("nurse_activity_4", "123456");

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"无权限创建",
                                  "activityTime":"2026-04-01T09:30:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void nurseLeader_update_activity_success() throws Exception {
        createUser("admin_activity_5", "admin");
        createUser("leader_activity_1", "nurse_leader");
        String adminToken = loginAndGetToken("admin_activity_5", "123456");
        String leaderToken = loginAndGetToken("leader_activity_1", "123456");
        Long activityId = createActivity(adminToken, "原始活动");

        mockMvc.perform(put("/api/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"更新后的活动",
                                  "description":"  内容已调整  ",
                                  "activityTime":"2026-05-01T15:00:00",
                                  "location":"  康复训练区  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.activityId").value(activityId))
                .andExpect(jsonPath("$.data.title").value("更新后的活动"))
                .andExpect(jsonPath("$.data.description").value("内容已调整"))
                .andExpect(jsonPath("$.data.activityTime").value("2026-05-01T15:00:00"))
                .andExpect(jsonPath("$.data.location").value("康复训练区"));
    }

    @Test
    void caregiver_should_not_update_activity() throws Exception {
        createUser("admin_activity_6", "admin");
        createUser("caregiver_activity_1", "caregiver");
        String adminToken = loginAndGetToken("admin_activity_6", "123456");
        String caregiverToken = loginAndGetToken("caregiver_activity_1", "123456");
        Long activityId = createActivity(adminToken, "待修改活动");

        mockMvc.perform(put("/api/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + caregiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"越权修改",
                                  "activityTime":"2026-05-01T15:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void admin_delete_activity_success() throws Exception {
        createUser("admin_activity_7", "admin");
        String adminToken = loginAndGetToken("admin_activity_7", "123456");
        Long activityId = createActivity(adminToken, "待删除活动");

        mockMvc.perform(delete("/api/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        assertThat(activityRepository.findById(activityId)).isEmpty();
    }

    @Test
    void family_should_not_delete_activity() throws Exception {
        createUser("admin_activity_8", "admin");
        createUser("family_activity_1", "family");
        String adminToken = loginAndGetToken("admin_activity_8", "123456");
        String familyToken = loginAndGetToken("family_activity_1", "123456");
        Long activityId = createActivity(adminToken, "不可删除活动");

        mockMvc.perform(delete("/api/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private Long createActivity(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"活动描述",
                                  "activityTime":"2026-03-01T10:00:00",
                                  "location":"多功能活动室"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("activityId").asLong();
    }

    private void bindNurse(Long elderId, Long nurseId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(null);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        careTeamAssignmentRepository.save(assignment);
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

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("token").asText();
    }
}
