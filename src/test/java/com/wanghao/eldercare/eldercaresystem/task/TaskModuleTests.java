package com.wanghao.eldercare.eldercaresystem.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.task.*;
import com.wanghao.eldercare.eldercaresystem.dto.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.task.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.task.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.task.*;
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
class TaskModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nurseA_my_tasks_only_assigned_to_me() throws Exception {
        User elder = createUser("elderTask1", "elder");
        User nurseA = createUser("nurseTaskA", "nurse");
        User nurseB = createUser("nurseTaskB", "nurse");
        bind(elder.getUserId(), nurseA.getUserId(), null);
        bind(elder.getUserId(), nurseB.getUserId(), null);

        createTaskEntity(elder.getUserId(), nurseA.getUserId(), nurseA.getUserId(), "pending", "A任务");
        createTaskEntity(elder.getUserId(), nurseB.getUserId(), nurseB.getUserId(), "pending", "B任务");

        String tokenA = loginAndGetToken("nurseTaskA", "123456");
        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].assignedTo").value(nurseA.getUserId()));
    }

    @Test
    void nurseB_complete_not_visible_task_returns_403() throws Exception {
        User elderA = createUser("elderTask2A", "elder");
        User elderB = createUser("elderTask2B", "elder");
        User nurseA = createUser("nurseTask2A", "nurse");
        User nurseB = createUser("nurseTask2B", "nurse");
        bind(elderA.getUserId(), nurseA.getUserId(), null);
        bind(elderB.getUserId(), nurseB.getUserId(), null);

        Task task = createTaskEntity(elderA.getUserId(), nurseA.getUserId(), nurseA.getUserId(), "pending", "仅护士A可见任务");

        String tokenA = loginAndGetToken("nurseTask2A", "123456");
        String tokenB = loginAndGetToken("nurseTask2B", "123456");

        mockMvc.perform(post("/api/tasks/{taskId}/start", task.getTaskId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"));

        mockMvc.perform(post("/api/tasks/{taskId}/complete", task.getTaskId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void start_idempotent_second_time_returns_40001() throws Exception {
        User elder = createUser("elderTask3", "elder");
        User nurse = createUser("nurseTask3", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), null);
        Task task = createTaskEntity(elder.getUserId(), nurse.getUserId(), nurse.getUserId(), "pending", "幂等测试任务");

        String token = loginAndGetToken("nurseTask3", "123456");

        mockMvc.perform(post("/api/tasks/{taskId}/start", task.getTaskId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"));

        mockMvc.perform(post("/api/tasks/{taskId}/start", task.getTaskId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_leader_reassign_success() throws Exception {
        User elder = createUser("elderTask4", "elder");
        User nurseA = createUser("nurseTask4A", "nurse");
        User nurseB = createUser("nurseTask4B", "nurse");
        createUser("leaderTask4", "nurse_leader");
        bind(elder.getUserId(), nurseA.getUserId(), null);
        bind(elder.getUserId(), nurseB.getUserId(), null);

        Task task = createTaskEntity(elder.getUserId(), nurseA.getUserId(), nurseA.getUserId(), "pending", "改派任务");
        String leaderToken = loginAndGetToken("leaderTask4", "123456");

        mockMvc.perform(post("/api/tasks/{taskId}/reassign", task.getTaskId())
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedTo\":" + nurseB.getUserId() + ",\"comment\":\"换班改派\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.assignedTo").value(nurseB.getUserId()));
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

    private Task createTaskEntity(Long elderId, Long assignedTo, Long createdBy, String status, String title) {
        Task task = new Task();
        task.setElderId(elderId);
        task.setTaskType("rectification");
        task.setTitle(title);
        task.setDescription("desc");
        task.setPriority("high");
        task.setStatus(status);
        task.setDueAt(LocalDateTime.now().plusHours(2));
        task.setAssignedTo(assignedTo);
        task.setCreatedBy(createdBy);
        task.setRelatedBizType("rectification");
        task.setRelatedBizId(1L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
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
