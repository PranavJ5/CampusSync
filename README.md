# CampusSync backend status

This branch contains the existing event/registration slice plus the backend work listed below. Phase 1 contains no AI/ML.

## Implemented API surface

- Events: `GET /api/events`, `GET /api/events/{id}`, `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}` (draft only), `POST /api/events/{id}/publish`, `/cancel`, and `/complete`.
- Registration: `POST` and `DELETE /api/events/{eventId}/registrations`; coordinator/admin `GET` participant list; single attendance at `PATCH /api/events/{eventId}/registrations/{userId}/attendance` and batch attendance at `PATCH /api/events/{eventId}/registrations/attendance`.
- Notifications: paginated `GET /api/notifications` with category/unread filters, `GET /api/notifications/unread-count`, `GET /api/notifications/{id}` (marks read), `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, and scoped `POST /api/notifications` publishing.
- Departments: authenticated list/detail, administrator create/update.
- Students: own profile and contact update, admin create/update/list, faculty department-scoped filtered/paginated listing (maximum 25 per page).
- Faculty: admin listing/create/update and own/admin profile read.
- Subjects: authenticated list/detail with department filtering, admin create/update, semester/year/faculty/division allocation fields.
- Committees: authenticated list/detail, administrator profile create/update, active member list with department/semester filters, history list, own membership list, and scoped coordinator/admin add/soft-remove membership operations. Only administrators may grant coordinator privileges.

Request DTOs validate required fields and supported member roles. Notification delivery is represented by a per-user `notification_recipient` row; reads and unread counts are scoped to the authenticated recipient. Event registration, cancellation, attendance, and event cancellation run transactionally.

## Authorization integration boundary

There is no authentication provider or Spring Security configuration in this repository. Protected controllers require a servlet `Principal`, resolve it to an active database user, and check database roles and committee membership. A Principal is trusted only when supplied by a trusted upstream servlet/container authentication mechanism. With no such integration, a normal unauthenticated request has no Principal and receives 401; no header-based or hard-coded bypass is provided. The authentication teammate must configure trusted principal creation before these endpoints can be used.

Administrators have global management rights. A coordinator operation is scoped to an active coordinator membership for that committee. Committee profile changes and coordinator assignment are administrator-only. Students can access their own student profile and notification/registration records; participant lists and attendance are coordinator/admin-only. Completed-event attendance correction is admin-only.

## Database and migration status

- `src/main/resources/db/manual/ADD_EVENT_REGISTRATION_DEADLINE.sql` is an unversioned manual script. It adds `event.registration_deadline`, backfills from `starts_at`, and creates `notification_recipient` if absent. The `ALTER TABLE ... ADD COLUMN` is not idempotent, and `CREATE TABLE IF NOT EXISTS` does not verify an existing table's columns or constraints.
- `src/main/resources/db/manual/PROPOSED_EVENT_COMMITTEE_NOTIFICATION_ALIGNMENT.sql` proposes a generated membership key, a unique active-membership guard, SRS event/notification fields, student semester and remaining academic attributes. It includes preflight queries for duplicate active memberships and subject codes. It is proposal-only, outside Flyway's discovery directory, and must not be applied as written until the live table definitions, data types, indexes, constraints, and `flyway_schema_history` are reviewed.
- This repository has no Flyway dependency or versioned migration. No database connection was made, so the migration version and live schema compatibility are unknown. Do not execute either SQL file against the existing database without explicit approval and a reviewed backup/rollback plan.
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

Still incomplete or partial: live schema/migration compatibility; poster image upload and 2 MB JPEG/PNG validation; participant filtering/export, registration history/upcoming-vs-past endpoint, attendance/reporting summaries; event schedule/venue update notification behavior is implemented but has not been integration-tested; notifications do not yet include public committee announcements, all admin audience combinations are limited to implemented resolver paths, and delivery from a configured system principal is not available; student photo upload/storage is represented only by a path; several SRS profile fields and historical values are nullable until authoritative data is supplied; committee achievements, deletion/deactivation controls, full subject allocation lifecycle, and admin reporting are not complete.

This README distinguishes implemented routes from deferred requirements; it does not assert that a live MySQL schema has been reconciled.
