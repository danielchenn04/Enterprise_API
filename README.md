# Enterprise API

A production-grade multi-tenant REST API built with Java 21 and Spring Boot 3, deployed on AWS ECS Fargate. Organizations and their users are fully isolated at both the application layer and the database layer (PostgreSQL Row-Level Security). Machine clients authenticate with scoped API keys; human users authenticate with JWTs.

---

## Architecture

```
                          ┌──────────────────────────────────────────────────┐
                          │                    AWS VPC                       │
  Internet                │  Public Subnets          Private Subnets         │
  ─────────               │  ─────────────           ───────────────         │
  Clients ──► WAF ──────► │  ALB (port 80/443)                               │
                          │       │                                          │
                          │       └──────────────► ECS Fargate Tasks (×2–10) │
                          │                              │         │         │
                          │                         RDS Proxy  Redis (HA)    │
                          │                              │                   │
                          │                         RDS Postgres 16          │
                          │                          (Multi-AZ)              │
                          └──────────────────────────────────────────────────┘
                                                   ▲
                          GitHub Actions ──────────┘
                          (build → test → push ECR → rolling ECS deploy)
```

### Request flow

1. Request hits **WAF** — IP-rate rule blocks >2,000 req/5 min per IP.
2. **ALB** forwards to a healthy ECS task (health-checked via `/actuator/health`).
3. **`JwtAuthFilter`** or **`ApiKeyAuthFilter`** authenticates the request and sets `TenantContext`.
4. **`RateLimitFilter`** checks a per-org token bucket in **Redis** (100 req/min FREE, 1,000 ENTERPRISE) and returns 429 if exhausted.
5. Spring Security `@PreAuthorize` enforces role checks (ADMIN vs MEMBER).
6. Service layer scopes every query by `org_id` and runs `SET LOCAL app.current_org` before each transaction so PostgreSQL **RLS** enforces isolation at the DB layer too.
7. All connections go through **RDS Proxy**, which pools connections and prevents exhaustion during scale-out.

---

## Package layout

| Package | Responsibility |
|---|---|
| `config/` | Security filter chain, OpenAPI, Redis, rate-limit properties |
| `security/` | `JwtAuthFilter`, `ApiKeyAuthFilter`, `TenantContext` (ThreadLocal) |
| `ratelimit/` | Bucket4j + Redis token-bucket limiter; `RateLimitFilter` |
| `tenant/` | Org registration, user signup/login, membership, role management |
| `apikey/` | API key issuance, rotation, revocation (only SHA-256 hashes stored) |
| `resource/project/` | Example business resource, all queries scoped by `org_id` |
| `common/` | Response envelope, global exception handler, custom exceptions |
| `observability/` | Custom Actuator health indicators (Flyway version, HikariCP pool) |

---

## Design decisions & trade-offs

### PostgreSQL Row-Level Security
Every business table has an `org_id` column. Before each transaction the service runs:
```sql
SET LOCAL app.current_org = '<uuid>';
```
PostgreSQL RLS policies then enforce that queries can only touch rows matching `app.current_org`. This is defense-in-depth: even if a bug in the application layer forgets an `org_id` filter, the database silently returns only the correct tenant's rows. The trade-off is that every transaction must set the session variable, adding a small overhead and requiring the application to manage it correctly.

### Redis-backed distributed rate limiting
Per-tenant rate limits are enforced with Bucket4j using Redis as the shared state store. This means limits are correctly applied across all ECS task replicas — an in-memory approach would allow each replica to serve the full quota independently, multiplying the effective limit by the number of tasks. The trade-off is a Redis round-trip on every authenticated request.

### RDS Proxy for connection pooling
ECS tasks connect to **RDS Proxy** rather than directly to Postgres. When auto-scaling fires up new tasks, each opens connections through the proxy rather than directly against the DB, preventing the connection count from spiking beyond Postgres's `max_connections`. The trade-off is added latency (~1 ms) per connection and proxy cost.

### Dual auth filters (JWT + API key)
Human users receive short-lived JWTs (24h) from `POST /auth/login`; machine clients use long-lived API keys (`eak_` prefix, SHA-256 hashed in the DB). Both resolve to the same `TenantContext` so all downstream code is auth-mechanism-agnostic. The trade-off is that two filter chains must be maintained.

### ECS auto-scaling trigger
CPU target-tracking at 60% was chosen over request-count-per-target because the API is compute-bound (BCrypt, JWT signing, JSON serialisation) rather than IO-bound. A 60% target leaves headroom for traffic spikes while scaling out before saturation. Scale-in cooldown is 300 s to avoid thrashing.

---

## Local development

### Prerequisites
- Java 21 (`brew install --cask temurin@21`)
- Docker / OrbStack

### Start the stack

```bash
# Start Postgres (port 5433) and Redis
docker compose up postgres redis -d

# Run the app
./mvnw spring-boot:run
```

### Run tests

```bash
# Unit tests only (no Docker required)
./mvnw test -Dtest="JwtServiceTest,ApiKeyServiceTest,RateLimitFilterTest"

# All tests including Testcontainers integration tests
DOCKER_API_COMPAT=true ./mvnw verify
```

### API docs (Swagger UI)

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### Health & metrics

```
http://localhost:8080/actuator/health    # includes Flyway version + HikariCP pool stats
http://localhost:8080/actuator/metrics/tenant.requests
http://localhost:8080/actuator/metrics/rate.limit.exceeded
```

---

## API reference

### Auth — `POST /api/v1/auth/signup`
```json
{ "orgName": "Acme", "email": "alice@acme.com", "password": "secret" }
```
Returns a JWT. Creates an org, user, and ADMIN membership atomically.

### Auth — `POST /api/v1/auth/login`
```json
{ "email": "alice@acme.com", "password": "secret" }
```

### Projects (authenticated, org-scoped)

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/projects` | ANY | List projects (paginated) |
| `GET` | `/api/v1/projects/{id}` | ANY | Get project by ID |
| `POST` | `/api/v1/projects` | ADMIN | Create project |
| `PATCH` | `/api/v1/projects/{id}` | ADMIN | Update project |
| `DELETE` | `/api/v1/projects/{id}` | ADMIN | Delete project |

### API Keys (ADMIN only)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/apikeys` | Issue a new key (raw key shown once) |
| `GET` | `/api/v1/apikeys` | List active keys (hints only) |
| `DELETE` | `/api/v1/apikeys/{id}` | Revoke a key |
| `POST` | `/api/v1/apikeys/{id}/rotate` | Revoke + issue replacement |

All responses use the envelope `{ "success": true, "data": ..., "timestamp": "..." }`.

---

## Deploying to AWS

### One-time setup

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # fill in db_password, jwt_secret
terraform init
terraform apply
```

### CI/CD (GitHub Actions)

Add these secrets to your GitHub repository:

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM user with ECS/ECR deploy permissions |
| `AWS_SECRET_ACCESS_KEY` | |
| `AWS_REGION` | e.g. `us-east-1` |
| `ECR_REPOSITORY` | Repository name (from `terraform output ecr_repository_url`) |
| `ECS_CLUSTER` | From `terraform output ecs_cluster_name` |
| `ECS_SERVICE` | From `terraform output ecs_service_name` |
| `ECS_TASK_DEFINITION` | Task family name (same as `app_name`, default `enterprise-api`) |

Every push to `main` triggers: build → unit + integration tests → Docker push to ECR → rolling ECS deploy (zero downtime, min 100% healthy).
