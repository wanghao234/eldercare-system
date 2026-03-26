package com.wanghao.eldercare.eldercaresystem.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.medication.Medication;
import com.wanghao.eldercare.eldercaresystem.entity.task.Task;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.visit.VisitRequest;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.medication.MedicationRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.task.TaskRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.VisitRequestRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
class PermissionAspectTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareTeamAssignmentRepository assignmentRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        visitRequestRepository.deleteAll();
        alarmRepository.deleteAll();
        assignmentRepository.deleteAll();
        medicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void scoped_interfaces_for_unbound_nurse_should_return_40301() throws Exception {
        User admin = createUser("adminPa", "admin");
        User elder = createUser("elderPa", "elder");
        User family = createUser("familyPa", "family");
        User nurseA = createUser("nursePaA", "nurse");
        User nurseB = createUser("nursePaB", "nurse");
        bind(elder.getUserId(), nurseA.getUserId(), family.getUserId());

        Alarm alarm = createAlarm(elder.getUserId());
        VisitRequest visit = createVisit(elder.getUserId(), family.getUserId());
        Task task = createTask(elder.getUserId(), admin.getUserId(), nurseA.getUserId());
        Medication medication = createMedication();

        String nurseBToken = loginAndGetToken("nursePaB", "123456");

        mockMvc.perform(get("/api/elders/{elderId}/profile", elder.getUserId())
                        .header("Authorization", "Bearer " + nurseBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(post("/api/alarms/{alarmId}/accept", alarm.getAlarmId())
                        .header("Authorization", "Bearer " + nurseBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(post("/api/visits/{id}/confirm", visit.getRequestId())
                        .header("Authorization", "Bearer " + nurseBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(post("/api/tasks/{taskId}/start", task.getTaskId())
                        .header("Authorization", "Bearer " + nurseBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + nurseBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":72}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(post("/api/medication-plans")
                        .header("Authorization", "Bearer " + nurseBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "medicationId":%d,
                                  "dosage":"1片",
                                  "frequency":"qd",
                                  "times":["08:00"],
                                  "startDate":"%s"
                                }
                                """.formatted(elder.getUserId(), medication.getMedicationId(), LocalDate.now())))
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

    private void bind(Long elderId, Long nurseId, Long familyId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    private Alarm createAlarm(Long elderId) {
        Alarm alarm = new Alarm();
        alarm.setElderId(elderId);
        alarm.setAlarmType("fall");
        alarm.setSeverity("high");
        alarm.setSource("button");
        alarm.setStatus("created");
        alarm.setCreatedAt(LocalDateTime.now());
        return alarmRepository.save(alarm);
    }

    private VisitRequest createVisit(Long elderId, Long familyId) {
        VisitRequest request = new VisitRequest();
        request.setElderId(elderId);
        request.setFamilyId(familyId);
        request.setRequestType("visit");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return visitRequestRepository.save(request);
    }

    private Task createTask(Long elderId, Long createdBy, Long assignedTo) {
        Task task = new Task();
        task.setElderId(elderId);
        task.setTaskType("care");
        task.setTitle("测试任务");
        task.setDescription("desc");
        task.setPriority("medium");
        task.setStatus("pending");
        task.setAssignedTo(assignedTo);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    private Medication createMedication() {
        Medication medication = new Medication();
        medication.setMedicationName("测试药品");
        medication.setSpec("100mg");
        medication.setUnit("片");
        medication.setDescription("for test");
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
}
