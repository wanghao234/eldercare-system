package com.wanghao.eldercare.eldercaresystem.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.notification.NotificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessagingModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository assignmentRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        messageRepository.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void family_send_to_non_bound_nurse_returns_403() throws Exception {
        User elder = createUser("elderMsg1", "elder");
        User family = createUser("familyMsg1", "family");
        createUser("nurseMsg1A", "nurse");
        User nurseB = createUser("nurseMsg1B", "nurse");
        bind(elder.getUserId(), family.getUserId(), null);

        String token = loginAndGetToken("familyMsg1", "123456");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"receiverId":%d,"contentType":"text","content":"你好"}
                                """.formatted(elder.getUserId(), nurseB.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void family_send_to_bound_nurse_success_and_message_saved() throws Exception {
        User elder = createUser("elderMsg2", "elder");
        User family = createUser("familyMsg2", "family");
        User nurse = createUser("nurseMsg2", "nurse");
        bind(elder.getUserId(), family.getUserId(), nurse.getUserId());

        String token = loginAndGetToken("familyMsg2", "123456");

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"receiverId":%d,"contentType":"text","content":"你好护士"}
                                """.formatted(elder.getUserId(), nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.receiverId").value(nurse.getUserId()));

        assertThat(messageRepository.findAll()).hasSize(1);
    }

    @Test
    void receiver_mark_read_success() throws Exception {
        User elder = createUser("elderMsg3", "elder");
        User family = createUser("familyMsg3", "family");
        User nurse = createUser("nurseMsg3", "nurse");
        bind(elder.getUserId(), family.getUserId(), nurse.getUserId());

        String familyToken = loginAndGetToken("familyMsg3", "123456");
        String nurseToken = loginAndGetToken("nurseMsg3", "123456");

        Long messageId = sendAndGetMessageId(familyToken, elder.getUserId(), nurse.getUserId(), "请查看");

        mockMvc.perform(post("/api/messages/{id}/read", messageId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Message m = messageRepository.findById(messageId).orElseThrow();
        assertThat(m.getIsRead()).isEqualTo(1);
    }

    @Test
    void notifications_my_contains_message_type() throws Exception {
        User elder = createUser("elderMsg4", "elder");
        User family = createUser("familyMsg4", "family");
        User nurse = createUser("nurseMsg4", "nurse");
        bind(elder.getUserId(), family.getUserId(), nurse.getUserId());

        String familyToken = loginAndGetToken("familyMsg4", "123456");
        String nurseToken = loginAndGetToken("nurseMsg4", "123456");

        sendAndGetMessageId(familyToken, elder.getUserId(), nurse.getUserId(), "消息通知测试");

        mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", "Bearer " + nurseToken)
                        .param("isRead", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.content[0].notifType").value("message"));
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

    private void bind(Long elderId, Long familyId, Long nurseId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setFamilyId(familyId);
        assignment.setNurseId(nurseId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
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

    private Long sendAndGetMessageId(String token, Long elderId, Long receiverId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"receiverId":%d,"contentType":"text","content":"%s"}
                                """.formatted(elderId, receiverId, content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("messageId").asLong();
    }
}
