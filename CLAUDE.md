# RepUp — instructions for Claude Code

RepUp is a personalised strength-training planner and workout logger, built as university coursework (TSI, Web Development and UX). **`SPEC.md` is the source of truth — read it before planning any work.** If a request conflicts with SPEC.md, point it out before coding.

The owner is building this entirely through vibe coding and does not write Java. Explain decisions in plain language and keep the code conventional, so it is easy to demo.

## Stack (do not change without asking)
Java 21 · Spring Boot 3 (Maven) · Thymeleaf · Spring Security · Spring Data JPA · PostgreSQL · Flyway · Tailwind CSS · vanilla JS + Chart.js · jsoup · JUnit 5. Deployed with Docker on Render; DB on Neon.

## Commands
- Start local DB: `docker compose up -d`
- Run app (dev profile): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Tests: `./mvnw test`
- Tailwind build/watch: see `package.json` / README once set up
(Update this section when commands change.)

## Code conventions
- Package root `com.repup`, layered: `web` (controllers), `service`, `repository`, `domain` (entities), `config`, `dto`.
- Controllers stay thin; business logic lives in services. `ProgramGenerator` and `ProgressionService` are pure Java and unit-tested.
- Schema changes only through new Flyway migrations (`V{n}__description.sql`); never edit an applied migration.
- Constructor injection, no field `@Autowired`. Records for DTOs.
- Thymeleaf: shared layout + fragments; `th:text` by default; `th:utext` only for jsoup-sanitised HTML.
- Tailwind utility classes; design tokens (accent colour, fonts) defined once in the Tailwind theme. Every UI must work at 360 px and in dark mode.
- No new dependencies without saying why; record significant ones in `DECISIONS.md`.

## Non-negotiables
- The browser never calls wger; only the back-end does (assignment requirement).
- Every query for user data checks ownership in the service layer.
- CSRF stays enabled; JSON endpoints send `X-CSRF-TOKEN`.
- No secrets in the repo; use env vars and keep `.env` git-ignored.
- Public pages must keep their SEO elements (title, meta description, canonical, JSON-LD) when changed.

## Workflow
1. For each feature: read the relevant SPEC.md section, propose a short plan, then implement in small steps.
2. After each step: compile, run tests, and say how to check it in the browser.
3. Suggest a commit with a clear message after each working step.
4. **Log decisions:** when choosing a library, pattern or approach (anything the report might need to justify), append to `DECISIONS.md`:
   ```
   ## <Decision title>
   - Decision:
   - Alternatives considered:
   - Why:
   - Trade-offs:
   ```
5. When a major feature is finished, add or refresh its section in `docs/demo-notes.md`: 3–6 plain-English sentences on how it works and where the code lives.
6. Keep the requirement → implementation table in SPEC.md §2 accurate.
