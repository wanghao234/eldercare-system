package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beds")
public class FacilityBed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "bed_code", length = 64)
    private String bedCode;

    @Column(name = "status", nullable = false, length = 32)
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

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
