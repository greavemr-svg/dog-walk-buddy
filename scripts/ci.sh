#!/usr/bin/env bash
# Lint, unit tests, build, then the changed-lines coverage gate. Used by CI and by agents locally,
# so both run the same thing. The commands match docs/project.md.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== Lint"
bash ./gradlew --no-daemon ktlintCheck lintDebug

echo "== Unit tests"
bash ./gradlew --no-daemon testDebugUnitTest

echo "== Build"
bash ./gradlew --no-daemon assembleDebug assembleRelease

echo "== Coverage"
if [ -n "${GITHUB_BASE_REF:-}" ]; then
  bash scripts/coverage.sh "origin/${GITHUB_BASE_REF}"
else
  bash scripts/coverage.sh
fi
