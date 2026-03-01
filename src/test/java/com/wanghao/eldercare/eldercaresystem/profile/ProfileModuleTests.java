package com.wanghao.eldercare.eldercaresystem.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.audit.AuditLog;
import com.wanghao.eldercare.eldercaresystem.audit.AuditLogRepository;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.profile.entity.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.profile.entity.StaffProfileEntity;
import com.wanghao.eldercare.eldercaresystem.profile.repo.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.profile.repo.StaffProfileRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "security.auth.block-family-elder-login=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        careTeamAssignmentRepository.deleteAll();
        elderProfileRepository.deleteAll();
        staffProfileRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nurse_access_visible_elder_profile_returns_200() throws Exception {
        User nurse = createUser("nurseP1", "nurse");
        User elder = createUser("elderP1", "elder");
        bind(elder.getUserId(), nurse.getUserId(), null, 1);
        saveElderProfile(elder.getUserId(), "320101199001011234");

        String nurseToken = loginAndGetToken("nurseP1", "123456");

        mockMvc.perform(get("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()));
    }

    @Test
    void nurse_access_invisible_elder_profile_returns_403() throws Exception {
        createUser("nurseP2", "nurse");
        User elder = createUser("elderP2", "elder");
        String nurseToken = loginAndGetToken("nurseP2", "123456");

        mockMvc.perform(get("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void family_access_bound_elder_profile_hides_id_number() throws Exception {
        User family = createUser("familyP1", "family");
        User elder = createUser("elderP3", "elder");
        bind(elder.getUserId(), null, family.getUserId(), 1);
        saveElderProfile(elder.getUserId(), "320101199001011234");

        String familyToken = loginAndGetToken("familyP1", "123456");

        mockMvc.perform(get("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.idNumber").value(nullValue()));
    }

    @Test
    void admin_put_elder_profile_success_and_audit_logged() throws Exception {
        createUser("adminP1", "admin");
        User elder = createUser("elderP4", "elder");
        String adminToken = loginAndGetToken("adminP1", "123456");

        mockMvc.perform(put("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gender":"male",
                                  "birthday":"1940-01-01",
                                  "idNumber":"320101194001011234",
                                  "careLevel":"L2",
                                  "notes":"updated by admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.careLevel").value("L2"));

        AuditLog latest = auditLogRepository.findAll().stream()
                .filter(log -> "UPDATE".equals(log.getAction()) && "elder_profile".equals(log.getEntityType()))
                .max(Comparator.comparing(AuditLog::getCreatedAt))
                .orElse(null);
        assertThat(latest).isNotNull();
        assertThat(latest.getEntityId()).isEqualTo(elder.getUserId());
    }

    @Test
    void nurse_put_elder_profile_cannot_update_id_number() throws Exception {
        User nurse = createUser("nurseP3", "nurse");
        User elder = createUser("elderP5", "elder");
        bind(elder.getUserId(), nurse.getUserId(), null, 1);
        saveElderProfile(elder.getUserId(), "320101199001011234");
        String nurseToken = loginAndGetToken("nurseP3", "123456");

        mockMvc.perform(put("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idNumber":"999999999999999999",
                                  "careLevel":"L3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careLevel").value("L3"));

        ElderProfileEntity profile = elderProfileRepository.findById(elder.getUserId()).orElseThrow();
        assertThat(profile.getIdNumber()).isEqualTo("320101199001011234");
    }

    @Test
    void nurse_can_get_put_self_staff_profile_but_not_others() throws Exception {
        User nurseA = createUser("nurseP4", "nurse");
        User nurseB = createUser("nurseP5", "nurse");
        saveStaffProfile(nurseA.getUserId());
        String nurseToken = loginAndGetToken("nurseP4", "123456");

        mockMvc.perform(get("/api/profiles/staff/{staffId}", nurseA.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.staffId").value(nurseA.getUserId()));

        mockMvc.perform(get("/api/profiles/staff/{staffId}", nurseB.getUserId())
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(put("/api/profiles/staff/{staffId}", nurseA.getUserId())
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone":"13900001111",
                                  "skills":["压疮护理","康复训练"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.phone").value("13900001111"));
    }

    @Test
    void admin_put_elder_profile_with_alias_fields_success() throws Exception {
        createUser("adminP2", "admin");
        User elder = createUser("elderP6", "elder");
        String adminToken = loginAndGetToken("adminP2", "123456");

                mockMvc.perform(put("/api/profiles/elders/{elderId}", elder.getUserId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"王秀兰",
                                  "birth_date":"1942-02-02",
                                  "id_number":"320101194202021234",
                                  "emergency_contact_phone":"13912345678",
                                  "care_level":"L3",
                                  "avatar_url":"https://example.com/a.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.realName").value("王秀兰"))
                .andExpect(jsonPath("$.data.careLevel").value("L3"))
                .andExpect(jsonPath("$.data.idNumber").value("320101194202021234"));
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

    private void bind(Long elderId, Long nurseId, Long familyId, int isActive) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(isActive);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        careTeamAssignmentRepository.save(assignment);
    }

    private void saveElderProfile(Long elderId, String idNumber) {
        ElderProfileEntity profile = new ElderProfileEntity();
        profile.setElderId(elderId);
        profile.setGender("male");
        profile.setBirthday(LocalDate.of(1940, 1, 1));
        profile.setIdNumber(idNumber);
        profile.setEmergencyContactName("family");
        profile.setEmergencyContactPhone("13811112222");
        profile.setCareLevel("L1");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        elderProfileRepository.save(profile);
    }

    private void saveStaffProfile(Long staffId) {
        StaffProfileEntity entity = new StaffProfileEntity();
        entity.setStaffId(staffId);
        entity.setJobTitle("护士");
        entity.setDepartment("护理部");
        entity.setCertificationNo("CERT-1000");
        entity.setHireDate(LocalDate.of(2020, 1, 1));
        entity.setSkillsJson("[\"基础护理\"]");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        staffProfileRepository.save(entity);
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
