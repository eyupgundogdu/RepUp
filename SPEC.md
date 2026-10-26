# RepUp — Project Specification

> Personalised strength-training planner and workout logger.
> Coursework for **UFCE4W-30-2 Web Development and User Experience** (TSI, 2026/27).
> Submission: report (PDF, max 2750 words) on Moodle by **13/12/2026 23:59**, plus a live code demo.

---

## 1. Concept

RepUp generates a **personalised weekly strength-training programme** from a short onboarding questionnaire (goal, experience level, training days per week, available equipment), lets the user **log each workout set by set** on their phone in the gym, starts a **rest timer** automatically after each completed set, and **suggests the next weight** using progressive-overload rules. A **public exercise library** (data from the wger API) gives the site indexable, SEO-relevant content and acts as the entry point for new visitors.

- **Target users:** people who train with weights (beginner to advanced) and want a plan plus simple tracking, used mostly on a phone in the gym.
- **Language:** English. **Units:** kilograms only.
- **Visual reference:** layout and workout-logging flow inspired by the Hevy app (clean, card-based, mobile-first). All branding (name, logo, colours) is RepUp's own. No Hevy assets are copied.

## 2. Assignment requirement → how RepUp meets it

Keep this table in sync with the implementation; it is the backbone of the report.

| Assignment requirement | RepUp implementation |
|---|---|
| Back-end + front-end | Spring Boot 3 (Java 21) back-end; server-rendered Thymeleaf front-end with small vanilla-JS modules |
| User session management | Spring Security form login, server-side `HttpSession`, secure session cookie, session fixation protection, timeout, logout |
| External API via back-end | wger REST API called **only from the back-end**; data synced and cached in PostgreSQL |
| Responsive design | Mobile-first Tailwind CSS; bottom nav bar on mobile, sidebar/top nav on desktop; tested on 3 breakpoints |
| SEO optimisation | Server-side rendering, unique titles/meta descriptions, clean slugs, canonical URLs, Open Graph, JSON-LD, `sitemap.xml`, `robots.txt`, semantic HTML, Lighthouse ≥ 90 SEO |
| Database communication | PostgreSQL via Spring Data JPA; schema versioned with Flyway migrations |
| UI/UX best practices | Hevy-style logging flow, dark mode, large tap targets, WCAG 2.1 AA, usability test with real users |
| MO4 web security (learning outcome) | BCrypt, CSRF tokens, XSS-safe templating + HTML sanitising, parameterised queries, authorisation on every user resource, security headers, login rate limiting, secrets in env vars |
| MO2 accessibility + usability testing | WCAG 2.1 AA checks (axe/Lighthouse), keyboard navigation, `aria-live` timer, task-based usability test with 3–5 users + SUS questionnaire |

## 3. Tech stack

| Layer | Choice | Why (short) |
|---|---|---|
| Language/runtime | Java 21 (LTS) | Required by module leader for back-end |
| Framework | Spring Boot 3.x, Maven | Industry standard; batteries included (security, data, validation) |
| Views | Thymeleaf (+ Thymeleaf Layout Dialect or fragments) | Server-side rendering → full HTML for crawlers (SEO); one deployable |
| Interactivity | Vanilla JS modules, Chart.js for charts | Only a few interactive widgets; no SPA framework needed |
| Styling | Tailwind CSS (standalone CLI or npm build into `static/`) | Fast responsive layout, built-in `dark:` variant |
| Security | Spring Security 6 | Form login, sessions, CSRF, BCrypt out of the box |
| Database | PostgreSQL 16 | Relational data (users → programmes → sessions → sets) |
| ORM / migrations | Spring Data JPA (Hibernate), Flyway | Repositories + versioned schema |
| HTTP client | Spring `RestClient` | Built-in, simple, synchronous |
| HTML sanitising | jsoup (`Safelist.basic()`) | wger descriptions contain HTML → sanitise before storing/rendering |
| Tests | JUnit 5, Spring Boot Test, MockMvc (Testcontainers optional) | Unit-test the programme generator and progression rules |
| Local dev | Docker Compose (PostgreSQL) | Same DB engine locally and in production |
| Hosting | Render (Docker web service) + Neon (managed PostgreSQL) | Free tiers; HTTPS by default. Verify current free-tier terms at deploy time |

## 4. Architecture

Classic layered monolith:

```
Browser ──HTTPS──> Spring Boot app (Render)
                     ├─ web/        Controllers (Thymeleaf pages) + small JSON endpoints for workout logging
                     ├─ service/    Business logic: ProgramGenerator, ProgressionService, WgerSyncService, ...
                     ├─ repository/ Spring Data JPA repositories
                     ├─ domain/     JPA entities
                     ├─ config/     SecurityConfig, WebConfig, scheduling
                     └─ templates/  Thymeleaf views + static/ (CSS, JS, images)
                   │                         │
                   ▼                         ▼
             PostgreSQL (Neon)        wger REST API (https://wger.de/api/v2/)
```

- Package root: `com.repup`.
- The browser **never** calls wger directly. Exercise data is synced into our DB by `WgerSyncService` and every page reads from our DB.
- Workout logging uses small `fetch()` calls to JSON endpoints (with the CSRF token in a header) so sets save instantly without a full page reload. Every other page is a normal server-rendered page.

## 5. External API: wger

- Base URL: `https://wger.de/api/v2/`. Public read endpoints are expected to work without authentication. **Verify the exact endpoint names and JSON fields against the live API before implementing** (the API shape has changed between versions).
- Expected useful endpoints: `exerciseinfo/` (exercise with category, muscles, secondary muscles, equipment, images, translations), `muscle/`, `equipment/`, `exercisecategory/`. Pagination via `limit`/`offset` and a `next` link. English is expected to be language id `2`; confirm.
- **Sync strategy**
  - `WgerSyncService` pulls all English exercises with pagination and upserts them into `exercise`, `muscle`, `equipment` and join tables, keyed by `wger_id`.
  - Runs on application start if the table is empty, then on a schedule (`@Scheduled`, e.g. weekly) and via an admin-only endpoint/command.
  - Skip exercises without an English name. Generate a unique URL slug from the name (`barbell-bench-press`).
  - Sanitise description HTML with jsoup before storing.
  - Store image URLs (hotlink wger images; add attribution).
  - Respectful client: timeouts, small delay between pages, a descriptive `User-Agent`, retries with back-off, failures logged without breaking the app (the site keeps serving cached data).
- **Why cache instead of calling live** (report material): speed (one DB query vs. N API calls), resilience (site works if wger is down), rate-limit friendliness, the ability to join exercises with our own tables (programmes, logs), and stable slugs for SEO.
- **Attribution:** wger data is CC-BY-SA. Show "Exercise data from wger.de (CC-BY-SA)" in the footer and on exercise pages.

## 6. Functional requirements

### Public area
- **FR-1 Landing page** (`/`): value proposition, screenshots/illustrations, "Get started free" CTA, link to the exercise library.
- **FR-2 Exercise library** (`/exercises`): paginated grid/list; filter by muscle group and equipment; text search by name; server-side filtering via query params (`/exercises?muscle=chest&equipment=dumbbell&q=press`) so filtered pages are shareable.
- **FR-3 Exercise detail** (`/exercises/{slug}`): name, image, sanitised description, primary/secondary muscles, equipment, category, "similar exercises" (same primary muscle). Logged-in users also see their own best set / last performance for that exercise.

### Accounts and sessions
- **FR-4 Register** (`/register`): email, display name, password (min 8 chars), confirm password; server-side validation; duplicate email handled gracefully. After registering → log in automatically → onboarding.
- **FR-5 Login / logout** (`/login`, POST `/logout`): Spring Security form login; generic error message ("Invalid email or password"); optional "Remember me".
- **FR-6 Access control:** all `/app/**` routes require authentication. Users can only read or modify **their own** programmes, sessions and sets (checked in the service layer, not just hidden in the UI).

### Personalisation
- **FR-7 Onboarding** (`/app/onboarding`, shown until completed): multi-step form for goal (strength | muscle), level (beginner | intermediate | advanced), days per week (2–6), equipment (full gym | dumbbells only | bodyweight/home).
- **FR-8 Programme generator:** creates the user's active programme from the onboarding answers using the rules in §8. The user sees the weekly overview (`/app/program`) with days, exercises, sets × rep range and rest time.
- **FR-9 Regenerate:** user can redo onboarding from Settings; the old programme is archived, not deleted (history keeps working).

### Workout logging
- **FR-10 Dashboard** (`/app`): next scheduled workout day with a "Start workout" button, last workout summary, number of workouts this week.
- **FR-11 Live workout screen** (`/app/workouts/{id}`): one card per exercise with a Hevy-style table:
  `Set | Previous | kg | Reps | ✓`
  - "Previous" shows the same set from the last session of this programme day (e.g. `60 kg × 10`).
  - kg and reps are pre-filled with the **suggested** values from §9; the user can change them.
  - Tapping ✓ saves the set immediately (JSON POST) and **starts the rest timer**.
  - Add set / remove set; add a note; "Finish workout" shows a summary (duration, total volume, new PRs).
  - If the page is reloaded, the in-progress session and already-saved sets are restored.
- **FR-12 Rest timer:**
  - Starts automatically on ✓; default duration comes from the programme exercise (§8), adjustable with −15 s / +15 s and skip.
  - Shown as a sticky bar at the bottom of the workout screen with a countdown.
  - Survives page navigation and reloads: store the **end timestamp** (not a counter) in `localStorage` and recompute remaining time.
  - At zero: short beep (Web Audio API), vibration (`navigator.vibrate`, where supported), visual highlight.
  - Accessible: remaining time announced via `aria-live="polite"` at sensible intervals, not every second.
- **FR-13 Progression suggestion** shown per exercise on the workout screen, using the rules in §9 (e.g. "Hit 3×12 last time → try 62.5 kg").

### History and progress
- **FR-14 History** (`/app/history`): list of finished workouts (date, day name, duration, volume); detail page per workout.
- **FR-15 Progress** (`/app/progress`, `/app/progress/{slug}`): per-exercise line chart (Chart.js) of top-set weight and estimated 1RM over time; personal records list (heaviest weight, best estimated 1RM, best volume session).

### Settings and theme
- **FR-16 Dark mode:** follows the OS preference (`prefers-color-scheme`) on first visit; a toggle in the header overrides it; choice stored in `localStorage` and, for logged-in users, in `user_profile.theme`. A tiny inline script in `<head>` applies the theme class **before** first paint (no flash of the wrong theme). Applies to every page, including public ones.
- **FR-17 Settings** (`/app/settings`): display name, change password, theme, default rest times, redo onboarding, delete account.

### Should-have (after MVP)
- **FR-18 Swap exercise:** on the programme or workout screen, swap an exercise for another with the same movement pattern and compatible equipment ("machine is busy").

### Could-have (only if time remains)
- AI coach (Gemini API) commenting on recent workouts; body-weight tracking; nutrition (Open Food Facts).

### Out of scope
Social features (following, sharing, feeds), native mobile app, payments, kg/lb switching, multiple languages.

## 7. Data model (PostgreSQL)

All tables have `id BIGSERIAL PK`, plus `created_at`/`updated_at` where useful. Managed by Flyway (`src/main/resources/db/migration/V1__init.sql`, ...).

| Table | Key columns |
|---|---|
| `app_user` | `email` (unique, lower-cased), `password_hash`, `display_name`, `role` (USER/ADMIN), `enabled` |
| `user_profile` | `user_id` (FK, unique), `goal`, `level`, `days_per_week`, `equipment_access`, `theme` (SYSTEM/LIGHT/DARK), `onboarding_completed` |
| `muscle` | `wger_id` (unique), `name`, `is_front` |
| `equipment` | `wger_id` (unique), `name` |
| `exercise_category` | `wger_id` (unique), `name` |
| `exercise` | `wger_id` (unique), `slug` (unique), `name`, `description_html` (sanitised), `category_id`, `image_url`, `last_synced_at` |
| `exercise_muscle` | `exercise_id`, `muscle_id`, `is_primary` |
| `exercise_equipment` | `exercise_id`, `equipment_id` |
| `exercise_pattern` | `exercise_id` (unique), `pattern` (see §8), `is_compound`, `min_level`, `equipment_access` set it fits, `load_increment_kg` (curated subset used by the generator) |
| `program` | `user_id`, `name`, `split_type`, `goal`, `level`, `active`, `archived_at` |
| `program_day` | `program_id`, `day_index`, `name` (e.g. "Upper A") |
| `program_exercise` | `program_day_id`, `exercise_id`, `position`, `sets`, `rep_min`, `rep_max`, `rest_seconds` |
| `workout_session` | `user_id`, `program_day_id` (nullable), `started_at`, `finished_at`, `notes` |
| `workout_set` | `session_id`, `exercise_id`, `set_number`, `weight_kg NUMERIC(6,2)`, `reps`, `completed_at` |

Indexes: `exercise(slug)`, `workout_set(session_id)`, `workout_session(user_id, started_at DESC)`, `workout_set(exercise_id)` joined with session for per-user progress queries.

## 8. Programme generation rules (rule-based, deterministic)

Implemented in `ProgramGenerator` as pure Java so it can be unit-tested without a DB.

**Curated exercise set.** wger exercises do not carry "movement pattern" information, so the generator uses a curated subset (~50 exercises) tagged in `exercise_pattern` (seeded by a Flyway migration that refers to exercises by `wger_id`; look up the real IDs during implementation). The public library still shows all wger exercises.

Movement patterns: `SQUAT`, `HINGE`, `LUNGE`, `HORIZONTAL_PUSH`, `VERTICAL_PUSH`, `HORIZONTAL_PULL`, `VERTICAL_PULL`, `CHEST_ISO`, `SHOULDER_ISO`, `BICEPS`, `TRICEPS`, `QUADS_ISO`, `HAMSTRINGS_ISO`, `CALVES`, `CORE`.

**Split by days per week**

| Days | Split | Day templates (patterns in order) |
|---|---|---|
| 2 | Full Body A/B | A: SQUAT, HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PUSH, CORE · B: HINGE, VERTICAL_PULL, HORIZONTAL_PUSH, LUNGE, BICEPS/TRICEPS |
| 3 | Full Body A/B/C | A and B as above · C: SQUAT, VERTICAL_PUSH, HORIZONTAL_PULL, HAMSTRINGS_ISO, CORE |
| 4 | Upper/Lower × 2 | Upper: HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PUSH, VERTICAL_PULL, TRICEPS, BICEPS · Lower: SQUAT, HINGE, LUNGE, HAMSTRINGS_ISO, CALVES, CORE (second Upper/Lower uses alternative exercises for the same patterns) |
| 5 | Upper / Lower / Push / Pull / Legs | Push: HORIZONTAL_PUSH, VERTICAL_PUSH, CHEST_ISO, SHOULDER_ISO, TRICEPS · Pull: VERTICAL_PULL, HORIZONTAL_PULL, HINGE, BICEPS · Legs: SQUAT, LUNGE, QUADS_ISO, HAMSTRINGS_ISO, CALVES |
| 6 | Push/Pull/Legs × 2 | As above, second round uses alternative exercises |

**Filtering:** pick, for each pattern slot, an exercise whose `equipment_access` includes the user's access and whose `min_level` ≤ user level. Prefer compounds for the first slots. Avoid repeating the same exercise within a week where an alternative exists. If no exercise fits a slot, drop the slot.

**Volume by level** (exercises per day / working sets per exercise): beginner 4–5 / 2–3, intermediate 5–6 / 3, advanced 6–7 / 3–4. Trim templates from the end to fit.

**Rep ranges and rest by goal**

| Goal | Compound | Isolation | Rest compound / isolation |
|---|---|---|---|
| Strength | 4–6 reps (advanced: 3–5) | 6–10 reps | 180 s / 120 s |
| Muscle | 8–12 reps | 10–15 reps | 90 s / 60 s |

## 9. Progression rules (double progression)

Implemented in `ProgressionService`, unit-tested.

- **No history** for the exercise → no weight suggestion; show the rep target and let the user enter a starting weight.
- **All working sets reached `rep_max`** last time at the same weight → suggest weight + `load_increment_kg` (default 2.5 kg upper body / isolation, 5 kg lower-body compounds), reps at `rep_min`.
- **Within the range** → same weight, aim for +1 rep on at least one set.
- **Below `rep_min` in two consecutive sessions** → suggest a 10 % deload (rounded to the nearest 2.5 kg).
- **Bodyweight exercises (0 kg)** → progress by reps only.
- **Estimated 1RM** (for charts and PRs): Epley, `weight × (1 + reps / 30)`, only for sets with ≤ 12 reps.

## 10. Pages and routes

| Route | Access | Notes |
|---|---|---|
| `/` | public | Landing |
| `/exercises`, `/exercises/{slug}` | public | SEO pages |
| `/register`, `/login` | public (redirect to `/app` if logged in) | |
| `POST /logout` | auth | |
| `/app` | auth | Dashboard; redirects to onboarding if not completed |
| `/app/onboarding` | auth | Multi-step |
| `/app/program` | auth | Weekly programme |
| `POST /app/workouts` | auth | Start a session for a programme day |
| `/app/workouts/{id}` | owner | Live workout screen |
| `POST /api/workouts/{id}/sets`, `PUT/DELETE /api/sets/{id}`, `POST /api/workouts/{id}/finish` | owner | JSON, CSRF header required |
| `/app/history`, `/app/history/{id}` | owner | |
| `/app/progress`, `/app/progress/{slug}` | auth | |
| `/app/settings` | auth | |
| `/sitemap.xml`, `/robots.txt` | public | Generated |
| Custom `404`/`500` pages | public | Styled, helpful links |

## 11. SEO requirements

- Public pages are fully server-rendered; content present in the initial HTML.
- Unique `<title>` and `<meta name="description">` per page (exercise pages: "Barbell Bench Press: muscles worked & how to do it | RepUp").
- Clean, lowercase, hyphenated slugs; `<link rel="canonical">`; canonical strips filter/sort params except where intended.
- Open Graph + Twitter card tags (title, description, image).
- JSON-LD: `WebSite` (with `SearchAction` for the library search) on the home page, `BreadcrumbList` on exercise pages, `Organization` in the layout.
- `sitemap.xml` listing `/`, `/exercises` and every exercise slug (generated from DB, `lastmod` = `last_synced_at`). `robots.txt` allows public pages, disallows `/app/` and `/api/`, and points to the sitemap. `/app/**` pages also send `noindex`.
- Semantic HTML (`header`, `nav`, `main`, `article`, one `h1` per page, ordered headings), descriptive `alt` text, lazy-loaded images with width/height set.
- Performance: Tailwind purged CSS, minimal JS, `Cache-Control` for static assets, gzip/brotli. Target Lighthouse (mobile) ≥ 90 for Performance, Accessibility, Best Practices, SEO on `/` and an exercise page.
- After deploy: verify the site in Google Search Console and submit the sitemap (screenshot for the report).

## 12. Security requirements (MO4)

- Passwords hashed with **BCrypt** (Spring `DelegatingPasswordEncoder`); never logged.
- **CSRF** protection enabled for all state-changing requests; JSON endpoints read the token from a `<meta>` tag and send it as `X-CSRF-TOKEN`.
- **Session:** server-side session; cookie `HttpOnly`, `Secure` (prod), `SameSite=Lax`; session ID rotated on login (fixation protection); idle timeout 30 min (remember-me optional, 14 days); logout invalidates the session.
- **Authorisation:** every service method that loads a programme/session/set checks ownership → 404 if not owner (prevents IDOR).
- **XSS:** Thymeleaf `th:text` (auto-escaped) everywhere; `th:utext` only for jsoup-sanitised wger descriptions. Content-Security-Policy header (no inline scripts except the hashed theme script, or use a nonce).
- **SQL injection:** only JPA repositories / parameterised queries; no string-concatenated SQL.
- **Input validation:** Bean Validation (`@Valid`, `@Email`, `@Size`, `@Min/@Max` for kg and reps).
- **Brute force:** rate-limit failed logins per IP+email (e.g. Bucket4j or a simple in-memory counter) with a temporary lockout message.
- **Headers:** HSTS (prod), `X-Content-Type-Options`, `Referrer-Policy`, frame-ancestors `'none'`.
- **Secrets:** DB URL/credentials and any API keys only in environment variables; `.env` git-ignored; no secrets in the repo.
- **Errors:** generic error pages; no stack traces shown to users.
- Dependencies kept current; run OWASP dependency-check or `mvn versions:display-dependency-updates` once before submission.

## 13. UI/UX, responsive design and accessibility (MO2)

- **Mobile-first.** Breakpoints: < 640 px (phone: bottom tab bar Home / Program / History / Progress), ≥ 768 px (tablet), ≥ 1024 px (desktop: top nav, wider grid).
- **Design language:** Hevy-inspired: neutral (slightly warm) greys, one brand accent colour, rounded cards, bold sans-serif (Inter), generous spacing, clear set table.
- **Brand accent: dark brown ("espresso"), defined as Tailwind theme tokens, never hard-coded:**
  - Light mode: accent `#5C3A21` (buttons, active tab, links; white text on it), hover `#4A2E1A`, soft tint `#F3EBE3` for highlighted rows/completed sets.
  - Dark mode: a dark brown would disappear on a near-black background, so the same hue is lightened to caramel: accent `#C8996B` (dark text `#1A120B` on accent buttons), hover `#D9AE82`, soft tint `#2A1F17`.
  - Verify every pairing meets WCAG AA contrast (≥ 4.5:1 for text) and adjust the shade if needed; record the final values in DECISIONS.md.
- **Dark mode:** true dark surfaces (`#0E0C0B` background, cards `#1A1715`), warm-neutral greys to match the brown accent.
- Tap targets ≥ 44 × 44 px; numeric inputs use `inputmode="decimal"` / `inputmode="numeric"` so phones show the number pad.
- Feedback for every action (saved ✓ state, toasts, loading states); empty states with a clear next step; confirm before destructive actions.
- **WCAG 2.1 AA:** colour contrast ≥ 4.5:1 for text, visible focus styles, full keyboard operation, form labels and error messages linked with `aria-describedby`, `prefers-reduced-motion` respected, skip-to-content link.
- **Usability testing:** 3–5 participants, 5 tasks (register + onboarding, find an exercise for back with dumbbells, start and log a workout, check progress for one exercise, switch to dark mode). Measure completion, time, errors; finish with the SUS questionnaire. Record findings + the changes made because of them (report material).

## 14. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | Public pages render server-side, TTFB < 500 ms on warm instance |
| NFR-2 | Works on the last 2 versions of Chrome, Firefox, Safari (iOS) and Edge |
| NFR-3 | Usable at 360 px width without horizontal scrolling |
| NFR-4 | Lighthouse mobile ≥ 90 in all four categories on key public pages |
| NFR-5 | WCAG 2.1 AA |
| NFR-6 | Site keeps working (with cached data) if wger is unavailable |
| NFR-7 | All secrets via env vars; HTTPS in production |
| NFR-8 | Unit tests for `ProgramGenerator` and `ProgressionService`; integration tests for auth and ownership checks |

## 15. Deployment

- Multi-stage `Dockerfile` (Maven build → slim JRE 21 runtime).
- `docker-compose.yml` for local PostgreSQL.
- Spring profiles: `dev` (local) and `prod` (Render). Config via env vars: `DATABASE_URL`/`SPRING_DATASOURCE_*`, `APP_BASE_URL`.
- Render web service from the GitHub repo (auto-deploy on push to `main`); Neon PostgreSQL. Health check endpoint (`/actuator/health`).
- Free-tier instances sleep when idle: open the site a few minutes before the demo.
- Deploy a "hello world" version in week 1 so deployment problems surface early.

## 16. Report support (keep updated during development)

- `DECISIONS.md`: one entry per significant decision: *Decision · Alternatives considered · Why chosen · Trade-offs*. The rubric's top band requires clear arguments for each choice, so this file is the main source for the report.
- `docs/screenshots/`: desktop + mobile, light + dark, Lighthouse results, Search Console, usability-test evidence.
- `docs/demo-notes.md`: short plain-language explanation of each major part (auth flow, wger sync, generator, progression, timer, SEO) for answering questions at the demo.

Report outline (≈ 2750 words): 1. Domain analysis · 2. Functional & non-functional requirements · 3. Architecture · 4. Technique selection and justification: sessions, responsive design, SEO, database, external API, security · 5. UI/UX, accessibility and usability testing · 6. Overall description of the application (with screenshots) · 7. Conclusion and future work · References.

## 17. Timeline (today: Mon 26 Oct 2026 → deadline Sun 13 Dec 2026)

| Week | Dates | Goals |
|---|---|---|
| 1 | 26 Oct – 1 Nov | Present topic to module leader (confirm Java back-end). Project skeleton, Git, Docker Postgres, Tailwind build, base layout, dark mode, deploy hello-world to Render + Neon |
| 2 | 2 – 8 Nov | Registration, login/logout, security config. wger sync + exercise library + detail pages |
| 3 | 9 – 15 Nov | SEO layer (meta, JSON-LD, sitemap, robots). Onboarding, curated patterns seed, `ProgramGenerator` + tests, programme page |
| 4 | 16 – 22 Nov | Live workout screen, set saving, rest timer, `ProgressionService` + tests |
| 5 | 23 – 29 Nov | Dashboard, history, progress charts + PRs, settings. Should-have: swap exercise |
| 6 | 30 Nov – 6 Dec | Polish, accessibility audit, Lighthouse, security review, usability test (3–5 people), screenshots, Search Console |
| 7 | 7 – 13 Dec | Write report, export PDF, demo notes. **Aim to submit by 12 Dec** |
