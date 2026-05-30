# ChronoClass — Global Class Offering Booking System

> **⚠️ NOTICE**: This software is submitted solely for technical evaluation purposes as part of a hiring assessment. It is NOT licensed for commercial use, production deployment, or redistribution. See [LICENSE](LICENSE) for details.

## 📋 Project Overview

ChronoClass is a production-ready backend service for a **global live-learning platform** where teachers conduct online classes for students across different countries and timezones.

### Core Concepts

| Concept | Description | Example |
|---------|-------------|---------|
| **Course** | A subject/class | Python Coding, Art Drawing |
| **Offering** | A schedulable section/batch of a course | Saturday Batch, Summer Camp |
| **Session** | An actual meeting time within an offering | June 7, 6 PM–7 PM |
| **Booking** | A parent's reservation of an entire offering | All 8 sessions of Saturday Batch |

### Key Features
- ✅ Teachers create offerings and add sessions in their own timezone
- ✅ Parents view schedules in their local timezone
- ✅ Booking happens at the offering level (all sessions together)
- ✅ Time conflict detection across all booked sessions
- ✅ Concurrency-safe booking with pessimistic locking
- ✅ Capacity management with overbooking prevention
- ✅ Comprehensive error handling
- ✅ Swagger/OpenAPI documentation

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |
| Containerization | Docker Compose |
| Testing | JUnit 5, H2 (in-memory) |

---

## 🚀 Setup Instructions

### Prerequisites
- Java 17+ (JDK)
- Maven 3.8+
- Docker & Docker Compose (for PostgreSQL)
- OR a local PostgreSQL 16 instance

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd ChronoClass
```

### Step 2: Start PostgreSQL (via Docker)
```bash
docker-compose up -d
```
This starts PostgreSQL on port `5432` with:
- Database: `chronoclass`
- Username: `chronoclass`
- Password: `chronoclass123`

### Step 3: Run the Application
```bash
mvn spring-boot:run
```
The application starts on `http://localhost:8080`.

### Step 4: Access API Documentation
Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🔧 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/chronoclass` | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | `chronoclass` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `chronoclass123` | Database password |
| `SERVER_PORT` | `8080` | Application port |

Override via command line:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://host:5432/db"
```

---

## 📡 API Documentation

### Teacher APIs

#### 1. Create Offering
```
POST /api/v1/teachers/{teacherId}/offerings
```
**Request Body:**
```json
{
  "courseName": "Python Coding",
  "courseDescription": "Learn Python from scratch",
  "title": "Saturday Batch",
  "teacherTimezone": "America/New_York",
  "maxStudents": 30
}
```

#### 2. Add Sessions to Offering
```
POST /api/v1/offerings/{offeringId}/sessions
```
**Request Body** (times in teacher's timezone):
```json
{
  "sessions": [
    { "startTime": "2026-06-07T18:00:00", "endTime": "2026-06-07T19:00:00" },
    { "startTime": "2026-06-14T18:00:00", "endTime": "2026-06-14T19:00:00" },
    { "startTime": "2026-06-21T18:00:00", "endTime": "2026-06-21T19:00:00" }
  ]
}
```

#### 3. Get Teacher's Offerings
```
GET /api/v1/teachers/{teacherId}/offerings
```

#### 4. Get Offering Details
```
GET /api/v1/offerings/{offeringId}?timezone=Asia/Kolkata
```

### Parent APIs

#### 5. Get Available Offerings
```
GET /api/v1/offerings?timezone=Asia/Kolkata
```
Session times are automatically converted to the parent's timezone.

#### 6. Book an Offering
```
POST /api/v1/parents/{parentId}/bookings
```
**Request Body:**
```json
{
  "offeringId": 1,
  "parentTimezone": "Asia/Kolkata"
}
```

#### 7. Get Parent's Bookings
```
GET /api/v1/parents/{parentId}/bookings?timezone=Asia/Kolkata
```

---

## 🗄️ Database Schema

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│   courses    │      │    offerings     │      │   sessions   │
├──────────────┤      ├──────────────────┤      ├──────────────┤
│ id (PK)      │──1:N─│ id (PK)          │──1:N─│ id (PK)      │
│ name         │      │ course_id (FK)   │      │ offering_id  │
│ description  │      │ teacher_id       │      │ start_time   │
│ created_at   │      │ title            │      │ end_time     │
│ updated_at   │      │ teacher_timezone │      │ created_at   │
└──────────────┘      │ max_students     │      └──────────────┘
                      │ enrolled_count   │
                      │ status           │      ┌──────────────┐
                      │ version          │      │   bookings   │
                      │ created_at       │      ├──────────────┤
                      │ updated_at       │──1:N─│ id (PK)      │
                      └──────────────────┘      │ offering_id  │
                                                │ parent_id    │
                                                │ parent_tz    │
                                                │ status       │
                                                │ version      │
                                                │ created_at   │
                                                └──────────────┘
```

### Key Design Decisions
- **All times stored in UTC** (`TIMESTAMP WITH TIME ZONE`) — prevents timezone bugs
- **`teacher_timezone`** on Offering — for correct display in teacher's local time
- **`parent_timezone`** on Booking — for correct display in parent's local time
- **`version` column** — enables optimistic locking for concurrent updates
- **`enrolled_count`** — denormalized for fast capacity checks, updated atomically
- **Unique constraint** `(parent_id, offering_id)` — prevents duplicate bookings at DB level

---

## ⚡ Concurrency Handling Approach

### Problem
Multiple parents may attempt to book the same offering simultaneously, or a parent may try to book overlapping offerings through concurrent requests.

### Solution: Two-Layer Locking Strategy

#### Layer 1: Pessimistic Locking (Primary)
During the booking flow, we acquire `SELECT ... FOR UPDATE` locks on:
1. The **target offering** row — serializes capacity checks
2. The **parent's existing booked sessions** — serializes conflict detection
3. The **target offering's sessions** — prevents modification during conflict check

This ensures that within a single transaction:
- Only one thread can check and modify capacity at a time
- Only one thread can check and create bookings for a parent at a time

#### Layer 2: Database Constraints (Safety Net)
- `UNIQUE(parent_id, offering_id)` — prevents duplicate bookings even if application logic fails
- `CHECK(enrolled_count <= max_students)` — prevents overbooking at the database level
- `@Version` (optimistic locking) — catches any missed concurrent updates

### Booking Flow
```
1. BEGIN TRANSACTION
2. SELECT offering FOR UPDATE  ← Lock the offering row
3. Check: already booked? capacity available? offering active?
4. SELECT target sessions FOR UPDATE  ← Lock target sessions
5. SELECT parent's booked sessions FOR UPDATE  ← Lock existing bookings
6. Check: any time overlaps between target and booked sessions?
7. If conflicts → ROLLBACK + throw TimeConflictException
8. If clear → INSERT booking, UPDATE enrolled_count
9. COMMIT
```

---

## 🌍 Timezone Handling Approach

### Strategy: Store UTC, Display Local

1. **Teacher creates sessions** → provides times in their local timezone (e.g., `America/New_York`)
2. **Application converts to UTC** → `LocalDateTime` + `ZoneId` → `Instant` (UTC)
3. **Database stores UTC** → `TIMESTAMP WITH TIME ZONE` column
4. **Parent views offerings** → sends timezone parameter (e.g., `Asia/Kolkata`)
5. **Application converts from UTC** → `Instant` → `ZonedDateTime` in parent's timezone
6. **Response displays local time** → ISO-8601 format with timezone offset

### Example
```
Teacher creates session: 2026-06-07 18:00 (America/New_York = UTC-4)
Stored in DB:            2026-06-07 22:00 UTC
Parent views (IST):      2026-06-08 03:30 (Asia/Kolkata = UTC+5:30)
```

### Implementation
- Uses Java `ZoneId`, `ZonedDateTime`, and `Instant` — no manual offset math
- IANA timezone database for correct DST handling
- Invalid timezone IDs return clear error messages

---

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Test Coverage
| Test | Description |
|------|-------------|
| Multi-parent concurrent booking | 5 parents book simultaneously (within capacity) |
| Overbooking prevention | 5 parents compete for 2 spots |
| Time conflict detection | Overlapping sessions rejected |
| Duplicate booking prevention | Same offering can't be booked twice |
| Non-overlapping bookings | Different time slots succeed |

### Test Configuration
Tests use H2 in-memory database with auto-schema generation (no PostgreSQL required).

---

## 📁 Project Structure
```
ChronoClass/
├── pom.xml                          # Maven dependencies
├── docker-compose.yml               # PostgreSQL container
├── LICENSE                          # Evaluation-only license
├── README.md                        # This file
├── src/
│   ├── main/
│   │   ├── java/com/chronoclass/
│   │   │   ├── ChronoClassApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java     # Swagger configuration
│   │   │   │   └── JacksonConfig.java     # JSON serialization config
│   │   │   ├── controller/
│   │   │   │   ├── TeacherController.java # Teacher REST endpoints
│   │   │   │   └── ParentController.java  # Parent REST endpoints
│   │   │   ├── dto/
│   │   │   │   ├── request/               # Request DTOs with validation
│   │   │   │   └── response/              # Response DTOs
│   │   │   ├── entity/
│   │   │   │   ├── Course.java
│   │   │   │   ├── Offering.java          # @Version for optimistic locking
│   │   │   │   ├── Session.java           # Times in UTC
│   │   │   │   └── Booking.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── TimeConflictException.java
│   │   │   │   └── ConcurrentBookingException.java
│   │   │   ├── repository/
│   │   │   │   ├── CourseRepository.java
│   │   │   │   ├── OfferingRepository.java  # Pessimistic lock queries
│   │   │   │   ├── SessionRepository.java   # Conflict detection queries
│   │   │   │   └── BookingRepository.java
│   │   │   └── service/
│   │   │       ├── TeacherService.java      # Offering & session management
│   │   │       └── ParentService.java       # Booking with concurrency control
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql      # Flyway migration
│   └── test/
│       ├── java/com/chronoclass/service/
│       │   └── BookingConcurrencyTest.java  # Concurrency integration tests
│       └── resources/
│           └── application-test.yml         # H2 test config
```

---

## 💡 Assumptions

1. **No Authentication** — Teacher and Parent IDs are passed directly in the URL/body. Authentication is out of scope for this assignment.
2. **No User Management** — No user registration/login. IDs are assumed to be valid.
3. **Booking is Final** — No cancellation/refund flow implemented (could be added as enhancement).
4. **Single Currency** — No payment integration.
5. **Session Times Don't Change** — Once sessions are added, they are immutable.
6. **Offering Capacity** — Default is 30 students if not specified.

---

## 🔮 Possible Enhancements

- [ ] User authentication (Spring Security + JWT)
- [ ] Booking cancellation with refund logic
- [ ] Email notifications for booking confirmations
- [ ] Recurring session generation (e.g., "every Saturday for 8 weeks")
- [ ] Teacher availability management
- [ ] Waitlist when offering is full
- [ ] Rate limiting on booking endpoints
- [ ] Caching with Redis for read-heavy queries
- [ ] CI/CD pipeline (GitHub Actions)

---

*Copyright (c) 2026 ChronoClass. This software is for evaluation purposes only.*
