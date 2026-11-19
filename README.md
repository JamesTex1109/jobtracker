# Job Tracker

![CI](../../actions/workflows/ci.yml/badge.svg)

A full-stack app for tracking job applications through a hiring pipeline:
**Wishlist → Applied → Interviewing → Offer / Rejected**. It has a REST API built with
Spring Boot and a drag-and-drop board UI.

## Features

- **User accounts**: sign up and sign in with email and password. Each user sees only their own applications.
- **REST API** for full CRUD, plus a status-transition endpoint
- **Search and filtering** by status and free text (company or role), with pagination
- **Pipeline stats**: counts per stage and a response rate (responses ÷ submitted)
- **Validation** with field-level errors returned as RFC 9457 `application/problem+json`
- **Schema migrations** with Flyway; Hibernate runs in `validate` mode, so the schema is owned by migrations
- **Domain rule**: moving an application out of Wishlist stamps its applied date automatically
- **Kanban UI** in plain JavaScript: drag cards between columns. It works in dark mode and on mobile.
- **CI** on GitHub Actions runs the test suite on every push

## Tech stack

Java 21 · Spring Boot 4 · Spring Security 7 · Spring Data JPA / Hibernate 7 · Flyway · H2 (dev) / PostgreSQL (prod) · JUnit 5 + MockMvc · GitHub Actions

## Run it

Only Java 21 is required. The Maven wrapper downloads everything else.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # with sample data
# open http://localhost:8080 and sign in as demo@jobtracker.dev / demo1234
```

To use PostgreSQL instead of the in-memory database:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/jobtracker \
DATABASE_USERNAME=jobtracker DATABASE_PASSWORD=secret \
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Run the tests with `./mvnw test`.

## API

All `/api/applications` endpoints require a signed-in session. Requests that change data
(`POST`/`PUT`/`PATCH`/`DELETE`) must also send the `XSRF-TOKEN` cookie value in an `X-XSRF-TOKEN` header.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create an account: `{"email","password","displayName"}` |
| `POST` | `/api/auth/login` | Form fields `email`, `password`; starts a session (`204`) or returns `401` |
| `POST` | `/api/auth/logout` | End the session (`204`) |
| `GET` | `/api/auth/me` | The signed-in user |
| `GET` | `/api/auth/csrf` | Issues the `XSRF-TOKEN` cookie |
| `GET` | `/api/applications?status=&q=&page=&size=` | Search (paged, newest first; max size 100) |
| `GET` | `/api/applications/{id}` | Get one |
| `POST` | `/api/applications` | Create (`201` + `Location` header) |
| `PUT` | `/api/applications/{id}` | Replace |
| `PATCH` | `/api/applications/{id}/status` | Move to a new stage: `{"status":"INTERVIEWING"}` |
| `DELETE` | `/api/applications/{id}` | Delete (`204`) |
| `GET` | `/api/applications/stats` | Pipeline counts and response rate |

Example with curl:

```bash
curl -c jar -b jar localhost:8080/api/auth/csrf
TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')
curl -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" -d email=demo@jobtracker.dev -d password=demo1234 localhost:8080/api/auth/login
curl -c jar -b jar localhost:8080/api/auth/csrf   # token rotates on login
TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')
curl -b jar -H "X-XSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"company":"Acme","role":"Backend Engineer","status":"APPLIED"}' localhost:8080/api/applications
```

Validation error response:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Validation failed",
  "errors": { "company": "must not be blank", "jobUrl": "must start with http:// or https://" }
}
```

## Design notes

- **Session cookies instead of JWTs.** This is a same-origin browser app. The session id lives in an
  `HttpOnly` cookie that JavaScript can't read, so an XSS bug can't steal it, and logging out actually
  revokes access. A JWT in `localStorage` gets neither guarantee. The tradeoff is CSRF exposure, which is
  handled with Spring Security's SPA CSRF support: a token cookie the frontend echoes back in a header.
- **Passwords** are hashed with BCrypt. Spring Security erases the hash from the in-memory principal after
  login, and the session id rotates on login to prevent session fixation.
- **Tenant isolation is enforced in the data layer**: every query is scoped by the owner's id
  (`findByIdAndOwnerId`, an `ownedBy` Specification). Another user's application returns `404`, not `403`,
  so ids can't be probed. An integration test verifies this.

- **Layering:** controller (HTTP and DTOs) → service (transactions and business rules) → repository.
  JPA entities never leave the service layer, so the API's JSON contract stays independent of the database schema.
- **Dynamic filters** use JPA Specifications instead of `:param IS NULL` JPQL tricks, which break on PostgreSQL's type inference.
- **Stats** are computed with a single `GROUP BY` query, not by loading every row.

## Roadmap

- [x] User accounts with session auth (each user sees only their own applications)
- [ ] Rate-limit login attempts
- [ ] Docker Compose with PostgreSQL, plus Testcontainers integration tests
- [ ] Status history / timeline per application
- [ ] Follow-up reminders for applications with no response after N days
- [ ] Deploy to a public URL
