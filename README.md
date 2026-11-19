# Job Tracker

![CI](../../actions/workflows/ci.yml/badge.svg)

A full-stack app for tracking job applications through a hiring pipeline:
**Wishlist → Applied → Interviewing → Offer / Rejected**. It has a REST API built with
Spring Boot and a drag-and-drop board UI.

## Features

- **REST API** for full CRUD, plus a status-transition endpoint
- **Search and filtering** by status and free text (company or role), with pagination
- **Pipeline stats**: counts per stage and a response rate (responses ÷ submitted)
- **Validation** with field-level errors returned as RFC 9457 `application/problem+json`
- **Schema migrations** with Flyway; Hibernate runs in `validate` mode, so the schema is owned by migrations
- **Domain rule**: moving an application out of Wishlist stamps its applied date automatically
- **Kanban UI** in plain JavaScript: drag cards between columns. It works in dark mode and on mobile.
- **CI** on GitHub Actions runs the test suite on every push

## Tech stack

Java 21 · Spring Boot 4 · Spring Data JPA / Hibernate 7 · Flyway · H2 (dev) / PostgreSQL (prod) · JUnit 5 + MockMvc · GitHub Actions

## Run it

Only Java 21 is required. The Maven wrapper downloads everything else.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # with sample data
# open http://localhost:8080
```

To use PostgreSQL instead of the in-memory database:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/jobtracker \
DATABASE_USERNAME=jobtracker DATABASE_PASSWORD=secret \
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Run the tests with `./mvnw test`.

## API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/applications?status=&q=&page=&size=` | Search (paged, newest first; max size 100) |
| `GET` | `/api/applications/{id}` | Get one |
| `POST` | `/api/applications` | Create (`201` + `Location` header) |
| `PUT` | `/api/applications/{id}` | Replace |
| `PATCH` | `/api/applications/{id}/status` | Move to a new stage: `{"status":"INTERVIEWING"}` |
| `DELETE` | `/api/applications/{id}` | Delete (`204`) |
| `GET` | `/api/applications/stats` | Pipeline counts and response rate |

Example:

```bash
curl -X POST localhost:8080/api/applications \
  -H 'Content-Type: application/json' \
  -d '{"company":"Acme","role":"Backend Engineer","status":"APPLIED","jobUrl":"https://acme.dev/jobs/42"}'
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

- **Layering:** controller (HTTP and DTOs) → service (transactions and business rules) → repository.
  JPA entities never leave the service layer, so the API's JSON contract stays independent of the database schema.
- **Dynamic filters** use JPA Specifications instead of `:param IS NULL` JPQL tricks, which break on PostgreSQL's type inference.
- **Stats** are computed with a single `GROUP BY` query, not by loading every row.

## Roadmap

- [ ] User accounts with JWT auth (each user sees only their own applications)
- [ ] Docker Compose with PostgreSQL, plus Testcontainers integration tests
- [ ] Status history / timeline per application
- [ ] Follow-up reminders for applications with no response after N days
- [ ] Deploy to a public URL
