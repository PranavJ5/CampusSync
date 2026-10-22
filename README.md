# CampusSync

## Authentication configuration

The backend uses stateless JWT authentication. Copy `src/main/resources/application.properties.example`
to a local configuration file or provide the same properties through environment-backed configuration.

Required values are `CAMPSYNC_DB_URL`, `CAMPSYNC_DB_USERNAME`, `CAMPSYNC_DB_PASSWORD`, and
`CAMPSYNC_JWT_SECRET`. The JWT secret must be at least 32 UTF-8 bytes. Optional values configure
token lifetime and allowed CORS origins.

Authentication endpoints:

- `POST /api/auth/login` with `{ "email": "...", "password": "..." }`
- `GET /api/auth/me` with `Authorization: Bearer <token>`

Run focused security tests with `mvn -Dtest="*Security*" test` and the complete suite with `mvn test`.
The sanitized Postman collection is under `postman/CampusSync-Auth.postman_collection.json`.
