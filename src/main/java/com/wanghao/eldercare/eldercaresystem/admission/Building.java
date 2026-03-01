package com.wanghao.eldercare.eldercaresystem.admission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "building_name", length = 128)
    private String buildingName;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
}
