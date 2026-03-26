package com.wanghao.eldercare.eldercaresystem.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
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
