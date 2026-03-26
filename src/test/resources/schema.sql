DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS elder_profile;
DROP TABLE IF EXISTS staff_profile;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS care_team_assignment;
DROP TABLE IF EXISTS alarm_action_logs;
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
  elder_id BIGINT NOT NULL,
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
  process_instance_id VARCHAR(128)
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

CREATE TABLE wf_definitions (
  definition_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_key VARCHAR(64) NOT NULL,
  process_name VARCHAR(128),
  version INT,
  status VARCHAR(32),
  definition_json TEXT,
  created_at DATETIME,
  updated_at DATETIME
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
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE wf_tasks (
  wf_task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instance_id BIGINT NOT NULL,
  node_key VARCHAR(64) NOT NULL,
  node_name VARCHAR(128),
  assignee_id BIGINT,
  candidate_role VARCHAR(32),
  status VARCHAR(32) NOT NULL,
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
  care_time VARCHAR(64),
  care_content TEXT,
  medication_reminder TEXT,
  diet_plan TEXT,
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
