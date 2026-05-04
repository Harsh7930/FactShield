#!/usr/bin/env bash
# FactShield — compile all Java sources under src/ into bin/ (package layout preserved).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

mkdir -p bin

echo "[build.sh] Compiling to bin/..."
javac -encoding UTF-8 -d bin -cp "lib/*" \
  src/dao/NewsDAO.java \
  src/detector/FakeNewsDetector.java \
  src/detector/FakeNewsGUI.java

echo "[build.sh] Done."
