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
import com.wanghao.eldercare.eldercaresystem.entity.audit.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.audit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.audit.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogQueryTests {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_query_audit_logs_success_with_multi_user_data() throws Exception {
        User admin = createUser("admin", "admin");
        User nurse = createUser("nurse1", "nurse");
        seedAudit(admin.getUserId(), "CREATE", "alarms", 11L, "10.0.0.1", "UA-ADMIN");
        seedAudit(nurse.getUserId(), "TRANSITION", "tasks", 22L, "10.0.0.2", "UA-NURSE");

        String adminToken = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[*].ip", hasItem("10.0.0.1")))
                .andExpect(jsonPath("$.data.content[*].userAgent", hasItem("UA-ADMIN")));
    }

    @Test
    void nurse_query_with_other_userId_only_returns_self() throws Exception {
        User admin = createUser("admin", "admin");
        User nurse = createUser("nurse1", "nurse");
        seedAudit(admin.getUserId(), "CREATE", "alarms", 11L, "10.0.0.1", "UA-ADMIN");
        seedAudit(nurse.getUserId(), "TRANSITION", "tasks", 22L, "10.0.0.2", "UA-NURSE");

        String nurseToken = loginAndGetToken("nurse1", "123456");
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + nurseToken)
                        .param("userId", String.valueOf(admin.getUserId()))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].userId").value(nurse.getUserId()));
    }

    @Test
    void family_query_audit_logs_returns_40301() throws Exception {
        createUser("family1", "family");
        String token = loginAndGetToken("family1", "123456");

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void nurse_result_hides_ip_and_user_agent_admin_can_see() throws Exception {
        User admin = createUser("admin", "admin");
        User nurse = createUser("nurse1", "nurse");
        seedAudit(nurse.getUserId(), "CREATE", "alarms", 33L, "10.10.10.10", "UA-TEST");
        seedAudit(admin.getUserId(), "CREATE", "alarms", 44L, "10.10.10.11", "UA-ADMIN");

        String nurseToken = loginAndGetToken("nurse1", "123456");
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ip").isEmpty())
                .andExpect(jsonPath("$.data.content[0].userAgent").isEmpty());

        String adminToken = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("userId", String.valueOf(nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ip").value("10.10.10.10"))
                .andExpect(jsonPath("$.data.content[0].userAgent").value("UA-TEST"));
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

    private void seedAudit(Long userId, String action, String entityType, Long entityId, String ip, String ua) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setIp(ip);
        log.setUserAgent(ua);
        log.setCreatedAt(LocalDateTime.now());
        log.setDetailJson("{\"traceId\":\"t1\",\"endpoint\":\"GET /x\",\"result\":\"SUCCESS\",\"errorCode\":\"0\",\"request\":{\"id\":1,\"password\":\"x\"}}");
        auditLogRepository.save(log);
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
}
