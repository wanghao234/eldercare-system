package com.wanghao.eldercare.eldercaresystem.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.workflow.*;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private WfTaskActionRepository wfTaskActionRepository;

    @Autowired
    private WfTaskRepository wfTaskRepository;

    @Autowired
    private WfInstanceRepository wfInstanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        wfTaskActionRepository.deleteAll();
        wfTaskRepository.deleteAll();
        wfInstanceRepository.deleteAll();
        alarmRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_alarm_creates_workflow_instance_and_accept_task() throws Exception {
        createUser("admin", "admin");
        User elder = createUser("elder1", "elder");
        String adminToken = loginAndGetToken("admin", "123456");

        Long alarmId = createAlarm(adminToken, elder.getUserId());

        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow();
        assertThat(alarm.getProcessInstanceId()).isNotNull();

        Long instanceId = alarm.getProcessInstanceId();
        WfInstance instance = wfInstanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getBizType()).isEqualTo("alarm");
        assertThat(instance.getBizId()).isEqualTo(alarmId);
        assertThat(instance.getStatus()).isEqualTo("running");

        List<WfTask> tasks = wfTaskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
        assertThat(tasks).isNotEmpty();
        assertThat(tasks.get(0).getNodeKey()).isEqualTo("accept_alarm");
        assertThat(tasks.get(0).getStatus()).isEqualTo("pending");
        assertThat(tasks.get(0).getCandidateRole()).isEqualTo("nurse");
    }

    @Test
    void nurse_claim_and_complete_workflow_task_success() throws Exception {
        createUser("admin", "admin");
        User elder = createUser("elder1", "elder");
        createUser("nurse1", "nurse");

        String adminToken = loginAndGetToken("admin", "123456");
        String nurseToken = loginAndGetToken("nurse1", "123456");
        Long alarmId = createAlarm(adminToken, elder.getUserId());

        Long wfTaskId = queryMyFirstTaskId(nurseToken);

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/claim", wfTaskId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("claimed"))
                .andExpect(jsonPath("$.data.assigneeId").isNumber());

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", wfTaskId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"complete",
                                  "comment":"完成接单处理",
                                  "formData":{"result":"ok"},
                                  "attachments":["a.png"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        WfTask task = wfTaskRepository.findById(wfTaskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo("completed");

        List<WfTaskAction> actions = wfTaskActionRepository.findByWfTaskIdOrderByActionTimeAsc(wfTaskId);
        assertThat(actions.stream().anyMatch(a -> "claim".equals(a.getAction()))).isTrue();
        assertThat(actions.stream().anyMatch(a -> "complete".equals(a.getAction()))).isTrue();

        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow();
        assertThat(alarm.getProcessInstanceId()).isNotNull();
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

    private Long createAlarm(String token, Long elderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/alarms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"button",
                                  "locationText":"room-101"
                                }
                                """.formatted(elderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();
    }

    private Long queryMyFirstTaskId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/workflows/tasks/my")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "pending,claimed")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("content").get(0).path("wfTaskId").asLong();
    }
}
