package com.wanghao.eldercare.eldercaresystem.qc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.qc.*;
import com.wanghao.eldercare.eldercaresystem.dto.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.Rectification;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.qc.*;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.RectificationRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.qc.*;
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
class QcModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QcAuditRepository qcAuditRepository;

    @Autowired
    private QcAuditItemRepository qcAuditItemRepository;

    @Autowired
    private QcIssueRepository qcIssueRepository;

    @Autowired
    private RectificationRepository rectificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        qcIssueRepository.deleteAll();
        qcAuditItemRepository.deleteAll();
        qcAuditRepository.deleteAll();
        rectificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fail_item_create_issue_auto_rectification() throws Exception {
        User leader = createUser("qcLeader", "nurse_leader");
        User elder = createUser("elderQc", "elder");
        User responsible = createUser("nurseResp", "nurse");

        String token = loginAndGetToken("qcLeader", "123456");

        Long auditId = createAudit(token, elder.getUserId());

        QcAuditItem item = new QcAuditItem();
        item.setAuditId(auditId);
        item.setItemCode("DOC-001");
        item.setItemName("护理文书完整性");
        item.setUpdatedAt(LocalDateTime.now());
        item = qcAuditItemRepository.save(item);

        mockMvc.perform(post("/api/qc/audits/{auditId}/items/{itemId}/check", auditId, item.getItemId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result":"fail",
                                  "issues":"记录不完整",
                                  "evidenceJson":{"img":"a.png"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.result").value("fail"));

        MvcResult issueResult = mockMvc.perform(post("/api/qc/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qcItemId":%d,
                                  "level":"major",
                                  "description":"抽查失败，需整改",
                                  "responsibleId":%d
                                }
                                """.formatted(item.getItemId(), responsible.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("rectifying"))
                .andExpect(jsonPath("$.data.rectificationId").isNumber())
                .andReturn();

        Long issueId = objectMapper.readTree(issueResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("issueId").asLong();

        QcIssue issue = qcIssueRepository.findById(issueId).orElseThrow();
        assertThat(issue.getRectificationId()).isNotNull();
        assertThat(issue.getStatus()).isEqualTo("rectifying");

        Rectification rectification = rectificationRepository.findById(issue.getRectificationId()).orElseThrow();
        assertThat(rectification.getSourceType()).isEqualTo("qc");
        assertThat(rectification.getSourceId()).isEqualTo(issue.getIssueId());
        assertThat(rectification.getTitle()).isEqualTo("质控问题整改");
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
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("token").asText();
    }

    private Long createAudit(String token, Long elderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/qc/audits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elderId\":" + elderId + ",\"title\":\"月度抽查\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
    }
}
