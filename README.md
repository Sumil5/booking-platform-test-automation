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
mvn test -Dgroups=smoke
```

`-Dgroups` takes a JUnit Platform tag expression over the Cucumber tags, written without `@`:

```bash
mvn test -Dgroups="bookings & !negative"
```

Scenarios tagged `@known-defect` describe correct behaviour that the live site currently
gets wrong (see `docs/API.md`). They are reported as skipped, with the reason. To run them,
for example to check whether the site has been fixed:

```bash
RUN_KNOWN_DEFECTS=true mvn test -Dgroups=known-defect
```

Check that every step is bound to code, without sending any requests:

```bash
mvn test -Dcucumber.execution.dry-run=true
```

## CI (GitLab)

`.gitlab-ci.yml` runs:

| Job | When | What |
|---|---|---|
| `compile-and-bind` | every pipeline | Compile and Cucumber dry-run; needs no secrets |
| `smoke` | merge requests, default branch | `@smoke` |
| `regression` | default branch, schedules, manual runs | `@regression` |
| `known-defects` | schedules, manual runs | `@known-defect` with `RUN_KNOWN_DEFECTS=true`, allowed to fail |

Set these under Settings > CI/CD > Variables:
- `BASE_URL`, scoped to the environment named by `TARGET_ENV` (default `demo`).
- `ADMIN_USERNAME` and `ADMIN_PASSWORD`, masked. Protected variables only reach
  protected branches, so leave them unprotected if merge request pipelines should run
  the smoke suite.

For a nightly run, add a schedule under Build > Pipeline schedules. The JUnit results
appear in the merge request widget, and the Cucumber HTML report is kept as a job artifact.

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
