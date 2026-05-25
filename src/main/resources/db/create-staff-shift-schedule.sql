CREATE TABLE IF NOT EXISTS `staff_shift_schedule` (
  `shift_id` BIGINT NOT NULL AUTO_INCREMENT,
  `staff_id` BIGINT NOT NULL COMMENT '护理人员ID，关联 users.user_id',
  `shift_date` DATE NOT NULL COMMENT '排班日期',
  `shift_type` VARCHAR(32) NOT NULL COMMENT '班次类型：morning/afternoon/night/full_day',
  `start_time` TIME NOT NULL COMMENT '班次开始时间',
  `end_time` TIME NOT NULL COMMENT '班次结束时间',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态：active/cancelled',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`shift_id`),
  KEY `idx_shift_staff_date` (`staff_id`, `shift_date`),
  KEY `idx_shift_date_time` (`shift_date`, `start_time`, `end_time`),
  CONSTRAINT `fk_staff_shift_schedule_staff` FOREIGN KEY (`staff_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='护理人员排班表';
