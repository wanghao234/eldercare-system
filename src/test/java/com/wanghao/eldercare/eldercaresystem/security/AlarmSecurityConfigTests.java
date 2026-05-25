package com.wanghao.eldercare.eldercaresystem.security;

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
class AlarmSecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void unauthenticated_post_alarm_is_permitted() throws Exception {
        createUser("admin-security", "admin");
        User elder = createUser("elder-security", "elder");

        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"ai_camera"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.alarmId").isNumber());
    }

    @Test
    void unauthenticated_get_alarm_list_stays_protected() throws Exception {
        mockMvc.perform(get("/api/alarms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("40101"));
    }

    @Test
    void doctor_can_get_alarm_list() throws Exception {
        createUser("doctor-security", "doctor");
        String token = loginAndGetToken("doctor-security", "123456");

        mockMvc.perform(get("/api/alarms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void family_cannot_get_alarm_list() throws Exception {
        createUser("family-security", "family");
        String token = loginAndGetToken("family-security", "123456");

        mockMvc.perform(get("/api/alarms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void doctor_sees_same_alarm_total_as_admin() throws Exception {
        createUser("admin-security-2", "admin");
        User elderA = createUser("elder-security-2a", "elder");
        User elderB = createUser("elder-security-2b", "elder");
        createUser("doctor-security-2", "doctor");

        String adminToken = loginAndGetToken("admin-security-2", "123456");
        String doctorToken = loginAndGetToken("doctor-security-2", "123456");

        createAlarm(elderA.getUserId());
        createAlarm(elderB.getUserId());

        mockMvc.perform(get("/api/alarms")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/alarms")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
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
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int tokenStart = content.indexOf("\"token\":\"");
        int valueStart = tokenStart + "\"token\":\"".length();
        int valueEnd = content.indexOf('"', valueStart);
        return content.substring(valueStart, valueEnd);
    }

    private Long createAlarm(Long elderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"ai_camera"
                                }
                                """.formatted(elderId)))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        int idStart = content.indexOf("\"alarmId\":");
        int valueStart = idStart + "\"alarmId\":".length();
        int valueEnd = content.indexOf('}', valueStart);
        return Long.parseLong(content.substring(valueStart, valueEnd).trim());
    }
}
