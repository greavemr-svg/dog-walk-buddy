#!/usr/bin/env bash
# Coverage report plus the changed-lines gate (80 %), see docs/project.md.
#
# Usage: bash scripts/coverage.sh [<base-ref>]
# Without <base-ref> it uses origin/$GITHUB_BASE_REF when that is set. With neither,
# it prints total coverage and skips the changed-lines gate.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly DIFF_COVER_VERSION="10.2.0"
readonly REPORT="app/build/reports/jacoco/jacocoDebugUnitTestReport/report.xml"
readonly THRESHOLD=80

base_ref="${1:-}"
if [ -z "$base_ref" ] && [ -n "${GITHUB_BASE_REF:-}" ]; then
  base_ref="origin/${GITHUB_BASE_REF}"
fi

bash ./gradlew --no-daemon jacocoDebugUnitTestReport

if [ ! -f "$REPORT" ]; then
  echo "Coverage report not found at $REPORT" >&2
  exit 1
fi

if command -v python3 >/dev/null 2>&1; then
  system_python=python3
else
  system_python=python
fi

echo "== Total line coverage"
"$system_python" -I - "$REPORT" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
for counter in root.findall("counter"):
    if counter.get("type") == "LINE":
        missed, covered = int(counter.get("missed")), int(counter.get("covered"))
        total = missed + covered
        percent = 100.0 * covered / total if total else 100.0
        print(f"{covered}/{total} lines covered ({percent:.1f} %)")
PY

if [ -z "$base_ref" ]; then
  echo "== No base ref given: skipping the changed-lines gate"
  exit 0
fi

# Pull-request checkouts are shallow and carry only the merge commit.
if [ "$(git rev-parse --is-shallow-repository)" = "true" ]; then
  git fetch --no-tags --unshallow origin
fi
if ! git rev-parse --verify --quiet "$base_ref" >/dev/null; then
  git fetch --no-tags origin "${base_ref#origin/}:refs/remotes/${base_ref}"
fi

if [ ! -d .venv-ci ]; then
  "$system_python" -m venv .venv-ci
fi
if [ -x .venv-ci/bin/python ]; then
  venv_python=.venv-ci/bin/python
else
  venv_python=.venv-ci/Scripts/python
fi
"$venv_python" -m pip install --quiet --disable-pip-version-check "diff-cover==${DIFF_COVER_VERSION}"

echo "== Changed-lines coverage against ${base_ref} (minimum ${THRESHOLD} %)"
"$venv_python" -m diff_cover.diff_cover_tool "$REPORT" \
  --compare-branch="$base_ref" \
  --src-roots app/src/main/java \
  --exclude "**/map/maplibre/**" \
  --fail-under="$THRESHOLD"
