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
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Bed;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private AdmissionRecordRepository admissionRecordRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

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
        admissionRecordRepository.deleteAll();
        bedRepository.deleteAll();
        elderProfileRepository.deleteAll();
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

    @Test
    void doctor_can_handle_admission_health_assessment_and_update_elder_profile() throws Exception {
        createUser("adminA", "admin");
        createUser("leaderA", "nurse_leader");
        createUser("doctorA", "doctor");
        User nurse = createUser("nurseA", "nurse");
        User elder = createUser("elderA", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminA", "123456");
        String leaderToken = loginAndGetToken("leaderA", "123456");
        String doctorToken = loginAndGetToken("doctorA", "123456");

        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        Long assignTaskId = queryMyFirstTaskId(leaderToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", assignTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"绑定护理员",
                                  "formData":{"nurseId":%d}
                                }
                                """.formatted(nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeKey").value("assign_nurse"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        Long healthTaskId = queryMyFirstTaskId(doctorToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/claim", healthTaskId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("claimed"));

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", healthTaskId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"完成健康评估",
                                  "formData":{
                                    "gender":"female",
                                    "birthday":"1940-01-02",
                                    "address":"上海市黄浦区养老路1号",
                                    "emergencyContactName":"家属张三",
                                    "emergencyContactPhone":"13811112222",
                                    "allergies":"青霉素",
                                    "chronicConditions":"高血压,糖尿病",
                                    "dietTaboo":"低糖饮食",
                                    "careLevel":"L3",
                                    "notes":"入住前需重点关注血压波动"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeKey").value("health_assess"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        ElderProfileEntity profile = elderProfileRepository.findById(elder.getUserId()).orElseThrow();
        assertThat(profile.getGender()).isEqualTo("female");
        assertThat(profile.getBirthday()).isEqualTo(LocalDate.of(1940, 1, 2));
        assertThat(profile.getAddress()).isEqualTo("上海市黄浦区养老路1号");
        assertThat(profile.getEmergencyContactName()).isEqualTo("家属张三");
        assertThat(profile.getEmergencyContactPhone()).isEqualTo("13811112222");
        assertThat(profile.getAllergies()).isEqualTo("青霉素");
        assertThat(profile.getChronicConditions()).isEqualTo("高血压,糖尿病");
        assertThat(profile.getDietTaboo()).isEqualTo("低糖饮食");
        assertThat(profile.getCareLevel()).isEqualTo("L3");
        assertThat(profile.getNotes()).isEqualTo("入住前需重点关注血压波动");

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        WfTask nextTask = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "bed_reserve").orElseThrow();
        assertThat(nextTask.getAssigneeId()).isEqualTo(nurse.getUserId());
        assertThat(nextTask.getStatus()).isEqualTo("pending");
    }

    @Test
    void doctor_can_view_admission_workflow_instance() throws Exception {
        createUser("adminInstance", "admin");
        createUser("doctorInstance", "doctor");
        User elder = createUser("elderInstance", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminInstance", "123456");
        String doctorToken = loginAndGetToken("doctorInstance", "123456");
        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        mockMvc.perform(get("/api/workflows/instances")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("bizType", "admission")
                        .param("bizId", String.valueOf(admissionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.bizType").value("admission"))
                .andExpect(jsonPath("$.data.bizId").value(admissionId));
    }

    @Test
    void nurse_leader_can_complete_health_assess_even_if_doctor_claimed() throws Exception {
        createUser("adminCross", "admin");
        createUser("leaderCross", "nurse_leader");
        createUser("doctorCross", "doctor");
        User nurse = createUser("nurseCross", "nurse");
        User elder = createUser("elderCross", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminCross", "123456");
        String leaderToken = loginAndGetToken("leaderCross", "123456");
        String doctorToken = loginAndGetToken("doctorCross", "123456");
        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        Long assignTaskId = queryMyFirstTaskId(leaderToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", assignTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"绑定护理员",
                                  "formData":{"nurseId":%d}
                                }
                                """.formatted(nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Long healthTaskId = queryMyFirstTaskId(doctorToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/claim", healthTaskId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", healthTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"护士长代办健康评估",
                                  "formData":{"careLevel":"L2"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.nodeKey").value("health_assess"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        assertThat(elderProfileRepository.findById(elder.getUserId()).orElseThrow().getCareLevel()).isEqualTo("L2");
        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        assertThat(wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "bed_reserve")).isPresent();
    }

    @Test
    void doctor_can_claim_legacy_health_assess_task_created_for_nurse_leader() throws Exception {
        createUser("adminLegacy", "admin");
        createUser("leaderLegacy", "nurse_leader");
        createUser("doctorLegacy", "doctor");
        User nurse = createUser("nurseLegacy", "nurse");
        User elder = createUser("elderLegacy", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminLegacy", "123456");
        String leaderToken = loginAndGetToken("leaderLegacy", "123456");
        String doctorToken = loginAndGetToken("doctorLegacy", "123456");
        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        Long assignTaskId = queryMyFirstTaskId(leaderToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", assignTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"绑定护理员",
                                  "formData":{"nurseId":%d}
                                }
                                """.formatted(nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        WfTask healthTask = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "health_assess").orElseThrow();
        healthTask.setCandidateRole("nurse_leader");
        wfTaskRepository.saveAndFlush(healthTask);

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/claim", healthTask.getWfTaskId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("claimed"))
                .andExpect(jsonPath("$.data.assigneeId").isNumber());
    }

    @Test
    void bound_nurse_can_confirm_reserved_bed_and_advance_workflow() throws Exception {
        createUser("adminReserve", "admin");
        createUser("leaderReserve", "nurse_leader");
        createUser("doctorReserve", "doctor");
        User nurse = createUser("nurseReserve", "nurse");
        User elder = createUser("elderReserve", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminReserve", "123456");
        String leaderToken = loginAndGetToken("leaderReserve", "123456");
        String doctorToken = loginAndGetToken("doctorReserve", "123456");
        String nurseToken = loginAndGetToken("nurseReserve", "123456");

        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        Long assignTaskId = queryMyFirstTaskId(leaderToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", assignTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"绑定护理员",
                                  "formData":{"nurseId":%d}
                                }
                                """.formatted(nurse.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Long healthTaskId = queryMyFirstTaskId(doctorToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", healthTaskId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"完成健康评估",
                                  "formData":{"careLevel":"L2"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Long bedReserveTaskId = queryMyFirstTaskId(nurseToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", bedReserveTaskId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"确认当前已预占床位"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.nodeKey").value("bed_reserve"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        WfTask nextTask = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "contract_deposit_confirm").orElseThrow();
        assertThat(nextTask.getStatus()).isEqualTo("pending");
        assertThat(bedRepository.findById(bed.getBedId()).orElseThrow().getStatus()).isEqualTo("reserved");
    }

    @Test
    void any_bound_care_team_member_can_handle_third_stage() throws Exception {
        createUser("adminTeam", "admin");
        createUser("leaderTeam", "nurse_leader");
        createUser("doctorTeam", "doctor");
        User nurse = createUser("nurseTeam", "nurse");
        User caregiver = createUser("caregiverTeam", "caregiver");
        User elder = createUser("elderTeam", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminTeam", "123456");
        String leaderToken = loginAndGetToken("leaderTeam", "123456");
        String doctorToken = loginAndGetToken("doctorTeam", "123456");
        String nurseToken = loginAndGetToken("nurseTeam", "123456");
        String caregiverToken = loginAndGetToken("caregiverTeam", "123456");

        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());

        Long assignTaskId = queryMyFirstTaskId(leaderToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", assignTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"绑定护理团队",
                                  "formData":{"nurseIds":[%d,%d]}
                                }
                                """.formatted(nurse.getUserId(), caregiver.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Long healthTaskId = queryMyFirstTaskId(doctorToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", healthTaskId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"完成健康评估",
                                  "formData":{"careLevel":"L2"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Long nurseTaskId = queryMyFirstTaskId(nurseToken);
        Long caregiverTaskId = queryMyFirstTaskId(caregiverToken);
        assertThat(nurseTaskId).isEqualTo(caregiverTaskId);

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        WfTask bedReserveTask = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "bed_reserve").orElseThrow();
        assertThat(bedReserveTask.getAssigneeId()).isNull();

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", caregiverTaskId)
                        .header("Authorization", "Bearer " + caregiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"护理团队成员确认床位"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.nodeKey").value("bed_reserve"))
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

    private Long createAdmission(String token, Long elderId, Long bedId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "startDate":"2030-01-01"
                                }
                                """.formatted(elderId, bedId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("id")
                .asLong();
    }

    private Bed createBed(String status) {
        Bed bed = new Bed();
        bed.setBedId(1L + bedRepository.count());
        bed.setRoomId(1L);
        bed.setBedNo("B-" + bed.getBedId());
        bed.setStatus(status);
        return bedRepository.save(bed);
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
