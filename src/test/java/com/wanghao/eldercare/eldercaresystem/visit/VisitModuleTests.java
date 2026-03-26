package com.wanghao.eldercare.eldercaresystem.visit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.visit.*;
import com.wanghao.eldercare.eldercaresystem.dto.visit.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.visit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.*;
import com.wanghao.eldercare.eldercaresystem.service.visit.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisitModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void family_create_visit_returns_200() throws Exception {
        User elder = createUser("elderV1", "elder");
        User family = createUser("familyV1", "family");
        bind(elder.getUserId(), null, family.getUserId(), 1);

        String familyToken = loginAndGetToken("familyV1", "123456");

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + familyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.requestId").isNumber());
    }

    @Test
    void other_family_access_returns_403() throws Exception {
        User elder = createUser("elderV2", "elder");
        User familyA = createUser("familyA", "family");
        User familyB = createUser("familyB", "family");
        bind(elder.getUserId(), null, familyA.getUserId(), 1);

        String tokenA = loginAndGetToken("familyA", "123456");
        String tokenB = loginAndGetToken("familyB", "123456");

        Long requestId = createVisitAndGetId(tokenA, elder.getUserId());

        mockMvc.perform(get("/api/visits/{id}", requestId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void nurse_confirm_returns_200() throws Exception {
        User elder = createUser("elderV3", "elder");
        User family = createUser("familyV3", "family");
        User nurse = createUser("nurseV3", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), family.getUserId(), 1);

        String familyToken = loginAndGetToken("familyV3", "123456");
        String nurseToken = loginAndGetToken("nurseV3", "123456");

        Long requestId = createVisitAndGetId(familyToken, elder.getUserId());

        mockMvc.perform(post("/api/visits/{id}/confirm", requestId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"护理确认\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    void approve_checkout_checkin_full_chain_success() throws Exception {
        User elder = createUser("elderV4", "elder");
        User family = createUser("familyV4", "family");
        User nurse = createUser("nurseV4", "nurse");
        createUser("leaderV4", "nurse_leader");
        bind(elder.getUserId(), nurse.getUserId(), family.getUserId(), 1);

        String familyToken = loginAndGetToken("familyV4", "123456");
        String nurseToken = loginAndGetToken("nurseV4", "123456");
        String leaderToken = loginAndGetToken("leaderV4", "123456");

        Long requestId = createVisitAndGetId(familyToken, elder.getUserId());

        mockMvc.perform(post("/api/visits/{id}/confirm", requestId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));

        mockMvc.perform(post("/api/visits/{id}/approve", requestId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        mockMvc.perform(post("/api/visits/{id}/check-out", requestId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"));

        mockMvc.perform(post("/api/visits/{id}/check-in", requestId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));
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

    private void bind(Long elderId, Long nurseId, Long familyId, Integer isActive) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(isActive);
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

    private Long createVisitAndGetId(String token, Long elderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody(elderId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("requestId").asLong();
    }

    private String createRequestBody(Long elderId) {
        return """
                {
                  "elderId":%d,
                  "requestType":"visit",
                  "plannedStartAt":"2030-01-01T10:00:00",
                  "plannedEndAt":"2030-01-01T12:00:00",
                  "destination":"医院花园",
                  "reason":"家庭探视",
                  "companionCount":2,
                  "extraJson":{"vehicle":"wheelchair"}
                }
                """.formatted(elderId);
    }
}
