package com.wanghao.eldercare.eldercaresystem.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "floors")
public class FacilityFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "floor_no", nullable = false)
    private Integer floorNo;

    @Column(name = "floor_name", length = 64)
    private String floorName;

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }
}
