package com.wanghao.eldercare.eldercaresystem.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
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
class StatsModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void nurse_can_view_personnel_stats() throws Exception {
        createUser("admin_stats_1", "admin");
        createUser("leader_stats_1", "nurse_leader");
        createUser("nurse_stats_1", "nurse");
        createUser("caregiver_stats_1", "caregiver");
        createUser("doctor_stats_1", "doctor");
        createUser("elder_stats_1", "elder");
        createUser("elder_stats_2", "elder");
        createUser("family_stats_1", "family");

        String nurseToken = loginAndGetToken("nurse_stats_1", "123456");

        mockMvc.perform(get("/api/stats/personnel")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalStaff").value(5))
                .andExpect(jsonPath("$.data.totalElders").value(2))
                .andExpect(jsonPath("$.data.totalFamilies").value(1))
                .andExpect(jsonPath("$.data.staffByRole.admin").value(1))
                .andExpect(jsonPath("$.data.staffByRole.nurse_leader").value(1))
                .andExpect(jsonPath("$.data.staffByRole.nurse").value(1))
                .andExpect(jsonPath("$.data.staffByRole.caregiver").value(1))
                .andExpect(jsonPath("$.data.staffByRole.doctor").value(1));
    }

    @Test
    void family_cannot_view_personnel_stats() throws Exception {
        createUser("family_stats_2", "family");
        String familyToken = loginAndGetToken("family_stats_2", "123456");

        mockMvc.perform(get("/api/stats/personnel")
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
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
