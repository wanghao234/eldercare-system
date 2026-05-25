package com.wanghao.eldercare.eldercaresystem.dto.shift;

import java.time.LocalDate;

public class StaffShiftConflictDTO {
    private Long staffId;
    private String staffName;
    private LocalDate shiftDate;
    private Long existingShiftId;
    private String existingTimeRange;
    private String newTimeRange;
    private String message;

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

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public Long getExistingShiftId() {
        return existingShiftId;
    }

    public void setExistingShiftId(Long existingShiftId) {
        this.existingShiftId = existingShiftId;
    }

    public String getExistingTimeRange() {
        return existingTimeRange;
    }

    public void setExistingTimeRange(String existingTimeRange) {
        this.existingTimeRange = existingTimeRange;
    }

    public String getNewTimeRange() {
        return newTimeRange;
    }

    public void setNewTimeRange(String newTimeRange) {
        this.newTimeRange = newTimeRange;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
