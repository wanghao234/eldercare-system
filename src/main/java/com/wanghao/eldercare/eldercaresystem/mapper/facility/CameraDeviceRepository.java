package com.wanghao.eldercare.eldercaresystem.mapper.facility;

import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CameraDeviceRepository extends JpaRepository<CameraDevice, Long>, JpaSpecificationExecutor<CameraDevice> {
}
