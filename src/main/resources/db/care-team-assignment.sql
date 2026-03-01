CREATE TABLE IF NOT EXISTS care_team_assignment (
  assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  nurse_id BIGINT NULL,
  family_id BIGINT NULL,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_care_team_nurse_active (nurse_id, is_active),
  INDEX idx_care_team_family_active (family_id, is_active),
  INDEX idx_care_team_elder_active (elder_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
