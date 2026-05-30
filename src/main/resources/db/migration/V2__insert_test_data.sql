-- ============================================================
-- ChronoClass — Test Data Initializer V2 (Auto-ID Generation)
-- ============================================================

-- Insert Courses (IDs will be auto-generated as 1, 2, 3)
INSERT INTO courses (name, description) VALUES
('Python Coding', 'Learn Python from scratch'),
('Minecraft Coding', 'Learn to mod Minecraft'),
('Public Speaking', 'Build confidence in public speaking');

-- Insert Offerings (IDs will be auto-generated as 1, 2, 3)
-- Teacher 1 (America/New_York)
INSERT INTO offerings (course_id, teacher_id, title, teacher_timezone, max_students, enrolled_count, status) VALUES
(1, 1, 'Saturday Batch', 'America/New_York', 20, 0, 'ACTIVE'),
(2, 1, 'Summer Camp', 'America/New_York', 15, 0, 'ACTIVE');

-- Teacher 2 (Europe/London)
INSERT INTO offerings (course_id, teacher_id, title, teacher_timezone, max_students, enrolled_count, status) VALUES
(3, 2, 'Evening Batch', 'Europe/London', 30, 0, 'ACTIVE');

-- Insert Sessions (Dates in June 2026 for future)
-- Offering 1: Saturday Batch (Weekly)
INSERT INTO sessions (offering_id, start_time, end_time) VALUES
(1, '2026-06-06 22:00:00+00', '2026-06-06 23:00:00+00'), -- 6 PM NY time is 10 PM UTC (EDT UTC-4)
(1, '2026-06-13 22:00:00+00', '2026-06-13 23:00:00+00'),
(1, '2026-06-20 22:00:00+00', '2026-06-20 23:00:00+00');

-- Offering 2: Summer Camp (Daily)
INSERT INTO sessions (offering_id, start_time, end_time) VALUES
(2, '2026-07-06 21:00:00+00', '2026-07-06 22:00:00+00'), -- 5 PM NY time is 9 PM UTC
(2, '2026-07-07 21:00:00+00', '2026-07-07 22:00:00+00'),
(2, '2026-07-08 21:00:00+00', '2026-07-08 22:00:00+00');

-- Offering 3: Evening Batch
INSERT INTO sessions (offering_id, start_time, end_time) VALUES
(3, '2026-06-01 17:00:00+00', '2026-06-01 18:00:00+00'), -- 6 PM London time is 5 PM UTC (BST UTC+1)
(3, '2026-06-08 17:00:00+00', '2026-06-08 18:00:00+00');
