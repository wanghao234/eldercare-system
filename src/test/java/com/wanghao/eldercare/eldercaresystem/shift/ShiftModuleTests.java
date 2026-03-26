package com.wanghao.eldercare.eldercaresystem.shift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.shift.*;
import com.wanghao.eldercare.eldercaresystem.dto.shift.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.shift.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.shift.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShiftModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository assignmentRepository;
    @Autowired
    private HandoverFocusElderRepository focusElderRepository;
    @Autowired
    private HandoverNoteRepository handoverNoteRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        focusElderRepository.deleteAll();
        handoverNoteRepository.deleteAll();
        shiftRepository.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void leader_create_shift_nurse_add_note_then_close_then_add_note_fail() throws Exception {
        User leader = createUser("leader_shift_1", "nurse_leader");
        User nurse = createUser("nurse_shift_1", "nurse");
        User elder = createUser("elder_shift_1", "elder");
        bind(elder.getUserId(), nurse.getUserId(), null, 1);

        String leaderToken = loginAndGetToken("leader_shift_1", "123456");
        String nurseToken = loginAndGetToken("nurse_shift_1", "123456");

        Long shiftId = createShift(leaderToken, LocalDate.now().plusDays(1), "morning", leader.getUserId());

        mockMvc.perform(post("/api/shifts/{shiftId}/handover-notes", shiftId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"首轮交接：关注夜间睡眠"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/shifts/{shiftId}/close", shiftId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"open","to":"closed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("closed"));

        mockMvc.perform(post("/api/shifts/{shiftId}/handover-notes", shiftId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"关闭后补充"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_cannot_create_shift_returns_40301() throws Exception {
        User nurse = createUser("nurse_shift_2", "nurse");
        String nurseToken = loginAndGetToken("nurse_shift_2", "123456");

        mockMvc.perform(post("/api/shifts")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shiftDate":"2030-01-01","shiftType":"night","leaderId":%d}
                                """.formatted(nurse.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private Long createShift(String token, LocalDate shiftDate, String shiftType, Long leaderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shiftDate":"%s","shiftType":"%s","leaderId":%d}
                                """.formatted(shiftDate, shiftType, leaderId)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("shiftId").asLong();
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

    private void bind(Long elderId, Long nurseId, Long familyId, Integer isActive) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setFamilyId(familyId);
        assignment.setIsActive(isActive);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }
}
