package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class RoomUpdateRequest {

    @NotBlank(message = "roomNumber不能为空")
    @JsonAlias("roomNo")
    private String roomNumber;
    @JsonAlias("roomName")
    private String roomType;
    private String note;
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
