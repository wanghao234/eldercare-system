package com.wanghao.eldercare.eldercaresystem.dto.digitaltwin;

import java.util.ArrayList;
import java.util.List;

public class DigitalTwinMapResponse {

    private Long mapId;
    private String mapName;
    private String buildingName;
    private Integer floorNo;
    private String mapImage;
    private Integer width;
    private Integer height;
    private List<DigitalTwinCameraPointVO> cameras = new ArrayList<>();
    private List<DigitalTwinAlarmPointVO> activeAlarms = new ArrayList<>();

    public Long getMapId() {
        return mapId;
    }

    public void setMapId(Long mapId) {
        this.mapId = mapId;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public String getMapImage() {
        return mapImage;
    }

    public void setMapImage(String mapImage) {
        this.mapImage = mapImage;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public List<DigitalTwinCameraPointVO> getCameras() {
        return cameras;
    }

    public void setCameras(List<DigitalTwinCameraPointVO> cameras) {
        this.cameras = cameras;
    }

    public List<DigitalTwinAlarmPointVO> getActiveAlarms() {
        return activeAlarms;
    }

    public void setActiveAlarms(List<DigitalTwinAlarmPointVO> activeAlarms) {
        this.activeAlarms = activeAlarms;
    }
}
