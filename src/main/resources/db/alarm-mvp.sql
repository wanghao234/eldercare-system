-- Alarm MVP schema
CREATE TABLE IF NOT EXISTS alarms (
  alarm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  room_id BIGINT NULL,
  bed_id BIGINT NULL,
  alarm_type VARCHAR(64) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL,
  location_text VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accepted_at DATETIME NULL,
  accepted_by BIGINT NULL,
  arrived_at DATETIME NULL,
  arrived_by BIGINT NULL,
  closed_at DATETIME NULL,
  closed_by BIGINT NULL,
  close_reason VARCHAR(500) NULL,
  process_instance_id VARCHAR(128) NULL,
  INDEX idx_alarm_elder_created (elder_id, created_at),
  INDEX idx_alarm_status_created (status, created_at),
  INDEX idx_alarm_severity_created (severity, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alarm_action_logs (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alarm_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note VARCHAR(500) NULL,
  attachments_json JSON NULL,
  INDEX idx_alarm_action_logs_alarm_time (alarm_id, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
