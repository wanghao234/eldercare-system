package com.wanghao.eldercare.eldercaresystem.dto.shift;

import java.util.List;

public class StaffShiftConflictResponse {
    private List<StaffShiftConflictDTO> conflicts;

    public StaffShiftConflictResponse() {
    }

    public StaffShiftConflictResponse(List<StaffShiftConflictDTO> conflicts) {
        this.conflicts = conflicts;
    }

    public List<StaffShiftConflictDTO> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<StaffShiftConflictDTO> conflicts) {
        this.conflicts = conflicts;
    }
}
