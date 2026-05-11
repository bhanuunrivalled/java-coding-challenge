# AI Usage

## Tool Used

- **Kiro** (AI-powered development environment by AWS)

## How I Used It

1. **Coding standards and architecture guidance** — I asked Kiro to follow standard Spring Boot layered architecture (Controller → Service → Repository) and to keep changes small and reviewable. When the code drifted from this pattern (e.g., duplicate logic across classes), I asked it to refactor into a cleaner structure.

2. **Delta load design** — I described the problem (delta load only fetched one day, no guard against empty DB) and discussed the approach before implementing. I reviewed each piece and pushed back when the code was too complex for the scope of this project.

3. **Documentation and research** — I used Kiro to look up AWS documentation (ECS, CloudWatch, Container Insights), Spring Boot actuator configuration, and H2 multi-instance behavior. We verified findings together before applying them.

4. **Error handling** — I noticed stack traces leaking in API responses and asked for a global exception handler. I directed the approach: clean JSON errors, no internals exposed.

5. **Testing and verification** — I used Kiro to run Maven commands, fix test failures, and add JaCoCo for coverage reporting. I verified the multi-instance H2 behavior myself by running two JARs locally.

## My Decision-Making

- **Directed** smaller changes per step — enforced max ~80 lines per change so I could review each piece
- **Directed** the refactoring — merged duplicate code (full load + delta load) into one `FxLoadService` after spotting the duplication myself
- **Simplified** overly complex suggestions — chose straightforward enum + record patterns over more advanced alternatives to keep the code readable
- **Verified independently** — ran the app, tested endpoints manually, confirmed multi-instance behavior with two JARs
