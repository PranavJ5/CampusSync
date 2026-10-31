# CampusSync handoff

## Phase completed

This branch contains the functional and pre-handoff work completed without using the shared MySQL database:

- Event management is restricted in both frontend routing and backend authorization to administrators and assigned committee coordinators.
- Faculty lecture scheduling is available through `/faculty/timetable` and `POST /api/timetable/faculty`; the backend verifies subject ownership and derives the academic class group from the subject assignment.
- Student and faculty timetable projections include the configured room/venue.
- Event links use `/events/{id}` and preserve the intended event route through role selection and login.
- Existing registration, cancellation, attendance, correction-history, and event-cancellation flows were preserved.
- Timetable room, faculty, class-group, and lab-batch conflict checks remain active; existing locking was preserved.
- Frontend loading/error states were retained while avoidable effect-triggered state warnings were removed.
- Authenticated frontend pages are route-lazy-loaded with `React.lazy`/`Suspense`.
- Global styling was refined around the existing forest-green/mint identity with responsive layout, focus-visible states, reduced-motion support, consistent forms/tables/cards, and less decorative motion.

## Important files

- `frontend/src/App.jsx` — protected routes, lazy loading, and route fallback.
- `frontend/src/index.css` — shared visual system and responsive styling.
- `frontend/src/pages/FacultyTimetablePage.jsx` — faculty lecture scheduling UI.
- `frontend/src/pages/AuthenticationLandingPage.jsx` and `LoginPage.jsx` — return-to-event login flow.
- `src/main/java/com/campussync/service/AuthorizationService.java` — event coordinator scoping.
- `src/main/java/com/campussync/service/TimetableService.java` and `TimetableController.java` — faculty scheduling and timetable validation.

## Verified commands

Use Java 21:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot'
mvn -o test
```

Latest result: 161 executed tests passed. `EventRegistrationMySqlConcurrencyTest` reported zero executed test methods because Docker was unavailable; this is not a database integration success.

Frontend:

```powershell
cd frontend
npm ci
npm run lint
npm run build
npm audit --offline --omit=optional
```

Latest successful results: lint completed with zero warnings/errors, build completed, and offline audit reported zero vulnerabilities. The first `npm ci` attempt encountered a Windows lock on a native `node_modules` binary; the installed dependency tree subsequently provided working `oxlint` and `vite` commands. Do not commit `node_modules` or `dist`.

The build is code-split: the largest JavaScript entry is approximately 414 kB minified (130 kB gzip), with authenticated pages emitted as separate chunks.

## Database-dependent work still pending

- Run the MySQL/Testcontainers concurrency test with Docker available and verify simultaneous timetable conflicts against MySQL.
- Verify registration persistence and attendance/correction visibility against an isolated configured MySQL database.
- Validate Flyway history, schema compatibility, role seed data, subject assignments, and room/timetable records against the teammate's database.
- Perform browser-level checks for role navigation, event links, login return flow, faculty scheduling, cancellation, and responsive layouts.

## Next teammate steps

1. Configure the local Java 21 and MySQL environment without committing secrets.
2. Review the existing uncommitted diff and preserve the tracked `application.properties.example` template.
3. Run the backend and frontend commands above.
4. Start Docker or provide an isolated database ending in `_test` before running database-dependent tests.
5. Exercise each role with real records: administrator, committee coordinator, faculty, and student.
6. Confirm the live schema and Flyway history before applying any migration to a non-empty database.

No commits, pushes, merges, or pull requests were created by this phase.
