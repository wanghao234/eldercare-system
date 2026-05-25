package com.wanghao.eldercare.eldercaresystem.dto.digitaltwin;

import java.math.BigDecimal;

public class DigitalTwinCameraPointVO {

    private Long cameraId;
    private String cameraName;
    private String locationText;
    private BigDecimal mapX;
    private BigDecimal mapY;
    private String status;

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
}
