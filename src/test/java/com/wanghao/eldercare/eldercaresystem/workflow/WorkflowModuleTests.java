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
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.*;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.BedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.CameraDeviceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.*;
import com.wanghao.eldercare.eldercaresystem.service.file.FileStorageService;
import com.wanghao.eldercare.eldercaresystem.service.workflow.*;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private CameraDeviceRepository cameraDeviceRepository;

    @Autowired
    private WfTaskActionRepository wfTaskActionRepository;

    @Autowired
    private WfTaskRepository wfTaskRepository;

    @Autowired
    private WfInstanceRepository wfInstanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        wfTaskActionRepository.deleteAll();
        wfTaskRepository.deleteAll();
        wfInstanceRepository.deleteAll();
        cameraDeviceRepository.deleteAll();
        admissionRecordRepository.deleteAll();
        bedRepository.deleteAll();
        elderProfileRepository.deleteAll();
        alarmRepository.deleteAll();
        userRepository.deleteAll();
        cleanupUploadDir();
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
    void create_alarm_with_camera_autofills_location_fields() throws Exception {
        createUser("admin", "admin");
        User elder = createUser("elder-camera", "elder");
        CameraDevice camera = createCameraDevice(elder.getUserId());
        String adminToken = loginAndGetToken("admin", "123456");

        MvcResult result = mockMvc.perform(post("/api/alarms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cameraId":%d,
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"ai_camera",
                                  "confidence":0.90,
                                  "note":"AI视觉识别检测到老人疑似摔倒",
                                  "attachmentsJson":"[]",
                                  "idempotencyKey":"fall-camera-autofill"
                                }
                                """.formatted(camera.getCameraId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        Long alarmId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();

        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow();
        assertThat(alarm.getElderId()).isEqualTo(elder.getUserId());
        assertThat(alarm.getCameraId()).isEqualTo(camera.getCameraId());
        assertThat(alarm.getRoomId()).isEqualTo(camera.getRoomId());
        assertThat(alarm.getBedId()).isEqualTo(camera.getBedId());
        assertThat(alarm.getLocationText()).isEqualTo(camera.getLocationText());
        assertThat(alarm.getMapX()).isEqualByComparingTo(camera.getMapX());
        assertThat(alarm.getMapY()).isEqualByComparingTo(camera.getMapY());
        assertThat(alarm.getConfidence()).isEqualByComparingTo("0.90");
        assertThat(alarm.getSource()).isEqualTo("ai_camera");
        assertThat(alarm.getIdempotencyKey()).isEqualTo("fall-camera-autofill");
    }

    @Test
    void create_alarm_with_same_idempotency_key_returns_existing_alarm() throws Exception {
        createUser("admin", "admin");
        User elder = createUser("elder-idempotent", "elder");
        CameraDevice camera = createCameraDevice(elder.getUserId());
        String adminToken = loginAndGetToken("admin", "123456");

        String payload = """
                {
                  "cameraId":%d,
                  "alarmType":"fall",
                  "severity":"high",
                  "source":"ai_camera",
                  "confidence":0.95,
                  "idempotencyKey":"fall-camera-unique-key"
                }
                """.formatted(camera.getCameraId());

        MvcResult firstResult = mockMvc.perform(post("/api/alarms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/alarms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        long firstAlarmId = objectMapper.readTree(firstResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();
        long secondAlarmId = objectMapper.readTree(secondResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();

        assertThat(secondAlarmId).isEqualTo(firstAlarmId);
        assertThat(alarmRepository.count()).isEqualTo(1);
        assertThat(wfInstanceRepository.count()).isEqualTo(1);
    }

    @Test
    void create_alarm_without_elder_still_creates_workflow_instance() throws Exception {
        User admin = createUser("admin-no-elder", "admin");

        MvcResult result = mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"ai_camera",
                                  "locationText":"公共区域"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        Long alarmId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();

        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow();
        assertThat(alarm.getElderId()).isNull();
        assertThat(alarm.getProcessInstanceId()).isNotNull();

        WfInstance instance = wfInstanceRepository.findById(alarm.getProcessInstanceId()).orElseThrow();
        assertThat(instance.getStartedBy()).isEqualTo(admin.getUserId());
    }

    @Test
    void admin_can_bind_elder_for_unbound_alarm() throws Exception {
        createUser("admin-bind", "admin");
        User elder = createUser("elder-bind", "elder");
        String adminToken = loginAndGetToken("admin-bind", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alarmType":"fall",
                                  "severity":"high",
                                  "source":"ai_camera",
                                  "locationText":"公共区域"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        Long alarmId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("alarmId")
                .asLong();

        mockMvc.perform(post("/api/alarms/{alarmId}/bind-elder", alarmId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "note":"人工确认后绑定老人"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.alarmId").value(alarmId))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()));

        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow();
        assertThat(alarm.getElderId()).isEqualTo(elder.getUserId());
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
                                  "attachmentsJson":["a.png"]
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
    void nurse_can_download_attachment_by_task() throws Exception {
        createUser("adminAttach", "admin");
        User elder = createUser("elderAttach", "elder");
        createUser("nurseAttach", "nurse");

        String adminToken = loginAndGetToken("adminAttach", "123456");
        String nurseToken = loginAndGetToken("nurseAttach", "123456");
        createAlarm(adminToken, elder.getUserId());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "handover.pdf",
                "application/pdf",
                "handover-content".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
        String fileUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("url")
                .asText();

        Long wfTaskId = queryMyFirstTaskId(nurseToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/claim", wfTaskId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", wfTaskId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"complete",
                                  "comment":"完成接单处理并上传附件",
                                  "attachments":["%s"]
                                }
                                """.formatted(fileUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        MvcResult downloadResult = mockMvc.perform(get("/api/workflows/tasks/{wfTaskId}/attachments/download", wfTaskId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .param("url", fileUrl))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadResult.getResponse().getContentAsByteArray())
                .isEqualTo("handover-content".getBytes(StandardCharsets.UTF_8));
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

    @Test
    void nurse_leader_can_download_filled_contract_template_for_fourth_stage() throws Exception {
        createUser("adminContract", "admin");
        createUser("leaderContract", "nurse_leader");
        createUser("doctorContract", "doctor");
        User nurse = createUser("nurseContract", "nurse");
        User elder = createUser("elderContract", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminContract", "123456");
        String leaderToken = loginAndGetToken("leaderContract", "123456");
        String doctorToken = loginAndGetToken("doctorContract", "123456");

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
                .andExpect(status().isOk());

        Long healthTaskId = queryMyFirstTaskId(doctorToken);
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
                                    "chronicConditions":"高血压",
                                    "dietTaboo":"低糖饮食",
                                    "careLevel":"L2",
                                    "notes":"需日常巡视"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        Long bedReserveTaskId = queryMyFirstTaskId(loginAndGetToken("nurseContract", "123456"));
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", bedReserveTaskId)
                        .header("Authorization", "Bearer " + loginAndGetToken("nurseContract", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"确认床位"
                                }
                                """))
                .andExpect(status().isOk());

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        Long contractTaskId = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "contract_deposit_confirm").orElseThrow().getWfTaskId();

        MvcResult result = mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/contract-template", contractTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formData":{
                                    "contractNo":"HT-2030-0008",
                                    "packageName":"高护套餐A",
                                    "depositAmount":5000.00,
                                    "monthlyFee":7800.00,
                                    "partyBName":"王家属",
                                    "partyBPhone":"13900001111",
                                    "partyBIdNumber":"310101199001010011",
                                    "partyBRelation":"儿子",
                                    "signLocation":"上海黄浦院区"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        byte[] docxBytes = result.getResponse().getContentAsByteArray();
        String documentXml = unzipDocumentXml(docxBytes);
        assertThat(result.getResponse().getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("elderContract%E9%A2%90%E5%85%BB%E4%BA%91%E7%AB%AF%E5%85%BB%E8%80%81%E9%99%A2%E6%8A%A4%E7%90%86%E6%9C%8D%E5%8A%A1%E5%90%88%E5%90%8C.docx");
        assertThat(documentXml).contains("合同编号：HT-2030-0008");
        assertThat(documentXml).contains("签署地点：上海黄浦院区");
        assertThat(documentXml).contains("乙方：王家属");
        assertThat(documentXml).contains("身份证号：310101199001010011");
        assertThat(documentXml).contains("联系电话：13900001111");
        assertThat(documentXml).contains("统一社会信用代码");
        assertThat(documentXml).contains("91310101MA1ELDER01");
        assertThat(documentXml).contains("上海市黄浦区颐养云端养老院 1 号楼");
        assertThat(documentXml).contains("400-820-5678");
        assertThat(documentXml).contains("1. 房间号：________________；床位号：B-1。");
        assertThat(documentXml).contains("经甲方初步评估，入住老人护理等级暂定为：L2。");
        assertThat(documentXml).contains("经办人：leaderContract");
        assertThat(documentXml).contains("联系电话：13800000000");
        assertThat(documentXml).contains("￥5000 元");
    }

    @Test
    void nurse_leader_can_import_contract_and_update_admission_record() throws Exception {
        createUser("adminImport", "admin");
        createUser("leaderImport", "nurse_leader");
        createUser("doctorImport", "doctor");
        User nurse = createUser("nurseImport", "nurse");
        User elder = createUser("elderImport", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminImport", "123456");
        String leaderToken = loginAndGetToken("leaderImport", "123456");
        String doctorToken = loginAndGetToken("doctorImport", "123456");
        String nurseToken = loginAndGetToken("nurseImport", "123456");

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
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());

        Long bedReserveTaskId = queryMyFirstTaskId(nurseToken);
        mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/complete", bedReserveTaskId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment":"确认床位"
                                }
                                """))
                .andExpect(status().isOk());

        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();
        Long contractTaskId = wfTaskRepository.findFirstByInstanceIdAndNodeKeyOrderByCreatedAtDesc(
                admission.getProcessInstanceId(), "contract_deposit_confirm").orElseThrow().getWfTaskId();

        MvcResult downloadResult = mockMvc.perform(post("/api/workflows/tasks/{wfTaskId}/contract-template", contractTaskId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formData":{
                                    "contractNo":"HT-2031-0012",
                                    "depositAmount":6000.00,
                                    "partyBName":"王家属",
                                    "partyBPhone":"13900001111",
                                    "partyBIdNumber":"310101199001010011"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockMultipartFile contractFile = new MockMultipartFile(
                "file",
                "导入合同.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                downloadResult.getResponse().getContentAsByteArray()
        );

        MvcResult importResult = mockMvc.perform(multipart("/api/workflows/tasks/{wfTaskId}/contract-import", contractTaskId)
                        .file(contractFile)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.admissionId").value(admissionId))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()))
                .andExpect(jsonPath("$.data.elderIdNumber").value("310101199001010011"))
                .andExpect(jsonPath("$.data.contractNo").value("HT-2031-0012"))
                .andExpect(jsonPath("$.data.depositAmount").value(6000.00))
                .andExpect(jsonPath("$.data.contractFileUrl").value(org.hamcrest.Matchers.containsString("/uploads/")))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(importResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String fileUrl = responseJson.path("data").path("contractFileUrl").asText();
        String storedFileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path storedPath = fileStorageService.getStorageAbsolutePath().resolve(storedFileName);
        assertThat(Files.exists(storedPath)).isTrue();

        AdmissionRecord updatedAdmission = admissionRecordRepository.findById(admissionId).orElseThrow();
        assertThat(updatedAdmission.getContractNo()).isEqualTo("HT-2031-0012");
        assertThat(updatedAdmission.getDepositAmount()).isEqualByComparingTo("6000.00");
        assertThat(updatedAdmission.getContractFileUrl()).isEqualTo(fileUrl);
        assertThat(elderProfileRepository.findById(elder.getUserId()).orElseThrow().getIdNumber())
                .isEqualTo("310101199001010011");

        MvcResult downloadUploadedResult = mockMvc.perform(get("/api/workflows/tasks/{wfTaskId}/contract-file", contractTaskId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadUploadedResult.getResponse().getContentAsByteArray())
                .isEqualTo(downloadResult.getResponse().getContentAsByteArray());
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

    private CameraDevice createCameraDevice(Long elderId) {
        CameraDevice camera = new CameraDevice();
        camera.setCameraName("电脑摄像头模拟设备");
        camera.setCameraCode("CAMERA-" + elderId);
        camera.setCameraType("webcam");
        camera.setElderId(elderId);
        camera.setRoomId(101L);
        camera.setBedId(1L);
        camera.setLocationText("电脑摄像头模拟区域");
        camera.setMapX(new BigDecimal("320.00"));
        camera.setMapY(new BigDecimal("180.00"));
        camera.setStatus("online");
        camera.setCreatedAt(LocalDateTime.now());
        camera.setUpdatedAt(LocalDateTime.now());
        return cameraDeviceRepository.save(camera);
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

    private void cleanupUploadDir() throws IOException {
        Path dir = fileStorageService.getStorageAbsolutePath();
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
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

    private String unzipDocumentXml(byte[] docxBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("word/document.xml not found");
    }
}
