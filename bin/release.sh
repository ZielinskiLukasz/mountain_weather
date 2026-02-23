#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
AAB="$PROJECT_DIR/app/build/outputs/bundle/release/app-release.aab"
APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"

usage() {
    cat <<EOF
Usage: $(basename "$0") [command]

Commands:
  bundle      Build signed AAB for Google Play (default)
  apk         Build release APK (unsigned)
  clean       Clean build cache and rebuild AAB
  verify      Verify AAB signature
  info        Show AAB/APK size and signing info
  version     Show current versionCode and versionName

EOF
    exit 0
}

do_bundle() {
    echo "==> Building signed release AAB..."
    cd "$PROJECT_DIR"
    ./gradlew bundleRelease --no-daemon
    echo ""
    echo "==> AAB ready: $AAB"
    ls -lh "$AAB"
    echo ""
    echo "Upload this file to Google Play Console:"
    echo "  Production → Create new release → Upload"
}

do_apk() {
    echo "==> Building release APK..."
    cd "$PROJECT_DIR"
    ./gradlew assembleRelease --no-daemon
    echo ""
    echo "==> APK ready: $APK"
    ls -lh "$APK"
}

do_clean() {
    echo "==> Cleaning build cache..."
    cd "$PROJECT_DIR"
    ./gradlew clean --no-daemon
    echo "==> Rebuilding AAB..."
    do_bundle
}

do_verify() {
    if [ ! -f "$AAB" ]; then
        echo "AAB not found. Run '$(basename "$0") bundle' first."
        exit 1
    fi
    echo "==> Verifying AAB signature..."
    jarsigner -verify -verbose -certs "$AAB" 2>&1 | head -20
    echo ""
    echo "==> Keystore info:"
    keytool -list -keystore "$PROJECT_DIR/keystore/release.jks" -storepass mountainweather2026 2>&1 | head -10
}

do_info() {
    echo "==> Build artifacts:"
    [ -f "$AAB" ] && echo "  AAB: $(ls -lh "$AAB" | awk '{print $5}')" || echo "  AAB: not built"
    [ -f "$APK" ] && echo "  APK: $(ls -lh "$APK" | awk '{print $5}')" || echo "  APK: not built"
    local debug_apk="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    [ -f "$debug_apk" ] && echo "  Debug APK: $(ls -lh "$debug_apk" | awk '{print $5}')" || echo "  Debug APK: not built"
}

do_version() {
    echo "==> Current version:"
    grep -E "versionCode|versionName" "$PROJECT_DIR/app/build.gradle.kts" | sed 's/^[ \t]*/  /'
}

CMD="${1:-bundle}"
shift 2>/dev/null || true

case "$CMD" in
    bundle)  do_bundle ;;
    apk)     do_apk ;;
    clean)   do_clean ;;
    verify)  do_verify ;;
    info)    do_info ;;
    version) do_version ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $CMD"; usage ;;
esac
