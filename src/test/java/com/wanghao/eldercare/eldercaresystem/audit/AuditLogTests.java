package com.wanghao.eldercare.eldercaresystem.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.audit.*;
import com.wanghao.eldercare.eldercaresystem.dto.audit.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.audit.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.audit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.audit.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogTests {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AlarmRepository alarmRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        alarmRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void login_success_writes_login_success_audit() throws Exception {
        User admin = createUser("admin", "admin", "active", "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        AuditLog log = findLatestByAction(AuditAction.LOGIN_SUCCESS);
        assertThat(log).isNotNull();
        assertThat(log.getEntityType()).isEqualTo("users");
        assertThat(log.getEntityId()).isEqualTo(admin.getUserId());

        JsonNode detail = objectMapper.readTree(log.getDetailJson());
        assertThat(detail.path("result").asText()).isEqualTo("SUCCESS");
        assertThat(detail.path("errorCode").asText()).isEqualTo("0");
    }

    @Test
    void login_fail_writes_login_fail_audit() throws Exception {
        createUser("admin", "admin", "active", "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("40101"));

        AuditLog log = findLatestByAction(AuditAction.LOGIN_FAIL);
        assertThat(log).isNotNull();
        JsonNode detail = objectMapper.readTree(log.getDetailJson());
        assertThat(detail.path("result").asText()).isEqualTo("FAIL");
        assertThat(detail.path("errorCode").asText()).isEqualTo("40101");
    }

    @Test
    void unauthenticated_create_alarm_is_permitted() throws Exception {
        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":1,
                                  "alarmType":"fall",
                                  "severity":"critical",
                                  "source":"manual"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.alarmId").isNumber());
    }

    @Test
    void unauthenticated_get_alarm_list_still_returns_401() throws Exception {
        mockMvc.perform(get("/api/alarms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("40101"));
    }

    @Test
    void forbidden_elder_profile_writes_403_sensitive_audit() throws Exception {
        User nurse = createUser("nurse1", "nurse", "active", "123456");
        User boundElder = createUser("elderBound", "elder", "active", "123456");
        User unboundElder = createUser("elderUnbound", "elder", "active", "123456");
        bind(boundElder.getUserId(), nurse.getUserId(), null);

        String nurseToken = loginAndGetToken("nurse1", "123456");
        mockMvc.perform(get("/api/elders/{elderId}/profile", unboundElder.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        AuditLog log = findLatestByAction(AuditAction.VIEW_SENSITIVE);
        assertThat(log).isNotNull();
        assertThat(log.getEntityType()).isEqualTo("elder_profile");
        assertThat(log.getEntityId()).isEqualTo(unboundElder.getUserId());
        JsonNode detail = objectMapper.readTree(log.getDetailJson());
        assertThat(detail.path("result").asText()).isEqualTo("FAIL");
        assertThat(detail.path("errorCode").asText()).isEqualTo("40301");
    }

    @Test
    void alarm_accept_success_writes_transition_audit() throws Exception {
        User admin = createUser("admin", "admin", "active", "123456");
        User elder = createUser("elder1", "elder", "active", "123456");
        Alarm alarm = createAlarm(elder.getUserId());

        String adminToken = loginAndGetToken("admin", "123456");
        mockMvc.perform(post("/api/alarms/{alarmId}/accept", alarm.getAlarmId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        AuditLog log = findLatestByAction(AuditAction.TRANSITION);
        assertThat(log).isNotNull();
        assertThat(log.getEntityType()).isEqualTo("alarms");
        assertThat(log.getEntityId()).isEqualTo(alarm.getAlarmId());
        assertThat(log.getUserId()).isEqualTo(admin.getUserId());

        JsonNode detail = objectMapper.readTree(log.getDetailJson());
        assertThat(detail.path("result").asText()).isEqualTo("SUCCESS");
        assertThat(detail.path("fromStatus").asText()).isEqualTo("created");
        assertThat(detail.path("toStatus").asText()).isEqualTo("accepted");
    }

    private User createUser(String username, String role, String status, String plainPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setStatus(status);
        user.setRealName(username);
        user.setPhone("13800000000");
        user.setEmail(username + "@test.local");
        user.setAvatarUrl("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Alarm createAlarm(Long elderId) {
        Alarm alarm = new Alarm();
        alarm.setElderId(elderId);
        alarm.setAlarmType("fall");
        alarm.setSeverity("critical");
        alarm.setSource("manual");
        alarm.setLocationText("A-101");
        alarm.setStatus("created");
        alarm.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        return alarmRepository.save(alarm);
    }

    private void bind(Long elderId, Long nurseId, Long familyId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        careTeamAssignmentRepository.save(assignment);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("token").asText();
    }

    private AuditLog findLatestByAction(String action) {
        return auditLogRepository.findAll().stream()
                .filter(log -> action.equals(log.getAction()))
                .max(Comparator.comparing(AuditLog::getLogId))
                .orElse(null);
    }
}
