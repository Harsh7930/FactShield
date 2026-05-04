#!/usr/bin/env bash
# FactShield — build then run from project root (so ai_detector.py is found via user.dir).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

bash "$ROOT/build.sh"

echo "[run.sh] Starting FactShield UI..."
exec java -cp "bin:lib/*" detector.FakeNewsGUI
