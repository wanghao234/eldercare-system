ALTER TABLE admission_records
  ADD COLUMN contract_file_url VARCHAR(255) NULL AFTER package_name;
