# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

IReader is a Kotlin Multiplatform reader app for novels/light novels/web novels/ebooks, targeting Android, Desktop (JVM), and iOS (WIP — see "iOS Developer Needed" in README.md, integration incomplete). Gradle root project name is `Infinity`.

Stack: Kotlin 2.3.21 (K2), Compose Multiplatform + Material 3, Koin 4.2.1 (DI), Ktor 3.5.0 (HTTP), SQLDelight 2.3.2 (DB), Ksoup (KMP Jsoup-style HTML parsing), Supabase 3.6.0 (backend/sync), detekt 1.23.8 (static analysis).

## Build & Test Commands

```bash
./gradlew test                              # Full test suite (all modules)
./gradlew :domain:test                      # Single module's common tests
./gradlew :data:desktopTest                 # Desktop-target tests for a module
./gradlew :presentation:testDebugUnitTest   # Android unit tests for a module
./gradlew detekt                            # Static analysis (all modules)
./gradlew :desktop:run                      # Run the desktop app
./gradlew :android:assembleDebug            # Build Android debug APK
```

Run a single test class/method with `--tests`, e.g. `./gradlew :domain:test --tests "ireader.domain.SomeTest"`.

Faster local dev builds: `SKIP_DETEKT=true` and `SKIP_TESTS=true` env vars disable those task types (already default-on in `gradle.properties` — unset them to force detekt/tests to run). Gradle JVM is capped at 4GB heap / Kotlin daemon at 2GB (`gradle.properties`) — don't bump these without checking memory pressure on CI.

There is no top-level lint/format command beyond `detekt`; config lives in `config/detekt.yml` with baseline `config/detekt-baseline.xml`.

## Module Structure (Clean Architecture)

```
domain/         Entities (Book, Chapter, History), use cases, repository interfaces — no data/presentation deps
data/           SQLDelight DB (data/src/commonMain/sqldelight), Ktor/Supabase API clients, repository impls
presentation/   Compose Multiplatform screens, ViewModels, navigation (presentation-core was merged in — see below)
core/           IO, HTTP, config, preferences, DB utilities, logging — shared low-level utilities
source-api/     Published extension API (Source, CatalogSource, HttpSource) that novel-source extensions implement
plugin-api/     Plugin interface definitions
source-runtime-js/  JS engine for user-provided sources (present but not yet wired into settings.gradle.kts)
i18n/           Internationalization strings/resources
android/        Android app entry point (Application, DI wiring)
android-compat/ Android API shims used by non-Android targets
desktop/        Desktop (JVM) app entry point (Main.kt, DesktopDI.kt)
iosApp/         iOS Xcode project (integration incomplete)
benchmark/      Performance benchmarks
```

Dependency direction is strict: `domain` has no dependency on `data` or `presentation`; `data` implements `domain` repository interfaces; `presentation` depends on `domain` (and, via DI, `data`). When adding code, place entities/use-cases in `domain`, persistence/network in `data`, and UI/ViewModels in `presentation`.

Each KMP module uses `src/commonMain`, `src/androidMain`, `src/desktopMain`, `src/iosMain` (+ matching `commonTest`/`androidTest`/`desktopTest`) source sets with `expect`/`actual` for platform-specific code. Note: `presentation-core` was merged into `presentation` (commit `9e2b0d917`) to cut build memory/time — don't recreate it as a separate module.

## Dependency Injection

Koin modules are wired per-platform at the app entry points: `android/src/main/java/org/ireader/app/MyApplication.kt` and `desktop/src/main/kotlin/ireader/desktop/di/DesktopDI.kt` (see also `desktop/.../Main.kt`). Platform-specific bindings (e.g. iOS) live under each module's `di/` package with `.ios.kt`/`.desktop.kt` actual files (e.g. `data/src/iosMain/kotlin/ireader/data/di/`).

## Source Extensions

Novel sources implement the `source-api` interfaces (`Source`, `CatalogSource`, `HttpSource`). Prefer `SimpleNovelSource` for typical HTML sites, `HttpSource` when you need lower-level control, or the DSL builder for prototyping. Extensions themselves live in the separate `IReader-extensions` repo, not here — this repo only defines/consumes the API surface (`source-api`) and the JS runtime for user-added sources (`source-runtime-js`).

## Conventions Enforced by Existing Agent Docs

This repo ships its own agent guidance in `AGENTS.md` and `agents/*.md` (Kiro/Superpowers-style custom agents) — treat these as authoritative for workflow style in addition to this file:

- **TDD is expected**: write a failing test in the relevant `commonTest`/platform test source set before implementation, then make it pass.
- **Vertical slices**: implement one failing test → passing code → next test, rather than writing all tests up front.
- **Respect module boundaries** described above (`domain` never imports `data` or `presentation`).
- Commit after each passing test/vertical slice rather than batching large changes.

## Repo Housekeeping Notes

- `docs/`, `plans/`, and `scripts/` contain a large number of ad-hoc planning docs and one-off Python/Kotlin scripts (i18n fixes, backup format tools, translation helpers) — these are historical/utility, not part of the build graph; check `scripts/README.md` before reusing one.
- `.codegraph/` provides a code index; if the `codegraph` CLI or `codegraph_explore` MCP tool is available, prefer it over grep for symbol lookups.
