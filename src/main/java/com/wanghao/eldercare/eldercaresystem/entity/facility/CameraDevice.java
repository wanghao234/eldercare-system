package com.wanghao.eldercare.eldercaresystem.entity.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "camera_device")
public class CameraDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "camera_id")
    private Long cameraId;

    @Column(name = "camera_name", nullable = false, length = 100)
    private String cameraName;

    @Column(name = "camera_code", length = 100, unique = true)
    private String cameraCode;

    @Column(name = "camera_type", length = 50)
    private String cameraType;

    @Column(name = "stream_url", length = 500)
    private String streamUrl;

    @Column(name = "elder_id")
    private Long elderId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "location_text", length = 255)
    private String locationText;

    @Column(name = "map_x", precision = 10, scale = 2)
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 10, scale = 2)
    private BigDecimal mapY;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getCameraCode() {
        return cameraCode;
    }

    public void setCameraCode(String cameraCode) {
        this.cameraCode = cameraCode;
    }

    public String getCameraType() {
        return cameraType;
    }

    public void setCameraType(String cameraType) {
        this.cameraType = cameraType;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public BigDecimal getMapX() {
        return mapX;
    }

    public void setMapX(BigDecimal mapX) {
        this.mapX = mapX;
    }

    public BigDecimal getMapY() {
        return mapY;
    }

    public void setMapY(BigDecimal mapY) {
        this.mapY = mapY;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
