#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode="${1:-frontend}"
case "$mode" in frontend|backend|all) ;; *) echo 'Usage: bash scripts/regression.sh [frontend|backend|all]' >&2; exit 2;; esac
if [[ "$mode" == frontend || "$mode" == all ]]; then
  for source_file in app.js api.js amounts.js schedule.js connected.js; do
    node --check "$project_root/fronted/travel-prototype/$source_file"
  done
  node --test "$project_root/fronted/travel-prototype/amounts.test.cjs" "$project_root/fronted/travel-prototype/schedule.test.cjs" "$project_root/fronted/travel-prototype/api.test.cjs"
fi
if [[ "$mode" == backend || "$mode" == all ]]; then
  : "${TRIP_LEDGER_TEST_DB_URL:?Set an isolated test database URL}"
  : "${TRIP_LEDGER_TEST_DB_USERNAME:?Set the test database username}"
  : "${TRIP_LEDGER_TEST_DB_PASSWORD:?Set the test database password}"
  if [[ "${TRIP_LEDGER_TEST_ENV_CONFIRMED:-}" != 1 ]]; then
    echo 'Confirm isolated, writable test dependencies with TRIP_LEDGER_TEST_ENV_CONFIRMED=1' >&2; exit 2
  fi
  : "${TRIP_LEDGER_IDEMPOTENCY_TEST_DB_URL:?Set the dedicated trip_ledger_idempotency_test database URL}"
  cd "$project_root/backend/trip-ledger"
  mvn -B test
fi
