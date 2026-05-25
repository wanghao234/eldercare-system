package com.wanghao.eldercare.eldercaresystem.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class StaffShiftScheduleDTO {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Long shiftId;
    private Long staffId;
    private String staffName;
    private String roleName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;
    private String weekDay;
    private String shiftType;
    private String shiftTypeName;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    private String timeRange;
    private String status;
    private String statusName;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static StaffShiftScheduleDTO from(StaffShiftSchedule entity, String staffName, String roleName) {
        StaffShiftScheduleDTO dto = new StaffShiftScheduleDTO();
        dto.setShiftId(entity.getShiftId());
        dto.setStaffId(entity.getStaffId());
        dto.setStaffName(staffName);
        dto.setRoleName(roleName);
        dto.setShiftDate(entity.getShiftDate());
        dto.setWeekDay(toWeekDay(entity.getShiftDate()));
        dto.setShiftType(entity.getShiftType());
        dto.setShiftTypeName(toShiftTypeName(entity.getShiftType()));
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setTimeRange(toTimeRange(entity.getStartTime(), entity.getEndTime()));
        dto.setStatus(entity.getStatus());
        dto.setStatusName(toStatusName(entity.getStatus()));
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

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

    public String getStaffRole() {
        return roleName;
    }

    public void setStaffRole(String staffRole) {
        this.roleName = staffRole;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(String weekDay) {
        this.weekDay = weekDay;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getShiftTypeName() {
        return shiftTypeName;
    }

    public void setShiftTypeName(String shiftTypeName) {
        this.shiftTypeName = shiftTypeName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static String toShiftTypeName(String shiftType) {
        if (shiftType == null) {
            return "";
        }
        return switch (shiftType) {
            case "morning" -> "早班";
            case "afternoon" -> "中班";
            case "night" -> "晚班";
            case "full_day" -> "全天班";
            default -> shiftType;
        };
    }

    private static String toStatusName(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "active" -> "生效中";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    private static String toWeekDay(LocalDate date) {
        if (date == null) {
            return "";
        }
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private static String toTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }
}
