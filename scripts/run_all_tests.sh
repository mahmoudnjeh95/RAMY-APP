#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Rami Tunisien — Full Test Suite Runner
#
# Usage:
#   ./scripts/run_all_tests.sh [--android-only | --ios-only | --unit-only]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[TEST]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

MODE="${1:-all}"

# ─── 1. JVM unit tests (fast, no device needed) ───────────────────────────────
run_jvm_tests() {
  log "Running JVM unit tests (commonTest on JVM)..."
  ./gradlew :shared:jvmTest \
    --tests "com.rami.*" \
    --continue \
    && log "JVM tests ✓" \
    || fail "JVM tests failed"
}

# ─── 2. iOS Simulator tests ───────────────────────────────────────────────────
run_ios_tests() {
  log "Running iOS simulator tests (commonTest on iosSimulatorArm64)..."

  # Resolve the available simulator
  SIM_ID=$(xcrun simctl list devices available -j \
    | python3 -c "
import json,sys
d=json.load(sys.stdin)
devs=[v for vlist in d['devices'].values() for v in vlist if v['isAvailable'] and 'iPhone' in v['name']]
print(devs[0]['udid'] if devs else '')
  ")

  if [[ -z "$SIM_ID" ]]; then
    warn "No iOS simulator found — skipping iOS tests"
    return 0
  fi

  log "Using simulator: $SIM_ID"
  xcrun simctl boot "$SIM_ID" 2>/dev/null || true

  ./gradlew :shared:iosSimulatorArm64Test \
    --tests "com.rami.*" \
    --continue \
    && log "iOS simulator tests ✓" \
    || fail "iOS simulator tests failed"
}

# ─── 3. Android device / emulator tests ───────────────────────────────────────
run_android_instrumented_tests() {
  log "Running Android instrumented tests (device/emulator)..."

  # Check for connected device or running emulator
  DEVICES=$(adb devices | grep -v "List of" | grep -v "^$" | wc -l | tr -d ' ')
  if [[ "$DEVICES" -eq 0 ]]; then
    warn "No Android device/emulator connected — skipping instrumented tests"
    warn "Start an emulator with: \$ANDROID_HOME/emulator/emulator -avd <name>"
    return 0
  fi

  ./gradlew :shared:connectedAndroidTest \
    --tests "com.rami.*" \
    --continue \
    && log "Android instrumented tests ✓" \
    || fail "Android instrumented tests failed"
}

# ─── 4. HTML test report summary ─────────────────────────────────────────────
show_report_paths() {
  echo ""
  log "Test reports:"
  echo "  JVM:     shared/build/reports/tests/jvmTest/index.html"
  echo "  iOS:     shared/build/reports/tests/iosSimulatorArm64Test/index.html"
  echo "  Android: shared/build/reports/androidTests/connected/index.html"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════════"
echo "  Rami Tunisien — Integration Test Suite"
echo "══════════════════════════════════════════════"
echo ""

case "$MODE" in
  --unit-only)    run_jvm_tests ;;
  --ios-only)     run_ios_tests ;;
  --android-only) run_android_instrumented_tests ;;
  all)
    run_jvm_tests
    run_ios_tests
    run_android_instrumented_tests
    ;;
  *)
    echo "Usage: $0 [--android-only | --ios-only | --unit-only]"
    exit 1
    ;;
esac

show_report_paths
echo ""
log "All tests passed ✓"
