-- ============================================================
-- ChronoClass — Database Schema V1
-- Copyright (c) 2026 Karan Jha. All rights reserved.
-- ============================================================

-- Courses table
CREATE TABLE courses (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Offerings table (a schedulable section of a course)
CREATE TABLE offerings (
    id                  BIGSERIAL       PRIMARY KEY,
    course_id           BIGINT          NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id          BIGINT          NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    teacher_timezone    VARCHAR(64)     NOT NULL,
    max_students        INT             NOT NULL DEFAULT 30,
    enrolled_count      INT             NOT NULL DEFAULT 0,
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_max_students_positive CHECK (max_students > 0),
    CONSTRAINT chk_enrolled_non_negative CHECK (enrolled_count >= 0),
    CONSTRAINT chk_enrolled_within_max CHECK (enrolled_count <= max_students),
    CONSTRAINT chk_offering_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED'))
);

-- Sessions table (actual meeting times)
CREATE TABLE sessions (
    id              BIGSERIAL       PRIMARY KEY,
    offering_id     BIGINT          NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    start_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_session_time_order CHECK (end_time > start_time)
);

-- Bookings table
CREATE TABLE bookings (
    id              BIGSERIAL       PRIMARY KEY,
    offering_id     BIGINT          NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    parent_id       BIGINT          NOT NULL,
    parent_timezone VARCHAR(64)     NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'CONFIRMED',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT uq_parent_offering UNIQUE (parent_id, offering_id)
);

-- ============================================================
-- Indexes for query performance
-- ============================================================

CREATE INDEX idx_offerings_teacher_id ON offerings(teacher_id);
CREATE INDEX idx_offerings_course_id ON offerings(course_id);
CREATE INDEX idx_offerings_status ON offerings(status);

CREATE INDEX idx_sessions_offering_id ON sessions(offering_id);
CREATE INDEX idx_sessions_start_time ON sessions(start_time);
CREATE INDEX idx_sessions_end_time ON sessions(end_time);
CREATE INDEX idx_sessions_time_range ON sessions(start_time, end_time);

CREATE INDEX idx_bookings_parent_id ON bookings(parent_id);
CREATE INDEX idx_bookings_offering_id ON bookings(offering_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_parent_status ON bookings(parent_id, status);
