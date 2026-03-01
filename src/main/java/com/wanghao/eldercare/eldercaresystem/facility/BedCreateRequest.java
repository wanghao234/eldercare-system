package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public class BedCreateRequest {

    @NotNull(message = "roomId不能为空")
    private Long roomId;

    @JsonAlias({"bedNo", "bed_no"})
    private String bedCode;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String resolveBedCode() {
        return bedCode;
    }
}
