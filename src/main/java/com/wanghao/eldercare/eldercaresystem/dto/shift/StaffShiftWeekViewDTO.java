package com.wanghao.eldercare.eldercaresystem.dto.shift;

import java.util.List;

public class StaffShiftWeekViewDTO {
    private Long staffId;
    private String staffName;
    private String roleName;
    private List<StaffShiftWeekDayDTO> days;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<StaffShiftWeekDayDTO> getDays() {
        return days;
    }

    public void setDays(List<StaffShiftWeekDayDTO> days) {
        this.days = days;
    }
}
