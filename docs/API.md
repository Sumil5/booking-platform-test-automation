# Restful-Booker-Platform API map

Target: `https://automationintesting.online` (set via `BASE_URL`, see `.env.example`).

## How this map was built

The platform is open source
([mwinteringham/restful-booker-platform](https://github.com/mwinteringham/restful-booker-platform)).
This map comes from reading the source at commit `efe2deb` (2026-10-23): the Spring Boot
controllers, services and validation annotations of the `auth`, `room`, `booking` and
`message` services, plus the Next.js `assets` app that serves the public `/api/*` paths.

## Live verification (2026-11-02)

The full suite and manual `curl` probes were run against `https://automationintesting.online`:

- **Matches this map:** every fixed-path endpoint. That covers login and validate
  bodies and status codes, all room, booking and message validation messages (400),
  401 for a missing token on `POST /api/room` and `GET /api/booking`, 409 for
  overlapping or reversed booking dates, the room list and availability search, the
  booking list and summary, the message list and count, and room/booking/message
  creation.
- **Broken on the live deployment:** **every `/{id}` path answers `500 Internal Server
  Error`** (plain text), whatever the id, token or method. That includes `GET
  /api/room/1`, `GET /api/booking/1` with a valid admin token, `GET /api/message/1`,
  `PUT /api/message/1/read` and every `PUT`/`DELETE`. These are the paths Next.js
  rewrites straight to the Spring services, so the rewrite target is probably
  misconfigured in that deployment. The tables below describe the behaviour the source
  code defines, which is what the suite asserts. The 35 scenarios that use these paths
  are tagged `@known-defect` and skipped by default until the deployment is fixed.
  Cleanup of the data that other scenarios create also uses these paths, so it fails
  too, and that data stays on the site until the next reset.

## GitLab CI run (2026-11-03)

Pipeline on GitLab.com, `main` at commit `c2a719e`, started manually: **passed with
warnings** in 1 min 41 s.

| Job | Result |
|---|---|
| `compile-and-bind` | Passed |
| `regression` | Passed: 40 passed, 35 skipped (all `@known-defect`), 0 failed; scenarios took 28 s |
| `known-defects` | Failed, allowed to fail: the same 35 scenarios still hit the `/{id}` 500s |

The Cucumber report from the runner confirms that every password is masked, that one
token was used for the whole run (one login), and that the report records the GitLab job
URL, commit and branch.

## GitLab CI run after merge (2026-11-03): a reset mid-run

The push pipeline for `main` at `41061d0` ran `regression` and got 36 passed, 35 skipped
and **4 failed**. All 4 failed while creating a room: `POST /api/room` returned
`500 {"errors":["An unexpected error occurred"]}`.

- Every valid room creation up to 17 s into the run returned 200; every one from 20 s
  onwards returned 500.
- The room service was healthy throughout: invalid room payloads still got their 400s,
  and a request with no token still got its 401.
- The 500 body exactly matches what this endpoint returns for an invalid token (see the
  first quirk below).

Conclusion: the auth service reset between 17 s and 20 s and deleted the run's token,
which the provider was still trusting inside its 60-second revalidation window. The
framework now recovers from this (see ARCHITECTURE.md, "One login per run, with two
safety nets"). A live check with a simulated revoked token returned 500, then a re-login
and a replay that returned 200, while invalid, tampered and missing tokens kept their
original 500, 500 and 401 responses.

## Architecture: two layers behind `/api`

```
client ──► Next.js (assets) ──┬─► app/api/** route handler ──► Spring service   (static paths)
                              └─► rewrite /api/<svc>/:path* ──► Spring service   (everything else)
```

`next.config.js` returns its rewrites as a plain array, so they are *afterFiles*
rewrites. Next.js checks static routes before them and dynamic (`[id]`) routes after
them. In practice:

| Path pattern | Served by | Consequence |
|---|---|---|
| `/api/auth/login`, `/api/auth/validate`, `/api/auth/logout` | Next.js handler | Reshaped bodies (`{token}`, `{valid:true}`), 401 for bad credentials |
| `/api/room`, `/api/booking`, `/api/booking/summary`, `/api/message`, `/api/message/count` | Next.js handler | Missing cookie gives **401** before the backend is called; error bodies are re-wrapped |
| `/api/room/{id}`, `/api/booking/{id}`, `/api/message/{id}`, `/api/message/{id}/read` | Rewritten straight to Spring | Backend status codes (**403** for bad token, 202 for delete/update) and backend bodies |

The **auth token** is sent as a cookie, `Cookie: token=<value>`, on every layer.

Each service resets its in-memory H2 database on a timer (about every 10 minutes on the
public site). The auth reset deletes **all tokens**, so a cached token can stop working
mid-run.

## Auth

| Method | Path | Body | Success | Failures |
|---|---|---|---|---|
| POST | `/api/auth/login` | `{"username","password"}` | 200 `{"token":"<16 chars>"}` | 401 `{"error":"Invalid credentials"}` |
| POST | `/api/auth/validate` | `{"token"}` | 200 `{"valid":true}` | 401 `{"error":"No token provided"}` when the token is missing; 403 `{"error":"Invalid token"}` when it is unknown |
| POST | `/api/auth/logout` | `{"token"}` | 200 `{"success":true}` (the Next.js handler does **not** call the backend, so the token stays valid) | 400 when the token is missing |

## Rooms

Room model: `roomid`, `roomName` (required, non-empty), `type` (`Single|Double|Twin|Family|Suite`),
`accessible` (bool), `image`, `description`, `features` (string[]), `roomPrice` (1–999).

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| GET | `/api/room` | no | 200 `{"rooms":[Room…]}` | `?checkin=YYYY-MM-DD&checkout=YYYY-MM-DD` returns only rooms without a booking inside that range |
| GET | `/api/room/{id}` | no | 200 `Room` | 404 when the room is unknown |
| POST | `/api/room` | yes | 200 `{"success":true}` (no id is returned; look the room up by name) | 401 `{"errors":["Authentication required"]}` without a cookie; 400 `{"errors":[…]}` validation |
| PUT | `/api/room/{id}` | yes | 202 `Room` | 400 validation, 403 bad token, 404 |
| DELETE | `/api/room/{id}` | yes | 202 | 403 bad token, 404 |

Validation messages: `Room name must be set`,
`Type can only contain the room options Single, Double, Twin, Family or Suite`,
`must be greater than or equal to 1`, `must be less than or equal to 999`.

## Bookings

Booking model: `bookingid`, `roomid` (>= 1), `firstname` (3–18, not blank), `lastname` (3–30, not blank),
`depositpaid` (bool), `bookingdates {checkin, checkout}` (ISO dates, checkin < checkout),
optional `email` (valid address) and `phone` (11–21 chars).
When both `email` and `phone` are present, the booking service also posts a message to the
message service.

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| POST | `/api/booking` | no | 201 `Booking` (flattened by Next.js, includes `bookingid`) | 400 `{"errors":[…]}` validation; 409 when checkout is not after checkin or the dates overlap an existing booking for the room |
| GET | `/api/booking?roomid={id}` | yes | 200 `{"bookings":[Booking…]}` | 401 without a cookie, 400 when `roomid` is missing |
| GET | `/api/booking/summary?roomid={id}` | yes (Next.js) | 200 `{"bookings":[{"bookingDates":{…}}]}` | |
| GET | `/api/booking/{id}` | yes | 200 `Booking` | 403 bad token, 404 unknown id |
| PUT | `/api/booking/{id}` | yes | 200 `{"bookingid", "booking":{…}}` | 400 validation, 403, 404, 409 |
| DELETE | `/api/booking/{id}` | yes | 202 | 403, 404 |

Validation messages: `Firstname should not be blank`, `Lastname should not be blank`,
`size must be between 3 and 18`, `size must be between 3 and 30`,
`must be greater than or equal to 1`, `must be a well-formed email address`,
`size must be between 11 and 21`.

## Messages

Message model: `messageid`, `name` (not blank), `email` (valid, not blank),
`phone` (11–21), `subject` (5–100), `description` (20–2000).

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| POST | `/api/message` | no | 200 `{"success":true}` (no id is returned; look it up in the list) | 400 with a bare JSON array of messages |
| GET | `/api/message` | no | 200 `{"messages":[{"id","name","subject","read"}]}` | |
| GET | `/api/message/count` | no | 200 `{"count":<unread>}` | |
| GET | `/api/message/{id}` | no (backend does not check) | 200 `Message` | 404 |
| PUT | `/api/message/{id}/read` | yes | 202 | 403 bad token |
| DELETE | `/api/message/{id}` | yes | 202 | 403, 404 |

Validation messages: `Name may not be blank`, `Email may not be blank`,
`must be a well-formed email address`, `Phone must be between 11 and 21 characters.`,
`Subject must be between 5 and 100 characters.`,
`Message must be between 20 and 2000 characters.`

## Known quirks worth knowing

- Several Next.js handlers call `response.json()` on backend error responses that have
  **empty bodies** (for example a 403). That throws, and the handler then answers
  **500**. Confirmed live: `POST /api/room` with an invalid token returns 500
  `{"errors":["An unexpected error occurred"]}` instead of 403. The suite does not assert
  on this path.
- `POST /api/auth/logout` does not invalidate the token (see above). The suite does not
  test logout invalidation, because that test would fail by design.
- Room and message creation do not return the new id. The framework finds the new record
  by a unique, generated name or subject.
