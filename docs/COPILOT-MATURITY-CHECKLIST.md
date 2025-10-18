# Copilot Workspace Maturity Checklist

This checklist is designed to bring any Copilot-managed workspace to the same operational and engineering maturity as the ExploreSG `exploresg-auth-service` project.

Use this as a gate for onboarding a new repository or auditing an existing one. Each section includes concrete verification steps, example commands, and suggested file locations.

---

## 1. Project metadata and repo hygiene

- [ ] Repository description, README, and CONTRIBUTING exist and are up-to-date
  - Files: `README.md`, `CONTRIBUTING.md`, `HELP.md` or `docs/README.md`
  - Verify: README explains service purpose, endpoints, and local run instructions.
- [ ] License file present
  - File: `LICENSE`
- [ ] Code of Conduct / SECURITY.md
  - File: `SECURITY.md`
- [ ] Issue templates and PR templates
  - Directory: `.github/ISSUE_TEMPLATE`, `.github/PULL_REQUEST_TEMPLATE`

---

## 2. Configuration & environment variables

- [ ] Centralized configuration files
  - Files: `src/main/resources/application.properties` (and profile-specific `application-*.properties`)
  - Verify properties use environment overrides with defaults where appropriate: `property=${ENV_VAR:default}`
- [ ] `.env` files and examples
  - Files: `.env`, `.env.example`, `.env.production`
  - Verify: `.env.example` documents required vars with descriptions and expected formats
- [ ] Secrets handling
  - Verify: No secrets in repo (use `git-secrets`, `pre-commit` hooks)
  - Recommended: Use external secret manager (Azure Key Vault, AWS Secrets Manager, HashiCorp Vault)
- [ ] Validate required environment variables at startup
  - Options: fail-fast with clear message, or provide sensible defaults for non-critical vars
  - File: `ApplicationConfig` or a startup health-check bean

---

## 3. CORS and API gateway readiness

- [ ] CORS explicitly configured (not left to defaults)
  - File: `SecurityConfig.java` or `WebMvcConfig.java`
  - Verify: `CorsConfigurationSource` or `addCorsMappings` present; allowed origins list referenced from properties
- [ ] Preflight behavior covered
  - Verify: `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Credentials`, `Access-Control-Expose-Headers`, `Access-Control-Max-Age` set appropriately
- [ ] Production origins set in `env.production` and example files
- [ ] Recommendation: prefer exact origins over `*` in production

---

## 4. Logging & Observability

- [ ] Structured logging
  - Format logs as JSON or consistent structured format via Logback or Log4J configuration
  - File: `src/main/resources/logback-spring.xml` or equivalent
- [ ] Request tracing
  - Include request-id/correlation-id for distributed tracing; add interceptor to propagate header
  - Verify presence of `RequestLoggingInterceptor` or servlet filter
- [ ] Metrics
  - Expose application metrics via Micrometer + Prometheus
  - Endpoints: `/actuator/metrics`, `/actuator/prometheus` (or `/actuator/prometheus` managed by micrometer-registry-prometheus)
- [ ] Health checks
  - Actuator health configured; readiness and liveness probes set up for Kubernetes
  - Files: `application-prod.properties` and `kubernetes/*.yaml` contain probes
- [ ] Tracing & APM
  - Integrate OpenTelemetry/Zipkin/Jaeger where appropriate; document sampling and retention
- [ ] Log retention and aggregation
  - Document shipping logs to Elasticsearch/Cloud Logging or use log forwarder

---

## 5. Security

- [ ] Authentication & Authorization
  - JWT secret/source config reviewed, preferably loaded from secret store
  - Access control rules present in `SecurityConfig.java`
- [ ] Input validation and sanitization
  - Use validation annotations (`@Valid`, `@NotNull`) on DTOs
- [ ] Dependency scanning
  - Configure Dependabot or renovate and periodically run vulnerability scans
- [ ] SAST / Code scanning
  - Enable GitHub Advanced Security or other SAST tools in CI
- [ ] Secrets detection
  - Add `git-secrets` or similar pre-commit checks
- [ ] Security headers
  - Configure common security headers (CSP, X-Frame-Options, X-Content-Type-Options) via filters or web server config

---

## 6. Docker & container hardening

- [ ] Minimal base images
  - Use slim or distroless base images where possible (e.g., eclipse-temurin:jre-jammy-slim or `gcr.io/distroless/java`)
- [ ] Non-root user
  - Ensure container runs as non-root user and drops capabilities
- [ ] Multi-stage builds
  - Use multi-stage Dockerfile to keep final image small
- [ ] Image scanning
  - Scan images for CVEs (Trivy, Clair)
- [ ] Immutable tags and reproducible builds
  - Avoid `latest` in production images; pin versions
- [ ] Reduce attack surface
  - Remove package managers and debugging tools from final image
- [ ] Resource limits and security context
  - Set CPU/memory requests and limits; set Kubernetes Pod security contexts
- [ ] Dockerfile checklist
  - Files: `Dockerfile` should include healthcheck, `USER`, explicit `WORKDIR`, no secrets in build args

---

## 7. Testing strategy

- [ ] Unit tests
  - Keep pure unit tests fast and isolated; use Mockito where necessary
- [ ] Integration tests
  - Use profile `integration-test`; test against in-memory DB (H2) or Testcontainers
- [ ] CI pipeline runs all tests
  - Ensure `mvn test` is run in CI; `mvn verify` for integration tests if applicable
- [ ] Test stability
  - Avoid brittle timing-based assertions; mock external dependencies
- [ ] Code coverage
  - Use JaCoCo to measure coverage; set a sensible threshold

---

## 8. CI / CD

- [ ] Build pipeline
  - Use GitHub Actions / Azure Pipelines to build, test, and publish artifacts
  - Steps: checkout, cache dependencies, mvn -B -DskipITs test, build image, scan image
- [ ] PR checks
  - Enforce tests pass, linters, and security checks before merge
- [ ] Release process
  - Tagging strategy and artifact publishing (Docker registry, Maven repo)
- [ ] Deployment
  - Use IaC (Bicep/Terraform/Helm) with separate staging and production flows
- [ ] Rollback strategy
  - Implement blue/green or canary rollouts where possible

---

## 9. Documentation & runbooks

- [ ] Runbooks for:
  - Deployments, rollbacks, incident response, log investigation, on-call escalation
- [ ] Architecture diagram
  - Add diagrams under `docs/` or `kubernetes/` to show service topology
- [ ] Environment-specific docs
  - Document how `application-prod.properties` and `.env.production` are used

---

## 10. Verification checklist (how to audit a workspace)

- [ ] Local run
  - `./mvnw -DskipITs test` passes locally
  - `docker build -t myapp:local .` succeeds and container starts on `java -jar` without secrets
- [ ] Docker test
  - Run unit tests inside a Maven docker image (example):

```powershell
# Windows PowerShell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.4-eclipse-temurin-17 mvn -B -DskipITs test
```

- [ ] Lint & static analysis
  - Run code formatter, linter, and SAST scans through CI
- [ ] Observability smoke test
  - Check `/actuator/health` and `/actuator/prometheus` endpoints
- [ ] Security smoke test
  - Check for missing security headers and ensure CSP/other headers are present

---

## 11. Useful commands and snippets

- Run unit tests in Docker (PowerShell):

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.4-eclipse-temurin-17 mvn -B -DskipITs test
```

- Scan image with Trivy:

```bash
trivy image --severity HIGH,CRITICAL myregistry/myimage:tag
```

- Check CORS preflight (PowerShell):

```powershell
$resp = Invoke-WebRequest -Uri "https://api.example.com/endpoint" -Method Options -Headers @{
    "Origin" = "https://www.example.com"
    "Access-Control-Request-Method" = "POST"
}
$resp.Headers
```

---

## 12. Example file checklist for a minimal Java Spring Boot app

- `pom.xml` – pinned dependencies, maven plugins for surefire/failsafe and jacoco
- `Dockerfile` – multi-stage, non-root, healthchecks
- `src/main/resources/application.properties` and `application-prod.properties`
- `src/main/java/.../config/SecurityConfig.java` – CORS & security rules
- `src/main/resources/logback-spring.xml`
- `src/test/resources/application-integration-test.properties`
- `docs/COPILOT-MATURITY-CHECKLIST.md`

---

## 13. Next steps (recommended)

1. Commit this checklist to the `docs/` folder and propose a PR.
2. Run the verification steps in CI for a clean run.
3. Iterate on any gaps found (especially security headers, secrets, and image scanning).
4. Add an automated job in CI to fail if critical checklist items are missing (optional advanced step).

---

If you'd like, I can:

- Open a PR with this file on the current branch.
- Add a small GitHub Actions workflow to run the Docker-based unit test command.
- Add a pre-commit hook or GitHub Action to scan for secrets.

Tell me which follow-up you'd like and I'll proceed.
