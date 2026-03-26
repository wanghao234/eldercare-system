package com.wanghao.eldercare.eldercaresystem.common.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;

public final class Role {

    private Role() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String NURSE_LEADER = "NURSE_LEADER";
    public static final String NURSE = "NURSE";
    public static final String CAREGIVER = "CAREGIVER";
    public static final String DOCTOR = "DOCTOR";
    public static final String FAMILY = "FAMILY";
    public static final String ELDER = "ELDER";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_NURSE_LEADER = "ROLE_NURSE_LEADER";
    public static final String ROLE_NURSE = "ROLE_NURSE";
    public static final String ROLE_CAREGIVER = "ROLE_CAREGIVER";
    public static final String ROLE_DOCTOR = "ROLE_DOCTOR";
    public static final String ROLE_FAMILY = "ROLE_FAMILY";
    public static final String ROLE_ELDER = "ROLE_ELDER";
}
