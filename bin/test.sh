#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
    cat <<EOF
Usage: $(basename "$0") [command]

Commands:
  unit          Run JVM unit tests (no emulator needed)
  ui            Run instrumented UI tests (requires emulator/device)
  all           Run both unit and UI tests
  report        Open HTML test report in browser

EOF
    exit 0
}

do_unit() {
    echo "==> Running unit tests..."
    cd "$PROJECT_DIR"
    ./gradlew testDebugUnitTest --no-daemon
    echo "==> Unit tests complete."
    echo "    Report: app/build/reports/tests/testDebugUnitTest/index.html"
}

do_ui() {
    echo "==> Running instrumented UI tests (requires emulator/device)..."
    cd "$PROJECT_DIR"
    ./gradlew connectedDebugAndroidTest --no-daemon
    echo "==> UI tests complete."
    echo "    Report: app/build/reports/androidTests/connected/debug/index.html"
}

do_all() {
    do_unit
    echo ""
    do_ui
}

do_report() {
    local unit_report="$PROJECT_DIR/app/build/reports/tests/testDebugUnitTest/index.html"
    local ui_report="$PROJECT_DIR/app/build/reports/androidTests/connected/debug/index.html"

    if [ -f "$unit_report" ]; then
        echo "==> Opening unit test report..."
        xdg-open "$unit_report" 2>/dev/null || echo "    $unit_report"
    fi
    if [ -f "$ui_report" ]; then
        echo "==> Opening UI test report..."
        xdg-open "$ui_report" 2>/dev/null || echo "    $ui_report"
    fi
}

CMD="${1:-unit}"
shift 2>/dev/null || true

case "$CMD" in
    unit)    do_unit ;;
    ui)      do_ui ;;
    all)     do_all ;;
    report)  do_report ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $CMD"; usage ;;
esac
