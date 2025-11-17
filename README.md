# Digital Wallet API

Spring Boot 3 service that powers a digital wallet. Customers can open wallets, deposit money, withdraw/pay, and view their transaction history. Employees have elevated visibility and can approve or deny high-value transactions. The backend is fully stateless (JWT) and uses an in-memory H2 database for local development.

## Architecture Overview

| Layer | Responsibility |
|-------|----------------|
| Controller (`com.wallet.controller`) | HTTP adapters, request validation, response mapping. |
| Service (`com.wallet.service`) | Business logic, orchestration, authorization checks. |
| Strategy (`com.wallet.service.strategy`) | Deposit/withdraw behavior encapsulated via Strategy pattern. |
| Policy (`com.wallet.service.policy`) | Access-control helpers (e.g., wallet ownership rules). |
| Repository (`com.wallet.repository`) | Spring Data JPA repositories for entities. |
| Security (`com.wallet.security`) | JWT generation/verification and authentication filter. |

The H2 datasource auto-loads sample data from `src/main/resources/data.sql` to make manual testing easy. See the “Seed Users” section for credentials.

### Design Notes

- **Transaction Strategies.** I moved the deposit/withdraw math out of `TransactionService` after tripping over branching logic during reviews; the Strategy interfaces make it obvious where balance math lives, and pave the way for new transaction types later.
- **WalletAccessPolicy.** Separate policy object keeps ownership checks honest. Once we have more roles (auditors, finance ops), we can evolve rules in one place instead of sprinkling `if (role == ...)`.
- **Integration-first mindset.** I prefer covering flows with MockMvc tests (see `integration/` tests) to catch wiring regressions—unit tests are great, but hitting the controllers keeps JWT/security filters in play.
- **TODO** When we onboard Redis (or another distributed cache), audit the strategy layer to ensure we evict derived wallet projections properly.

## Requirements & Business Rules

* Wallets have two toggles: `activeForShopping` and `activeForWithdraw`. Shopping payments are only allowed when both the withdraw flag and shopping flag are enabled.
* Every deposit/withdraw creates a `WalletTransaction`. Amounts above 1000 units start as `PENDING`; lower amounts auto-approve.
* Approved deposits affect both `balance` and `usableBalance`. Pending deposits only increase `balance`.
* Approved withdraws reduce both balances. Pending withdraws only reduce `usableBalance` (funds are reserved). Denials roll these adjustments back.
* Employees (`UserRole.EMPLOYEE`) can see or approve everything; customers are restricted to their own wallets/transactions.

## Local Development

### Prerequisites

* Java 21
* Maven 3.9+
* Docker (optional, for container builds)

### Build & Test

```bash
# Format sources and ensure consistent style
mvn spotless:apply

# Compile, run the entire unit suite (30 tests), and create the boot jar
mvn clean verify
```

The Maven lifecycle is wired to run Spotless and Checkstyle during `verify`, and JaCoCo reports live under `target/site/jacoco/index.html`.

### Run the API (local JVM)

```bash
mvn spring-boot:run
# or
mvn clean package && java -jar target/wallet-app-0.0.1-SNAPSHOT.jar
```

The service listens on `http://localhost:8080`. Swagger UI is available at `/swagger-ui.html`. Hot reload is enabled via `spring-boot-devtools`, so saving changes in your IDE triggers an automatic restart when using `spring-boot:run`.

### Seed Users

| Role     | TCKN        | Password       |
|----------|-------------|----------------|
| Employee | 10000000001 | Password123!   |
| Customer | 10000000012 | Customer123!   |

Use the employee token to exercise approval endpoints. New customers can self-register via `POST /api/v1/auth/register`.
The seed script (`src/main/resources/data.sql`) only creates the two users above and two sample wallets so you can start with a clean transaction history—use the HTTP collection to create deposits/withdraws that fit your scenario.

## API Surface

All endpoints are rooted at `/api/v1`. Authentication uses `Authorization: Bearer <token>`.

| Method & Path | Description | Auth |
|---------------|-------------|------|
| `POST /auth/register` | Create a new customer. | Public |
| `POST /auth/login` | Exchange credentials for access/refresh tokens. | Public |
| `GET /customers/me` | Profile of the logged-in user. | Customer/Employee |
| `GET /customers` | List all customers. | Employee |
| `POST /wallets` | Create a wallet tied to the caller (employees may pass `customerId` in the body to target another user). | Customer/Employee |
| `GET /wallets` | List wallets (employees can pass `customerId`, `currency`). | Authenticated |
| `GET /wallets/{id}` | Wallet details after authorization. | Authenticated |
| `PUT /wallets/{id}/settings` | Toggle shopping/withdraw flags. | Authenticated owner/employee |
| `POST /transactions/deposit` | Deposit funds from IBAN/payment source. | Authenticated |
| `POST /transactions/withdraw` | Withdraw/pay to IBAN/payment destination. | Authenticated |
| `GET /transactions?walletId=` | Wallet transaction history. | Authenticated owner/employee |
| `GET /transactions/{id}` | Transaction detail. | Authenticated owner/employee |
| `POST /transactions/{id}` | Approve/deny pending transactions. | Employee |

See `wallet-api.http` for ready-to-run HTTP examples. Swagger UI (`/swagger-ui.html`) now includes a `BearerAuth` button so you can paste a JWT once and call secured endpoints interactively.

### Transaction Approval Flow

- Deposits or withdraws above 1000 units are automatically marked `PENDING`, everything else is auto-approved.
- Employees finalize a transaction by calling `POST /api/v1/transactions/{id}` with `{"status":"APPROVED"}` or `{"status":"DENIED"}`.
- Pending deposits temporarily increase only `balance`; approvals move the amount into `usableBalance` while denials roll `balance` back.
- Pending withdraws reserve the amount by decreasing only `usableBalance`; approvals reduce `balance` while denials restore the reserved funds.

## Docker & Compose

The repository ships with a multi-stage `Dockerfile`. Build/run manually:

```bash
# build the production image (runs mvn clean package -DskipTests inside the container)
docker build -t wallet-app .

# run with a strong JWT secret
docker run -p 8080:8080 -e JWT_SECRET=$(openssl rand -hex 32) wallet-app
```

`docker-compose.yml` provides a turnkey dev environment:

```bash
# build the image and start the container
docker compose up -d

# view logs / stop
docker compose logs -f
docker compose down
```

Configurable env vars (override via `.env` or CLI):

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | `change-me-...` | Secret used to sign JWTs inside the container. |
| `JAVA_OPTS` | (empty) | Extra JVM options (e.g., `-Xmx512m`). |

### `.dockerignore`

Keeps contexts lightweight by ignoring `target/`, IDE metadata, HTTP files, etc.

## Database Access

* Default datasource: H2 in-memory (`jdbc:h2:mem:walletdb`).
* H2 console exposed at `/h2-console` (enabled for local dev only).
* Seed data inserted via `src/main/resources/data.sql`.
* To inspect data live, connect to the console with `jdbc:h2:mem:walletdb`, username `sa`, empty password.

## Monitoring & Metrics

* Spring Boot Actuator is on the classpath. Health/info endpoints live under `/actuator/**` and are publicly accessible.
* Example: `curl http://localhost:8080/actuator/health` (requires hitting from inside the Docker container due to host restrictions in this environment).
* Extend exposure via `management.endpoints.web.exposure.include` in `application.yml` if you wish to enable metrics/env/configprops, then secure them appropriately (e.g., via Spring Security rules).

## Testing & Coverage

* `mvn test` – runs all service/strategy/policy/integration specs (30+ tests).
* `mvn verify` – runs tests + Spotless + Checkstyle + JaCoCo.
* JaCoCo HTML report: `target/site/jacoco/index.html`.

## Formatting & Linting

`spotless-maven-plugin` enforces Google Java Format. It is hooked into the Maven lifecycle (`mvn verify`) and can be run manually via:

```bash
mvn spotless:apply      # format source + pom
mvn spotless:check      # verify formatting without changing files
```

Checkstyle currently reports warnings without failing the build; enable stricter behavior by setting `<failsOnError>true</failsOnError>` once the codebase is compliant.

## Next Steps

* Add more integration/api tests (MockMvc/WebTestClient or Testcontainers) to cover edge cases.
* Swap H2 for PostgreSQL/MySQL in production profiles.
* Plug in an external message bus to publish wallet/transaction events in real time.
