# RSVP backend

Modular Spring Boot API for events, people, travel, tasks, money, documents, and calendar integrations. RSVP is the source of truth. Google Calendar, Apple Calendar, and Google Drive are integrations.

## What you need to provide

Generate the two secrets locally. Do not commit real values.

```bash
openssl rand -base64 48   # JWT_SECRET, use at least 32 characters
openssl rand -base64 32   # ENCRYPTION_KEY, must decode to 32 bytes
```

| Input | Required to boot | Used for |
| --- | --- | --- |
| `DB_URL` | Yes, outside the dev defaults | MySQL 8 connection |
| `DB_USERNAME` | Yes, outside the dev defaults | Database user |
| `DB_PASSWORD` | Yes, outside the dev defaults | Database password |
| `JWT_SECRET` | Yes in `prod` | Access-token signing. Minimum 32 characters |
| `ENCRYPTION_KEY` | Yes in `prod` | AES-256-GCM encryption of Google tokens. Base64 of 32 bytes |
| `PUBLIC_BASE_URL` | Yes for calendar links | Public URL of this API, no trailing slash |
| `GOOGLE_CLIENT_ID` | Only to connect Google | OAuth client from Google Cloud |
| `GOOGLE_CLIENT_SECRET` | Only to connect Google | OAuth client secret |
| `GOOGLE_REDIRECT_URI` | Only to connect Google | Must be `{PUBLIC_BASE_URL}/api/v1/integrations/google/callback` and must match the Google Cloud authorized redirect |
| `APPLE_CLIENT_ID` | Not used by this version | Reserved for a future direct Apple API |
| `APPLE_CLIENT_SECRET` | Not used by this version | Reserved. This API never asks for an Apple ID password |

Google Cloud OAuth scopes to enable:

- `openid`
- `email`
- `https://www.googleapis.com/auth/calendar`
- `https://www.googleapis.com/auth/drive.readonly`

Apple Calendar uses a secret iCalendar subscription URL from `POST /api/v1/integrations/apple/connect`. That URL must be reachable by Apple, so `PUBLIC_BASE_URL` cannot stay on `localhost` for a phone or Mac outside this machine.

Dev defaults in `application.yml` are only for a local database started by Docker Compose. Replace them before any shared or production deployment.

## Run locally

```bash
docker compose up mysql -d
./mvnw spring-boot:run
```

API: `http://localhost:8080`

OpenAPI: `http://localhost:8080/swagger-ui.html`

Health: `http://localhost:8080/actuator/health`

The full application image:

```bash
docker compose up --build
```

Copy `.env.example` and export those variables when you use the `prod` profile. Compose already passes local placeholder secrets for a private workstation.

## Tests

```bash
./mvnw test
```

Unit tests do not need MySQL. The end-to-end test uses Testcontainers and is skipped when Docker is not running.
