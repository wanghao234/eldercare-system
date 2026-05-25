ALTER TABLE vital_sign_records
  ADD COLUMN device_type VARCHAR(32) NULL AFTER source,
  ADD COLUMN device_id VARCHAR(128) NULL AFTER device_type,
  ADD COLUMN device_name VARCHAR(128) NULL AFTER device_id;
