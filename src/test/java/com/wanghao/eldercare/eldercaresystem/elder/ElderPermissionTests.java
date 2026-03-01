package com.wanghao.eldercare.eldercaresystem.elder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.user.User;
import com.wanghao.eldercare.eldercaresystem.user.UserRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ElderPermissionTests {

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
    void admin_can_access_any_elder_profile() throws Exception {
        createUser("admin", "admin", "active", "123456");
        User elder = createUser("elderA", "elder", "active", "123456");

        String adminToken = loginAndGetToken("admin", "123456");

        mockMvc.perform(get("/api/elders/{elderId}/profile", elder.getUserId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()));
    }

    @Test
    void nurse_access_unbound_elder_returns_403() throws Exception {
        User nurse = createUser("nurse1", "nurse", "active", "123456");
        User boundElder = createUser("elderBound", "elder", "active", "123456");
        User unboundElder = createUser("elderUnbound", "elder", "active", "123456");

        bindNurseAndFamily(boundElder.getUserId(), nurse.getUserId(), null, 1);

        String nurseToken = loginAndGetToken("nurse1", "123456");

        mockMvc.perform(get("/api/elders/{elderId}/profile", unboundElder.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void family_access_bound_elder_returns_200() throws Exception {
        User family = createUser("family1", "family", "active", "123456");
        User elder = createUser("elderA", "elder", "active", "123456");

        bindNurseAndFamily(elder.getUserId(), null, family.getUserId(), 1);

        String familyToken = loginAndGetToken("family1", "123456");

        mockMvc.perform(get("/api/elders/{elderId}/profile", elder.getUserId())
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()));
    }

    private User createUser(String username, String role, String status, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
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

    private void bindNurseAndFamily(Long elderId, Long nurseId, Long familyId, Integer isActive) {
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
}
