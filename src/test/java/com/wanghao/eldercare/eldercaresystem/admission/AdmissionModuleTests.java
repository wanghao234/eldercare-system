package com.wanghao.eldercare.eldercaresystem.admission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.admission.*;
import com.wanghao.eldercare.eldercaresystem.dto.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityBed;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityBedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfInstanceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskActionRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskRepository;
import com.wanghao.eldercare.eldercaresystem.service.admission.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdmissionModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AdmissionRecordRepository admissionRecordRepository;

    @Autowired
    private DischargeRecordRepository dischargeRecordRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private FacilityBedRepository facilityBedRepository;

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
        dischargeRecordRepository.deleteAll();
        admissionRecordRepository.deleteAll();
        wfTaskActionRepository.deleteAll();
        wfTaskRepository.deleteAll();
        wfInstanceRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        facilityBedRepository.deleteAll();
        bedRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admission_create_should_set_pending_and_reserve_bed() throws Exception {
        createUser("adminA", "admin");
        User elder = createUser("elderA", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminA", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "status":"active",
                                  "startDate":"2030-01-01"
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        AdmissionRecord admission = admissionRecordRepository.findAll().get(0);
        Bed updated = bedRepository.findById(bed.getBedId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("reserved");
        assertThat(admission.getStatus()).isEqualTo("pending");
        assertThat(admission.getProcessInstanceId()).isNotNull();
    }

    @Test
    void admission_create_should_persist_contract_package() throws Exception {
        createUser("adminA2", "admin");
        User elder = createUser("elderA2", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminA2", "123456");

        MvcResult result = mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "contractNo":"HT-2026-0001",
                                  "packageName":"高护套餐A",
                                  "status":"active",
                                  "startDate":"2030-01-01",
                                  "depositAmount":5000.00
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        Long admissionId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
        AdmissionRecord admission = admissionRecordRepository.findById(admissionId).orElseThrow();

        assertThat(admission.getContractNo()).isEqualTo("HT-2026-0001");
        assertThat(admission.getPackageName()).isEqualTo("高护套餐A");
        assertThat(admission.getProcessInstanceId()).isNotNull();
    }

    @Test
    void admission_create_by_elder_name_and_bed_code_should_success() throws Exception {
        createUser("adminName", "admin");
        User elder = createUser("elderName", "elder");
        Bed bed = createBed("available");
        String adminToken = loginAndGetToken("adminName", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderName":"%s",
                                  "bedCode":"%s",
                                  "startDate":"2030-03-01"
                                }
                                """.formatted(elder.getRealName(), bed.getBedNo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        AdmissionRecord admission = admissionRecordRepository.findAll().get(0);
        Bed updated = bedRepository.findById(bed.getBedId()).orElseThrow();
        assertThat(admission.getElderId()).isEqualTo(elder.getUserId());
        assertThat(admission.getBedId()).isEqualTo(bed.getBedId());
        assertThat(admission.getStatus()).isEqualTo("pending");
        assertThat(updated.getStatus()).isEqualTo("reserved");
    }

    @Test
    void elder_with_unfinished_admission_should_not_create_new_one() throws Exception {
        createUser("adminDup", "admin");
        User elder = createUser("elderDup", "elder");
        Bed bed1 = createBed("available");
        Bed bed2 = createBed("available");
        String adminToken = loginAndGetToken("adminDup", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bedId":%d,"startDate":"2030-03-01"}
                                """.formatted(elder.getUserId(), bed1.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bedId":%d,"startDate":"2030-03-02"}
                                """.formatted(elder.getUserId(), bed2.getBedId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void two_elders_should_not_share_same_bed() throws Exception {
        createUser("adminSameBed", "admin");
        User elderA = createUser("elderSameBedA", "elder");
        User elderB = createUser("elderSameBedB", "elder");
        Bed bed = createBed("available");
        String adminToken = loginAndGetToken("adminSameBed", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bedId":%d,"startDate":"2030-03-01"}
                                """.formatted(elderA.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bedId":%d,"startDate":"2030-03-02"}
                                """.formatted(elderB.getUserId(), bed.getBedId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void admission_list_should_include_form_fields() throws Exception {
        createUser("adminA3", "admin");
        User elder = createUser("elderA3", "elder");
        Bed bed = createBed("available");
        bindFacilityBedCode(bed.getBedId(), bed.getRoomId(), "A1012");

        String adminToken = loginAndGetToken("adminA3", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "contractNo":"HT-2026-0002",
                                  "packageName":"高护套餐B",
                                  "status":"active",
                                  "startDate":"2030-02-01",
                                  "depositAmount":6666.00
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elder.getUserId()))
                .andExpect(jsonPath("$.data.content[0].elderUsername").value("elderA3"))
                .andExpect(jsonPath("$.data.content[0].elderName").value("elderA3"))
                .andExpect(jsonPath("$.data.content[0].bedId").value(bed.getBedId()))
                .andExpect(jsonPath("$.data.content[0].bedCode").value("A1012"))
                .andExpect(jsonPath("$.data.content[0].contractNo").value("HT-2026-0002"))
                .andExpect(jsonPath("$.data.content[0].packageName").value("高护套餐B"))
                .andExpect(jsonPath("$.data.content[0].processInstanceId").isNumber())
                .andExpect(jsonPath("$.data.content[0].depositAmount").value(6666.00));
    }

    @Test
    void family_can_list_visible_admissions() throws Exception {
        User family = createUser("familyAdmission", "family");
        User elder = createUser("elderVisible", "elder");
        Bed bed = createBed("available");
        createUser("adminAdmission", "admin");
        String adminToken = loginAndGetToken("adminAdmission", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "startDate":"2030-02-01"
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        admissionRecordRepository.findAll().stream().findFirst().orElseThrow();
        createCareTeamAssignment(elder.getUserId(), family.getUserId());
        String familyToken = loginAndGetToken("familyAdmission", "123456");

        mockMvc.perform(get("/api/admissions")
                        .header("Authorization", "Bearer " + familyToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elder.getUserId()));
    }

    @Test
    void doctor_can_list_admissions() throws Exception {
        createUser("doctorAdmission", "doctor");
        User elder = createUser("elderDoctorVisible", "elder");
        Bed bed = createBed("available");
        createUser("adminDoctorAdmission", "admin");
        String adminToken = loginAndGetToken("adminDoctorAdmission", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "startDate":"2030-02-01"
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        String doctorToken = loginAndGetToken("doctorAdmission", "123456");

        mockMvc.perform(get("/api/admissions")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elder.getUserId()));
    }

    @Test
    void discharge_complete_sets_bed_available_and_admission_ended() throws Exception {
        createUser("adminB", "admin");
        User elder = createUser("elderB", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminB", "123456");
        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());
        Long dischargeId = createDischarge(adminToken, admissionId);

        mockMvc.perform(post("/api/discharges/{id}/settlement", dischargeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementAmount":1000,
                                  "refundAmount":200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("settling"));

        mockMvc.perform(post("/api/discharges/{id}/complete", dischargeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualDate":"2030-01-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));

        Bed updatedBed = bedRepository.findById(bed.getBedId()).orElseThrow();
        AdmissionRecord updatedAdmission = admissionRecordRepository.findById(admissionId).orElseThrow();

        assertThat(updatedBed.getStatus()).isEqualTo("available");
        assertThat(updatedAdmission.getStatus()).isEqualTo("ended");
        assertThat(updatedAdmission.getEndDate()).isEqualTo(LocalDate.of(2030, 1, 10));
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

    private Bed createBed(String status) {
        Bed bed = new Bed();
        bed.setBedId(1L + bedRepository.count());
        bed.setRoomId(1L);
        bed.setBedNo("B-" + bed.getBedId());
        bed.setStatus(status);
        return bedRepository.save(bed);
    }

    private void bindFacilityBedCode(Long bedId, Long roomId, String bedCode) {
        FacilityBed facilityBed = new FacilityBed();
        facilityBed.setBedId(bedId);
        facilityBed.setRoomId(roomId);
        facilityBed.setBedCode(bedCode);
        facilityBed.setStatus("available");
        facilityBedRepository.save(facilityBed);
    }

    private void createCareTeamAssignment(Long elderId, Long familyId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        careTeamAssignmentRepository.save(assignment);
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

    private Long createAdmission(String token, Long elderId, Long bedId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "status":"active",
                                  "startDate":"2030-01-01"
                                }
                                """.formatted(elderId, bedId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("id").asLong();
    }

    private Long createDischarge(String token, Long admissionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/discharges")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "admissionId":%d,
                                  "reason":"康复出院"
                                }
                                """.formatted(admissionId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("id").asLong();
    }
}
