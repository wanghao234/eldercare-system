package com.wanghao.eldercare.eldercaresystem.workflow;

import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.dto.workflow.CompleteWfTaskRequest;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfInstance;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTask;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfInstanceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskActionRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskRepository;
import com.wanghao.eldercare.eldercaresystem.service.workflow.WorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class FlowableAdmissionWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private TaskService flowableTaskService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WfInstanceRepository wfInstanceRepository;

    @Autowired
    private WfTaskRepository wfTaskRepository;

    @Autowired
    private WfTaskActionRepository wfTaskActionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User doctor;
    private User nurse;

    @BeforeEach
    void setUp() {
        wfTaskActionRepository.deleteAll();
        wfTaskRepository.deleteAll();
        wfInstanceRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("flowable-admin", "admin");
        doctor = createUser("flowable-doctor", "doctor");
        nurse = createUser("flowable-nurse", "nurse");
    }

    @Test
    void admission_flowable_process_syncs_wf_projection_until_completed() throws Exception {
        assertThat(actTableExists("ACT_RE_PROCDEF")).isTrue();
        assertThat(actTableExists("ACT_RU_TASK")).isTrue();
        assertThat(repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("admission")
                .latestVersion()
                .singleResult()).isNotNull();

        String adminToken = loginAndGetToken(admin.getUsername(), "123456");
        MvcResult startResult = mockMvc.perform(post("/api/workflows/instances/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "processKey":"admission",
                                  "bizType":"admission",
                                  "bizId":10001,
                                  "elderId":20001,
                                  "familyUserId":30001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.instanceId").isNumber())
                .andReturn();

        Long instanceId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("instanceId")
                .asLong();
        WfInstance instance = wfInstanceRepository.findById(instanceId).orElseThrow();

        assertThat(instance.getExternalInstanceId()).isNotBlank();
        assertThat(instance.getEngineType()).isEqualTo("flowable");
        assertThat(instance.getStatus()).isEqualTo("running");
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .singleResult()
                .getBusinessKey()).isEqualTo("admission:10001");
        assertThat(flowableTaskService.createTaskQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .taskDefinitionKey("bind_nurse")
                .singleResult()).isNotNull();

        WfTask bindNurse = latestTask(instance.getInstanceId(), "bind_nurse");
        assertProjection(bindNurse, "绑定护理员", "admin");

        claimAndComplete(admin, bindNurse, "绑定完成");

        WfTask healthAssessment = latestTask(instance.getInstanceId(), "health_assessment");
        assertProjection(healthAssessment, "健康评估", "doctor");
        claimAndComplete(doctor, healthAssessment, "评估完成");

        WfTask bedConfirm = latestTask(instance.getInstanceId(), "bed_confirm");
        assertProjection(bedConfirm, "床位确认 / 入住安排", "nurse");
        claimAndComplete(nurse, bedConfirm, "床位确认完成");

        WfTask depositContractConfirm = latestTask(instance.getInstanceId(), "deposit_contract_confirm");
        assertProjection(depositContractConfirm, "押金 / 合同确认", "admin");
        claimAndComplete(admin, depositContractConfirm, "押金合同确认完成");

        WfTask admissionConfirm = latestTask(instance.getInstanceId(), "admission_confirm");
        assertProjection(admissionConfirm, "入住确认", "nurse");
        claimAndComplete(nurse, admissionConfirm, "入住确认完成");

        WfInstance completed = wfInstanceRepository.findById(instance.getInstanceId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo("completed");
        assertThat(completed.getEndedAt()).isNotNull();
        assertThat(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getExternalInstanceId())
                .singleResult()
                .getEndTime()).isNotNull();

        List<String> actions = wfTaskActionRepository.findByInstanceIdOrderByActionTimeAsc(instance.getInstanceId())
                .stream()
                .map(action -> action.getAction().toLowerCase())
                .toList();
        assertThat(actions).contains("claim", "complete");

        mockMvc.perform(get("/api/workflows/instances/{instanceId}/diagram", instance.getInstanceId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.bpmnXml").isNotEmpty())
                .andExpect(jsonPath("$.data.activeNodeKeys").isArray())
                .andExpect(jsonPath("$.data.completedNodeKeys[0]").value("bind_nurse"))
                .andExpect(jsonPath("$.data.taskNodes[0].externalTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.actionLogs").isArray());
    }

    private WfTask latestTask(Long instanceId, String nodeKey) {
        return wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(instanceId, nodeKey).orElseThrow();
    }

    private void assertProjection(WfTask task, String taskName, String candidateRole) {
        assertThat(task.getExternalTaskId()).isNotBlank();
        assertThat(task.getTaskName()).isEqualTo(taskName);
        assertThat(task.getCandidateRole()).isEqualTo(candidateRole);
        assertThat(task.getStatus()).isEqualTo("pending");
    }

    private void claimAndComplete(User user, WfTask task, String comment) {
        workflowService.claim(currentUser(user), task.getWfTaskId());
        WfTask claimedTask = wfTaskRepository.findById(task.getWfTaskId()).orElseThrow();
        assertThat(claimedTask.getStatus()).isEqualTo("claimed");
        assertThat(claimedTask.getAssigneeId()).isEqualTo(user.getUserId());
        assertThat(claimedTask.getClaimedAt()).isNotNull();

        CompleteWfTaskRequest request = new CompleteWfTaskRequest();
        request.setComment(comment);
        workflowService.complete(currentUser(user), task.getWfTaskId(), request);
    }

    private boolean actTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }

    private CurrentUser currentUser(User user) {
        return new CurrentUser(user.getUserId(), user.getUsername(), user.getRole());
    }

    private User createUser(String username, String role) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus("active");
        user.setRealName(username);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
