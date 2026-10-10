# Test RSVP in Postman or Insomnia

Use one dummy wedding and send the requests in order. Each create request returns an id. Copy that id into your environment before the next request.

Base URL: `http://localhost:8080`

Swagger, if you want to compare: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Before the first request

Start MySQL, then from the project folder:

```bash
./mvnw spring-boot:run
```

Create an environment named `RSVP Local`.

| Variable | Starting value |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `token` | leave empty |
| `tokenB` | leave empty |
| `refreshToken` | leave empty |
| `eventId` | leave empty |
| `eventId2` | leave empty |
| `eventId3` | leave empty |
| `contactId` | leave empty |
| `raviId` | leave empty |
| `participantId` | leave empty |
| `travelId` | leave empty |
| `taskId` | leave empty |
| `expenseId` | leave empty |
| `documentId` | leave empty |
| `notificationId` | leave empty |
| `feedUrl` | leave empty |

In Postman, select this environment, then for protected requests set **Authorization** to **Bearer Token** and value `{{token}}`.

In Insomnia, set **Auth** to **Bearer** and token `{{token}}`.

Public requests (register, login, refresh, logout, health, and the Apple feed URL) do not send a token.

Success responses look like:

```json
{
  "data": {},
  "message": "Event created successfully"
}
```

Lists also include `pagination`. Errors look like:

```json
{
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Cannot change event status from PLANNING to ARCHIVED"
}
```

The dummy emails can be registered once. If you run the guide again, change `demo` to `demo2` in both emails.

## 1. Health

`GET {{baseUrl}}/actuator/health`

No auth. No body.

Expect `200` and `"status": "UP"`.

## 2. Register Asha

`POST {{baseUrl}}/api/v1/auth/register`

Header: `Content-Type: application/json`

No auth.

```json
{
  "name": "Asha Mehta",
  "email": "asha.demo@example.com",
  "password": "correct-horse",
  "phone": "+919800000001",
  "timezone": "Asia/Kolkata",
  "locale": "en-IN"
}
```

Expect `201`. The user object has no `passwordHash`.

Save:

- `data.accessToken` → `token`
- `data.refreshToken` → `refreshToken`

## 3. Register Bina

`POST {{baseUrl}}/api/v1/auth/register`

```json
{
  "name": "Bina Rao",
  "email": "bina.demo@example.com",
  "password": "correct-horse",
  "phone": "+919800000002",
  "timezone": "Asia/Kolkata",
  "locale": "en-IN"
}
```

Expect `201`.

Save `data.accessToken` → `tokenB`. Keep Asha's token in `token`. The rest of the guide uses Asha unless a step says to use Bina.

Sending Asha's register body again returns `409` and `DUPLICATE_EMAIL`.

## 4. Current user

`GET {{baseUrl}}/api/v1/auth/me`

Auth: Bearer `{{token}}`

Expect `200`, email `asha.demo@example.com`.

## 5. Update profile

`PUT {{baseUrl}}/api/v1/users/me`

Auth: Bearer `{{token}}`

```json
{
  "name": "Asha Mehta",
  "phone": "+919800000001",
  "timezone": "Asia/Kolkata",
  "locale": "en-IN",
  "profileImageUrl": "https://example.com/asha.png"
}
```

Expect `200` and message `Profile updated`.

## 6. Login

`POST {{baseUrl}}/api/v1/auth/login`

No auth.

```json
{
  "email": "asha.demo@example.com",
  "password": "correct-horse"
}
```

Expect `200`. You can replace `token` with the new `data.accessToken`. A wrong password returns `401`.

## 7. Create the wedding

`POST {{baseUrl}}/api/v1/events`

Auth: Bearer `{{token}}`

```json
{
  "name": "Wedding 2026",
  "description": "Asha and family",
  "startDateTime": "2026-12-18T04:30:00Z",
  "endDateTime": "2026-12-18T16:30:00Z",
  "timezone": "Asia/Kolkata",
  "locationName": "Bangalore",
  "address": "MG Road",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "color": "#9B4D6A",
  "notes": "Dummy wedding used to test every flow"
}
```

Expect `201` and `data.status` `PLANNING`.

Save `data.id` → `eventId`.

## 8. Create the conference

Same URL and auth. This second event is only here so one caterer can be linked twice.

```json
{
  "name": "Conference 2027",
  "description": "Reuse the same caterer",
  "startDateTime": "2027-03-10T03:30:00Z",
  "endDateTime": "2027-03-10T12:30:00Z",
  "timezone": "Asia/Singapore",
  "locationName": "Singapore"
}
```

Save `data.id` → `eventId2`.

## 9. List and update the wedding

`GET {{baseUrl}}/api/v1/events?search=Wedding&status=PLANNING&page=0&size=20`

Auth: Bearer `{{token}}`

Expect the wedding in `data`, plus `pagination`.

`PUT {{baseUrl}}/api/v1/events/{{eventId}}`

```json
{
  "name": "Wedding 2026",
  "description": "Asha and family",
  "startDateTime": "2026-12-18T04:30:00Z",
  "endDateTime": "2026-12-18T16:30:00Z",
  "timezone": "Asia/Kolkata",
  "locationName": "Bangalore Palace",
  "address": "Palace Road",
  "color": "#9B4D6A",
  "notes": "Venue changed"
}
```

Expect `data.locationName` `Bangalore Palace`.

## 10. Reject a bad status, then confirm

`PATCH {{baseUrl}}/api/v1/events/{{eventId}}/status`

```json
{
  "status": "ARCHIVED"
}
```

Expect `409` and `INVALID_STATUS_TRANSITION`. A planning event cannot jump to archived.

Send the same request with the allowed status:

```json
{
  "status": "CONFIRMED"
}
```

Expect `200` and `data.status` `CONFIRMED`.

## 11. Bina cannot open Asha's wedding

`GET {{baseUrl}}/api/v1/events/{{eventId}}`

Auth: Bearer `{{tokenB}}`

Expect `404`. Switch the token back to `{{token}}` for every request after this one.

## 12. Create the caterer

`POST {{baseUrl}}/api/v1/contacts`

```json
{
  "name": "ABC Catering",
  "phone": "+91 98765 43210",
  "email": "abc.catering@example.com",
  "companyName": "ABC Catering",
  "type": "VENDOR",
  "notes": "Vegetarian menu"
}
```

Expect `201`. `data.normalizedPhone` is `+919876543210`.

Save `data.id` → `contactId`.

## 13. Duplicate phone

`POST {{baseUrl}}/api/v1/contacts`

```json
{
  "name": "ABC Again",
  "phone": "09876543210",
  "type": "VENDOR"
}
```

Expect `409` and `DUPLICATE_CONTACT`. The spacing and the leading `0` are the same Indian number.

## 14. Find the caterer by phone

`GET {{baseUrl}}/api/v1/contacts/search?phone=+919876543210`

Expect `data.name` `ABC Catering`.

## 15. Link the caterer to both events

`POST {{baseUrl}}/api/v1/events/{{eventId}}/contacts`

```json
{
  "contactId": "{{contactId}}",
  "role": "CATERER",
  "vendorRole": "CATERER",
  "status": "CONFIRMED",
  "contractAmount": 80000,
  "currency": "INR",
  "notes": "Dinner for 200"
}
```

Expect `201`.

`POST {{baseUrl}}/api/v1/events/{{eventId2}}/contacts`

```json
{
  "contactId": "{{contactId}}",
  "role": "CATERER",
  "vendorRole": "CATERER",
  "status": "PROSPECT"
}
```

Expect `201`. The same contact id is now on two events.

`GET {{baseUrl}}/api/v1/events/{{eventId}}/contacts`

Expect one vendor row with status `CONFIRMED` and contract amount `80000`.

## 16. Add an attendee

`POST {{baseUrl}}/api/v1/contacts`

```json
{
  "name": "Ravi Shah",
  "phone": "+91 91234 56780",
  "email": "ravi.shah@example.com",
  "type": "ATTENDEE"
}
```

Save `data.id` → `raviId`.

`POST {{baseUrl}}/api/v1/events/{{eventId}}/participants`

```json
{
  "contactId": "{{raviId}}",
  "participantType": "ATTENDEE",
  "attendanceStatus": "INVITED",
  "notes": "Table 4"
}
```

Save `data.id` → `participantId`.

`PUT {{baseUrl}}/api/v1/events/{{eventId}}/participants/{{participantId}}`

```json
{
  "contactId": "{{raviId}}",
  "participantType": "ATTENDEE",
  "attendanceStatus": "CONFIRMED"
}
```

Expect `data.attendanceStatus` `CONFIRMED`.

## 17. Travel

`POST {{baseUrl}}/api/v1/events/{{eventId}}/travels`

This body is rejected because the flight arrives before it departs:

```json
{
  "type": "FLIGHT",
  "title": "Impossible flight",
  "fromLocation": "Mumbai",
  "toLocation": "Delhi",
  "departureAt": "2026-12-18T08:30:00Z",
  "arrivalAt": "2026-12-18T02:00:00Z",
  "timezone": "Asia/Kolkata"
}
```

Expect `400` and `INVALID_DATE_RANGE`.

Send the real flight on the same URL:

```json
{
  "type": "FLIGHT",
  "title": "Mumbai to Bangalore",
  "fromLocation": "Mumbai",
  "toLocation": "Bangalore",
  "departureAt": "2026-12-18T01:30:00Z",
  "arrivalAt": "2026-12-18T03:15:00Z",
  "timezone": "Asia/Kolkata",
  "bookingReference": "ABC123",
  "providerName": "Air India",
  "notes": "Land before the ceremony",
  "details": {
    "airline": "Air India",
    "flightNumber": "AI123",
    "pnr": "ABC123",
    "seat": "14A",
    "terminal": "2"
  }
}
```

Expect `201`. Save `data.id` → `travelId`.

A cab uses the same URL and a different type:

```json
{
  "type": "CAB",
  "title": "Airport to palace",
  "fromLocation": "BLR Airport",
  "toLocation": "Bangalore Palace",
  "departureAt": "2026-12-18T03:45:00Z",
  "arrivalAt": "2026-12-18T05:00:00Z",
  "timezone": "Asia/Kolkata",
  "providerName": "Ola",
  "details": {
    "vehicle": "SUV",
    "pickupCode": "WED-18"
  }
}
```

Expect `201` and `data.type` `CAB`.

## 18. Tasks and reminders

`POST {{baseUrl}}/api/v1/events/{{eventId}}/tasks`

Email reminders are not enabled:

```json
{
  "title": "Email the caterer",
  "reminderEnabled": true,
  "reminderAt": "2020-01-01T00:00:00Z",
  "reminderChannel": "EMAIL"
}
```

Expect `400` and `CHANNEL_NOT_ENABLED`.

The in-app reminder time is in the past, so the job can deliver it within about one minute:

```json
{
  "title": "Call caterer",
  "description": "Confirm the vegetarian menu",
  "status": "TODO",
  "priority": "HIGH",
  "dueAt": "2026-12-01T12:30:00Z",
  "contactId": "{{contactId}}",
  "reminderEnabled": true,
  "reminderAt": "2020-01-01T00:00:00Z",
  "reminderChannel": "IN_APP"
}
```

Expect `201`. Save `data.id` → `taskId`.

Wait about one minute, then:

`GET {{baseUrl}}/api/v1/notifications?unreadOnly=true`

If `data` has an item, save `data[0].id` → `notificationId` and send:

`PATCH {{baseUrl}}/api/v1/notifications/{{notificationId}}/read`

Expect the notification to be marked read.

Complete the task:

`PATCH {{baseUrl}}/api/v1/tasks/{{taskId}}/complete`

No body. Expect `data.status` `COMPLETED`.

`GET {{baseUrl}}/api/v1/events/{{eventId}}/tasks?status=COMPLETED&priority=HIGH`

Expect title `Call caterer`.

## 19. Expenses and payments

`POST {{baseUrl}}/api/v1/events/{{eventId}}/expenses`

```json
{
  "title": "Catering",
  "description": "Dinner",
  "amount": 80000,
  "currency": "INR",
  "category": "FOOD",
  "vendorContactId": "{{contactId}}",
  "expenseDate": "2026-10-18",
  "notes": "Advance due"
}
```

Expect `201`. Save `data.id` → `expenseId`.

`POST {{baseUrl}}/api/v1/expenses/{{expenseId}}/payments`

```json
{
  "amount": 40000,
  "currency": "INR",
  "paymentDate": "2026-10-18",
  "paymentMethod": "UPI",
  "reference": "UPI-WED-1",
  "notes": "Advance"
}
```

Expect `201`. The expense is now partly paid. 40,000 remains.

Send a payment that is too large:

```json
{
  "amount": 50000,
  "currency": "INR",
  "paymentDate": "2026-10-19",
  "paymentMethod": "CASH"
}
```

Expect `409` and `PAYMENT_EXCEEDS_BALANCE`.

Add the unpaid hotel bill:

`POST {{baseUrl}}/api/v1/events/{{eventId}}/expenses`

```json
{
  "title": "Hotel",
  "amount": 32000,
  "currency": "INR",
  "category": "HOTEL",
  "expenseDate": "2026-12-17"
}
```

Expect `data.paymentStatus` `PENDING`.

`GET {{baseUrl}}/api/v1/events/{{eventId}}/expenses/summary`

Expect:

```json
{
  "data": {
    "totalExpense": 112000.0000,
    "totalPaid": 40000.0000,
    "totalPending": 72000.0000,
    "currency": "INR"
  }
}
```

## 20. Document

Create a text file on your computer named `ticket.txt` with this line:

```text
Dummy flight ticket for Wedding 2026
```

`POST {{baseUrl}}/api/v1/documents?eventId={{eventId}}&travelId={{travelId}}`

Auth: Bearer `{{token}}`

Body type: **form-data**, not JSON.

| Key | Type | Value |
| --- | --- | --- |
| `file` | File | `ticket.txt` |

Expect `201` and `data.storageProvider` `LOCAL`. Save `data.id` → `documentId`.

`GET {{baseUrl}}/api/v1/documents/{{documentId}}`

Expect the file name and provider. The JSON has no access token.

`GET {{baseUrl}}/api/v1/documents/{{documentId}}/content`

Expect the text `Dummy flight ticket for Wedding 2026`.

Drive linking needs Google credentials. Without them:

`POST {{baseUrl}}/api/v1/documents/google-drive`

```json
{
  "externalFileId": "dummy-drive-file-id",
  "eventId": "{{eventId}}",
  "name": "Contract.pdf"
}
```

Expect `503` and `INTEGRATION_NOT_CONFIGURED`.

## 21. Dashboard, summary, timeline, and search

All of these use Bearer `{{token}}` and no body.

`GET {{baseUrl}}/api/v1/dashboard`

Expect upcoming events, tasks, travel, pending payments, and recent events.

`GET {{baseUrl}}/api/v1/events/{{eventId}}/summary`

Expect the wedding plus counts for the participant, vendor, travel, completed task, expense totals, and document.

`GET {{baseUrl}}/api/v1/events/{{eventId}}/timeline`

Expect items of type `TRAVEL`, `TASK`, and `EXPENSE`.

`GET {{baseUrl}}/api/v1/search?q=cater`

Expect ABC Catering under `data.contacts`.

## 22. Calendar files

`GET {{baseUrl}}/api/v1/events/{{eventId}}/calendar.ics`

`GET {{baseUrl}}/api/v1/travels/{{travelId}}/calendar.ics`

`GET {{baseUrl}}/api/v1/tasks/{{taskId}}/calendar.ics`

The response is text, not JSON. Each file contains `BEGIN:VCALENDAR`, `UID`, and `SUMMARY`.

## 23. Apple calendar

`POST {{baseUrl}}/api/v1/integrations/apple/connect`

No body. Auth: Bearer `{{token}}`

Expect `200` and a `subscriptionUrl`. Save `data.subscriptionUrl` → `feedUrl`.

`GET {{feedUrl}}`

No auth. Expect an `.ics` body that starts with `BEGIN:VCALENDAR`. This URL is what you would subscribe to in Apple Calendar. The app does not ask for an Apple password.

## 24. Google calendar

`POST {{baseUrl}}/api/v1/integrations/google/connect`

No body. Auth: Bearer `{{token}}`

Without `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, expect `503` and `INTEGRATION_NOT_CONFIGURED`.

After those values are set and the app is restarted, the same request returns `data.authorizationUrl`. Open that URL in a browser and approve access. Google then calls the API callback. After that, send:

`GET {{baseUrl}}/api/v1/integrations/google/calendars`

`POST {{baseUrl}}/api/v1/events/{{eventId}}/calendar/sync`

`GET {{baseUrl}}/api/v1/calendar/connections`

## 25. Complete, archive, and delete a different event

Leave the wedding as it is. Create a small event for the end of the lifecycle.

`POST {{baseUrl}}/api/v1/events`

```json
{
  "name": "Rehearsal dinner",
  "startDateTime": "2026-12-17T13:30:00Z",
  "endDateTime": "2026-12-17T16:30:00Z",
  "timezone": "Asia/Kolkata",
  "locationName": "Bangalore"
}
```

Save `data.id` → `eventId3`.

`PATCH {{baseUrl}}/api/v1/events/{{eventId3}}/status`

```json
{
  "status": "CONFIRMED"
}
```

`POST {{baseUrl}}/api/v1/events/{{eventId3}}/complete`

No body. Expect `data.status` `COMPLETED` and a `completedAt` time.

`POST {{baseUrl}}/api/v1/events/{{eventId3}}/archive`

No body. Expect `data.status` `ARCHIVED`.

`DELETE {{baseUrl}}/api/v1/events/{{eventId3}}`

Expect message `Event deleted`.

`GET {{baseUrl}}/api/v1/events/{{eventId3}}`

Expect `404`. The row is soft-deleted.

## 26. Refresh and logout

`POST {{baseUrl}}/api/v1/auth/refresh`

No auth.

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Expect `200` and a new pair of tokens. Save the new `data.accessToken` to `token` and the new `data.refreshToken` to `refreshToken`.

Send the refresh request again with the **old** refresh token, before you overwrite it. The easiest way is to paste the original refresh token into the body once more:

```json
{
  "refreshToken": "paste-the-first-refresh-token-here"
}
```

Expect `401`. Reuse revokes that user's refresh sessions.

Log in again with the login body from step 6, save the new `data.refreshToken`, then:

`POST {{baseUrl}}/api/v1/auth/logout`

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Expect message `Logged out`.

## What each request proves

| Step | Dummy data | Pass |
| --- | --- | --- |
| Register | Asha Mehta, Bina Rao, password `correct-horse` | `201`, no password hash |
| Wedding | Bangalore, `Asia/Kolkata` | `PLANNING` |
| Bad status | `ARCHIVED` while planning | `409` `INVALID_STATUS_TRANSITION` |
| Confirm | `CONFIRMED` | `200` |
| Privacy | Bina's token on Asha's event | `404` |
| Caterer | `+91 98765 43210` | normalized phone `+919876543210` |
| Duplicate phone | `09876543210` | `409` `DUPLICATE_CONTACT` |
| Two events | Same `contactId` on wedding and conference | both `201` |
| Attendee | Ravi Shah, then `CONFIRMED` | attendance updated |
| Bad flight | Arrival before departure | `400` `INVALID_DATE_RANGE` |
| Flight and cab | AI123, then Ola cab | both `201` |
| Email reminder | channel `EMAIL` | `400` `CHANNEL_NOT_ENABLED` |
| In-app reminder | `Call caterer` | notification after about one minute |
| Money | 80,000 catering, 40,000 UPI, 32,000 hotel | summary 112000 / 40000 / 72000 INR |
| Overpay | 50,000 against 40,000 left | `409` `PAYMENT_EXCEEDS_BALANCE` |
| Document | `ticket.txt` | content download matches the file |
| Read models | dashboard, summary, timeline, `q=cater` | wedding, flight, task, and caterer appear |
| Apple | connect, then open `feedUrl` | `.ics` contains `BEGIN:VCALENDAR` |
| Google without secrets | connect | `503` `INTEGRATION_NOT_CONFIGURED` |
| Lifecycle | Rehearsal dinner | confirm, complete, archive, then `404` after delete |
| Refresh reuse | first refresh token sent twice | `401` |
