#!/usr/bin/env bash
# Publish the plugin to JetBrains Marketplace.
#
# Loads secrets from .env at the repo root (gitignored), then runs
# `./gradlew publishPlugin`. Forwards any extra args to gradle.
#
# Usage:
#   scripts/publish.sh                          # publishes to channel from .env
#   scripts/publish.sh --info                   # publish with extra gradle flags
#   JETBRAINS_MARKETPLACE_CHANNEL=eap scripts/publish.sh   # one-off channel override

set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
    echo "error: .env not found at repo root." >&2
    echo "       copy .env.example to .env and fill in JETBRAINS_MARKETPLACE_TOKEN." >&2
    exit 1
fi

# Export everything in .env into the environment for gradle to pick up.
set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ -z "${JETBRAINS_MARKETPLACE_TOKEN:-}" ]]; then
    echo "error: JETBRAINS_MARKETPLACE_TOKEN is empty in .env." >&2
    echo "       grab one at https://plugins.jetbrains.com/author/me/tokens" >&2
    exit 1
fi

echo "Publishing to channel: ${JETBRAINS_MARKETPLACE_CHANNEL:-default}"

exec ./gradlew publishPlugin "$@"
