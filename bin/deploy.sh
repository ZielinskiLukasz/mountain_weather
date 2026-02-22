#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
PACKAGE="com.example.mountainweather"
ACTIVITY=".MainActivity"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

usage() {
    cat <<EOF
Usage: $(basename "$0") [command]

Commands:
  build       Build debug APK
  install     Install APK on connected device/emulator
  launch      Launch the app
  run         Build + install + launch (default)
  logcat      Show app logs (live)
  kill        Force-stop the app
  uninstall   Uninstall the app
  locale PL   Set app locale (e.g. pl-PL, en-US)

EOF
    exit 0
}

do_build() {
    echo "==> Building debug APK..."
    cd "$PROJECT_DIR"
    ./gradlew assembleDebug --no-daemon
    echo "==> Build complete: $APK"
}

do_install() {
    echo "==> Installing on device..."
    "$ADB" install -r "$APK"
    echo "==> Installed."
}

do_launch() {
    echo "==> Launching $PACKAGE..."
    "$ADB" shell am start -n "$PACKAGE/$ACTIVITY"
}

do_run() {
    do_build
    do_install
    do_launch
}

do_logcat() {
    PID=$("$ADB" shell pidof "$PACKAGE" 2>/dev/null || true)
    if [ -z "$PID" ]; then
        echo "App not running, showing all logs for package..."
        "$ADB" logcat --regex="$PACKAGE" -v time
    else
        echo "==> Showing logs for PID $PID..."
        "$ADB" logcat --pid="$PID" -v time
    fi
}

do_kill() {
    echo "==> Force-stopping $PACKAGE..."
    "$ADB" shell am force-stop "$PACKAGE"
}

do_uninstall() {
    echo "==> Uninstalling $PACKAGE..."
    "$ADB" uninstall "$PACKAGE"
}

do_locale() {
    local loc="${1:?Usage: $(basename "$0") locale <locale-code, e.g. pl-PL>}"
    echo "==> Setting app locale to $loc..."
    "$ADB" shell cmd locale set-app-locales "$PACKAGE" --locales "$loc"
    echo "==> Locale set. Restart the app to see changes."
}

CMD="${1:-run}"
shift 2>/dev/null || true

case "$CMD" in
    build)     do_build ;;
    install)   do_install ;;
    launch)    do_launch ;;
    run)       do_run ;;
    logcat)    do_logcat ;;
    kill)      do_kill ;;
    uninstall) do_uninstall ;;
    locale)    do_locale "$@" ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $CMD"; usage ;;
esac
