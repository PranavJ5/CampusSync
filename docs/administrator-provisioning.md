# Administrator account provisioning

CampusSync intentionally has no public administrator or coordinator signup route. `POST /api/auth/register` always creates a `STUDENT` account and student profile; any submitted role field is ignored.

For the `local-test` profile, Flyway migrations `V1001__seed_campus_departments_and_committees.sql`, `V1002__seed_student_role.sql`, and `V1003__seed_committee_coordinator_role.sql` add missing department, committee, and approved role reference rows (`ADMINISTRATOR`, `FACULTY`, `STUDENT`, and `COMMITTEE_COORDINATOR`). Existing department and committee records matching the requested names are activated without changing their IDs or department relationships; no committee department is inferred. Existing non-empty local-test schemas without Flyway history are baselined at V1000 before these migrations; Hibernate remains in `validate` mode and Flyway clean is disabled. A role-code/name uniqueness conflict fails migration visibly rather than changing or replacing an existing role. These migrations do not run in other profiles.

An authenticated `ADMINISTRATOR` can create a standalone `COMMITTEE_COORDINATOR` account and assign its initial committee through `POST /api/admin/coordinators`. The endpoint accepts the initial password only in the protected request, stores it with the configured BCrypt encoder, and never returns it. An existing standalone coordinator can be assigned through `POST /api/admin/coordinators/{userId}/committees/{committeeId}`.

Administrators manage committee master data at `/admin/committees`: create/edit committee name, code, category, description, established year, and optional department; activate/deactivate it; assign exactly one active faculty coordinator; assign student coordinators; and add/remove student members. Assignment of a student or faculty coordinator does not alter that account's primary role. Coordinator powers are granted only by an active `COORDINATOR` membership in the specific committee, and the backend checks that scope for committee/member/event operations. An assigned coordinator can manage ordinary membership only; coordinator assignment and committee master data remain administrator-only. Leaving members are marked inactive with `left_on`, preserving membership history.

The five canonical reference committees are provisioned by the local-test-only migration for that profile and by the synthetic demo seeder for `campussync_demo`: Sports Committee (`SPORTS`), Oculus Committee (`OCULUS`), E-Cell (`E_CELL`), Rotaract (`ROTARACT`), and IEEE CS (`IEEE_CS`). Do not create duplicates or change these reference names. In the demo data, faculty and student coordinator mappings use existing synthetic usernames rather than fixed database IDs. For already-seeded demo accounts, the reference-only seeder mode is safe to rerun and only inserts missing exact reference rows/memberships; conflicting records are rejected for administrator review.

New administrator accounts must be provisioned by an authorized deployment operator through the organization's restricted, audited account-provisioning process. This project has no administrator-provisioning API. The process must:

1. Run only in a protected administrative environment, never through the student registration form or a browser-supplied role.
2. Resolve the existing `ADMINISTRATOR` role on the server side and create an active account with unique username and email.
3. Hash the initial password with the application's configured BCrypt `PasswordEncoder`; never store or log a plaintext password or expose a password hash.
4. Perform the account and role assignment transactionally, verify the account can authenticate with the `ADMINISTRATOR` role, and deliver any initial credential through an approved out-of-band channel.
5. Keep an audit record of the operator and provisioning action without recording credentials or tokens.

This repository does not include a public provisioning endpoint, a default administrator credential, or a provisioning SQL script. Do not apply ad hoc SQL or change the existing database schema for this process.
