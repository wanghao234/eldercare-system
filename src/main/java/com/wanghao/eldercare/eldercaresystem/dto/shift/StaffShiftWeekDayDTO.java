package com.wanghao.eldercare.eldercaresystem.dto.shift;

import java.time.LocalDate;
import java.util.List;

public class StaffShiftWeekDayDTO {
    private LocalDate date;
    private String weekDay;
    private List<StaffShiftScheduleDTO> shifts;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(String weekDay) {
        this.weekDay = weekDay;
    }

    public List<StaffShiftScheduleDTO> getShifts() {
        return shifts;
    }

    public void setShifts(List<StaffShiftScheduleDTO> shifts) {
        this.shifts = shifts;
    }
}
