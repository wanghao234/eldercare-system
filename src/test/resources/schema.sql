DROP TABLE IF EXISTS care_plan_tasks;
DROP TABLE IF EXISTS staff_shift_schedule;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS elder_profile;
DROP TABLE IF EXISTS staff_profile;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS care_team_assignment;
DROP TABLE IF EXISTS alarm_action_logs;
DROP TABLE IF EXISTS digital_twin_map;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS activity_participants;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS supply_issue_records;
DROP TABLE IF EXISTS supply_stocks;
DROP TABLE IF EXISTS supply_items;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS bill_items;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS fee_items;
DROP TABLE IF EXISTS medication_admin_records;
DROP TABLE IF EXISTS medication_plans;
DROP TABLE IF EXISTS medications;
DROP TABLE IF EXISTS vital_sign_records;
DROP TABLE IF EXISTS weight_records;
DROP TABLE IF EXISTS bowel_records;
DROP TABLE IF EXISTS fluid_intake_records;
DROP TABLE IF EXISTS meal_intake_records;
DROP TABLE IF EXISTS qc_issues;
DROP TABLE IF EXISTS qc_audit_items;
DROP TABLE IF EXISTS qc_audits;
DROP TABLE IF EXISTS care_plan_change_requests;
DROP TABLE IF EXISTS care_plans;
DROP TABLE IF EXISTS visit_request_logs;
DROP TABLE IF EXISTS visit_requests;
DROP TABLE IF EXISTS handover_focus_elders;
DROP TABLE IF EXISTS handover_notes;
DROP TABLE IF EXISTS shifts;
DROP TABLE IF EXISTS discharge_records;
DROP TABLE IF EXISTS admission_records;
DROP TABLE IF EXISTS beds;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS floors;
DROP TABLE IF EXISTS buildings;
DROP TABLE IF EXISTS rectification_actions;
DROP TABLE IF EXISTS rectifications;
DROP TABLE IF EXISTS wf_task_action;
DROP TABLE IF EXISTS wf_tasks;
DROP TABLE IF EXISTS wf_instances;
DROP TABLE IF EXISTS wf_definitions;
DROP TABLE IF EXISTS alarms;
DROP TABLE IF EXISTS audit_log;

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  real_name VARCHAR(64),
  phone VARCHAR(32),
  email VARCHAR(128),
  avatar_url VARCHAR(255),
  last_login_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted_at DATETIME
);

CREATE TABLE elder_profile (
  elder_id BIGINT PRIMARY KEY,
  gender VARCHAR(16),
  birthday DATE,
  id_number VARCHAR(64),
  address VARCHAR(255),
  emergency_contact_name VARCHAR(64),
  emergency_contact_phone VARCHAR(32),
  allergies VARCHAR(500),
  chronic_conditions VARCHAR(500),
  diet_taboo VARCHAR(500),
  care_level VARCHAR(32),
  notes VARCHAR(1000),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE staff_profile (
  staff_id BIGINT PRIMARY KEY,
  job_title VARCHAR(64),
  department VARCHAR(64),
  certification_no VARCHAR(128),
  hire_date DATE,
  skills_json TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE roles (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
  perm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_code VARCHAR(64) NOT NULL UNIQUE,
  perm_name VARCHAR(128) NOT NULL,
  perm_type VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE care_team_assignment (
  assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  nurse_id BIGINT,
  family_id BIGINT,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alarms (
  alarm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT,
  room_id BIGINT,
  bed_id BIGINT,
  alarm_type VARCHAR(64) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL,
  location_text VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  accepted_at DATETIME,
  accepted_by BIGINT,
  arrived_at DATETIME,
  arrived_by BIGINT,
  closed_at DATETIME,
  closed_by BIGINT,
  close_reason VARCHAR(500),
  process_instance_id VARCHAR(128),
  camera_id BIGINT,
  confidence DECIMAL(5,2),
  snapshot_url VARCHAR(255),
  attachments_json TEXT,
  map_x DECIMAL(10,2),
  map_y DECIMAL(10,2),
  idempotency_key VARCHAR(100),
  CONSTRAINT uk_alarm_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE alarm_action_logs (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alarm_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  action_time DATETIME NOT NULL,
  note VARCHAR(500),
  attachments_json TEXT
);

CREATE TABLE camera_device (
  camera_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  camera_name VARCHAR(100) NOT NULL,
  camera_code VARCHAR(100) UNIQUE,
  camera_type VARCHAR(50) DEFAULT 'webcam',
  stream_url VARCHAR(500),
  elder_id BIGINT,
  room_id BIGINT,
  bed_id BIGINT,
  location_text VARCHAR(255),
  map_x DECIMAL(10,2),
  map_y DECIMAL(10,2),
  status VARCHAR(20) DEFAULT 'online',
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE digital_twin_map (
  map_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  map_name VARCHAR(100) NOT NULL,
  building_id BIGINT,
  floor_id BIGINT,
  building_name VARCHAR(100),
  floor_no INT,
  map_image VARCHAR(255) NOT NULL,
  width INT NOT NULL,
  height INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wf_definitions (
  def_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  is_active TINYINT NOT NULL DEFAULT 1,
  bpmn_xml LONGTEXT,
  engine_type VARCHAR(32),
  external_deployment_id VARCHAR(128),
  external_process_definition_id VARCHAR(128),
  deployment_time DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wf_instances (
  instance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_key VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_by BIGINT NOT NULL,
  started_at DATETIME NOT NULL,
  ended_at DATETIME,
  engine_type VARCHAR(32),
  external_instance_id VARCHAR(128),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE wf_tasks (
  wf_task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instance_id BIGINT NOT NULL,
  external_task_id VARCHAR(128),
  node_key VARCHAR(64) NOT NULL,
  node_name VARCHAR(128),
  assignee_id BIGINT,
  candidate_role VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  priority INT,
  due_at DATETIME,
  claimed_at DATETIME,
  completed_at DATETIME,
  comment TEXT,
  form_data_json TEXT,
  attachments_json TEXT,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE wf_task_action (
  action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  wf_task_id BIGINT NOT NULL,
  instance_id BIGINT,
  action VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  action_time DATETIME NOT NULL,
  comment TEXT,
  extra_json TEXT
);

CREATE TABLE visit_requests (
  request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  family_id BIGINT NOT NULL,
  request_type VARCHAR(32) NOT NULL,
  planned_start_at DATETIME,
  planned_end_at DATETIME,
  destination VARCHAR(255),
  reason TEXT,
  companion_count INT,
  status VARCHAR(32) NOT NULL,
  confirmed_at DATETIME,
  confirmed_by BIGINT,
  approved_at DATETIME,
  approved_by BIGINT,
  rejected_at DATETIME,
  rejected_by BIGINT,
  reject_reason VARCHAR(500),
  check_out_at DATETIME,
  check_out_by BIGINT,
  check_in_at DATETIME,
  check_in_by BIGINT,
  cancelled_at DATETIME,
  cancelled_by BIGINT,
  cancel_reason VARCHAR(500),
  extra_json TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE visit_request_logs (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  action_time DATETIME NOT NULL,
  comment VARCHAR(500),
  extra_json TEXT
);

CREATE TABLE shifts (
  shift_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shift_date DATE NOT NULL,
  shift_type VARCHAR(16) NOT NULL,
  leader_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'open',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_shift_date_type UNIQUE (shift_date, shift_type)
);

CREATE TABLE staff_shift_schedule (
  shift_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_id BIGINT NOT NULL,
  shift_date DATE NOT NULL,
  shift_type VARCHAR(32) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE handover_notes (
  note_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shift_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE handover_focus_elders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shift_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_shift_focus_elder UNIQUE (shift_id, elder_id)
);

CREATE TABLE care_plans (
  care_plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  version INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  start_date DATE,
  end_date DATE,
  care_level VARCHAR(32),
  care_time VARCHAR(64),
  care_content TEXT,
  medication_reminder TEXT,
  diet_plan TEXT,
  health_assessment TEXT,
  nursing_problem TEXT,
  risk_tags VARCHAR(255),
  nursing_goal TEXT,
  daily_care TEXT,
  medication_care TEXT,
  health_monitoring TEXT,
  rehabilitation_activity TEXT,
  psychological_care TEXT,
  safety_precaution TEXT,
  execution_frequency VARCHAR(128),
  evaluation TEXT,
  ai_generated TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  approved_by BIGINT,
  approved_at DATETIME,
  record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE care_plan_change_requests (
  change_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  from_care_plan_id BIGINT,
  change_type VARCHAR(32) NOT NULL,
  proposed_json TEXT NOT NULL,
  evidence_json TEXT,
  reason VARCHAR(255) NOT NULL,
  requested_by BIGINT NOT NULL,
  requested_at DATETIME,
  status VARCHAR(16) NOT NULL,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_comment VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE care_plan_tasks (
  task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  care_plan_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  assigned_nurse_id BIGINT,
  task_type VARCHAR(64),
  task_title VARCHAR(128) NOT NULL,
  task_content TEXT,
  frequency_desc VARCHAR(128),
  suggested_time VARCHAR(128),
  scheduled_date DATE,
  scheduled_time TIME,
  scheduled_at DATETIME,
  task_source VARCHAR(32) DEFAULT 'care_plan',
  task_group_key VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  execution_result TEXT,
  executed_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE tasks (
  task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT,
  task_type VARCHAR(32),
  title VARCHAR(255),
  description TEXT,
  priority VARCHAR(32),
  status VARCHAR(32),
  scheduled_at DATETIME,
  due_at DATETIME,
  assigned_to BIGINT,
  created_by BIGINT,
  completed_by BIGINT,
  completed_at DATETIME,
  related_biz_type VARCHAR(64),
  related_biz_id BIGINT,
  process_instance_id BIGINT,
  wf_task_id BIGINT,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE messages (
  message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL,
  content TEXT,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL
);

CREATE TABLE activities (
  activity_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  description TEXT,
  activity_time DATETIME NOT NULL,
  location VARCHAR(128),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activity_participants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'signed',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_activity_elder UNIQUE (activity_id, elder_id)
);

CREATE TABLE supply_items (
  supply_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_name VARCHAR(128) NOT NULL UNIQUE,
  category VARCHAR(64) NOT NULL,
  unit VARCHAR(32),
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE supply_stocks (
  stock_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  supply_item_id BIGINT NOT NULL,
  quantity DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  min_threshold DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  location VARCHAR(64),
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE supply_issue_records (
  issue_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  supply_item_id BIGINT NOT NULL,
  quantity DECIMAL(12,2) NOT NULL,
  issued_to BIGINT,
  issued_by BIGINT,
  issue_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note VARCHAR(255),
  related_task_id BIGINT
);

CREATE TABLE notifications (
  notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  to_user_id BIGINT NOT NULL,
  title VARCHAR(255),
  content TEXT,
  notif_type VARCHAR(64),
  biz_type VARCHAR(64),
  biz_id BIGINT,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  read_at DATETIME
);

CREATE TABLE audit_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT,
  detail_json TEXT,
  ip VARCHAR(64),
  user_agent VARCHAR(255),
  created_at DATETIME NOT NULL
);

CREATE TABLE medications (
  medication_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  medication_name VARCHAR(255) NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(64),
  description TEXT,
  created_at DATETIME
);

CREATE TABLE fee_items (
  fee_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_name VARCHAR(128) NOT NULL UNIQUE,
  category VARCHAR(32) NOT NULL,
  unit VARCHAR(16),
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bills (
  bill_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(16) NOT NULL DEFAULT 'unpaid',
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_date DATE,
  created_by BIGINT
);

CREATE TABLE bill_items (
  bill_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bill_id BIGINT NOT NULL,
  fee_item_id BIGINT NOT NULL,
  quantity DECIMAL(12,2) NOT NULL DEFAULT 1.00,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  note VARCHAR(255)
);

CREATE TABLE payments (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bill_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method VARCHAR(16) NOT NULL DEFAULT 'offline',
  transaction_no VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'paid',
  paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medication_plans (
  plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  medication_id BIGINT NOT NULL,
  dosage VARCHAR(128) NOT NULL,
  frequency VARCHAR(64) NOT NULL,
  times_json TEXT NOT NULL,
  medications_json TEXT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE medication_admin_records (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  medication_id BIGINT NOT NULL,
  plan_id BIGINT,
  administered_time DATETIME NOT NULL,
  administered_by BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  dosage VARCHAR(128),
  note VARCHAR(500),
  created_at DATETIME NOT NULL
);

CREATE TABLE meal_intake_records (
  meal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  meal_type VARCHAR(32),
  intake_ratio INT,
  diet_type VARCHAR(64),
  note VARCHAR(500),
  recorded_by BIGINT,
  record_time DATETIME,
  created_at DATETIME
);

CREATE TABLE fluid_intake_records (
  fluid_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  drink_type VARCHAR(64),
  volume_ml INT,
  note VARCHAR(500),
  recorded_by BIGINT,
  record_time DATETIME,
  created_at DATETIME
);

CREATE TABLE bowel_records (
  bowel_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  bristol_type INT,
  amount VARCHAR(32),
  incontinence TINYINT,
  blood_flag TINYINT,
  note VARCHAR(500),
  recorded_by BIGINT,
  record_time DATETIME,
  created_at DATETIME
);

CREATE TABLE weight_records (
  weight_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  weight_kg DECIMAL(10,2),
  measure_ctx VARCHAR(64),
  note VARCHAR(500),
  recorded_by BIGINT,
  record_time DATETIME,
  created_at DATETIME
);

CREATE TABLE vital_sign_records (
  vital_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  record_time DATETIME,
  heart_rate INT,
  systolic_bp INT,
  diastolic_bp INT,
  spo2 INT,
  temperature DECIMAL(5,2),
  blood_glucose DECIMAL(10,2),
  source VARCHAR(32),
  device_type VARCHAR(32),
  device_id VARCHAR(128),
  device_name VARCHAR(128),
  recorded_by BIGINT,
  note VARCHAR(500),
  created_at DATETIME
);

CREATE TABLE qc_audits (
  audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT,
  title VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE qc_audit_items (
  item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  audit_id BIGINT NOT NULL,
  item_code VARCHAR(64),
  item_name VARCHAR(255),
  result VARCHAR(16),
  issues TEXT,
  evidence_json TEXT,
  checked_by BIGINT,
  checked_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE qc_issues (
  issue_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qc_item_id BIGINT NOT NULL,
  audit_id BIGINT,
  elder_id BIGINT,
  level VARCHAR(32) NOT NULL,
  description TEXT,
  responsible_id BIGINT,
  status VARCHAR(32) NOT NULL,
  rectification_id BIGINT,
  created_by BIGINT NOT NULL,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE buildings (
  building_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_name VARCHAR(128) NOT NULL,
  deleted_at DATETIME
);

CREATE TABLE floors (
  floor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_id BIGINT,
  floor_no INT NOT NULL,
  floor_name VARCHAR(64),
  deleted_at DATETIME
);

CREATE TABLE rooms (
  room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  floor_id BIGINT,
  room_no VARCHAR(64),
  room_number VARCHAR(64),
  room_type VARCHAR(32),
  note VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  CONSTRAINT uk_floor_room_number UNIQUE (floor_id, room_number)
);

CREATE TABLE beds (
  bed_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT,
  bed_no VARCHAR(64),
  bed_code VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'available',
  deleted_at DATETIME,
  CONSTRAINT uk_room_bed_code UNIQUE (room_id, bed_code)
);

CREATE TABLE admission_records (
  admission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  bed_id BIGINT NOT NULL,
  contract_no VARCHAR(64),
  package_name VARCHAR(128),
  contract_file_url VARCHAR(255),
  deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  start_date DATE NOT NULL,
  end_date DATE,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT,
  created_at DATETIME,
  updated_at DATETIME,
  process_instance_id BIGINT
);

CREATE TABLE discharge_records (
  discharge_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admission_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  bed_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  reason TEXT,
  requested_date DATE,
  actual_date DATE,
  settlement_amount DECIMAL(10,2),
  refund_amount DECIMAL(10,2),
  created_by BIGINT,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE rectifications (
  rectification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_type VARCHAR(64) NOT NULL,
  source_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  level VARCHAR(32) NOT NULL,
  owner_id BIGINT NOT NULL,
  due_at DATETIME,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NOT NULL,
  process_instance_id VARCHAR(128),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE rectification_actions (
  action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rectification_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  action_time DATETIME NOT NULL,
  content TEXT,
  attachments_json TEXT,
  extra_json TEXT
);

COMMENT ON TABLE users IS '系统用户表';
COMMENT ON COLUMN users.user_id IS '用户ID';
COMMENT ON COLUMN users.username IS '登录用户名';
COMMENT ON COLUMN users.password_hash IS '密码哈希';
COMMENT ON COLUMN users.role IS '用户角色';
COMMENT ON COLUMN users.status IS '状态';
COMMENT ON COLUMN users.real_name IS '真实姓名';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.avatar_url IS '头像地址';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.deleted_at IS '删除时间';
COMMENT ON TABLE elder_profile IS '老人档案表';
COMMENT ON COLUMN elder_profile.elder_id IS '老人ID';
COMMENT ON COLUMN elder_profile.gender IS '性别';
COMMENT ON COLUMN elder_profile.birthday IS '生日';
COMMENT ON COLUMN elder_profile.id_number IS '身份证号';
COMMENT ON COLUMN elder_profile.address IS '地址';
COMMENT ON COLUMN elder_profile.emergency_contact_name IS '紧急联系人姓名';
COMMENT ON COLUMN elder_profile.emergency_contact_phone IS '紧急联系人电话';
COMMENT ON COLUMN elder_profile.allergies IS '过敏史';
COMMENT ON COLUMN elder_profile.chronic_conditions IS '慢性病情况';
COMMENT ON COLUMN elder_profile.diet_taboo IS '饮食禁忌';
COMMENT ON COLUMN elder_profile.care_level IS '护理等级';
COMMENT ON COLUMN elder_profile.notes IS '备注';
COMMENT ON COLUMN elder_profile.created_at IS '创建时间';
COMMENT ON COLUMN elder_profile.updated_at IS '更新时间';
COMMENT ON TABLE staff_profile IS '员工档案表';
COMMENT ON COLUMN staff_profile.staff_id IS '员工ID';
COMMENT ON COLUMN staff_profile.job_title IS '岗位名称';
COMMENT ON COLUMN staff_profile.department IS '所属部门';
COMMENT ON COLUMN staff_profile.certification_no IS '资质证书编号';
COMMENT ON COLUMN staff_profile.hire_date IS '入职日期';
COMMENT ON COLUMN staff_profile.skills_json IS '技能信息JSON';
COMMENT ON COLUMN staff_profile.created_at IS '创建时间';
COMMENT ON COLUMN staff_profile.updated_at IS '更新时间';
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.role_id IS '角色ID';
COMMENT ON COLUMN roles.role_code IS '角色编码';
COMMENT ON COLUMN roles.role_name IS '角色名称';
COMMENT ON COLUMN roles.created_at IS '创建时间';
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.perm_id IS '权限ID';
COMMENT ON COLUMN permissions.perm_code IS '权限编码';
COMMENT ON COLUMN permissions.perm_name IS '权限名称';
COMMENT ON COLUMN permissions.perm_type IS '权限类型';
COMMENT ON COLUMN permissions.created_at IS '创建时间';
COMMENT ON TABLE user_role IS '用户角色关联表';
COMMENT ON COLUMN user_role.id IS '主键ID';
COMMENT ON COLUMN user_role.user_id IS '用户ID';
COMMENT ON COLUMN user_role.role_id IS '角色ID';
COMMENT ON COLUMN user_role.created_at IS '创建时间';
COMMENT ON TABLE role_permission IS '角色权限关联表';
COMMENT ON COLUMN role_permission.id IS '主键ID';
COMMENT ON COLUMN role_permission.role_id IS '角色ID';
COMMENT ON COLUMN role_permission.perm_id IS '权限ID';
COMMENT ON COLUMN role_permission.created_at IS '创建时间';
COMMENT ON TABLE care_team_assignment IS '照护团队分配表';
COMMENT ON COLUMN care_team_assignment.assignment_id IS '分配ID';
COMMENT ON COLUMN care_team_assignment.elder_id IS '老人ID';
COMMENT ON COLUMN care_team_assignment.nurse_id IS '护士ID';
COMMENT ON COLUMN care_team_assignment.family_id IS '家属ID';
COMMENT ON COLUMN care_team_assignment.is_active IS '是否启用';
COMMENT ON COLUMN care_team_assignment.created_at IS '创建时间';
COMMENT ON COLUMN care_team_assignment.updated_at IS '更新时间';
COMMENT ON TABLE alarms IS '报警事件表';
COMMENT ON COLUMN alarms.alarm_id IS '报警ID';
COMMENT ON COLUMN alarms.elder_id IS '老人ID';
COMMENT ON COLUMN alarms.room_id IS '房间ID';
COMMENT ON COLUMN alarms.bed_id IS '床位ID';
COMMENT ON COLUMN alarms.alarm_type IS '报警类型';
COMMENT ON COLUMN alarms.severity IS '报警级别';
COMMENT ON COLUMN alarms.source IS '来源';
COMMENT ON COLUMN alarms.location_text IS '位置描述';
COMMENT ON COLUMN alarms.status IS '状态';
COMMENT ON COLUMN alarms.created_at IS '创建时间';
COMMENT ON COLUMN alarms.accepted_at IS '接警时间';
COMMENT ON COLUMN alarms.accepted_by IS '接警人ID';
COMMENT ON COLUMN alarms.arrived_at IS '到场时间';
COMMENT ON COLUMN alarms.arrived_by IS '到场人ID';
COMMENT ON COLUMN alarms.closed_at IS '关闭时间';
COMMENT ON COLUMN alarms.closed_by IS '关闭人ID';
COMMENT ON COLUMN alarms.close_reason IS '关闭原因';
COMMENT ON COLUMN alarms.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN alarms.camera_id IS '摄像头ID';
COMMENT ON COLUMN alarms.confidence IS '识别置信度';
COMMENT ON COLUMN alarms.snapshot_url IS '截图地址';
COMMENT ON COLUMN alarms.attachments_json IS '附件JSON';
COMMENT ON COLUMN alarms.map_x IS '地图X坐标';
COMMENT ON COLUMN alarms.map_y IS '地图Y坐标';
COMMENT ON COLUMN alarms.idempotency_key IS '幂等键';
COMMENT ON TABLE alarm_action_logs IS '报警处理日志表';
COMMENT ON COLUMN alarm_action_logs.log_id IS '日志ID';
COMMENT ON COLUMN alarm_action_logs.alarm_id IS '报警ID';
COMMENT ON COLUMN alarm_action_logs.action IS '操作类型';
COMMENT ON COLUMN alarm_action_logs.actor_id IS '操作人ID';
COMMENT ON COLUMN alarm_action_logs.action_time IS '操作时间';
COMMENT ON COLUMN alarm_action_logs.note IS '备注';
COMMENT ON COLUMN alarm_action_logs.attachments_json IS '附件JSON';
COMMENT ON TABLE camera_device IS '摄像头设备表';
COMMENT ON COLUMN camera_device.camera_id IS '摄像头ID';
COMMENT ON COLUMN camera_device.camera_name IS '摄像头名称';
COMMENT ON COLUMN camera_device.camera_code IS '摄像头编码';
COMMENT ON COLUMN camera_device.camera_type IS '摄像头类型';
COMMENT ON COLUMN camera_device.stream_url IS '视频流地址';
COMMENT ON COLUMN camera_device.elder_id IS '老人ID';
COMMENT ON COLUMN camera_device.room_id IS '房间ID';
COMMENT ON COLUMN camera_device.bed_id IS '床位ID';
COMMENT ON COLUMN camera_device.location_text IS '位置描述';
COMMENT ON COLUMN camera_device.map_x IS '地图X坐标';
COMMENT ON COLUMN camera_device.map_y IS '地图Y坐标';
COMMENT ON COLUMN camera_device.status IS '状态';
COMMENT ON COLUMN camera_device.remark IS '备注';
COMMENT ON COLUMN camera_device.created_at IS '创建时间';
COMMENT ON COLUMN camera_device.updated_at IS '更新时间';
COMMENT ON TABLE digital_twin_map IS '数字孪生地图配置表';
COMMENT ON COLUMN digital_twin_map.map_id IS '地图ID';
COMMENT ON COLUMN digital_twin_map.map_name IS '地图名称';
COMMENT ON COLUMN digital_twin_map.building_id IS '楼栋ID';
COMMENT ON COLUMN digital_twin_map.floor_id IS '楼层ID';
COMMENT ON COLUMN digital_twin_map.building_name IS '楼栋名称';
COMMENT ON COLUMN digital_twin_map.floor_no IS '楼层号';
COMMENT ON COLUMN digital_twin_map.map_image IS '地图图片地址';
COMMENT ON COLUMN digital_twin_map.width IS '地图宽度';
COMMENT ON COLUMN digital_twin_map.height IS '地图高度';
COMMENT ON COLUMN digital_twin_map.status IS '状态';
COMMENT ON COLUMN digital_twin_map.created_at IS '创建时间';
COMMENT ON COLUMN digital_twin_map.updated_at IS '更新时间';
COMMENT ON TABLE wf_definitions IS '工作流定义表';
COMMENT ON COLUMN wf_definitions.def_id IS '工作流定义ID';
COMMENT ON COLUMN wf_definitions.process_key IS '流程标识';
COMMENT ON COLUMN wf_definitions.name IS '名称';
COMMENT ON COLUMN wf_definitions.version IS '版本号';
COMMENT ON COLUMN wf_definitions.is_active IS '是否启用';
COMMENT ON COLUMN wf_definitions.bpmn_xml IS 'BPMN XML内容';
COMMENT ON COLUMN wf_definitions.engine_type IS '流程引擎类型';
COMMENT ON COLUMN wf_definitions.external_deployment_id IS '外部部署ID';
COMMENT ON COLUMN wf_definitions.external_process_definition_id IS '外部流程定义ID';
COMMENT ON COLUMN wf_definitions.deployment_time IS '部署时间';
COMMENT ON COLUMN wf_definitions.created_at IS '创建时间';
COMMENT ON TABLE wf_instances IS '工作流实例表';
COMMENT ON COLUMN wf_instances.instance_id IS '工作流实例ID';
COMMENT ON COLUMN wf_instances.process_key IS '流程标识';
COMMENT ON COLUMN wf_instances.biz_type IS '业务类型';
COMMENT ON COLUMN wf_instances.biz_id IS '业务ID';
COMMENT ON COLUMN wf_instances.status IS '状态';
COMMENT ON COLUMN wf_instances.started_by IS '发起人ID';
COMMENT ON COLUMN wf_instances.started_at IS '发起时间';
COMMENT ON COLUMN wf_instances.ended_at IS '结束时间';
COMMENT ON COLUMN wf_instances.engine_type IS '流程引擎类型';
COMMENT ON COLUMN wf_instances.external_instance_id IS '外部流程实例ID';
COMMENT ON COLUMN wf_instances.created_at IS '创建时间';
COMMENT ON COLUMN wf_instances.updated_at IS '更新时间';
COMMENT ON TABLE wf_tasks IS '工作流任务表';
COMMENT ON COLUMN wf_tasks.wf_task_id IS '工作流任务ID';
COMMENT ON COLUMN wf_tasks.instance_id IS '工作流实例ID';
COMMENT ON COLUMN wf_tasks.external_task_id IS '外部任务ID';
COMMENT ON COLUMN wf_tasks.node_key IS '节点标识';
COMMENT ON COLUMN wf_tasks.node_name IS '节点名称';
COMMENT ON COLUMN wf_tasks.assignee_id IS '负责人ID';
COMMENT ON COLUMN wf_tasks.candidate_role IS '候选角色';
COMMENT ON COLUMN wf_tasks.status IS '状态';
COMMENT ON COLUMN wf_tasks.priority IS '优先级';
COMMENT ON COLUMN wf_tasks.due_at IS '截止时间';
COMMENT ON COLUMN wf_tasks.claimed_at IS '认领时间';
COMMENT ON COLUMN wf_tasks.completed_at IS '完成时间';
COMMENT ON COLUMN wf_tasks.comment IS '备注';
COMMENT ON COLUMN wf_tasks.form_data_json IS '表单数据JSON';
COMMENT ON COLUMN wf_tasks.attachments_json IS '附件JSON';
COMMENT ON COLUMN wf_tasks.created_at IS '创建时间';
COMMENT ON COLUMN wf_tasks.updated_at IS '更新时间';
COMMENT ON TABLE wf_task_action IS '工作流任务操作日志表';
COMMENT ON COLUMN wf_task_action.action_id IS '操作ID';
COMMENT ON COLUMN wf_task_action.wf_task_id IS '工作流任务ID';
COMMENT ON COLUMN wf_task_action.instance_id IS '工作流实例ID';
COMMENT ON COLUMN wf_task_action.action IS '操作类型';
COMMENT ON COLUMN wf_task_action.actor_id IS '操作人ID';
COMMENT ON COLUMN wf_task_action.action_time IS '操作时间';
COMMENT ON COLUMN wf_task_action.comment IS '备注';
COMMENT ON COLUMN wf_task_action.extra_json IS '扩展信息JSON';
COMMENT ON TABLE visit_requests IS '探访申请表';
COMMENT ON COLUMN visit_requests.request_id IS '申请ID';
COMMENT ON COLUMN visit_requests.elder_id IS '老人ID';
COMMENT ON COLUMN visit_requests.family_id IS '家属ID';
COMMENT ON COLUMN visit_requests.request_type IS '申请类型';
COMMENT ON COLUMN visit_requests.planned_start_at IS '计划开始时间';
COMMENT ON COLUMN visit_requests.planned_end_at IS '计划结束时间';
COMMENT ON COLUMN visit_requests.destination IS '目的地';
COMMENT ON COLUMN visit_requests.reason IS '原因';
COMMENT ON COLUMN visit_requests.companion_count IS '陪同人数';
COMMENT ON COLUMN visit_requests.status IS '状态';
COMMENT ON COLUMN visit_requests.confirmed_at IS '确认时间';
COMMENT ON COLUMN visit_requests.confirmed_by IS '确认人ID';
COMMENT ON COLUMN visit_requests.approved_at IS '审批通过时间';
COMMENT ON COLUMN visit_requests.approved_by IS '审批人ID';
COMMENT ON COLUMN visit_requests.rejected_at IS '驳回时间';
COMMENT ON COLUMN visit_requests.rejected_by IS '驳回人ID';
COMMENT ON COLUMN visit_requests.reject_reason IS '驳回原因';
COMMENT ON COLUMN visit_requests.check_out_at IS '签出时间';
COMMENT ON COLUMN visit_requests.check_out_by IS '签出人ID';
COMMENT ON COLUMN visit_requests.check_in_at IS '签入时间';
COMMENT ON COLUMN visit_requests.check_in_by IS '签入人ID';
COMMENT ON COLUMN visit_requests.cancelled_at IS '取消时间';
COMMENT ON COLUMN visit_requests.cancelled_by IS '取消人ID';
COMMENT ON COLUMN visit_requests.cancel_reason IS '取消原因';
COMMENT ON COLUMN visit_requests.extra_json IS '扩展信息JSON';
COMMENT ON COLUMN visit_requests.created_at IS '创建时间';
COMMENT ON COLUMN visit_requests.updated_at IS '更新时间';
COMMENT ON TABLE visit_request_logs IS '探访申请操作日志表';
COMMENT ON COLUMN visit_request_logs.log_id IS '日志ID';
COMMENT ON COLUMN visit_request_logs.request_id IS '申请ID';
COMMENT ON COLUMN visit_request_logs.action IS '操作类型';
COMMENT ON COLUMN visit_request_logs.actor_id IS '操作人ID';
COMMENT ON COLUMN visit_request_logs.action_time IS '操作时间';
COMMENT ON COLUMN visit_request_logs.comment IS '备注';
COMMENT ON COLUMN visit_request_logs.extra_json IS '扩展信息JSON';
COMMENT ON TABLE shifts IS '交接班班次表';
COMMENT ON COLUMN shifts.shift_id IS '班次ID';
COMMENT ON COLUMN shifts.shift_date IS '班次日期';
COMMENT ON COLUMN shifts.shift_type IS '班次类型';
COMMENT ON COLUMN shifts.leader_id IS '负责人ID';
COMMENT ON COLUMN shifts.status IS '状态';
COMMENT ON COLUMN shifts.created_at IS '创建时间';
COMMENT ON TABLE staff_shift_schedule IS '员工排班表';
COMMENT ON COLUMN staff_shift_schedule.shift_id IS '班次ID';
COMMENT ON COLUMN staff_shift_schedule.staff_id IS '员工ID';
COMMENT ON COLUMN staff_shift_schedule.shift_date IS '班次日期';
COMMENT ON COLUMN staff_shift_schedule.shift_type IS '班次类型';
COMMENT ON COLUMN staff_shift_schedule.start_time IS '开始时间';
COMMENT ON COLUMN staff_shift_schedule.end_time IS '结束时间';
COMMENT ON COLUMN staff_shift_schedule.status IS '状态';
COMMENT ON COLUMN staff_shift_schedule.remark IS '备注';
COMMENT ON COLUMN staff_shift_schedule.created_at IS '创建时间';
COMMENT ON COLUMN staff_shift_schedule.updated_at IS '更新时间';
COMMENT ON TABLE handover_notes IS '交接班记录表';
COMMENT ON COLUMN handover_notes.note_id IS '交接记录ID';
COMMENT ON COLUMN handover_notes.shift_id IS '班次ID';
COMMENT ON COLUMN handover_notes.created_by IS '创建人ID';
COMMENT ON COLUMN handover_notes.content IS '内容';
COMMENT ON COLUMN handover_notes.created_at IS '创建时间';
COMMENT ON TABLE handover_focus_elders IS '交接重点老人表';
COMMENT ON COLUMN handover_focus_elders.id IS '主键ID';
COMMENT ON COLUMN handover_focus_elders.shift_id IS '班次ID';
COMMENT ON COLUMN handover_focus_elders.elder_id IS '老人ID';
COMMENT ON COLUMN handover_focus_elders.note IS '备注';
COMMENT ON COLUMN handover_focus_elders.created_at IS '创建时间';
COMMENT ON TABLE care_plans IS '护理计划表';
COMMENT ON COLUMN care_plans.care_plan_id IS '护理计划ID';
COMMENT ON COLUMN care_plans.elder_id IS '老人ID';
COMMENT ON COLUMN care_plans.version IS '版本号';
COMMENT ON COLUMN care_plans.status IS '状态';
COMMENT ON COLUMN care_plans.start_date IS '开始日期';
COMMENT ON COLUMN care_plans.end_date IS '结束日期';
COMMENT ON COLUMN care_plans.care_level IS '护理等级';
COMMENT ON COLUMN care_plans.care_time IS '护理时间';
COMMENT ON COLUMN care_plans.care_content IS '护理内容';
COMMENT ON COLUMN care_plans.medication_reminder IS '用药提醒';
COMMENT ON COLUMN care_plans.diet_plan IS '饮食计划';
COMMENT ON COLUMN care_plans.health_assessment IS '健康评估';
COMMENT ON COLUMN care_plans.nursing_problem IS '护理问题';
COMMENT ON COLUMN care_plans.risk_tags IS '风险标签';
COMMENT ON COLUMN care_plans.nursing_goal IS '护理目标';
COMMENT ON COLUMN care_plans.daily_care IS '日常护理';
COMMENT ON COLUMN care_plans.medication_care IS '用药护理';
COMMENT ON COLUMN care_plans.health_monitoring IS '健康监测';
COMMENT ON COLUMN care_plans.rehabilitation_activity IS '康复活动';
COMMENT ON COLUMN care_plans.psychological_care IS '心理关怀';
COMMENT ON COLUMN care_plans.safety_precaution IS '安全防护';
COMMENT ON COLUMN care_plans.execution_frequency IS '执行频率';
COMMENT ON COLUMN care_plans.evaluation IS '护理评价';
COMMENT ON COLUMN care_plans.ai_generated IS '是否AI生成';
COMMENT ON COLUMN care_plans.created_by IS '创建人ID';
COMMENT ON COLUMN care_plans.approved_by IS '审批人ID';
COMMENT ON COLUMN care_plans.approved_at IS '审批通过时间';
COMMENT ON COLUMN care_plans.record_time IS '记录时间';
COMMENT ON COLUMN care_plans.created_at IS '创建时间';
COMMENT ON COLUMN care_plans.updated_at IS '更新时间';
COMMENT ON TABLE care_plan_change_requests IS '护理计划变更申请表';
COMMENT ON COLUMN care_plan_change_requests.change_id IS '变更申请ID';
COMMENT ON COLUMN care_plan_change_requests.elder_id IS '老人ID';
COMMENT ON COLUMN care_plan_change_requests.from_care_plan_id IS '来源护理计划ID';
COMMENT ON COLUMN care_plan_change_requests.change_type IS '变更类型';
COMMENT ON COLUMN care_plan_change_requests.proposed_json IS '变更内容JSON';
COMMENT ON COLUMN care_plan_change_requests.evidence_json IS '佐证材料JSON';
COMMENT ON COLUMN care_plan_change_requests.reason IS '原因';
COMMENT ON COLUMN care_plan_change_requests.requested_by IS '申请人ID';
COMMENT ON COLUMN care_plan_change_requests.requested_at IS '申请时间';
COMMENT ON COLUMN care_plan_change_requests.status IS '状态';
COMMENT ON COLUMN care_plan_change_requests.reviewed_by IS '审核人ID';
COMMENT ON COLUMN care_plan_change_requests.reviewed_at IS '审核时间';
COMMENT ON COLUMN care_plan_change_requests.review_comment IS '审核意见';
COMMENT ON COLUMN care_plan_change_requests.created_at IS '创建时间';
COMMENT ON COLUMN care_plan_change_requests.updated_at IS '更新时间';
COMMENT ON TABLE care_plan_tasks IS '护理计划任务表';
COMMENT ON COLUMN care_plan_tasks.task_id IS '任务ID';
COMMENT ON COLUMN care_plan_tasks.care_plan_id IS '护理计划ID';
COMMENT ON COLUMN care_plan_tasks.elder_id IS '老人ID';
COMMENT ON COLUMN care_plan_tasks.assigned_nurse_id IS '分配护士ID';
COMMENT ON COLUMN care_plan_tasks.task_type IS '任务类型';
COMMENT ON COLUMN care_plan_tasks.task_title IS '任务标题';
COMMENT ON COLUMN care_plan_tasks.task_content IS '任务内容';
COMMENT ON COLUMN care_plan_tasks.frequency_desc IS '频率描述';
COMMENT ON COLUMN care_plan_tasks.suggested_time IS '建议执行时间';
COMMENT ON COLUMN care_plan_tasks.scheduled_date IS '计划执行日期';
COMMENT ON COLUMN care_plan_tasks.scheduled_time IS '计划执行时间';
COMMENT ON COLUMN care_plan_tasks.scheduled_at IS '计划执行时间点';
COMMENT ON COLUMN care_plan_tasks.task_source IS '任务来源';
COMMENT ON COLUMN care_plan_tasks.task_group_key IS '任务分组键';
COMMENT ON COLUMN care_plan_tasks.status IS '状态';
COMMENT ON COLUMN care_plan_tasks.execution_result IS '执行结果';
COMMENT ON COLUMN care_plan_tasks.executed_at IS '执行时间';
COMMENT ON COLUMN care_plan_tasks.created_at IS '创建时间';
COMMENT ON COLUMN care_plan_tasks.updated_at IS '更新时间';
COMMENT ON TABLE tasks IS '通用任务表';
COMMENT ON COLUMN tasks.task_id IS '任务ID';
COMMENT ON COLUMN tasks.elder_id IS '老人ID';
COMMENT ON COLUMN tasks.task_type IS '任务类型';
COMMENT ON COLUMN tasks.title IS '标题';
COMMENT ON COLUMN tasks.description IS '描述';
COMMENT ON COLUMN tasks.priority IS '优先级';
COMMENT ON COLUMN tasks.status IS '状态';
COMMENT ON COLUMN tasks.scheduled_at IS '计划执行时间点';
COMMENT ON COLUMN tasks.due_at IS '截止时间';
COMMENT ON COLUMN tasks.assigned_to IS '分配给用户ID';
COMMENT ON COLUMN tasks.created_by IS '创建人ID';
COMMENT ON COLUMN tasks.completed_by IS '完成人ID';
COMMENT ON COLUMN tasks.completed_at IS '完成时间';
COMMENT ON COLUMN tasks.related_biz_type IS '关联业务类型';
COMMENT ON COLUMN tasks.related_biz_id IS '关联业务ID';
COMMENT ON COLUMN tasks.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN tasks.wf_task_id IS '工作流任务ID';
COMMENT ON COLUMN tasks.created_at IS '创建时间';
COMMENT ON COLUMN tasks.updated_at IS '更新时间';
COMMENT ON TABLE messages IS '站内消息表';
COMMENT ON COLUMN messages.message_id IS '消息ID';
COMMENT ON COLUMN messages.elder_id IS '老人ID';
COMMENT ON COLUMN messages.sender_id IS '发送人ID';
COMMENT ON COLUMN messages.receiver_id IS '接收人ID';
COMMENT ON COLUMN messages.content_type IS '内容类型';
COMMENT ON COLUMN messages.content IS '内容';
COMMENT ON COLUMN messages.is_read IS '是否已读';
COMMENT ON COLUMN messages.created_at IS '创建时间';
COMMENT ON TABLE activities IS '活动表';
COMMENT ON COLUMN activities.activity_id IS '活动ID';
COMMENT ON COLUMN activities.title IS '标题';
COMMENT ON COLUMN activities.description IS '描述';
COMMENT ON COLUMN activities.activity_time IS '活动时间';
COMMENT ON COLUMN activities.location IS '地点';
COMMENT ON COLUMN activities.created_by IS '创建人ID';
COMMENT ON COLUMN activities.created_at IS '创建时间';
COMMENT ON TABLE activity_participants IS '活动参与记录表';
COMMENT ON COLUMN activity_participants.id IS '主键ID';
COMMENT ON COLUMN activity_participants.activity_id IS '活动ID';
COMMENT ON COLUMN activity_participants.elder_id IS '老人ID';
COMMENT ON COLUMN activity_participants.status IS '状态';
COMMENT ON COLUMN activity_participants.created_at IS '创建时间';
COMMENT ON TABLE supply_items IS '物资品项表';
COMMENT ON COLUMN supply_items.supply_item_id IS '物资品项ID';
COMMENT ON COLUMN supply_items.item_name IS '项目名称';
COMMENT ON COLUMN supply_items.category IS '分类';
COMMENT ON COLUMN supply_items.unit IS '单位';
COMMENT ON COLUMN supply_items.is_active IS '是否启用';
COMMENT ON COLUMN supply_items.created_at IS '创建时间';
COMMENT ON TABLE supply_stocks IS '物资库存表';
COMMENT ON COLUMN supply_stocks.stock_id IS '库存ID';
COMMENT ON COLUMN supply_stocks.supply_item_id IS '物资品项ID';
COMMENT ON COLUMN supply_stocks.quantity IS '数量';
COMMENT ON COLUMN supply_stocks.min_threshold IS '最低库存阈值';
COMMENT ON COLUMN supply_stocks.location IS '地点';
COMMENT ON COLUMN supply_stocks.updated_at IS '更新时间';
COMMENT ON TABLE supply_issue_records IS '物资领用记录表';
COMMENT ON COLUMN supply_issue_records.issue_id IS '问题ID';
COMMENT ON COLUMN supply_issue_records.supply_item_id IS '物资品项ID';
COMMENT ON COLUMN supply_issue_records.quantity IS '数量';
COMMENT ON COLUMN supply_issue_records.issued_to IS '领用对象ID';
COMMENT ON COLUMN supply_issue_records.issued_by IS '发放人ID';
COMMENT ON COLUMN supply_issue_records.issue_time IS '领用时间';
COMMENT ON COLUMN supply_issue_records.note IS '备注';
COMMENT ON COLUMN supply_issue_records.related_task_id IS '关联任务ID';
COMMENT ON TABLE notifications IS '通知表';
COMMENT ON COLUMN notifications.notification_id IS '通知ID';
COMMENT ON COLUMN notifications.to_user_id IS '接收用户ID';
COMMENT ON COLUMN notifications.title IS '标题';
COMMENT ON COLUMN notifications.content IS '内容';
COMMENT ON COLUMN notifications.notif_type IS '通知类型';
COMMENT ON COLUMN notifications.biz_type IS '业务类型';
COMMENT ON COLUMN notifications.biz_id IS '业务ID';
COMMENT ON COLUMN notifications.is_read IS '是否已读';
COMMENT ON COLUMN notifications.created_at IS '创建时间';
COMMENT ON COLUMN notifications.read_at IS '阅读时间';
COMMENT ON TABLE audit_log IS '审计日志表';
COMMENT ON COLUMN audit_log.log_id IS '日志ID';
COMMENT ON COLUMN audit_log.user_id IS '用户ID';
COMMENT ON COLUMN audit_log.action IS '操作类型';
COMMENT ON COLUMN audit_log.entity_type IS '实体类型';
COMMENT ON COLUMN audit_log.entity_id IS '实体ID';
COMMENT ON COLUMN audit_log.detail_json IS '详情JSON';
COMMENT ON COLUMN audit_log.ip IS 'IP地址';
COMMENT ON COLUMN audit_log.user_agent IS '用户代理';
COMMENT ON COLUMN audit_log.created_at IS '创建时间';
COMMENT ON TABLE medications IS '药品表';
COMMENT ON COLUMN medications.medication_id IS '药品ID';
COMMENT ON COLUMN medications.medication_name IS '药品名称';
COMMENT ON COLUMN medications.spec IS '规格';
COMMENT ON COLUMN medications.unit IS '单位';
COMMENT ON COLUMN medications.description IS '描述';
COMMENT ON COLUMN medications.created_at IS '创建时间';
COMMENT ON TABLE fee_items IS '收费项目表';
COMMENT ON COLUMN fee_items.fee_item_id IS '收费项目ID';
COMMENT ON COLUMN fee_items.item_name IS '项目名称';
COMMENT ON COLUMN fee_items.category IS '分类';
COMMENT ON COLUMN fee_items.unit IS '单位';
COMMENT ON COLUMN fee_items.unit_price IS '单价';
COMMENT ON COLUMN fee_items.is_active IS '是否启用';
COMMENT ON COLUMN fee_items.created_at IS '创建时间';
COMMENT ON TABLE bills IS '账单表';
COMMENT ON COLUMN bills.bill_id IS '账单ID';
COMMENT ON COLUMN bills.elder_id IS '老人ID';
COMMENT ON COLUMN bills.period_start IS '账期开始日期';
COMMENT ON COLUMN bills.period_end IS '账期结束日期';
COMMENT ON COLUMN bills.total_amount IS '总金额';
COMMENT ON COLUMN bills.status IS '状态';
COMMENT ON COLUMN bills.generated_at IS '生成时间';
COMMENT ON COLUMN bills.due_date IS '到期日期';
COMMENT ON COLUMN bills.created_by IS '创建人ID';
COMMENT ON TABLE bill_items IS '账单明细表';
COMMENT ON COLUMN bill_items.bill_item_id IS '账单明细ID';
COMMENT ON COLUMN bill_items.bill_id IS '账单ID';
COMMENT ON COLUMN bill_items.fee_item_id IS '收费项目ID';
COMMENT ON COLUMN bill_items.quantity IS '数量';
COMMENT ON COLUMN bill_items.unit_price IS '单价';
COMMENT ON COLUMN bill_items.amount IS '数量';
COMMENT ON COLUMN bill_items.note IS '备注';
COMMENT ON TABLE payments IS '缴费记录表';
COMMENT ON COLUMN payments.payment_id IS '缴费ID';
COMMENT ON COLUMN payments.bill_id IS '账单ID';
COMMENT ON COLUMN payments.amount IS '数量';
COMMENT ON COLUMN payments.method IS '支付方式';
COMMENT ON COLUMN payments.transaction_no IS '交易流水号';
COMMENT ON COLUMN payments.status IS '状态';
COMMENT ON COLUMN payments.paid_at IS '支付时间';
COMMENT ON COLUMN payments.created_at IS '创建时间';
COMMENT ON TABLE medication_plans IS '用药计划表';
COMMENT ON COLUMN medication_plans.plan_id IS '计划ID';
COMMENT ON COLUMN medication_plans.elder_id IS '老人ID';
COMMENT ON COLUMN medication_plans.medication_id IS '药品ID';
COMMENT ON COLUMN medication_plans.dosage IS '剂量';
COMMENT ON COLUMN medication_plans.frequency IS '频次';
COMMENT ON COLUMN medication_plans.times_json IS '服药时间JSON';
COMMENT ON COLUMN medication_plans.medications_json IS '药品明细JSON';
COMMENT ON COLUMN medication_plans.start_date IS '开始日期';
COMMENT ON COLUMN medication_plans.end_date IS '结束日期';
COMMENT ON COLUMN medication_plans.status IS '状态';
COMMENT ON COLUMN medication_plans.created_by IS '创建人ID';
COMMENT ON COLUMN medication_plans.created_at IS '创建时间';
COMMENT ON COLUMN medication_plans.updated_at IS '更新时间';
COMMENT ON TABLE medication_admin_records IS '用药执行记录表';
COMMENT ON COLUMN medication_admin_records.record_id IS '记录ID';
COMMENT ON COLUMN medication_admin_records.elder_id IS '老人ID';
COMMENT ON COLUMN medication_admin_records.medication_id IS '药品ID';
COMMENT ON COLUMN medication_admin_records.plan_id IS '计划ID';
COMMENT ON COLUMN medication_admin_records.administered_time IS '执行时间';
COMMENT ON COLUMN medication_admin_records.administered_by IS '执行人ID';
COMMENT ON COLUMN medication_admin_records.status IS '状态';
COMMENT ON COLUMN medication_admin_records.dosage IS '剂量';
COMMENT ON COLUMN medication_admin_records.note IS '备注';
COMMENT ON COLUMN medication_admin_records.created_at IS '创建时间';
COMMENT ON TABLE meal_intake_records IS '膳食摄入记录表';
COMMENT ON COLUMN meal_intake_records.meal_id IS '膳食记录ID';
COMMENT ON COLUMN meal_intake_records.elder_id IS '老人ID';
COMMENT ON COLUMN meal_intake_records.meal_type IS '餐次';
COMMENT ON COLUMN meal_intake_records.intake_ratio IS '摄入比例';
COMMENT ON COLUMN meal_intake_records.diet_type IS '饮食类型';
COMMENT ON COLUMN meal_intake_records.note IS '备注';
COMMENT ON COLUMN meal_intake_records.recorded_by IS '记录人ID';
COMMENT ON COLUMN meal_intake_records.record_time IS '记录时间';
COMMENT ON COLUMN meal_intake_records.created_at IS '创建时间';
COMMENT ON TABLE fluid_intake_records IS '饮水摄入记录表';
COMMENT ON COLUMN fluid_intake_records.fluid_id IS '饮水记录ID';
COMMENT ON COLUMN fluid_intake_records.elder_id IS '老人ID';
COMMENT ON COLUMN fluid_intake_records.drink_type IS '饮品类型';
COMMENT ON COLUMN fluid_intake_records.volume_ml IS '饮水量毫升';
COMMENT ON COLUMN fluid_intake_records.note IS '备注';
COMMENT ON COLUMN fluid_intake_records.recorded_by IS '记录人ID';
COMMENT ON COLUMN fluid_intake_records.record_time IS '记录时间';
COMMENT ON COLUMN fluid_intake_records.created_at IS '创建时间';
COMMENT ON TABLE bowel_records IS '排便记录表';
COMMENT ON COLUMN bowel_records.bowel_id IS '排便记录ID';
COMMENT ON COLUMN bowel_records.elder_id IS '老人ID';
COMMENT ON COLUMN bowel_records.bristol_type IS '布里斯托分型';
COMMENT ON COLUMN bowel_records.amount IS '数量';
COMMENT ON COLUMN bowel_records.incontinence IS '是否失禁';
COMMENT ON COLUMN bowel_records.blood_flag IS '是否带血';
COMMENT ON COLUMN bowel_records.note IS '备注';
COMMENT ON COLUMN bowel_records.recorded_by IS '记录人ID';
COMMENT ON COLUMN bowel_records.record_time IS '记录时间';
COMMENT ON COLUMN bowel_records.created_at IS '创建时间';
COMMENT ON TABLE weight_records IS '体重记录表';
COMMENT ON COLUMN weight_records.weight_id IS '体重记录ID';
COMMENT ON COLUMN weight_records.elder_id IS '老人ID';
COMMENT ON COLUMN weight_records.weight_kg IS '体重千克';
COMMENT ON COLUMN weight_records.measure_ctx IS '测量场景';
COMMENT ON COLUMN weight_records.note IS '备注';
COMMENT ON COLUMN weight_records.recorded_by IS '记录人ID';
COMMENT ON COLUMN weight_records.record_time IS '记录时间';
COMMENT ON COLUMN weight_records.created_at IS '创建时间';
COMMENT ON TABLE vital_sign_records IS '生命体征记录表';
COMMENT ON COLUMN vital_sign_records.vital_id IS '生命体征记录ID';
COMMENT ON COLUMN vital_sign_records.elder_id IS '老人ID';
COMMENT ON COLUMN vital_sign_records.record_time IS '记录时间';
COMMENT ON COLUMN vital_sign_records.heart_rate IS '心率';
COMMENT ON COLUMN vital_sign_records.systolic_bp IS '收缩压';
COMMENT ON COLUMN vital_sign_records.diastolic_bp IS '舒张压';
COMMENT ON COLUMN vital_sign_records.spo2 IS '血氧饱和度';
COMMENT ON COLUMN vital_sign_records.temperature IS '体温';
COMMENT ON COLUMN vital_sign_records.blood_glucose IS '血糖';
COMMENT ON COLUMN vital_sign_records.source IS '来源';
COMMENT ON COLUMN vital_sign_records.device_type IS '设备类型';
COMMENT ON COLUMN vital_sign_records.device_id IS '设备ID';
COMMENT ON COLUMN vital_sign_records.device_name IS '设备名称';
COMMENT ON COLUMN vital_sign_records.recorded_by IS '记录人ID';
COMMENT ON COLUMN vital_sign_records.note IS '备注';
COMMENT ON COLUMN vital_sign_records.created_at IS '创建时间';
COMMENT ON TABLE qc_audits IS '质控检查表';
COMMENT ON COLUMN qc_audits.audit_id IS '质控检查ID';
COMMENT ON COLUMN qc_audits.elder_id IS '老人ID';
COMMENT ON COLUMN qc_audits.title IS '标题';
COMMENT ON COLUMN qc_audits.status IS '状态';
COMMENT ON COLUMN qc_audits.created_by IS '创建人ID';
COMMENT ON COLUMN qc_audits.created_at IS '创建时间';
COMMENT ON COLUMN qc_audits.updated_at IS '更新时间';
COMMENT ON TABLE qc_audit_items IS '质控检查项目表';
COMMENT ON COLUMN qc_audit_items.item_id IS '检查项目ID';
COMMENT ON COLUMN qc_audit_items.audit_id IS '质控检查ID';
COMMENT ON COLUMN qc_audit_items.item_code IS '项目编码';
COMMENT ON COLUMN qc_audit_items.item_name IS '项目名称';
COMMENT ON COLUMN qc_audit_items.result IS '检查结果';
COMMENT ON COLUMN qc_audit_items.issues IS '问题描述';
COMMENT ON COLUMN qc_audit_items.evidence_json IS '佐证材料JSON';
COMMENT ON COLUMN qc_audit_items.checked_by IS '检查人ID';
COMMENT ON COLUMN qc_audit_items.checked_at IS '检查时间';
COMMENT ON COLUMN qc_audit_items.updated_at IS '更新时间';
COMMENT ON TABLE qc_issues IS '质控问题表';
COMMENT ON COLUMN qc_issues.issue_id IS '问题ID';
COMMENT ON COLUMN qc_issues.qc_item_id IS '质控项目ID';
COMMENT ON COLUMN qc_issues.audit_id IS '质控检查ID';
COMMENT ON COLUMN qc_issues.elder_id IS '老人ID';
COMMENT ON COLUMN qc_issues.level IS '级别';
COMMENT ON COLUMN qc_issues.description IS '描述';
COMMENT ON COLUMN qc_issues.responsible_id IS '责任人ID';
COMMENT ON COLUMN qc_issues.status IS '状态';
COMMENT ON COLUMN qc_issues.rectification_id IS '整改ID';
COMMENT ON COLUMN qc_issues.created_by IS '创建人ID';
COMMENT ON COLUMN qc_issues.created_at IS '创建时间';
COMMENT ON COLUMN qc_issues.updated_at IS '更新时间';
COMMENT ON TABLE buildings IS '楼栋表';
COMMENT ON COLUMN buildings.building_id IS '楼栋ID';
COMMENT ON COLUMN buildings.building_name IS '楼栋名称';
COMMENT ON COLUMN buildings.deleted_at IS '删除时间';
COMMENT ON TABLE floors IS '楼层表';
COMMENT ON COLUMN floors.floor_id IS '楼层ID';
COMMENT ON COLUMN floors.building_id IS '楼栋ID';
COMMENT ON COLUMN floors.floor_no IS '楼层号';
COMMENT ON COLUMN floors.floor_name IS '楼层名称';
COMMENT ON COLUMN floors.deleted_at IS '删除时间';
COMMENT ON TABLE rooms IS '房间表';
COMMENT ON COLUMN rooms.room_id IS '房间ID';
COMMENT ON COLUMN rooms.floor_id IS '楼层ID';
COMMENT ON COLUMN rooms.room_no IS '房间号';
COMMENT ON COLUMN rooms.room_number IS '房间编号';
COMMENT ON COLUMN rooms.room_type IS '房间类型';
COMMENT ON COLUMN rooms.note IS '备注';
COMMENT ON COLUMN rooms.status IS '状态';
COMMENT ON TABLE beds IS '床位表';
COMMENT ON COLUMN beds.bed_id IS '床位ID';
COMMENT ON COLUMN beds.room_id IS '房间ID';
COMMENT ON COLUMN beds.bed_no IS '床位号';
COMMENT ON COLUMN beds.bed_code IS '床位编码';
COMMENT ON COLUMN beds.status IS '状态';
COMMENT ON COLUMN beds.deleted_at IS '删除时间';
COMMENT ON TABLE admission_records IS '入住记录表';
COMMENT ON COLUMN admission_records.admission_id IS '入住记录ID';
COMMENT ON COLUMN admission_records.elder_id IS '老人ID';
COMMENT ON COLUMN admission_records.bed_id IS '床位ID';
COMMENT ON COLUMN admission_records.contract_no IS '合同编号';
COMMENT ON COLUMN admission_records.package_name IS '套餐名称';
COMMENT ON COLUMN admission_records.contract_file_url IS '合同文件地址';
COMMENT ON COLUMN admission_records.deposit_amount IS '押金金额';
COMMENT ON COLUMN admission_records.start_date IS '开始日期';
COMMENT ON COLUMN admission_records.end_date IS '结束日期';
COMMENT ON COLUMN admission_records.status IS '状态';
COMMENT ON COLUMN admission_records.created_by IS '创建人ID';
COMMENT ON COLUMN admission_records.created_at IS '创建时间';
COMMENT ON COLUMN admission_records.updated_at IS '更新时间';
COMMENT ON COLUMN admission_records.process_instance_id IS '流程实例ID';
COMMENT ON TABLE discharge_records IS '退住记录表';
COMMENT ON COLUMN discharge_records.discharge_id IS '退住记录ID';
COMMENT ON COLUMN discharge_records.admission_id IS '入住记录ID';
COMMENT ON COLUMN discharge_records.elder_id IS '老人ID';
COMMENT ON COLUMN discharge_records.bed_id IS '床位ID';
COMMENT ON COLUMN discharge_records.status IS '状态';
COMMENT ON COLUMN discharge_records.reason IS '原因';
COMMENT ON COLUMN discharge_records.requested_date IS '申请日期';
COMMENT ON COLUMN discharge_records.actual_date IS '实际日期';
COMMENT ON COLUMN discharge_records.settlement_amount IS '结算金额';
COMMENT ON COLUMN discharge_records.refund_amount IS '退款金额';
COMMENT ON COLUMN discharge_records.created_by IS '创建人ID';
COMMENT ON COLUMN discharge_records.created_at IS '创建时间';
COMMENT ON COLUMN discharge_records.updated_at IS '更新时间';
COMMENT ON TABLE rectifications IS '整改任务表';
COMMENT ON COLUMN rectifications.rectification_id IS '整改ID';
COMMENT ON COLUMN rectifications.source_type IS '来源类型';
COMMENT ON COLUMN rectifications.source_id IS '来源ID';
COMMENT ON COLUMN rectifications.title IS '标题';
COMMENT ON COLUMN rectifications.description IS '描述';
COMMENT ON COLUMN rectifications.level IS '级别';
COMMENT ON COLUMN rectifications.owner_id IS '负责人ID';
COMMENT ON COLUMN rectifications.due_at IS '截止时间';
COMMENT ON COLUMN rectifications.status IS '状态';
COMMENT ON COLUMN rectifications.created_by IS '创建人ID';
COMMENT ON COLUMN rectifications.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN rectifications.created_at IS '创建时间';
COMMENT ON COLUMN rectifications.updated_at IS '更新时间';
COMMENT ON TABLE rectification_actions IS '整改处理记录表';
COMMENT ON COLUMN rectification_actions.action_id IS '操作ID';
COMMENT ON COLUMN rectification_actions.rectification_id IS '整改ID';
COMMENT ON COLUMN rectification_actions.action_type IS '处理类型';
COMMENT ON COLUMN rectification_actions.actor_id IS '操作人ID';
COMMENT ON COLUMN rectification_actions.action_time IS '操作时间';
COMMENT ON COLUMN rectification_actions.content IS '内容';
COMMENT ON COLUMN rectification_actions.attachments_json IS '附件JSON';
COMMENT ON COLUMN rectification_actions.extra_json IS '扩展信息JSON';
