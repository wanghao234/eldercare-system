package com.wanghao.eldercare.eldercaresystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleMapperTests {

    @Test
    void nurseLeaderAlias_should_map_to_same_authority() {
        assertEquals("ROLE_NURSE_LEADER", RoleMapper.toAuthority("nurse_leader"));
        assertEquals("ROLE_NURSE_LEADER", RoleMapper.toAuthority("护士长"));
    }

    @Test
    void caregiverAlias_should_map_to_same_authority() {
        assertEquals("ROLE_CAREGIVER", RoleMapper.toAuthority("caregiver"));
        assertEquals("ROLE_CAREGIVER", RoleMapper.toAuthority("护工"));
    }

    @Test
    void doctorAlias_should_map_to_same_authority() {
        assertEquals("ROLE_DOCTOR", RoleMapper.toAuthority("doctor"));
        assertEquals("ROLE_DOCTOR", RoleMapper.toAuthority("医生"));
    }
}
