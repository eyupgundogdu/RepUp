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

## Load local DB settings from .env via Spring's config import
- Decision: The `dev` profile reads `.env` with `spring.config.import: optional:file:.env[.properties]`, so the app and Docker Compose share one git-ignored file for the database name, user and password.
- Alternatives considered: Exporting the variables in the shell before each run; a dotenv library; hard-coding dev credentials in `application-dev.yml`.
- Why: Built into Spring Boot (no new dependency), nothing secret is committed, and `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` works without extra setup. Production (Render) uses real environment variables instead.
- Trade-offs: Only works when the app is started from the project folder; `.env` must stay in simple `KEY=value` form.

## Tailwind CSS v4 built with the standalone npm CLI
- Decision: Compile Tailwind v4 (`tailwindcss` + `@tailwindcss/cli`, pinned to 4.3.3) with `npm run build:css` from `src/main/frontend/app.css` into `src/main/resources/static/css/app.css`. The output file is git-ignored and is rebuilt in the Docker image.
- Alternatives considered: Tailwind Play CDN script; Tailwind v3 with `tailwind.config.js`; running npm from Maven via `frontend-maven-plugin`; Bootstrap.
- Why: The CDN is meant for prototyping only (large runtime script, no purging, hurts Lighthouse). v4 keeps all configuration in one CSS file and only ships the classes the templates use (≈9 KB today). Running the CLI directly keeps the Maven build plain Java and easy to explain.
- Trade-offs: Node.js is needed for front-end work and in the Docker build stage; developers must run `npm run watch:css` while editing templates.

## Design tokens as CSS variables with a .dark class
- Decision: Colours are CSS variables on `:root` (light) and overridden on `.dark` (dark); Tailwind's `@theme inline` maps them to utilities like `bg-accent`, `text-muted`, `bg-surface`. Dark mode is class-based (`@custom-variant dark`), so a user toggle can override the OS setting.
- Alternatives considered: Tailwind's default media-query dark mode; writing `dark:` variants on every element.
- Why: Each colour is defined once, components stay free of duplicated `dark:` colour classes, and a class lets the Settings page store the user's choice.
- Trade-offs: Needs a small inline script in `<head>` to set the class before first paint (no flash of the wrong theme), which must be allowed by the Content-Security-Policy.

## Colour palette and WCAG contrast check
- Decision: Final values (unchanged from SPEC §13 after checking). Light: bg `#FAF8F6`, surface `#FFFFFF`, text `#1C1917`, muted `#57534E`, accent `#5C3A21`, hover `#4A2E1A`, soft `#F3EBE3`, on-accent `#FFFFFF`. Dark: bg `#0E0C0B`, surface `#1A1715`, text `#F5F5F4`, muted `#A8A29E`, accent `#C8996B`, hover `#D9AE82`, soft `#2A1F17`, on-accent `#1A120B`.
- Alternatives considered: Tailwind's stock stone/amber palette.
- Why: Measured with the WCAG 2.1 relative-luminance formula; every text pairing passes AA (≥ 4.5:1). Lowest ratios: dark accent text on soft tint 6.29:1, dark accent on surface 6.99:1, light muted on bg 7.20:1. Button text: light 10.09:1 (white on `#5C3A21`), dark 7.24:1 (`#1A120B` on `#C8996B`). Body text is 16–18:1 in both themes.
- Trade-offs: Borders (`#E7E2DD` / `#2E2925`) are decorative and below 3:1, so they must never be the only way to show state or a form field's edge.

## Self-hosted Inter variable font
- Decision: Serve the Latin subset of Inter (variable weight, 48 KB woff2, from `@fontsource-variable/inter` 5.3.0) from `/fonts/`, preloaded in `<head>`, with `font-display: swap`. Licence (SIL OFL 1.1) is kept next to the file.
- Alternatives considered: Google Fonts link; system font stack only.
- Why: No request to a third party (better privacy, no extra DNS/TLS handshake, simpler CSP); one small file covers all weights.
- Trade-offs: Only Latin characters use Inter; others (e.g. Turkish ş, ğ) fall back to the system font. The font must be updated by hand.

## Shared layout with plain Thymeleaf fragments (no Layout Dialect)
- Decision: One `layout.html` defines `page(title, description, content)`; each page replaces its `<html>` with that fragment and passes its own `<main>`. The layout owns the `<head>` (title, meta description, canonical link, Organization JSON-LD, fonts, CSS, theme script), the skip link, header and footer.
- Alternatives considered: `thymeleaf-layout-dialect` (decorator pattern); copying the head into every page.
- Why: Built into Thymeleaf, so no extra dependency; SEO tags live in one place and every page is forced to supply a title and description.
- Trade-offs: Pages that need extra `<head>` content (e.g. page-specific JSON-LD or Open Graph) will need another fragment parameter, added in the Week 3 SEO work.

## Canonical URL built from APP_BASE_URL
- Decision: `SeoModelAdvice` adds `canonicalUrl` = `app.base-url` + request path to every page model; query strings are dropped.
- Alternatives considered: Building the URL from the incoming request host; hard-coding the domain.
- Why: The request host can be spoofed or differ behind Render's proxy; a configured base URL is stable and gives one canonical address per page (filter/tracking parameters do not create duplicates).
- Trade-offs: `APP_BASE_URL` must be set correctly in production, or canonical links point to localhost.

## No-flash dark mode: inline head script + deferred toggle script
- Decision: A one-line inline script in `<head>`, placed before the stylesheet, reads `localStorage.theme` (or the OS `prefers-color-scheme`) and sets `class="dark"` before first paint. `/js/theme.js` (deferred) handles the header toggle, saves the choice, and follows live OS changes only while no choice is saved.
- Alternatives considered: Applying the theme from an external script (flashes the light theme first); CSS-only `prefers-color-scheme` (no manual override); storing the theme in a cookie and rendering the class on the server.
- Why: Meets FR-16 with no flash, works for logged-out visitors, and stays tiny. The cookie approach is not needed until the theme is saved per user (`user_profile.theme`, Week 5).
- Trade-offs: The inline script must be allowed by the Content-Security-Policy. Its hash is `sha256-itl8bNhUz+0cvkem/Ed2RBsdLTZ1wlO1XpKFstUATYY=`; it must be recalculated if the script changes by even one character.
