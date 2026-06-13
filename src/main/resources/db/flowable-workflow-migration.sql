ALTER TABLE wf_definitions
  ADD COLUMN engine_type VARCHAR(32) NULL COMMENT '流程引擎类型',
  ADD COLUMN external_deployment_id VARCHAR(128) NULL COMMENT '外部部署ID',
  ADD COLUMN external_process_definition_id VARCHAR(128) NULL COMMENT '外部流程定义ID',
  ADD COLUMN deployment_time DATETIME NULL COMMENT '部署时间';

ALTER TABLE wf_instances
  ADD COLUMN engine_type VARCHAR(32) NULL COMMENT '流程引擎类型',
  ADD COLUMN external_instance_id VARCHAR(128) NULL COMMENT '外部流程实例ID';

ALTER TABLE wf_tasks
  ADD COLUMN external_task_id VARCHAR(128) NULL COMMENT '外部任务ID',
  ADD COLUMN priority INT NULL COMMENT '优先级';

ALTER TABLE wf_task_action
  ADD COLUMN instance_id BIGINT NULL COMMENT '工作流实例ID';

CREATE INDEX idx_wf_instances_external_instance_id ON wf_instances (external_instance_id);
CREATE INDEX idx_wf_tasks_external_task_id ON wf_tasks (external_task_id);
