# RSVP backend

Modular Spring Boot API for events, people, travel, tasks, money, documents, and calendar integrations. RSVP is the source of truth. Google Calendar, Apple Calendar, and Google Drive are integrations.

- How to run the API: [Run locally](#run-locally)
- How to click through every flow with dummy data: [TESTING.md](TESTING.md)
- What the project does, and interview questions with answers: [What this project does](#what-this-project-does)

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

## What this project does

RSVP is a personal event-management API. One user owns many events, and each event is the place where travel, people, vendors, tasks, reminders, expenses, documents, and calendar entries come together. The Spring Boot app is the source of truth. Google Calendar, Apple Calendar, and Google Drive follow RSVP. A failed sync does not undo the event that was just saved.

A user registers, gets a short-lived JWT and a rotating refresh token, then creates an event with its own timezone. Around that event they attach reusable contacts (the same caterer can belong to several events), generic travel such as a flight or train, tasks with in-app reminders, expenses with partial payments, and documents stored locally or linked from Google Drive. Creating or changing an event, trip, or task publishes a sync request after the database transaction commits. A listener then creates, updates, or deletes the matching Google Calendar entry and stores the external id so a retry cannot create duplicates. Apple Calendar is a secret iCalendar subscription URL, so the app never asks for an Apple password. A dashboard, event summary, timeline, and search answer: what do I need to know and do for this event?

It is one modular monolith under `com.rsvp`, with packages for auth, events, contacts, travel, tasks, expenses, documents, calendar, and integrations. Controllers take and return DTOs. Services own the rules. MySQL is migrated with Flyway.

## Interview questions

### 1. Why is this a modular monolith instead of microservices?

The domains share one database and one user. A request such as “event summary” needs events, participants, tasks, expenses, and calendar status together. Splitting that into services would add network calls and distributed transactions before there is an independent scaling problem. Modules stay separate through package boundaries and provider interfaces (`CalendarProvider`, `DocumentProvider`), so the event package does not import Google’s HTTP client. Spring application events are the seam where a queue could be added later. Kafka, Redis, and separate deployables wait until a real load or team boundary needs them.

### 2. Why is Contact not copied onto every event?

`ABC Catering` and `+919876543210` is one contact owned by the user. `EventContact` is the association: role, vendor status, contract amount, and notes for one event. `EventParticipant` is the attendance association: invited, confirmed, declined, attended, no-show. The same contact row can be a vendor on a wedding and an attendee on a conference. Uniqueness is `UNIQUE(user_id, normalized_phone, deleted_marker)`, so the same person cannot be inserted twice for one user, and a soft-deleted contact can be created again.

### 3. How does phone normalization work, and why is the unique key not the raw string?

`+91 98765 43210`, `+919876543210`, and `09876543210` are the same number. `PhoneNormalizer` uses libphonenumber, parses with the region from the user’s locale (default `IN`), and stores E.164 such as `+919876543210`. The unique constraint is on that value, scoped to the user. A global unique phone would block two users from knowing the same vendor. An invalid number is rejected with a business error before insert.

### 4. How do you stop an event from moving COMPLETED back to PLANNING?

`EventLifecycle` is an explicit map. `PLANNING` can go to `CONFIRMED`, `CANCELLED`, or `COMPLETED`. `CONFIRMED` can go to `IN_PROGRESS`, `CANCELLED`, or `COMPLETED`. `IN_PROGRESS` can only be completed. `COMPLETED` and `CANCELLED` can only be archived. `ARCHIVED` has no outgoing transition. Anything else throws `INVALID_STATUS_TRANSITION`. The scheduler uses `autoCompletable`, which is true only for `CONFIRMED` and `IN_PROGRESS`, and only when `endDateTime` is in the past. `PLANNING` and `CANCELLED` are never auto-completed. Completing an event leaves the calendar entry in place, because history should stay on the user’s calendar.

### 5. Why are expense totals not stored on the event?

`totalExpense`, `totalPaid`, and `totalPending` are derived. Each expense has an amount in `BigDecimal` with currency stored on the row, defaulting to `INR`. Payments are child rows. `ExpenseCalculator` ignores deleted and cancelled expenses, sums paid amounts, and sets status to `PENDING`, `PARTIALLY_PAID`, `PAID`, or `CANCELLED`. A catering bill of 80,000 with one UPI payment of 40,000 stays `PARTIALLY_PAID` with 40,000 pending. A payment larger than the remaining balance is rejected. Storing a cached total would have to be updated on every payment, deletion, and cancellation, and it would drift. Money is never `double` or `float`.

### 6. Walk through authentication and what happens if a refresh token is reused.

Passwords are BCrypt hashes. Login returns an access JWT of about 15 minutes (`sub` is the user UUID, plus an internal user id and `type=access`) and an opaque refresh token. Only the SHA-256 hash of the refresh token is stored. Refresh revokes the current row and issues a new pair. If a revoked or expired refresh token is presented, every refresh token for that user is revoked, because reuse means the token leaked. Logout revokes the presented token. A user is limited to 10 live refresh sessions. Login is rate-limited in memory. The API response and logs never include `passwordHash`, access tokens, or refresh tokens.

### 7. How do you guarantee user A cannot read user B’s event?

Every query is scoped by the authenticated user id from the JWT. Services load with the equivalent of `findByIdAndUserId`. A missing row and another user’s row both surface as not found, so the API does not reveal that the other event exists. Security is stateless JWT on the filter chain. Public routes are register, login, refresh, logout, the Google OAuth callback, calendar feed URLs, health, and Swagger. Everything else requires a valid access token.

### 8. Why does a Google Calendar failure not roll back the event?

The event transaction commits first. The service publishes `CalendarSyncRequested`, and `CalendarSyncListener` handles it with `@TransactionalEventListener(AFTER_COMMIT)` and `@Async`. If Google is down, the mapping row is marked `FAILED` and the event remains valid. A scheduled retry picks up `FAILED` and `PENDING` mappings. RSVP data is the product, and the calendar is a side effect. Doing the Google call inside the same transaction would hold a database lock during an HTTP call and would delete the user’s event because Google had a timeout.

### 9. How do you stop a retry from creating three Google events?

Each RSVP subject (event, travel, or task) has a `CalendarEventMapping` for a calendar connection, including the external event id. The upsert also searches Google by a private extended property, the idempotency key. If the mapping id is missing but Google already has the event, the provider updates that event and deletes extras. If the remote event was deleted, it creates one again and stores the new id. The conflict policy for this version is RSVP wins: RSVP overwrites the Google copy. A later policy could be last-modified-wins, but that needs a defined merge.

### 10. Why can one RSVP event create several calendar events?

The event, each trip, and each task are different times and places. The mapping table is unique on connection plus subject type plus subject id, so “flight Mumbai to Delhi” is one calendar event and “call the caterer” is another, while the conference itself is a third. Deleting the RSVP record deletes the external event. Completing it does not.

### 11. What does the Apple integration actually do?

There is no shared Apple account and no Apple password. `POST /api/v1/integrations/apple/connect` creates a `CalendarConnection` with a secret feed token and returns `/api/v1/calendar/feeds/{token}.ics`. Apple Calendar subscribes to that URL. The feed is generated from RSVP, so RSVP stays authoritative. `APPLE_CLIENT_ID` and `APPLE_CLIENT_SECRET` are reserved and unused. Direct CalDAV or EventKit sync would be a new `CalendarProvider` implementation. The same `.ics` endpoints exist for one event, one trip, and one task, with a stable UID.

### 12. How are OAuth tokens stored, and what does a document response contain?

Google access and refresh tokens are encrypted with AES-256-GCM before they are written to `external_account_connections`. The key is a 32-byte value supplied as base64 in `ENCRYPTION_KEY`. Document rows store `storageProvider`, `externalFileId`, and `externalUrl`. They do not store tokens. A document response is id, name, provider, and mime type. Opening the file goes through the backend, which decrypts the token and proxies the content. The browser never receives the Google token.

### 13. How are timezones handled?

Instants are stored in UTC. The event also stores an IANA timezone such as `Asia/Singapore`, which can differ from the user’s `Asia/Kolkata`. Travel and tasks have their own timezone. The API does not assume one zone for every event. Display conversion belongs to the client using the zone stored on the record. Calendar requests pass that zone through so Google does not shift the meeting.

### 14. What happens to unique constraints when you soft-delete?

Events, contacts, expenses, and documents use `deleted` and `deletedAt` so history and money are not physically removed. A plain `UNIQUE(email)` would block re-registration after deactivation. Users and contacts add `deleted_marker` to the unique key, so an active email or phone stays unique and a deleted one can be used again. There is no global filter that hides deleted rows from every query, because sync code sometimes needs to load a deleted event and then delete the external calendar event.

### 15. What are the production limits of the current design?

The login rate limit is in memory, so it resets on restart and is not shared across instances. Calendar sync uses Spring events, which are lost if the process dies after commit and before the listener runs. The mapping status and retry job reduce that window, and a durable queue would close it. The scheduler lock table is the extension point for more than one instance, but the jobs still run inside this process. Only the `IN_APP` reminder channel is enabled. Email and push exist on the enum and are rejected until a sender is implemented. Google and Apple credentials are optional at boot. The app runs without them, and connecting Google fails clearly when the client id and secret are absent.

Under real traffic, the first changes would be a durable queue for calendar sync and reminders, a shared rate limit, and schedulers on one instance or behind the database lock. The domain model does not need to change for that.
