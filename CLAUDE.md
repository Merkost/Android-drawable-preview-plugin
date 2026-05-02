# Project conventions

## No code comments

Do not add comments to source code. Not docstrings, not block comments, not `// why` lines.

If a piece of behaviour needs justification:
- one-shot rationale → commit message body
- recurring convention → this file
- user-facing docs → README / RELEASE.md

The codebase is read directly; clear names + small functions do the explaining. Comments rot, names compile.

## Branches

- `master` — released versions only. No direct commits. Fast-forward from `develop` at release time.
- `develop` — integration branch. All ongoing work merges here first.
- `release/X.Y.Z` — long-running release-prep branch (e.g. `release/1.3.0`) for the next version's surface area. Rebases onto `develop` regularly.
- `feature/<name>` — short-lived per-change branches off `develop`.

## Versioning + IDE compatibility

- Single source of truth: `gradle/libs.versions.toml`.
- `sinceBuild` ratchets only with releases that genuinely need it. Verify the new floor by looking inside `~/.gradle/caches/.../android-studio-<version>-mac_arm/lib/modules/` for any `bundledModule(...)` we depend on.
- Avoid `untilBuild` — verify against the next AS via `verifyPlugin` and bump per release instead of locking out users on a future IDE.

## Secrets

- `~/.gradle/gradle.properties` and `<repo>/.env` are both legitimate places for the marketplace token / signing-key paths. The `.env` is gitignored at repo root and read by `scripts/publish.sh`.
- Plugin signing keys live in `<repo>/keys/` (gitignored, `chmod 600`). Backup at `/Users/merkost/Work/My apps/Drawable Preview/signing/`.
- Never commit `local.properties` (already gitignored).

## Build / test workflow

- `./gradlew test buildPlugin` — every commit should leave this green.
- `./gradlew runIde` — sandbox install against `studio.dir` from `local.properties`.
- `./gradlew verifyPlugin` — gate before any release.
- `./scripts/publish.sh` — full publish flow (signs, uploads, channels via `JETBRAINS_MARKETPLACE_CHANNEL`).

## Why some things are the way they are

- **Batik excludes `xml-apis`** — the 2009 transitive ships its own `javax.xml.parsers.DocumentBuilder*` which collides with the JDK's classes via the plugin classloader. Don't re-add it.
- **Compose runtime + Jewel via `bundledModule`** — IDE provides them, we don't ship our own. Module names drift between AS major lines (`compose.runtime.desktop` doesn't exist on 251, only on 253). Verify before bumping.
- **`buildSearchableOptions` disabled by default** — it spins up a sandbox IDE which conflicts with any locally-running AS instance. CI can re-enable.
- **`AdaptiveIconDrawable` mask shape via `SettingsUtils.withMaskShape{}` thread-local** — avoids threading a parameter through every drawable while still letting the popup override per-render.
