package com.wanghao.eldercare.eldercaresystem.rectification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RectificationActionRepository extends JpaRepository<RectificationAction, Long> {
    List<RectificationAction> findByRectificationIdOrderByActionTimeAsc(Long rectificationId);
}
