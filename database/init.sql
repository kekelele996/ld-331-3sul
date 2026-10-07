CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor VARCHAR(80) NOT NULL,
  action VARCHAR(120) NOT NULL,
  target VARCHAR(120) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schedule_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department VARCHAR(80) NOT NULL,
  schedule_date DATE NOT NULL,
  shift_name VARCHAR(40) NOT NULL,
  staff_name VARCHAR(80) NOT NULL,
  INDEX idx_schedule_staff_date (staff_name, schedule_date)
);

-- 护士长手动换班调整记录（留痕可追溯，也是一键生成时保留班次的权威依据）
CREATE TABLE IF NOT EXISTS schedule_adjustment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department VARCHAR(80) NOT NULL,
  schedule_date DATE NOT NULL,
  staff_name VARCHAR(80) NOT NULL,
  old_shift VARCHAR(40) NOT NULL,
  new_shift VARCHAR(40) NOT NULL,
  reason VARCHAR(200) DEFAULT NULL,
  operator VARCHAR(80) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dept_date_staff (department, schedule_date, staff_name),
  INDEX idx_adjustment_staff (staff_name)
);

INSERT INTO schedule_snapshot (department, schedule_date, shift_name, staff_name)
VALUES ('急诊科', CURDATE(), '白班', '陈医生'), ('急诊科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '夜班', '周护士');
