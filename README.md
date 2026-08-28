# currency-converter

A small Spring Boot service that converts money between currencies against
per-client balances. It pulls live rates from open.er-api.com (free, no API
key), does the conversion, and keeps a history of everything that happened.

---

## Running it

The easy way — Postgres and the app together:

```bash
docker compose up --build
```

The app runs the Flyway migrations on boot and seeds a few demo clients.

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Tests (Docker has to be running — the integration test starts a real Postgres
with Testcontainers; the FX provider itself is stubbed:

```bash
./mvnw test
```

---

## Configuration

Everything is configurable via environment variables. The defaults work fine
for the Docker setup.

| Variable | Default | Description |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/currency_converter` | JDBC URL |
| `DB_USERNAME` | `currency` | DB user |
| `DB_PASSWORD` | `currency` | DB password |
| `SERVER_PORT` | `8080` | HTTP port |
| `FX_PROVIDER_BASE_URL` | `https://open.er-api.com/v6` | FX provider base URL |
| `FX_PROVIDER_CONNECT_TIMEOUT_MS` | `3000` | Connect timeout |
| `FX_PROVIDER_READ_TIMEOUT_MS` | `5000` | Read timeout |
| `FX_CACHE_TTL_SECONDS` | `60` | How long rates are cached |
| `CONVERSIONS_RETURN_IN_PROGRESS` | `false` | When true, a conversion request for a client that's already converting returns a 409 instead of waiting |

Demo clients seeded by the Flyway migrations (`V2__seed_demo_clients.sql` +
`V3__add_demo_client_003.sql`):

| Client id | Balances |
| --- | --- |
| `CLIENT-001` | 10,000.00 USD, 8,000.00 EUR |
| `CLIENT-002` | 5,000.00 GBP |
| `CLIENT-003` | 6,500.00 CAD |

---

## API

### Get a rate

```bash
curl "http://localhost:8080/rates?from=USD&to=EUR"
```

```json
{ "from": "USD", "to": "EUR", "rate": 0.8531, "timestamp": "2026-08-26T18:00:00Z" }
```

### Convert

```bash
curl -X POST "http://localhost:8080/conversions" \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: CLIENT-001" \
  -H "Idempotency-Key: 6c5f9a1e-1234" \
  -d '{"sourceCurrency":"USD","targetCurrency":"EUR","amount":100.00}'
```

```json
{
  "transactionId": "b2d8f4c0-6f77-4d0e-9c3a-3b6a2f0e9c11",
  "sourceAmount": 100.00,
  "sourceCurrency": "USD",
  "targetAmount": 85.31,
  "targetCurrency": "EUR",
  "rate": 0.8531,
  "timestamp": "2026-08-26T18:01:00Z",
  "balances": [
    { "currency": "EUR", "amount": 8085.31 },
    { "currency": "USD", "amount": 9900.00 }
  ]
}
```

The client id goes in the `X-Client-Id` header rather than the body — keeps the
payload clean and matches how balances are stored (per client). There's also an
optional `Idempotency-Key` header: send the same key twice and you get the same
result back instead of a double debit.

### Conversion history

```bash
curl "http://localhost:8080/conversions?clientId=CLIENT-001&page=0&size=20"
curl "http://localhost:8080/conversions?transactionId=b2d8f4c0-6f77-4d0e-9c3a-3b6a2f0e9c11"
curl "http://localhost:8080/conversions?date=2026-08-26&clientId=CLIENT-001"
```

At least one of `transactionId`, `date`, or `clientId` is required. Returns a
standard paged payload: `{ "content": [...], "page": { "size", "number",
"totalElements", "totalPages" } }`.

### Balances

```bash
curl "http://localhost:8080/clients/CLIENT-001/balances"
```

### Errors

Errors come back in the same shape with a machine-readable `code`:

```json
{ "code": "INSUFFICIENT_FUNDS", "message": "Insufficient funds in source currency balance", "timestamp": "..." }
```

| HTTP | Code | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Bad input, or history query without any filter |
| 404 | `CLIENT_NOT_FOUND` | Unknown client id |
| 404 | `BALANCE_NOT_FOUND` | Client doesn't hold the source currency |
| 404 | `RATE_NOT_FOUND` | Provider doesn't know the currency |
| 422 | `INSUFFICIENT_FUNDS` | Source balance can't cover the amount |
| 409 | `CONVERSION_IN_PROGRESS` | A conversion for the same client is already running (only when `CONVERSIONS_RETURN_IN_PROGRESS=true`) |
| 503 | `PROVIDER_UNAVAILABLE` | FX provider unreachable or timed out |
| 500 | `INTERNAL_ERROR` | Something unexpected (details are logged, not sent to the client) |

---

## Decisions worth knowing about

### Concurrency: one lock per client, in memory

The classic problem here: two `POST /conversions` arrive at the same time for
the same client. If both read the balance, subtract, and write it back, you lose
a debit. So the read → check → debit has to be atomic per client.

The app runs as a single instance, so I went with an in-JVM lock keyed on the
client id (`ClientLockManager`) and hold it for the whole conversion, including
the DB commit. It's the simplest thing that's actually correct here — no DB row
locks, no retry logic, no lock-ordering deadlocks when two clients convert in
opposite directions. The lock is on the client, not the currency pair, so all of
a client's balances stay consistent.

By default a second request for the same client just waits for the first one to
finish. If you'd rather it fail fast, set `CONVERSIONS_RETURN_IN_PROGRESS=true`
(`conversions.return-in-progress` in the yaml) — then it returns a 409
(`CONVERSION_IN_PROGRESS`) instead of waiting. Same-idempotency-key replays are
still served from the pre-lock lookup, so they never hit that 409.

If we ever ran more than one instance, an in-JVM lock stops working (the second
instance wouldn't see it). The natural move is `RedisLockRegistry`, which has
the same `Lock` interface, so it'd be a small swap inside `ClientLockManager`.
One catch with Redis locks: the lease has to be longer than the slowest
transaction, or the lock can expire while a conversion is still running and a
second instance grabs it. Our `finally { lock.unlock() }` releases the lock on
every path and avoids leaks, but for Redis the lease size is what actually
prevents the double-spend.

### Idempotency

The optional `Idempotency-Key` header is stored on
the conversion record with a unique constraint on `(client_id, idempotency_key)`.

We look the key up twice: once before locking (the common case — it's already
there) and again after we've got the lock. The second check matters because two
requests with the same key can both miss on the first lookup; once they
serialize on the lock, conversion is done and lock released.

### Rate caching

Rates are cached with Caffeine (`@Cacheable`) for 60 seconds
(`FX_CACHE_TTL_SECONDS`), keyed by `from:to`. A plain TTL is easy to reason
about and bounds how stale a rate can be; FX rates don't move fast enough for
60s to matter here.

### Money

Everything money-related is `BigDecimal`: balances and amounts at scale 2, rates
at scale 10, rounding with `HALF_EVEN` (banker's rounding). No `double`, no
`float`. Request amounts are validated to at most two decimals and a sane upper
bound.

### FX provider

I used open.er-api.com: free, no API key, and you can pass any base currency
(`GET /v6/latest/{base}`). It sits behind an `FxRateProvider` interface, so
swapping to Frankfurter or exchangerate.host would be a small, contained change.

### Schema

Flyway owns the schema (`V1__init.sql` for the tables, `V2__seed_demo_clients.sql`
for the demo clients). Hibernate's `ddl-auto` is `validate`, so Hibernate never
touches the schema — Flyway is the single source of truth.

### Why a real Postgres in the tests

The integration test boots an actual Postgres 16 via Testcontainers
(`@ServiceConnection`) instead of H2. Testing against the real thing also means the constraints,
migrations, and timestamps handling actually get exercised.
---
