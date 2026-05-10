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

## Change management
- If a requested change is large, split it into smaller steps and explain trade-offs.
- Never implement more than 100 lines of code in a single task step without explicit user review and approval.
- Call out security, observability, and scaling impact in implementation notes.

## Design checker skill (Clean Architecture)
When proposing or reviewing changes, run this design checker by default and report violations before coding:

1. Dependency rule
- Source dependencies must point inward: `api`/`batch`/`infrastructure` -> `service` -> `domain`.
- Inner layers must not import framework/web/db/client details from outer layers.

2. Package responsibility
- `currency.api`: controllers, request/response mapping only.
- `currency.batch`: batch job config, reader/processor/writer, scheduling/launch.
- `currency.service`: application use-cases and orchestration.
- `currency.domain`: core business model/rules.
- `currency.infrastructure`: external clients, persistence adapters, repository implementations.

3. Boundary data
- Do not pass framework-specific objects across boundaries.
- Use simple DTO/record structures when crossing adapter/use-case boundaries.

4. Framework independence
- Keep Spring Batch/JPA/HTTP annotations and APIs out of `domain` and, where practical, out of core `service` logic.

5. Runtime separation intent
- API reads from local store.
- Batch owns upstream ingestion (full + delta).
- API must not call upstream providers directly.

6. Testability guardrails
- Use-case logic should be unit-testable without booting Spring context.
- API contracts should be covered by integration tests.
- Batch transformation rules should have focused processor tests.

If any rule is violated, list findings with severity and file references before implementing additional changes.
