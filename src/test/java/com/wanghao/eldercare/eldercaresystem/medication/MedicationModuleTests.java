package com.wanghao.eldercare.eldercaresystem.medication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.medication.*;
import com.wanghao.eldercare.eldercaresystem.dto.medication.*;
import com.wanghao.eldercare.eldercaresystem.entity.audit.AuditLog;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.medication.*;
import com.wanghao.eldercare.eldercaresystem.entity.task.Task;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.audit.AuditLogRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.medication.*;
import com.wanghao.eldercare.eldercaresystem.mapper.task.TaskRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.medication.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicationModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;
    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private MedicationPlanRepository medicationPlanRepository;
    @Autowired
    private MedicationAdminRecordRepository medicationAdminRecordRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        medicationAdminRecordRepository.deleteAll();
        medicationPlanRepository.deleteAll();
        medicationRepository.deleteAll();
        taskRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_create_medication_success_and_audit_logged() throws Exception {
        createUser("adminMed0", "admin");
        String adminToken = loginAndGetToken("adminMed0", "123456");

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName":"阿莫西林",
                                  "spec":"500mg*24粒",
                                  "unit":"盒",
                                  "description":"抗感染"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.medicationName").value("阿莫西林"));

        AuditLog latest = auditLogRepository.findAll().stream()
                .filter(log -> "CREATE".equals(log.getAction()) && "medications".equals(log.getEntityType()))
                .max(Comparator.comparing(AuditLog::getCreatedAt))
                .orElse(null);
        assertThat(latest).isNotNull();
    }

    @Test
    void nurse_create_plan_success_for_visible_elder_and_tasks_generated() throws Exception {
        User elder = createUser("elderMed1", "elder");
        User nurse = createUser("nurseMed1", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), null);

        Medication medication = createMedication("阿司匹林");
        String nurseToken = loginAndGetToken("nurseMed1", "123456");

        mockMvc.perform(post("/api/medication-plans")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "dosage":"1片",
                                  "frequency":"bid",
                                  "times":["08:00","20:00"],
                                  "startDate":"%s",
                                  "endDate":"%s"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId(), LocalDate.now(), LocalDate.now().plusDays(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("active"));

        List<Task> tasks = taskRepository.findByRelatedBizTypeAndRelatedBizId("med_plan", medicationPlanRepository.findAll().get(0).getPlanId());
        assertThat(tasks.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void nurse_create_plan_for_invisible_elder_returns_403() throws Exception {
        User elder = createUser("elderMed1b", "elder");
        createUser("nurseMed1b", "nurse");
        Medication medication = createMedication("可见性测试药品");
        String nurseToken = loginAndGetToken("nurseMed1b", "123456");

        mockMvc.perform(post("/api/medication-plans")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "dosage":"1片",
                                  "frequency":"bid",
                                  "times":["08:00","20:00"],
                                  "startDate":"%s",
                                  "endDate":"%s"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId(), LocalDate.now(), LocalDate.now().plusDays(1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void family_create_plan_returns_403() throws Exception {
        User elder = createUser("elderMed2", "elder");
        User family = createUser("familyMed2", "family");
        User nurse = createUser("nurseMed2", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), family.getUserId());

        Medication medication = createMedication("维生素C");
        String familyToken = loginAndGetToken("familyMed2", "123456");

        mockMvc.perform(post("/api/medication-plans")
                        .header("Authorization", "Bearer " + familyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "dosage":"1片",
                                  "frequency":"qd",
                                  "times":["09:00"],
                                  "startDate":"%s"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId(), LocalDate.now())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void nurse_create_medication_record_success() throws Exception {
        User elder = createUser("elderMed3", "elder");
        User nurse = createUser("nurseMed3", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), null);

        Medication medication = createMedication("二甲双胍");
        String nurseToken = loginAndGetToken("nurseMed3", "123456");

        Long planId = createPlanAndGetId(nurseToken, elder.getUserId(), medication.getMedicationId());

        mockMvc.perform(post("/api/medication-records")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "planId":%d,
                                  "administeredTime":"2026-03-01 08:05:00",
                                  "status":"given",
                                  "dosage":"1片",
                                  "note":"按时给药"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId(), planId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("given"));
    }

    @Test
    void nurse_create_medication_record_for_invisible_elder_returns_403() throws Exception {
        User elder = createUser("elderMed3b", "elder");
        createUser("nurseMed3b", "nurse");
        Medication medication = createMedication("记录可见性测试药品");
        String nurseToken = loginAndGetToken("nurseMed3b", "123456");

        mockMvc.perform(post("/api/medication-records")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "administeredTime":"2026-03-01 08:05:00",
                                  "status":"given",
                                  "dosage":"1片"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void plan_status_idempotent_second_transition_returns_40001() throws Exception {
        User elder = createUser("elderMed4", "elder");
        User nurse = createUser("nurseMed4", "nurse");
        bind(elder.getUserId(), nurse.getUserId(), null);

        Medication medication = createMedication("氯化钾");
        String nurseToken = loginAndGetToken("nurseMed4", "123456");

        Long planId = createPlanAndGetId(nurseToken, elder.getUserId(), medication.getMedicationId());

        mockMvc.perform(patch("/api/medication-plans/{planId}/status", planId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"from\":\"active\",\"to\":\"paused\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("paused"));

        mockMvc.perform(patch("/api/medication-plans/{planId}/status", planId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"from\":\"active\",\"to\":\"paused\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_list_records_without_elderId_only_visible_elders() throws Exception {
        User elderVisible = createUser("elderMed5A", "elder");
        User elderInvisible = createUser("elderMed5B", "elder");
        User nurse = createUser("nurseMed5", "nurse");
        User admin = createUser("adminMed5", "admin");
        bind(elderVisible.getUserId(), nurse.getUserId(), null);

        Medication medication = createMedication("列表过滤测试药品");
        String nurseToken = loginAndGetToken("nurseMed5", "123456");
        String adminToken = loginAndGetToken("adminMed5", "123456");

        Long visiblePlanId = createPlanAndGetId(adminToken, elderVisible.getUserId(), medication.getMedicationId());
        Long invisiblePlanId = createPlanAndGetId(adminToken, elderInvisible.getUserId(), medication.getMedicationId());

        createRecord(adminToken, elderVisible.getUserId(), medication.getMedicationId(), visiblePlanId, "given");
        createRecord(adminToken, elderInvisible.getUserId(), medication.getMedicationId(), invisiblePlanId, "missed");

        mockMvc.perform(get("/api/medication-records")
                        .header("Authorization", "Bearer " + nurseToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elderVisible.getUserId()));
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

    private Medication createMedication(String name) {
        Medication medication = new Medication();
        medication.setMedicationName(name);
        medication.setSpec("10mg*30片");
        medication.setUnit("盒");
        medication.setDescription("测试药品");
        medication.setCreatedAt(LocalDateTime.now());
        return medicationRepository.save(medication);
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

    private Long createPlanAndGetId(String token, Long elderId, Long medicationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/medication-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "dosage":"1片",
                                  "frequency":"bid",
                                  "times":["08:00","20:00"],
                                  "startDate":"%s",
                                  "endDate":"%s"
                                }
                                """.formatted(elderId, medicationId, LocalDate.now(), LocalDate.now().plusDays(3))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("planId").asLong();
    }

    private void createRecord(String token, Long elderId, Long medicationId, Long planId, String status) throws Exception {
        mockMvc.perform(post("/api/medication-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "planId":%d,
                                  "administeredTime":"2026-03-01 08:05:00",
                                  "status":"%s",
                                  "dosage":"1片"
                                }
                                """.formatted(elderId, medicationId, planId, status)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
