# Project 1 — Multi-Tenant Enterprise API Platform

**Build spec for Daniel Chen · WHOOP Enterprise Backend prep**

A B2B backend where organizations (tenants) register, manage member accounts and roles,
receive scoped API keys, and query their data through a versioned REST API — hardened with
edge protection, per-tenant rate limiting, auto-scaling, and connection pooling.

**Primary stack:** Java 21 · Spring Boot 3 · PostgreSQL · Redis · Docker · AWS (ECS Fargate,
RDS, ALB, WAF, CloudWatch) · GitHub Actions · JUnit 5 + Testcontainers · OpenAPI.

---

## Gap coverage (why each piece is here)

| Feature | WHOOP JD line it closes | Your current gap |
|---|---|---|
| Java + Spring Boot backend | "RESTful APIs (Java preferred)" | Java as a *production* language |
| Multi-tenant orgs, roles, API keys | "authentication... account management" | enterprise-scale auth |
| PostgreSQL + Row-Level Security | "relational databases such as PostgreSQL" | reinforces (already strong) |
| Deploy on AWS (ECS/RDS/ALB) | "operating services in cloud environments (AWS)" | **AWS — biggest gap** |
| JUnit + Testcontainers | "high-quality, well-tested code" | automated testing |
| Actuator + CloudWatch dashboards | "observability, monitoring, operational practices" | observability |
| Per-tenant rate limiting | "reliability... appropriate for enterprise customers" | reliability under load |
| ECS auto-scaling + RDS Proxy pooling | "scalable, reliable, and maintainable software" | scalability evidence |
| GitHub Actions CI/CD | "engineering foundations that scale" | SDLC |
| OpenAPI docs + README design doc | "documentation, patterns" | technical documentation |

---

## Repository structure

```
enterprise-api/
├── src/
│   ├── main/
│   │   ├── java/com/danielchen/enterpriseapi/
│   │   │   ├── EnterpriseApiApplication.java
│   │   │   ├── config/            # security, rate-limit, datasource, OpenAPI beans
│   │   │   ├── security/          # JWT filter, API-key filter, tenant resolver
│   │   │   ├── ratelimit/         # token-bucket limiter (Bucket4j + Redis)
│   │   │   ├── tenant/            # org registration, membership, roles
│   │   │   ├── apikey/            # key issue / rotate / revoke
│   │   │   ├── resource/          # the actual business resource(s), e.g. "projects"
│   │   │   ├── common/            # error handling, response envelope, pagination
│   │   │   └── observability/     # metrics, custom Actuator health indicators
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/      # Flyway migrations (schema + RLS policies)
│   └── test/
│       └── java/...               # unit + Testcontainers integration tests
├── Dockerfile
├── docker-compose.yml             # local: app + postgres + redis
├── infra/                         # IaC: Terraform or AWS CDK
│   ├── network.tf                 # VPC, subnets, security groups
│   ├── data.tf                    # RDS Postgres, RDS Proxy, ElastiCache Redis
│   ├── compute.tf                 # ECS cluster, service, task def, autoscaling
│   └── edge.tf                    # ALB, WAF (rate-based rule), Shield
├── .github/workflows/ci.yml       # build → test → image → deploy
├── openapi.yaml                   # generated / published API contract
└── README.md                      # architecture, design decisions, run instructions
```

---

## Milestone checklist

Each milestone is shippable on its own and earns a concrete resume bullet. Build top to bottom.

### M1 — Skeleton + first resource (Java REST + Postgres)
- [ ] Spring Boot 3 project (Web, Data JPA, Validation, Flyway, Actuator).
- [ ] `docker-compose.yml` running app + Postgres + Redis locally.
- [ ] One business resource (e.g. `projects`) with full CRUD REST endpoints, `/api/v1/...`.
- [ ] Flyway migration for the initial schema.
- [ ] Consistent JSON response envelope + global exception handler.

*Earns:* "Built a versioned RESTful API in Java/Spring Boot backed by PostgreSQL."

### M2 — Identity: tenants, accounts, roles
- [ ] `organizations`, `users`, `memberships` tables; a user belongs to an org with a role.
- [ ] Signup/login issuing JWTs (Spring Security + jjwt); passwords hashed with BCrypt.
- [ ] `JwtAuthFilter` validates the token and resolves the **tenant + role** onto the request.
- [ ] Role-based authorization (`ADMIN` vs `MEMBER`) on endpoints.

*Earns:* "Implemented organization-level authentication and account management: signup/login,
JWT issuance, and role-based access control."

### M3 — Tenant isolation (defense in depth)
- [ ] Every business row carries `org_id`; queries are scoped to the caller's tenant.
- [ ] **PostgreSQL Row-Level Security** policies as a second enforcement layer (set
      `app.current_org` per connection; policy filters on it).
- [ ] Integration test proving Tenant A cannot read Tenant B's rows.

*Earns:* "Enforced multi-tenant data isolation via app-layer scoping and PostgreSQL
Row-Level Security, guaranteeing per-tenant privacy."

### M4 — API keys for machine clients
- [ ] Issue / list / rotate / revoke API keys per org (store only a hash of the key).
- [ ] `ApiKeyAuthFilter` accepts `Authorization: ApiKey ...` and resolves the tenant.
- [ ] Keys scoped to a role; revoked keys rejected immediately.

*Earns:* "Added server-to-server API-key auth with hashing, scoping, and rotation/revocation."

### M5 — Per-tenant rate limiting (reliability)
- [ ] Token-bucket limiter with **Bucket4j backed by Redis** (distributed, survives scale-out).
- [ ] Quota keyed by tenant/API key; return **HTTP 429** with `Retry-After` when exceeded.
- [ ] Configurable tiers (e.g. free vs enterprise limits).
- [ ] Test: a burst from one tenant is throttled without affecting another tenant.

*Earns:* "Protected the shared platform from noisy-neighbor overload with a Redis-backed
per-tenant token-bucket rate limiter returning 429s."

### M6 — Tests + observability
- [ ] JUnit 5 unit tests for services + filters.
- [ ] **Testcontainers** integration tests spinning up real Postgres + Redis.
- [ ] Actuator health/metrics; custom health indicators for DB + Redis.
- [ ] Micrometer metrics (request latency, 429 count, per-tenant volume).

*Earns:* "Achieved reliable, well-tested code with unit + Testcontainers integration tests
and Micrometer/Actuator metrics."

### M7 — Cloud deploy on AWS
- [ ] Dockerfile (multi-stage, small runtime image).
- [ ] IaC (Terraform or CDK): VPC, **RDS Postgres**, **RDS Proxy** (connection pooling),
      **ElastiCache Redis**, **ECS Fargate** service behind an **ALB**.
- [ ] **AWS WAF** rate-based rule + Shield on the ALB (edge DDoS/abuse protection).
- [ ] **ECS auto-scaling** policy (target-tracking on CPU or request-count-per-target).
- [ ] CloudWatch dashboard + an alarm (e.g. on 5xx rate or p99 latency).

*Earns:* "Deployed to AWS on ECS Fargate behind an ALB with WAF/Shield, RDS Proxy connection
pooling, and CloudWatch-driven auto-scaling."

### M8 — CI/CD + docs
- [ ] GitHub Actions: build → run tests → build image → push to ECR → deploy to ECS
      (rolling update).
- [ ] Publish OpenAPI/Swagger UI.
- [ ] README with the architecture diagram, design decisions, and trade-offs
      (why RLS, why Redis for rate limiting, how auto-scaling is triggered).

*Earns:* "Automated build/test/deploy with GitHub Actions and documented the system contract
via OpenAPI and an architecture design doc."

---

## Suggested pace

Part-time over ~2–3 weeks. If time is tight, **M1–M3 + M6–M7 are the non-negotiable core**
(they close Java, auth, PostgreSQL/RLS, testing, observability, and AWS). M4, M5, and M8 are
high-value additions that make it interview-proof — M5 especially, since "how do you stop one
customer overwhelming a shared API?" is a classic enterprise-backend question you'll then
answer from experience.

## Resume bullets you'll be able to write (final, honest form)

- Built and deployed a multi-tenant enterprise REST API in **Java/Spring Boot** on **AWS
  (ECS Fargate, RDS, ALB)**, serving organizations with role-based accounts and scoped API keys.
- Enforced per-tenant data isolation through application scoping and **PostgreSQL Row-Level
  Security**, with Testcontainers integration tests verifying cross-tenant privacy.
- Protected the platform under load with a **Redis-backed per-tenant rate limiter** (429 +
  Retry-After), **ECS auto-scaling**, and **RDS Proxy** connection pooling.
- Instrumented the service with **Actuator/Micrometer + CloudWatch** dashboards and alarms, and
  automated build/test/deploy via **GitHub Actions** (rolling ECS deploys).
