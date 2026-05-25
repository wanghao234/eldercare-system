ALTER TABLE `care_plan_tasks`
  ADD COLUMN IF NOT EXISTS `scheduled_date` date DEFAULT NULL COMMENT '计划执行日期' AFTER `suggested_time`,
  ADD COLUMN IF NOT EXISTS `scheduled_time` time DEFAULT NULL COMMENT '计划执行时间' AFTER `scheduled_date`,
  ADD COLUMN IF NOT EXISTS `scheduled_at` datetime DEFAULT NULL COMMENT '计划执行日期时间' AFTER `scheduled_time`,
  ADD COLUMN IF NOT EXISTS `task_source` varchar(32) DEFAULT 'care_plan' COMMENT '任务来源：care_plan/手动创建等' AFTER `scheduled_at`,
  ADD COLUMN IF NOT EXISTS `task_group_key` varchar(64) DEFAULT NULL COMMENT '同一护理项目展开后的分组标识' AFTER `task_source`;

CREATE INDEX IF NOT EXISTS `idx_task_plan_schedule` ON `care_plan_tasks` (`care_plan_id`, `scheduled_at`);
