package com.wanghao.eldercare.eldercaresystem.dto.shift;

import java.util.List;

public class StaffShiftBatchResultDTO {
    private int createdCount;
    private List<StaffShiftScheduleDTO> items;

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public List<StaffShiftScheduleDTO> getItems() {
        return items;
    }

    public void setItems(List<StaffShiftScheduleDTO> items) {
        this.items = items;
    }
}
