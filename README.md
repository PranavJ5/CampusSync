# CampusSync

CampusSync is a Smart Student Information & Campus Activity Platform for SPIT Semester V Mini Project PR1. It gives students a single place to discover activities and manage registrations, while faculty coordinators and administrators manage committees, events, and institutional data.

- Guide: Prof. Govind Gaundalkar
- Team: Pranav Jadhav, Pritam Suresh Jadhav, and Member 1

## Architecture

The target application uses a separate client and API architecture:

| Layer | Technology | Responsibility |
| --- | --- | --- |
| Frontend | React, Vite, Bootstrap | Responsive dashboards, event catalogue, registrations, and notifications |
| Backend | Spring Boot 3, Java 21 | REST API, business rules, JWT-protected access, and reporting |
| Data | MySQL 8 | Users, roles, departments, committees, events, registrations, and notifications |

The current repository contains the Spring Boot foundation, JPA entities/repositories, and HTML wireframes under `src/main/resources/static/wireframes`. The React/Vite client and REST/security implementation are planned components and are not present in this revision yet.

## Prerequisites

- JDK 21
- Maven 3.9 or newer (or the Maven wrapper when one is added)
- MySQL Server 8
- Node.js 20 LTS or newer and npm (for the planned frontend)
- Git

## Configuration

Create local environment configuration with placeholder values only. Do not commit `.env` files, credentials, access tokens, or passwords.

```dotenv
DB_URL=jdbc:mysql://localhost:3306/campussync
DB_USERNAME=your_mysql_user
DB_PASSWORD=your_mysql_password
APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=choose_a_secure_local_password
JWT_SECRET=replace_with_a_long_random_local_secret
JWT_EXPIRATION_MS=3600000
```

When application configuration is added, map these values through environment variables or local, ignored configuration. Never place real values in source control.

## Run the backend

1. Create a local MySQL 8 database named `campussync`.
2. Set the environment variables above in your shell or IDE run configuration.
3. From the project root, run:

```bash
mvn clean spring-boot:run
```

The current backend is a foundation only; REST controllers, JWT security configuration, and database migration scripts still need to be added by the backend owners.

## Run the frontend

Once the React/Vite frontend exists at `frontend/`, run:

```bash
cd frontend
npm install
npm run dev
```

Validate a production build with:

```bash
npm run build
```

## Team work division

| Member | Area |
| --- | --- |
| Member 1 | Spring Boot API, authentication/JWT, persistence, and migrations |
| Member 2 | Domain features and backend business logic for departments, committees, events, and registrations |
| Pritam Suresh Jadhav (Member 3) | Frontend integration, resilient client state, dashboards, event/registration/notification UX, and project documentation |

## Git workflow

1. Start from the shared integration branch: `git checkout develop && git pull origin develop`.
2. Create a focused branch using the `feature/` prefix, for example `feature/frontend-integration-docs`.
3. Keep commits small and use semantic messages such as `feat(frontend): add event registration feedback`.
4. Run the relevant build before pushing, then open a pull request to `develop`.
5. Do not commit `node_modules`, `dist`, `target`, `.env`, access tokens, or personal credentials.

> Repository note: at the time this document was updated, this remote exposes `main` but does not yet expose a `develop` branch. Create/publish `develop` before the shared-branch workflow can be used.

## Phase 3 demonstration

See [the presentation guide](docs/presentation-guide.md) for the role-based walkthrough and expected observable outcomes.
