package com.wanghao.eldercare.eldercaresystem.mapper.digitaltwin;

import com.wanghao.eldercare.eldercaresystem.entity.digitaltwin.DigitalTwinMap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalTwinMapRepository extends JpaRepository<DigitalTwinMap, Long> {

    Optional<DigitalTwinMap> findFirstByStatusIgnoreCaseOrderByMapIdAsc(String status);
}
