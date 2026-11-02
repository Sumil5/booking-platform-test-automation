# booking-platform-test-automation

BDD API tests for the [Restful-Booker-Platform](https://automationintesting.online), using
Java 21, Maven, REST Assured and Cucumber.

- [`docs/API.md`](docs/API.md): the API endpoints, auth model and known quirks
- [`ARCHITECTURE.md`](ARCHITECTURE.md): design decisions

## Prerequisites

- JDK 21+
- Maven 3.9+

## Configure

Copy `.env.example` to `.env` (git-ignored) and fill it in, or export the same keys as
environment variables. JVM `-D` properties take precedence over both.

| Key | Required | Purpose |
|---|---|---|
| `BASE_URL` | yes | Deployment to test, e.g. `https://automationintesting.online` |
| `ADMIN_USERNAME` | yes | Admin login. The public demo shows its credentials on the `/admin` login page |
| `ADMIN_PASSWORD` | yes | Admin password |
| `HTTP_TIMEOUT_MS` | no | Connect and read timeout (default 15000) |
| `TOKEN_REVALIDATE_SECONDS` | no | How long a cached token is trusted before it is checked again (default 60) |

## Run

All scenarios (`@regression`):

```bash
mvn test
```

Smoke suite only:

```bash
mvn test -Dcucumber.filter.tags=@smoke
```

Any tag expression works:

```bash
mvn test -Dcucumber.filter.tags="@bookings and not @negative"
```

Check that every step is bound to code, without sending any requests:

```bash
mvn test -Dcucumber.execution.dry-run=true
```

## Reports

After a run, open `target/cucumber-reports/cucumber.html`. Each scenario includes its HTTP
exchanges (secrets masked) and a cleanup summary. The same folder holds `cucumber.json`
for CI tooling.

## Layout

```
src/test/java/com/rbp/api/
  config/      Config: env / .env / -D resolution
  auth/        TokenProvider (login once, cache, revalidate), TokenVariant
  client/      REST Assured clients per service and shared Specs
  model/       Request payload records
  data/        TestDataFactory (unique data), PayloadMutator (invalid variants)
  context/     ScenarioContext, CleanupRegistry
  support/     Fixtures, ResponseBodies, HttpExchangeLog
  steps/       Step definitions
  hooks/       Cleanup and report attachments
src/test/resources/features/   auth, rooms, bookings, messages
```
