package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beds")
public class Bed {

    @Id
    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "bed_code", length = 64)
    private String bedNo;

    @Column(name = "status", length = 32)
    private String status;

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
