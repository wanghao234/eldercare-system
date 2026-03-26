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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

