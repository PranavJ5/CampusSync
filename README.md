# CampusSync

CampusSync is a Smart Student Information & Campus Activity Platform for SPIT Semester V Mini Project PR1. It gives students a single place to discover activities and manage registrations, while faculty coordinators and administrators manage committees, events, and institutional data.

- Guide: Prof. Govind Gaundalkar
- Team: Pranav Jogi, Pritam Suresh Jadhav, and Sarthak Hade

## Backend Implementation Status

This branch incorporates the backend implementation alongside the frontend application:
- Contains the event and registration management, auth/security layer, committee management, and notifications.
- Current limitations / deferred items: live schema/migration reconciliation, poster image file uploads, participant export filtering, and the full subject allocation lifecycle.

## Phase 3 demonstration

See [the presentation guide](docs/presentation-guide.md) for the role-based walkthrough and expected observable outcomes.
# CampusSync backend status

This branch contains the existing event/registration slice plus the backend work listed below. Phase 1 contains no AI/ML.

## Implemented API surface

- Events: `GET /api/events`, `GET /api/events/{id}`, administrator-only `GET /api/events/management` (includes drafts), `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}` (draft only), `POST /api/events/{id}/publish`, `/cancel`, and `/complete`. The administrator event-management UI is available at `/admin/events`.
- Authentication and student signup: `POST /api/auth/login` returns the authenticated account role, optionally rejects a mismatch with the selected sign-in option, and `POST /api/auth/register` transactionally creates a student account/profile. `GET /api/public/departments` returns active department choices. Public administrator signup is not available.
- Reference data: local-test Flyway migrations `V1001__seed_campus_departments_and_committees.sql`, `V1002__seed_student_role.sql`, and `V1003__seed_committee_coordinator_role.sql` idempotently provision the three signup departments, five committees, and all four approved roles without fixed IDs or assumed committee department ownership. Local-test Flyway baselines an existing non-empty schema at V1000; `ddl-auto=validate` remains enabled.
- Coordinator provisioning: administrator-only `POST /api/admin/coordinators` creates a `COMMITTEE_COORDINATOR` account and active committee membership; `POST /api/admin/coordinators/{userId}/committees/{committeeId}` assigns an already-provisioned active coordinator to another active committee. The response never contains a password.
- Faculty provisioning: administrator-only `POST /api/faculty/provision` transactionally creates a FACULTY login and faculty profile; the request password is BCrypt-hashed and never returned. The FACULTY role is seeded by local-test V1003; no additional schema migration is needed.
- Dashboards: administrator-only `GET /api/dashboard/admin` returns database-backed account, event, committee, department, registration, and event participation summaries; student-only `GET /api/dashboard/student` returns published events with seat counts and the student's registration status, that student's active registration count and recent registrations/notifications. Role-scoped `GET /api/timetable/student/me` and `/api/timetable/faculty/me` return current effective schedules and calculate the next class from the current date and time.
- Timetable: administrator-only `GET|POST /api/timetable/management` and `PUT /api/timetable/management/{id}` manage subject/break entries, weekday/time, venue, academic year, effective date, active state, and optional B1–B4 lab batch. The administrator UI is available at `/admin/timetable`; it also assigns active student profiles to a lab batch. Student schedules include division-wide entries plus only that student's assigned batch; faculty schedules include only their assigned subject classes and show each batch.
- Attendance: authenticated faculty may list assigned subjects (`GET /api/attendance/faculty/subjects`), list division students/sessions, create legacy manual sessions (`POST /api/attendance/faculty/sessions`), or create a timetable-linked session for a date (`POST /api/attendance/faculty/timetable/{entryId}/sessions`). Linked sessions are unique per timetable entry and date. Faculty can load and submit/correct full session rosters (`GET|PUT /api/attendance/faculty/sessions/{id}/attendance`) and inspect correction history (`GET /api/attendance/faculty/sessions/{id}/changes`). Attendance is marked only when faculty submits a conducted session roster. Students use `GET /api/attendance/student/me` for private percentages/history; administrators use `GET /api/attendance/admin/report` for grouped summaries. The shortage threshold defaults to 75% and is configured by `campussync.attendance.threshold-percent` / `CAMPSYNC_ATTENDANCE_THRESHOLD_PERCENT`.
- Registration: `POST` and `DELETE /api/events/{eventId}/registrations`; coordinator/admin `GET` participant list; single attendance at `PATCH /api/events/{eventId}/registrations/{userId}/attendance` and batch attendance at `PATCH /api/events/{eventId}/registrations/attendance`.
- Notifications: paginated `GET /api/notifications` with category/unread filters, `GET /api/notifications/unread-count`, `GET /api/notifications/{id}` (marks read), `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, and scoped `POST /api/notifications` publishing.
- Departments: authenticated list/detail, administrator create/update.
- Students: own profile and contact update, admin create/update/list and lab-batch assignment (`PATCH /api/students/{id}/lab-batch`), faculty department-scoped filtered/paginated listing (maximum 25 per page).
- Faculty: admin listing/create/update and own/admin profile read.
- Subjects: authenticated list/detail with department filtering, admin create/update, semester/year/faculty/division allocation fields.
- Committees: authenticated list/detail, administrator profile create/update, active member list with department/semester filters, history list, own membership list, and scoped coordinator/admin add/soft-remove membership operations. Only administrators may grant coordinator privileges.

Request DTOs validate required fields and supported member roles. Notification delivery is represented by a per-user `notification_recipient` row; reads and unread counts are scoped to the authenticated recipient. Event registration, cancellation, attendance, and event cancellation run transactionally.

## Authentication and authorization

Spring Security authenticates email/password credentials with BCrypt and issues JWTs. The login response role comes from the authenticated database principal; the optional student/administrator sign-in choice is checked against that role and never grants permissions. Student registration assigns `STUDENT` on the server and creates the account/profile transactionally. Administrator registration is not public and must use a protected provisioning process.

Administrators have global event-management and dashboard access. Committee coordinators remain scoped to active coordinator membership for committee event operations and participant reports; their management list and committee choices are filtered to their own active assignments. Students can access only their own student dashboard, registrations, and notification records. Protected backend endpoints remain authoritative regardless of frontend navigation. New administrator accounts require the protected operator process described in [administrator provisioning](docs/administrator-provisioning.md); no public administrator signup is exposed.

Timetable entries are stored separately from attendance and are not generated from subject allocations. Administrators must first provision actual subject assignments (including faculty, semester, and division), then enter verified slots through `/admin/timetable`. Subject assignments are currently managed through the administrator-only `/api/subjects` API; faculty accounts/profiles can be provisioned at `/admin/faculty`. Break entries may be configured without a subject. The timetable editor supports parallel lab entries by selecting B1–B4; `/admin/timetable` also loads existing active students by department/semester/division and lets administrators set each student's lab batch. Students see division-wide entries and only the lab rows for their assigned batch; if an assignment is missing, batch-specific labs are withheld and an administrator warning is shown. No sample schedule or attendance rows are seeded.

Attendance requires an administrator to provision a faculty account/profile, create or update a subject with its assigned faculty, semester, and division (`POST /api/subjects` and `PUT /api/subjects/{id}`), and assign student divisions using `PUT /api/students/{id}` (`division` field). Lab-batch assignments are administrator-only and can be set from `/admin/timetable` or `PATCH /api/students/{id}/lab-batch` with `{"labBatch":"B1"}` through `{"labBatch":"B4"}`; send `null` to clear. New public student signups intentionally leave division and batch unset; students cannot select their own attendance group. Local-test V1000 creates the subject and faculty tables but does not seed subject records or faculty profiles, and V1001–V1006 do not fabricate them; empty subject/timetable lists are expected until administrators configure actual academic records. Timetable-linked sessions snapshot the scheduled lab batch, and attendance rosters are filtered to that batch; manual sessions continue to use the full subject division. Attendance is never generated from a timetable entry. V1004 adds student division and attendance/session/audit tables, V1005 adds timetable entries and the optional class-session timetable link with its per-entry/date uniqueness rule, and V1006 adds student/timetable/session lab-batch support and permits distinct batch labs to overlap. Review schema compatibility and backup/rollback before applying migrations to any existing database; they have not been applied locally. Existing migration histories should be at V1005 before migrating to V1006.

## Database and migration status

- `src/main/resources/db/manual/ADD_EVENT_REGISTRATION_DEADLINE.sql` is an unversioned manual script. It adds `event.registration_deadline`, backfills from `starts_at`, and creates `notification_recipient` if absent. The `ALTER TABLE ... ADD COLUMN` is not idempotent, and `CREATE TABLE IF NOT EXISTS` does not verify an existing table's columns or constraints.
- `src/main/resources/db/manual/PROPOSED_EVENT_COMMITTEE_NOTIFICATION_ALIGNMENT.sql` proposes a generated membership key, a unique active-membership guard, SRS event/notification fields, student semester and remaining academic attributes. It includes preflight queries for duplicate active memberships and subject codes. It is proposal-only, outside Flyway's discovery directory, and must not be applied as written until the live table definitions, data types, indexes, constraints, and `flyway_schema_history` are reviewed.
- Flyway dependencies are present. Attendance migration `V1004__attendance_management.sql`, timetable migration `V1005__timetable_entries.sql`, and lab-batch migration `V1006__timetable_lab_batches.sql` are included for the default `db/migration` location and the isolated local-test migration location. The live database migration history and schema compatibility are unknown; do not apply them to an existing database without reviewing the live table definitions, constraints, Flyway history, backup, and rollback plan.
- `application-demo.properties` is a local-only profile pinned to `127.0.0.1:3306/campussync_demo`. It requires `CAMPSYNC_DEMO_DB_USERNAME`, `CAMPSYNC_DEMO_DB_PASSWORD`, and `CAMPSYNC_DEMO_JWT_SECRET`; it disables Flyway and SQL initialization and uses Hibernate `validate`, so it neither creates a database nor applies migrations or seeds records. The database must be separately verified and provisioned before startup. Do not use the `local-test` profile for demo data.
- The demo-only core bootstrap is `src/main/resources/db/demo-migration/V1000__demo_core_schema.sql`. The guarded, one-shot migration entry point is `scripts/migrate-demo-schema.ps1`; after independently creating and verifying an empty `campussync_demo` schema, set the local `CAMPSYNC_DEMO_MIGRATION_DB_USERNAME` and `CAMPSYNC_DEMO_MIGRATION_DB_PASSWORD` variables for a schema-scoped migration account and `CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME` to the independently verified MySQL server hostname, then run `.\scripts\migrate-demo-schema.ps1` from PowerShell. The runner requires typing `campussync_demo` before connecting, enforces the configured server hostname and port 3306, and then requires the exact returned identity `campussync_demo@<server-hostname>:3306`. Before confirming, verify that the displayed MySQL server identity belongs to the isolated demo instance and that the local endpoint is not a production tunnel. The JDBC URL is fixed to `127.0.0.1:3306/campussync_demo`; the runner refuses a non-empty schema and applies demo V1000 followed by standard V1004, V1005, and V1006 in order. It creates neither the database nor any seed records. Keep the migration account separate from the runtime account (`CAMPSYNC_DEMO_DB_USERNAME` / `CAMPSYNC_DEMO_DB_PASSWORD`), and provision both only on the isolated demo MySQL instance. Do not point it at production or `campussync_local_test`; never change the pinned URL. After migration, start the app normally with the `demo` profile (Flyway remains disabled). A failed/partial migration requires manual inspection before any retry.

### Explicit synthetic demo data seeding

The demo data seeder is a separate, manually invoked tool; it is not an application startup hook or Flyway migration. It is fixed to `jdbc:mysql://127.0.0.1:3306/campussync_demo`, rejects a database URL override, checks the independently supplied MySQL hostname and port, requires exact `campussync_demo` confirmation, validates the complete expected schema and exactly successful Flyway versions V1000/V1004/V1005/V1006, refuses any non-empty application table, checks the seeder account grants, and repeats these preflight checks inside its insert transaction. It rolls back all inserts on error and never creates sessions or attendance.

Run only after the isolated database and its grants have been independently verified:

```powershell
# Safe check only; does not ask for database credentials or connect to MySQL.
.\scripts\seed-demo-data.ps1 -VerifyConsole

# Starts the one-shot seeder after the console check passes.
.\scripts\seed-demo-data.ps1
```

The launcher compiles and resolves the runtime classpath with Maven, then starts the seeder as a separate `java.exe` process so it inherits the terminal. It verifies `System.console()` before asking for database credentials; `-VerifyConsole` runs only that check and exits without connecting to MySQL. If it reports that no interactive console is available, use a real interactive PowerShell terminal rather than a task, redirected session, or IDE output panel. The script prompts for the seeder username, independently verified MySQL hostname, and seeder password; the password prompt is non-echoing and its temporary process environment variable is cleared/restored when the command exits. The Java seeder prompts twice for each of the 17 synthetic account passwords using a non-echoing console, requires unique passwords of 8–72 UTF-8 bytes, BCrypt-hashes them in memory, and does not print, persist, or accept them as command arguments. The process must have an interactive console or it fails closed.

Provision a separate seeder account; do not reuse the migration or runtime account. The initial full seed requires `SELECT` on `campussync_demo.*` and `INSERT` on `role`, `department`, `user`, `faculty`, `student`, `subject`, `timetable_entry`, `committee`, and `committee_member` within that schema. Committee-reference-only mode requires `SELECT` on `campussync_demo.*` and only `INSERT` on `committee` and `committee_member`; those are the only table writes that mode performs. Both modes reject grants outside the approved demo-only scopes, including `UPDATE`, global privileges, and privileges on other schemas. The seeder does not create database users or change grants.

The seeder also inserts the five canonical committee references from the local-test reference migration—Sports Committee, Oculus Committee, E-Cell, Rotaract, and IEEE CS—and assigns one existing synthetic faculty coordinator and the eight existing synthetic student coordinators using their usernames, never fixed database IDs. The grant allowlist includes `INSERT` on `committee` and `committee_member` for this purpose.

For an already-seeded `campussync_demo` database, run the idempotent reference-only mode with the committee-scoped grants above:

```powershell
.\scripts\seed-demo-data.ps1 -CommitteeReferencesOnly
```

This mode requires the exact migrated demo schema, the expected server identity, and all eight active synthetic faculty and student profiles. It only inserts missing canonical committee rows and coordinator memberships. If an existing row or active membership conflicts with the expected synthetic definition, it refuses the transaction rather than changing/deleting that record. It does not modify any other application rows.

Seeded fixture records are synthetic: four required application roles; one active `DEMO_CE` / `DEMO Computer Engineering` department; one `demo.admin`, eight `demo.faculty01`–`demo.faculty08`, and eight `demo.student01`–`demo.student08` accounts using `campussync.invalid` email addresses; eight faculty profiles; eight active Semester 5, Division B students, two each in B1–B4; eight subjects (`DEMO-DC`, `DEMO-SE`, `DEMO-AISC`, `DEMO-TOC`, `DEMO-CNS`, `DEMO-MDM-DSA`, `DEMO-MDM-IOT`, `DEMO-DSA`); and five canonical committee references with 13 scoped coordinator memberships. Student/faculty account roles are not changed when they receive committee coordinator membership; committee privileges come only from the active assignment. The standalone DSA subject is distinct from MDM-II (DSA). All fixture credits are synthetic 3.0 values, and all faculty mappings, names, times, rooms, venues, and labels are synthetic. The 27 timetable entries comprise 17 conflict-checked weekday lecture/lab entries and 10 break entries, with one lab per batch B1–B4, effective 10 August 2026 for AY 2026-27. No class sessions, attendance records, audit rows, or non-demo institutional data are seeded. The initial data seed refuses to run again after any application row exists; committee reference-only mode is intentionally separate and idempotent.
- Existing entity column names such as `department_code`, `enrollment_number`, `event_title`, `starts_at`, and `capacity` are retained for compatibility with the current project mappings. The SRS uses different names in places. The proposed SQL is additive where possible; exact renames and constraint names still require live schema inspection.

## Concurrency test

`EventRegistrationMySqlConcurrencyTest` uses two independent service transactions, a barrier, and an isolated MySQL 8.0.36 database to race for one remaining seat. It has been successfully executed against isolated MySQL 8.0.36: 1 test passed, 0 skipped. The test uses `spring.jpa.hibernate.ddl-auto=create-drop`; any external database must be dedicated and disposable, with a database name ending in `_test`.

With Docker available, start Docker Desktop/the Docker daemon and run:

```powershell
mvn -Dtest=EventRegistrationMySqlConcurrencyTest test
```

Alternatively, provide an isolated MySQL test database through environment variables (do not use the existing CampusSync database):

```powershell
$env:CAMPUSSYNC_TEST_MYSQL_URL = 'jdbc:mysql://127.0.0.1:3306/campussync_test'
$env:CAMPUSSYNC_TEST_MYSQL_USERNAME = 'test_user'
$env:CAMPUSSYNC_TEST_MYSQL_PASSWORD = 'test_password'
mvn -Dtest=EventRegistrationMySqlConcurrencyTest test
```

The test refuses an external JDBC URL unless its database name ends in `_test`; because it uses `create-drop`, the database must contain no valuable data. The application migration scripts are not applied by this test.

## SRS status

Implemented or materially covered: event draft/publish lifecycle; deadline and capacity checks; student-only self-registration; duplicate prevention; registration cancellation before deadline; confirmation/cancellation notifications; event cancellation with reason and registration cancellation; completion transition; event publication and 24-hour reminder notifications; notification recipient isolation and read state; committee membership soft removal and repeat membership history; admin/coordinator scoping; student semester and academic fields; faculty/department/subject/committee additions.

Still incomplete or partial: live schema/migration compatibility and verification/configuration of the supplied timetable image's exact academic records (the database was not accessible from this environment); poster image upload and 2 MB JPEG/PNG validation; participant filtering/export and registration history/upcoming-vs-past endpoint; event schedule/venue update notification behavior is implemented but has not been integration-tested; notifications do not yet include public committee announcements, all admin audience combinations are limited to implemented resolver paths, and delivery from a configured system principal is not available; student photo upload/storage is represented only by a path; several SRS profile fields and historical values are nullable until authoritative data is supplied; committee achievements, deletion/deactivation controls, and full subject allocation lifecycle.

This README distinguishes implemented routes from deferred requirements; it does not assert that a live MySQL schema has been reconciled.
