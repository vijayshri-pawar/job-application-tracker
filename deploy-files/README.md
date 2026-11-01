# Job Application Tracker

![CI](https://github.com/vijayshri-pawar/job-application-tracker/actions/workflows/ci.yml/badge.svg)

**Live demo:** _add your Render link here after deploying_ (the free server sleeps when idle, so the first load can take about a minute)

Full-stack job application tracker: a REST API plus a web frontend (HTML, CSS, JavaScript) served by the same Spring Boot app. Built with **Java 21, Spring Boot 4, Spring Data JPA and PostgreSQL**.
Users sign up and log in (Spring Security + JWT). Each user has their own applications, companies, interview rounds (with mistakes and improvements after each round) and documents.

## Tech stack

- Java 21, Spring Boot 4.1
- Spring Web (REST), Spring Data JPA / Hibernate, Bean Validation
- Spring Security with stateless JWT login (HS256), BCrypt password hashing
- PostgreSQL (H2 in-memory database for tests)
- Lombok, JUnit 5, Mockito, MockMvc

## Project structure

```
src/main/java/com/vijayshri/jobtrackerbackend
├── config        Security, JWT, CORS, one-time data migration
├── controller    REST endpoints
├── dto           Request / response objects (records) with validation rules
├── exception     Custom exceptions + global error handler
├── model         JPA entities and the Status enum
├── repository    Spring Data JPA repositories (per-user queries)
├── security      Token creation, reading the logged-in user
└── service       Business logic
```

## Running locally

1. Start PostgreSQL and create a database (default: `MydataBase` on port `5433`).
2. Set the database password as an environment variable. It is **not** stored in the code.
   - IntelliJ: *Run → Edit Configurations → Environment variables* → `DB_PASSWORD=your_password`
   - Terminal (Windows PowerShell): `$env:DB_PASSWORD="your_password"`
3. Run the app: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
4. Open **http://localhost:8080** in the browser. The frontend and the API (`/api/...`) run on the same server.

Optional environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/MydataBase` |
| `DB_USERNAME` | `postgres` |
| `PORT` | `8080` |
| `CORS_ORIGINS` | `http://127.0.0.1:5500,http://localhost:5500` |
| `SHOW_SQL` | `false` |
| `JWT_SECRET` | a development-only value. **Set your own long random value (32+ characters) before deploying.** |
| `JWT_EXPIRATION_MINUTES` | `1440` (24 hours) |

## Running tests

```
./mvnw test
```

Tests use an in-memory H2 database, so PostgreSQL does not need to be running.

## How login works

1. The user signs up or logs in at `/api/auth/register` or `/api/auth/login`.
2. The server checks the password against its BCrypt hash and returns a signed JWT (valid for 24 hours).
3. The frontend sends the token with every request: `Authorization: Bearer <token>`.
4. Spring Security verifies the token's signature and expiry. The user id inside it decides whose data is returned.
5. A user can only see and change their own data. Another user's id returns 404.

When the first account is created, it takes over any data that existed before login was added.

## API endpoints

### Authentication (public)

| Method | Endpoint | Body | Returns |
|---|---|---|---|
| POST | `/api/auth/register` | `name`, `email`, `password` (8+ characters) | 201 with `token` and `user` |
| POST | `/api/auth/login` | `email`, `password` | `token` and `user`, or 401 |

All endpoints below need the `Authorization: Bearer <token>` header (otherwise 401).

### My account – `/api/users/me`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Current user's profile |
| PUT | `/api/users/me` | Update name and email |
| PUT | `/api/users/me/password` | Change password (`currentPassword`, `newPassword`), returns 204 |

### Job applications – `/api/applications`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/applications?status=&search=&sort=` | List applications. All filters are optional: `status` (Applied, Interview, Offer, Rejected), `search` (role or company name), `sort` (`newest` or `oldest`) |
| GET | `/api/applications/stats` | Dashboard numbers: total, interviews, offers, response rate |
| GET | `/api/applications/{id}` | Get one application |
| GET | `/api/applications/{id}/interview-rounds` | Interview rounds of an application |
| GET | `/api/applications/{id}/documents` | Documents of an application |
| POST | `/api/applications` | Create (returns 201) |
| PUT | `/api/applications/{id}` | Update |
| DELETE | `/api/applications/{id}` | Delete (returns 204) |

Example request body:

```json
{
  "companyId": 1,
  "role": "Java Developer",
  "dateApplied": "2026-10-30",
  "status": "Applied",
  "jobLink": "https://careers.example.com/123",
  "location": "Pune"
}
```

### Other resources

Each has `GET` (all), `GET /{id}`, `POST`, `PUT /{id}` and `DELETE /{id}`:

| Resource | Endpoint | Required fields |
|---|---|---|
| Companies | `/api/companies` | `name` |
| Interview rounds | `/api/interview-rounds` | `applicationId`, `roundName` |
| Documents | `/api/documents` | `applicationId`, `fileName`, `fileUrl` |

### Error responses

All errors return the same JSON shape with the right status code (400, 401, 404, 409):

```json
{
  "timestamp": "2026-11-01T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": { "role": "Role title is required" }
}
```

## Deployment

The app runs in Docker on Render with a Neon PostgreSQL database. See [DEPLOY.md](DEPLOY.md) for the step-by-step guide.

## Planned improvements

- Real file upload for documents
- Pagination for the application list
