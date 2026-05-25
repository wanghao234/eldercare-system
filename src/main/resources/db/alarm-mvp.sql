-- Alarm MVP schema
CREATE TABLE IF NOT EXISTS alarms (
  alarm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NULL,
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
  camera_id BIGINT NULL,
  confidence DECIMAL(5,2) NULL,
  snapshot_url VARCHAR(255) NULL,
  attachments_json TEXT NULL,
  map_x DECIMAL(10,2) NULL,
  map_y DECIMAL(10,2) NULL,
  idempotency_key VARCHAR(100) NULL,
  INDEX idx_alarm_elder_created (elder_id, created_at),
  INDEX idx_alarm_status_created (status, created_at),
  INDEX idx_alarm_severity_created (severity, created_at),
  UNIQUE INDEX uk_alarm_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE alarms
  MODIFY COLUMN elder_id BIGINT NULL;

ALTER TABLE alarms
  ADD COLUMN IF NOT EXISTS snapshot_url VARCHAR(255) NULL COMMENT '报警截图地址';

ALTER TABLE alarms
  ADD COLUMN IF NOT EXISTS attachments_json TEXT NULL COMMENT '附件JSON';

CREATE TABLE IF NOT EXISTS camera_device (
  camera_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '摄像头ID',
  camera_name VARCHAR(100) NOT NULL COMMENT '摄像头名称',
  camera_code VARCHAR(100) UNIQUE COMMENT '摄像头唯一编码',
  camera_type VARCHAR(50) DEFAULT 'webcam' COMMENT '摄像头类型：webcam/ip_camera',
  stream_url VARCHAR(500) COMMENT '视频流地址，本地摄像头可为空，IP摄像头填写RTSP地址',
  elder_id BIGINT COMMENT '绑定老人ID',
  room_id BIGINT COMMENT '绑定房间ID',
  bed_id BIGINT COMMENT '绑定床位ID',
  location_text VARCHAR(255) COMMENT '位置描述',
  map_x DECIMAL(10,2) COMMENT '数字孪生X坐标',
  map_y DECIMAL(10,2) COMMENT '数字孪生Y坐标',
  status VARCHAR(20) DEFAULT 'online' COMMENT '状态：online/offline/error',
  remark VARCHAR(255) COMMENT '备注',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头设备表';

INSERT INTO camera_device (
  camera_name,
  camera_code,
  camera_type,
  stream_url,
  elder_id,
  room_id,
  bed_id,
  location_text,
  map_x,
  map_y,
  status
)
SELECT
  '电脑摄像头模拟设备',
  'CAMERA_WEB_001',
  'webcam',
  NULL,
  1,
  101,
  1,
  '电脑摄像头模拟区域',
  320,
  180,
  'online'
WHERE NOT EXISTS (
  SELECT 1 FROM camera_device WHERE camera_code = 'CAMERA_WEB_001'
);

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
