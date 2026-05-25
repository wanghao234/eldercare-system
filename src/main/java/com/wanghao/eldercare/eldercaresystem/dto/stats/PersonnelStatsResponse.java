package com.wanghao.eldercare.eldercaresystem.dto.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class PersonnelStatsResponse {
    private long totalStaff;
    private long totalElders;
    private long totalFamilies;
    private Map<String, Long> staffByRole = new LinkedHashMap<>();

    public long getTotalStaff() {
        return totalStaff;
    }

    public void setTotalStaff(long totalStaff) {
        this.totalStaff = totalStaff;
    }

    public long getTotalElders() {
        return totalElders;
    }

    public void setTotalElders(long totalElders) {
        this.totalElders = totalElders;
    }

    public long getTotalFamilies() {
        return totalFamilies;
    }

    public void setTotalFamilies(long totalFamilies) {
        this.totalFamilies = totalFamilies;
    }

    public Map<String, Long> getStaffByRole() {
        return staffByRole;
    }

    public void setStaffByRole(Map<String, Long> staffByRole) {
        this.staffByRole = staffByRole;
    }
}
