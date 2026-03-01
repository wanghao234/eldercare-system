package com.wanghao.eldercare.eldercaresystem.dict;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictItemRepository extends JpaRepository<DictItem, Long> {
    List<DictItem> findByTypeCodeAndIsActiveOrderBySortNoAsc(String typeCode, Integer isActive);
}
