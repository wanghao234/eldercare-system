package com.wanghao.eldercare.eldercaresystem.rectification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.rectification.*;
import com.wanghao.eldercare.eldercaresystem.dto.rectification.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.rectification.*;
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
class RectificationModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RectificationRepository rectificationRepository;

    @Autowired
    private RectificationActionRepository rectificationActionRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        rectificationActionRepository.deleteAll();
        rectificationRepository.deleteAll();
        alarmRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_create_rectification_returns_200() throws Exception {
        createUser("admin", "admin");
        User owner = createUser("owner1", "nurse");
        String token = loginAndGetToken("admin", "123456");

        mockMvc.perform(post("/api/rectifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceType":"alarm",
                                  "sourceId":1,
                                  "title":"整改任务",
                                  "description":"需要整改",
                                  "level":"major",
                                  "ownerId":%d,
                                  "dueAt":"2030-01-01T00:00:00"
                                }
                                """.formatted(owner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.rectificationId").isNumber());
    }

    @Test
    void non_admin_and_non_owner_view_detail_returns_403() throws Exception {
        createUser("admin", "admin");
        User owner = createUser("owner1", "nurse");
        createUser("nurse2", "nurse");

        String adminToken = loginAndGetToken("admin", "123456");
        Long rectificationId = createRectification(adminToken, owner.getUserId());

        String nurse2Token = loginAndGetToken("nurse2", "123456");
        mockMvc.perform(get("/api/rectifications/{id}", rectificationId)
                        .header("Authorization", "Bearer " + nurse2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void owner_add_action_in_executing_returns_200() throws Exception {
        createUser("admin", "admin");
        User owner = createUser("owner1", "nurse");

        String adminToken = loginAndGetToken("admin", "123456");
        Long rectificationId = createRectification(adminToken, owner.getUserId());

        transition(adminToken, rectificationId, "open", "analyzing");
        transition(adminToken, rectificationId, "analyzing", "planning");
        transition(adminToken, rectificationId, "planning", "executing");

        String ownerToken = loginAndGetToken("owner1", "123456");

        mockMvc.perform(post("/api/rectifications/{id}/actions", rectificationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType":"submit",
                                  "content":"执行中提交证据",
                                  "attachments":["a.png"],
                                  "extraJson":{"k":"v"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.actionType").value("submit"));
    }

    @Test
    void illegal_transition_returns_40001() throws Exception {
        createUser("admin", "admin");
        User owner = createUser("owner1", "nurse");

        String adminToken = loginAndGetToken("admin", "123456");
        Long rectificationId = createRectification(adminToken, owner.getUserId());

        mockMvc.perform(post("/api/rectifications/{id}/transition", rectificationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from":"open",
                                  "to":"closed",
                                  "comment":"非法迁移"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void create_rectification_from_alarm_visible_success_invisible_forbidden() throws Exception {
        createUser("admin", "admin");
        User nurseA = createUser("nurseA", "nurse");
        User nurseB = createUser("nurseB", "nurse");
        User elderA = createUser("elderA", "elder");
        User elderB = createUser("elderB", "elder");

        bind(elderA.getUserId(), nurseA.getUserId(), null);
        bind(elderB.getUserId(), nurseB.getUserId(), null);

        Alarm alarmA = createAlarm(elderA.getUserId(), nurseA.getUserId());
        Alarm alarmB = createAlarm(elderB.getUserId(), nurseB.getUserId());

        String nurseAToken = loginAndGetToken("nurseA", "123456");

        mockMvc.perform(post("/api/alarms/{alarmId}/rectifications", alarmA.getAlarmId())
                        .header("Authorization", "Bearer " + nurseAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.rectificationId").isNumber());

        mockMvc.perform(post("/api/alarms/{alarmId}/rectifications", alarmB.getAlarmId())
                        .header("Authorization", "Bearer " + nurseAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void create_rectification_from_same_alarm_twice_returns_40001() throws Exception {
        createUser("admin", "admin");
        User nurseA = createUser("nurseA", "nurse");
        User elderA = createUser("elderA", "elder");
        bind(elderA.getUserId(), nurseA.getUserId(), null);
        Alarm alarmA = createAlarm(elderA.getUserId(), nurseA.getUserId());

        String nurseAToken = loginAndGetToken("nurseA", "123456");

        mockMvc.perform(post("/api/alarms/{alarmId}/rectifications", alarmA.getAlarmId())
                        .header("Authorization", "Bearer " + nurseAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/alarms/{alarmId}/rectifications", alarmA.getAlarmId())
                        .header("Authorization", "Bearer " + nurseAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
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

    private Long createRectification(String token, Long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rectifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceType":"alarm",
                                  "sourceId":1,
                                  "title":"整改任务",
                                  "description":"需要整改",
                                  "level":"major",
                                  "ownerId":%d,
                                  "dueAt":"2030-01-01T00:00:00"
                                }
                                """.formatted(ownerId)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("rectificationId").asLong();
    }

    private void transition(String token, Long id, String from, String to) throws Exception {
        mockMvc.perform(post("/api/rectifications/{id}/transition", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "from":"%s",
                                  "to":"%s",
                                  "comment":"状态流转"
                                }
                                """.formatted(from, to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
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

    private Alarm createAlarm(Long elderId, Long acceptedBy) {
        Alarm alarm = new Alarm();
        alarm.setElderId(elderId);
        alarm.setAlarmType("fall");
        alarm.setSeverity("critical");
        alarm.setSource("manual");
        alarm.setLocationText("A-101");
        alarm.setStatus("closed");
        alarm.setCreatedAt(LocalDateTime.now().minusHours(1));
        alarm.setAcceptedAt(LocalDateTime.now().minusMinutes(50));
        alarm.setAcceptedBy(acceptedBy);
        alarm.setClosedAt(LocalDateTime.now().minusMinutes(5));
        alarm.setClosedBy(acceptedBy);
        alarm.setCloseReason("处理完成");
        return alarmRepository.save(alarm);
    }
}
