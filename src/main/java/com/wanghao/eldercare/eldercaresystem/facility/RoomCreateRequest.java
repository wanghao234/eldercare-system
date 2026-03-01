package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RoomCreateRequest {

    @NotNull(message = "floorId不能为空")
    private Long floorId;

    @NotBlank(message = "roomNumber不能为空")
    @JsonAlias("roomNo")
    private String roomNumber;

    @JsonAlias("roomName")
    private String roomType;
    private String note;

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
