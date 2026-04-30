# Releasing

Reference for cutting a new release of Android Drawable Preview. Each section is a step you run in order; everything is reproducible from a clean checkout.

---

## 1. Decide the version

Follow loose semver:

| Change | Bump |
| --- | --- |
| Bug fixes only | patch — `1.2.0` → `1.2.1` |
| New features, backwards-compatible | minor — `1.2.0` → `1.3.0` |
| `sinceBuild` raised, or behaviour change visible to existing users | major — `1.x` → `2.0.0` |

Update both the version string and the change-notes:

- `build.gradle.kts` — `version = "X.Y.Z"`
- `src/main/resources/META-INF/plugin.xml` — append a new section to `<change-notes>` describing user-visible changes (not commit messages).

If targeting a newer IDE, also bump the catalog:

- `gradle/libs.versions.toml` — `androidStudio` and `sinceBuild`. Verify that any `bundledModule(...)` references resolve on the new minimum (see "Verifying IDE compatibility" below).

---

## 2. Verify the build

```sh
./gradlew clean test buildPlugin
```

Expected:

- All tests green.
- Artifact at `build/distributions/drawable-preview-<version>.zip` (~4 MB).

Optionally exercise the plugin against a local IDE (sandbox install):

```sh
./gradlew runIde
```

Note: requires that no other Android Studio instance is running with the same paths selector — `buildSearchableOptions` is disabled by default for the same reason.

---

## 3. Run the plugin verifier

```sh
./gradlew verifyPlugin
```

This runs the JetBrains Plugin Verifier against the IDE versions declared in `intellijPlatform.pluginVerification.ides { recommended() }`. Reports land at `build/reports/pluginVerifier/`. Investigate any binary-incompatibility findings before publishing.

---

## 4. Merge and tag

Work happens on `develop`; releases are cut from `master`:

```sh
git checkout master
git merge --ff-only develop
git tag -a vX.Y.Z -m "Release X.Y.Z"
git push origin master vX.Y.Z develop
```

Always fast-forward. If `develop` has diverged from `master`, rebase `develop` first rather than creating a merge commit.

---

## 5. GitHub Release

```sh
gh release create vX.Y.Z \
  --title "vX.Y.Z — short summary" \
  --notes-file release-notes-X.Y.Z.md \
  build/distributions/drawable-preview-X.Y.Z.zip
```

Use the change-notes section you wrote in `plugin.xml` as the basis for `release-notes-X.Y.Z.md`. Keep the Markdown copy out of git — it's a one-off scratch file per release.

---

## 6. Publish to JetBrains Marketplace

One-time setup per maintainer:

1. Generate a token at https://plugins.jetbrains.com/author/me/tokens.
2. Add to `~/.gradle/gradle.properties` (the *user* one — never the repo's):
   ```
   jetbrains.marketplace.token=<your-token>
   ```

Each release:

```sh
./gradlew publishPlugin
```

By default this uploads to channel `default` (stable). For previews / EAP:

```sh
JETBRAINS_MARKETPLACE_CHANNEL=eap ./gradlew publishPlugin
```

Marketplace review typically takes a few hours; the plugin appears in search once approved.

---

## Optional: signing the plugin

Unsigned plugins install fine but the IDE shows a "not signed" warning. To sign:

```sh
openssl genrsa -aes256 -out private.pem 4096
openssl req -new -x509 -key private.pem -days 365 -out chain.crt
```

Add to `~/.gradle/gradle.properties`:

```
jetbrains.plugin.cert.chain.file=/path/to/chain.crt
jetbrains.plugin.private.key.file=/path/to/private.pem
jetbrains.plugin.private.key.password=<passphrase>
```

`./gradlew buildPlugin` will then produce a signed zip; `signPlugin` is silently skipped when any of those three properties are missing.

Keep the keys safe — losing them means rotating to a new identity on Marketplace, which requires JetBrains support.

---

## Verifying IDE compatibility

Bumping `sinceBuild` or adding a new `bundledModule(...)` requires checking that the module exists in the minimum supported IDE. Quick approach:

```sh
# Resolve the IDE through the Gradle cache (no separate download needed)
./gradlew dependencies --configuration intellijPlatformDependencyArchive

# Then peek inside the cached install:
AS_DIR=$(find ~/.gradle/caches -name "android-studio-X.Y.Z.W-mac_arm" -type d 2>/dev/null | head -1)
ls "$AS_DIR/lib/modules" | grep -i jewel
ls "$AS_DIR/lib/modules" | grep -i compose
```

If the modules you depend on aren't present, either:

- raise `sinceBuild` so we drop the older IDEs, or
- guard the dependency / feature behind a runtime check.

Example: the Compose Resource Manager tool window on `feature/compose-resource-manager` depends on Jewel modules that exist in 251+ but not in 243. It can ship in 1.3.0 only after we lift `sinceBuild` to 251 (or wherever Jewel was first bundled).

---

## Branch policy

- `master` — always reflects the latest released version. No direct commits.
- `develop` — integration branch. All feature work merges here first.
- `feature/<name>` — short-lived branches for individual changes; merged into `develop` once green.

Never push directly to `master`. Cut releases by fast-forwarding from `develop` (step 4 above).
