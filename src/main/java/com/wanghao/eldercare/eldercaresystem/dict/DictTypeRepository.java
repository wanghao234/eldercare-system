package com.wanghao.eldercare.eldercaresystem.dict;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictTypeRepository extends JpaRepository<DictType, String> {
    List<DictType> findAllByIsActiveOrderByTypeCode(Integer isActive);
}
