# RepUp — Decision log

Every significant technical choice, with its reasoning. This file is the main source for the report's "selection of techniques and approaches" section.

## Server-side rendering with Spring Boot + Thymeleaf (instead of a SPA)
- Decision: Render all pages on the server with Thymeleaf; add small vanilla-JS modules only where interactivity is needed (workout logging, rest timer, charts, theme toggle).
- Alternatives considered: Spring Boot REST API + Next.js (React SSR); Spring Boot API + React SPA (Vite).
- Why: SEO is a requirement, and server-rendered HTML is fully visible to crawlers without extra work. One application keeps session management simple (same-origin cookie, Spring Security form login, built-in CSRF) and halves the deployment surface. The back-end must be Java, and a single Java codebase is easier to demo. A plain React SPA was rejected because client-side rendering weakens SEO.
- Trade-offs: Rich client interactions need hand-written JS; less "modern" than a React front-end.

## Rule-based programme generator (instead of AI-generated programmes)
- Decision: Generate programmes with deterministic rules (split by days, filter by equipment and level, rep ranges by goal), plus double-progression rules for weight suggestions.
- Alternatives considered: Generating programmes with an LLM API.
- Why: Predictable, explainable output (every recommendation can be justified), unit-testable, free, and no dependency on a third-party AI service during the demo. An AI coach remains a possible extension.
- Trade-offs: Less flexible than an AI; the rules and the curated exercise tagging must be maintained by hand.

## Dark-brown brand accent with a lighter dark-mode variant
- Decision: Brand accent is a dark "espresso" brown in light mode and a lighter caramel of the same hue in dark mode.
- Alternatives considered: Blue accent (like Hevy and most fitness apps); a single accent colour for both themes.
- Why: Brown sets RepUp apart from the many blue fitness apps and feels warm and "iron/leather gym". One dark brown cannot reach readable contrast on a near-black background, so dark mode uses a lighter tone of the same hue to keep WCAG AA contrast.
- Trade-offs: Two accent values to maintain; brown has less "energy" than bright colours, so the UI relies on clear layout rather than colour for emphasis.

## Kilograms only
- Decision: All weights in kg; no unit switching.
- Alternatives considered: kg/lb toggle.
- Why: The target users (Europe) use kg; it keeps the data model, progression rounding (2.5 kg steps) and UI simpler.
- Trade-offs: Not suitable for users who train in pounds.

## Cache wger data in PostgreSQL (instead of calling the API on every request)
- Decision: A back-end sync job copies wger exercises into our own tables; pages read from the DB.
- Alternatives considered: Live API calls per request; live calls with an in-memory cache.
- Why: Faster pages, the site keeps working if wger is down, fewer requests to a free community API, our data (programmes, logs) can be joined with exercises, and stable slugs give SEO-friendly URLs.
- Trade-offs: Data can be up to one sync interval out of date; needs sync and upsert logic.

## Stay on Spring Boot 3.5.x (instead of Spring Boot 4)
- Decision: Use Spring Boot 3.5.x (starter parent 3.5.16) for the whole project.
- Alternatives considered: Spring Boot 4.
- Why: Decided by the owner on 26 Oct. SPEC.md names Spring Boot 3, and most tutorials and examples still target 3.x, which makes the code easier to follow and to demo.
- Trade-offs: The 3.5 line gets fewer updates over time; dependency versions should be checked for security fixes before submission.

## Add Spring Security in Week 2, not in the skeleton
- Decision: Leave spring-boot-starter-security out of the initial skeleton and add it in Week 2 together with its configuration.
- Alternatives considered: Adding it on day one.
- Why: As soon as Spring Security is on the classpath it locks every page behind a generated login form until it is configured, which would get in the way of the hello-world deploy and the base layout work.
- Trade-offs: The week-1 hello-world deploy has no security headers or login yet.
