# Java Coding Challenge Instructions

## Delivery style
- Keep responses compact and practical.
- Prefer small, trackable changes.
- Avoid large refactors unless explicitly requested.

## Priority order
1. Security first (input validation, auth boundaries, secret handling, safe defaults).
2. Observability in production (structured logs, traceability, correlation IDs, clear error context).
3. Scalability and reliability (stateless design, timeouts, retries, graceful failure paths).
4. Testability and behavior focus (BDD-oriented scenarios and integration tests).
5. Deployment readiness (Docker for dev and prod with reproducible builds).

## Implementation expectations
- Add logging that is useful for production debugging, not noisy local-only logs.
- Ensure requests are traceable end-to-end (request ID or trace ID propagated through logs).
- Prefer secure-by-default configuration in `application.properties` and runtime settings.
- Design endpoints/services so they can scale horizontally.
- Add or update integration tests for user-visible behavior changes.

## Testing guidance
- Use behavior-driven naming for tests (`should_<expected_behavior>_when_<condition>`).
- Cover positive, negative, and boundary cases.
- Prefer integration tests for API contracts; keep unit tests focused and fast.

## Containerization guidance
- Maintain a production-ready `Dockerfile` and a development-friendly container workflow.
- Keep images minimal, pinned, and reproducible.
- Avoid baking secrets into images.

## Local run instructions (May 2026 baseline)
- Assume Java 25 is used for Maven/test commands in this project.
- Use this exact format when running tests:
  - `cd /Users/bhanu.akula/projects/java-coding-challenge`
  - `JAVA_HOME=/opt/homebrew/opt/openjdk@25 PATH="/opt/homebrew/opt/openjdk@25/bin:$PATH" ./mvnw test -f pom.xml`
- Use this exact format when running the app:
  - `cd /Users/bhanu.akula/projects/java-coding-challenge`
  - `JAVA_HOME=/opt/homebrew/opt/openjdk@25 PATH="/opt/homebrew/opt/openjdk@25/bin:$PATH" ./mvnw spring-boot:run -f pom.xml`

## Change management
- If a requested change is large, split it into smaller steps and explain trade-offs.
- Never implement more than 100 lines of code in a single task step without explicit user review and approval.
- Call out security, observability, and scaling impact in implementation notes.
