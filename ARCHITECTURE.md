# Architecture

API tests for the Restful-Booker-Platform, written in Java 21 with Maven, REST Assured,
Cucumber 7 (on the JUnit Platform) and PicoContainer.
The endpoint map and the platform behaviour these tests rely on are in [`docs/API.md`](docs/API.md).

## Layers

```
features/*.feature          Business-readable scenarios, tagged @smoke / @regression
        │
steps/*Steps                Gherkin bindings: no HTTP details, only intent and assertions
        │
support/Fixtures            Creates test data, finds generated ids, registers cleanup
context/ScenarioContext     Per-scenario state (last response, created ids, cleanup stack)
        │
client/*Client              One thin class per service (auth, room, booking, message)
        │
client/Specs  auth/TokenProvider  config/Config      Shared HTTP setup, token cache, settings
```

Each layer only knows about the one below it. Steps never build URLs, and clients never
assert. PicoContainer creates a fresh `ScenarioContext`, clients and fixtures for every
scenario. Only `Config` and `TokenProvider` live for the whole run.

## Key decisions

**Configuration comes from the environment and is never in code.**
`Config` resolves each key as a `-D` system property, then an environment variable, then
a git-ignored `.env` file. That order lets CI inject secrets while a developer keeps a
local `.env`. Nothing has a default in code. A missing `BASE_URL`, `ADMIN_USERNAME` or
`ADMIN_PASSWORD` stops the run straight away with one message that lists every missing
key. Pointing the suite at a local or staging deployment needs only a different
`BASE_URL`.

**One login per run, with a safety net.**
`TokenProvider` is a lazy, thread-safe singleton. It logs in on first use and caches the
token for the rest of the run. The public site wipes its databases about every 10
minutes, and that wipe deletes all tokens. So once the cached token has gone unconfirmed
for `TOKEN_REVALIDATE_SECONDS` (default 60), the provider checks it with a cheap
`/api/auth/validate` call and logs in again only if the check fails. A long run therefore
survives a reset without logging in before every request.

**Every scenario owns its data.**
The environment is shared and resets on a timer, so no scenario relies on seed data or
on another scenario. Scenarios create their own room and book that room, so booking dates
cannot clash with other users. Dates are also randomised 1–8 years ahead. Generated names
carry an `AT` marker and a random id. That is how the suite finds rooms and messages
again, since the API does not return their ids on create. It also makes leftovers easy
to spot.

**Cleanup is best effort and never hides a result.**
Each fixture registers its teardown in a per-scenario `CleanupRegistry`. The `@After` hook
runs the teardowns in reverse order (bookings before their room). A 404 counts as
success, because the scenario or a reset may have removed the record already. Cleanup
failures are logged and attached to the report, but they never fail the scenario.
Negative tests that unexpectedly create a record clean it up as well.

**Assertions follow what the platform actually does.**
`/api` is served by two layers: Next.js route handlers for static paths and a straight
rewrite to Spring for `/{id}` paths. The two layers answer differently. A missing token
gets 401 from Next.js and 403 from Spring, and each layer wraps error bodies its own way.
The suite deals with this as follows:
- Exact status codes wherever the source shows only one possible answer. Examples are
  401 for a missing token on `POST /api/room`, 403 for a tampered token on
  `GET /api/booking/{id}`, and 400 for validation errors.
- "Rejected as unauthorised" (401 or 403) where either layer could answer. Every such
  scenario also checks that the record was **not** changed, which matters more than the
  exact code.
- Writes are confirmed by reading the record back, not only by checking the write's
  response body.
- `ResponseBodies` reads every error-body shape (`errors`, `fieldErrors`, `error`, bare
  array), so a validation step only names the message it expects.

**Data-driven negative tests.**
Each validation case is one row in a Scenario Outline. `PayloadMutator` takes a valid
payload and changes one field to a JSON literal (or `<omit>`). New edge cases need no new
Java code.

**Tags.**
`@regression` is set on every feature. `@smoke` marks the critical success paths: login,
CRUD for rooms and bookings, and the contact form. It runs in seconds and is the first
thing to run against a new environment. `@negative`, `@auth`, `@rooms`, `@bookings` and
`@messages` allow narrower selections, for example
`-Dcucumber.filter.tags="@bookings and not @negative"`.

**Reports keep the evidence.**
Cucumber writes HTML, JSON (for CI tooling) and a timeline to `target/cucumber-reports/`.
An HTTP filter records every request and response in a scenario and attaches them to that
scenario in the HTML report. Passwords are fully masked. Tokens keep only their first 4
characters, enough to tell them apart. A failing scenario can then be diagnosed from the
report alone.

## Trade-offs and limits

- The API map comes from the platform's source code and was then checked against the
  live site (see "Live verification" in `docs/API.md`). On the live site, every `/{id}`
  path currently answers 500. The scenarios that use those paths stay red on purpose:
  they describe the correct behaviour and will pass once the deployment is fixed.
- Scenarios run one at a time by default, to be gentle on a shared public site. The
  token provider and per-scenario state are safe for parallel runs; to run in parallel,
  set `cucumber.execution.parallel.enabled=true`.
- Failed assertions are never retried. A reset in the middle of a scenario can still
  cause a rare failure, and hiding it would also hide real defects.
- No JSON-schema contract tests yet. The field-level assertions cover the same ground for
  now. Schemas would be the next step if the API starts to change.
