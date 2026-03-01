package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "floors")
public class Floor {

    @Id
    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "floor_no")
    private Integer floorNo;

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
}
