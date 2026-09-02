#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_KTS="$PROJECT_DIR/app/build.gradle.kts"
DEST_DIR="/home/zieloo/Pulpit/projects/weather_android/app/build/outputs"

USE_R8=1
POSITIONAL=()
for arg in "$@"; do
    case "$arg" in
        --no-r8|--nor8) USE_R8=0 ;;
        *) POSITIONAL+=("$arg") ;;
    esac
done
if [ ${#POSITIONAL[@]} -gt 0 ]; then
    set -- "${POSITIONAL[@]}"
else
    set --
fi

release_variant() {
    if [ "$USE_R8" -eq 1 ]; then
        echo "release"
    else
        echo "releaseNoR8"
    fi
}

assemble_task() {
    if [ "$USE_R8" -eq 1 ]; then
        echo "assembleRelease"
    else
        echo "assembleReleaseNoR8"
    fi
}

bundle_task() {
    if [ "$USE_R8" -eq 1 ]; then
        echo "bundleRelease"
    else
        echo "bundleReleaseNoR8"
    fi
}

apk_path() {
    local v
    v="$(release_variant)"
    echo "$PROJECT_DIR/app/build/outputs/apk/${v}/app-${v}.apk"
}

aab_path() {
    local v
    v="$(release_variant)"
    echo "$PROJECT_DIR/app/build/outputs/bundle/${v}/app-${v}.aab"
}

r8_label() {
    if [ "$USE_R8" -eq 1 ]; then
        echo "R8"
    else
        echo "no-R8"
    fi
}

usage() {
    cat <<EOF
Usage: $(basename "$0") [command] [--no-r8]

Commands:
  publish     Bump version, build APK+AAB, copy to releases (default)
  bundle      Build signed AAB for Google Play
  apk         Build release APK
  clean       Clean build cache and rebuild AAB
  verify      Verify AAB signature
  info        Show AAB/APK size and signing info
  version     Show current versionCode and versionName

Options:
  --no-r8     Disable R8 minify and resource shrinking
              Default: R8 is always on (assembleRelease / bundleRelease)

Examples:
  $(basename "$0") publish              # APK+AAB with R8
  $(basename "$0") bundle               # AAB with R8
  $(basename "$0") bundle --no-r8       # AAB without R8
  $(basename "$0") apk --no-r8          # APK without R8

EOF
    exit 0
}

do_publish() {
    echo "==> Bumping version..."
    local old_code old_name new_code new_name
    old_code=$(grep -oP 'versionCode\s*=\s*\K\d+' "$GRADLE_KTS")
    old_name=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE_KTS")

    new_code=$((old_code + 1))
    # increment last segment of versionName
    local prefix="${old_name%.*}"
    local last="${old_name##*.}"
    new_name="${prefix}.$((last + 1))"

    sed -i "s/versionCode = $old_code/versionCode = $new_code/" "$GRADLE_KTS"
    sed -i "s/versionName = \"$old_name\"/versionName = \"$new_name\"/" "$GRADLE_KTS"
    echo "  $old_name ($old_code) → $new_name ($new_code)"

    echo ""
    echo "==> Building release APK and AAB ($(r8_label))..."
    cd "$PROJECT_DIR"
    ./gradlew "$(assemble_task)" "$(bundle_task)" --no-daemon

    local ts
    ts=$(date +%Y%m%d_%H%M)
    local tag="${new_name}(${new_code})_${ts}"
    if [ "$USE_R8" -eq 0 ]; then
        tag="${tag}_nor8"
    fi

    mkdir -p "$DEST_DIR/apk/release" "$DEST_DIR/bundle/release"
    cp "$(apk_path)" "$DEST_DIR/apk/release/app-release-${tag}.apk"
    cp "$(aab_path)" "$DEST_DIR/bundle/release/app-release-${tag}.aab"

    echo ""
    echo "==> Done! Version $new_name ($new_code) ($(r8_label))"
    echo ""
    ls -lh "$DEST_DIR/apk/release/app-release-${tag}.apk"
    ls -lh "$DEST_DIR/bundle/release/app-release-${tag}.aab"
}

do_bundle() {
    echo "==> Building signed release AAB ($(r8_label))..."
    cd "$PROJECT_DIR"
    ./gradlew "$(bundle_task)" --no-daemon
    echo ""
    echo "==> AAB ready: $(aab_path)"
    ls -lh "$(aab_path)"
    echo ""
    echo "Upload this file to Google Play Console:"
    echo "  Production → Create new release → Upload"
}

do_apk() {
    echo "==> Building release APK ($(r8_label))..."
    cd "$PROJECT_DIR"
    ./gradlew "$(assemble_task)" --no-daemon
    echo ""
    echo "==> APK ready: $(apk_path)"
    ls -lh "$(apk_path)"
}

do_clean() {
    echo "==> Cleaning build cache..."
    cd "$PROJECT_DIR"
    ./gradlew clean --no-daemon
    echo "==> Rebuilding AAB ($(r8_label))..."
    do_bundle
}

do_verify() {
    local aab
    aab="$(aab_path)"
    if [ ! -f "$aab" ]; then
        echo "AAB not found. Run '$(basename "$0") bundle' first."
        exit 1
    fi
    echo "==> Verifying AAB signature ($(r8_label))..."
    jarsigner -verify -verbose -certs "$aab" 2>&1 | head -20
    echo ""
    echo "==> Keystore info:"
    keytool -list -keystore "$PROJECT_DIR/keystore/release.jks" -storepass mountainweather2026 2>&1 | head -10
}

do_info() {
    echo "==> Build artifacts:"
    local r8_aab="$PROJECT_DIR/app/build/outputs/bundle/release/app-release.aab"
    local r8_apk="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    local nor8_aab="$PROJECT_DIR/app/build/outputs/bundle/releaseNoR8/app-releaseNoR8.aab"
    local nor8_apk="$PROJECT_DIR/app/build/outputs/apk/releaseNoR8/app-releaseNoR8.apk"
    [ -f "$r8_aab" ] && echo "  AAB (R8):    $(ls -lh "$r8_aab" | awk '{print $5}')" || echo "  AAB (R8):    not built"
    [ -f "$r8_apk" ] && echo "  APK (R8):    $(ls -lh "$r8_apk" | awk '{print $5}')" || echo "  APK (R8):    not built"
    [ -f "$nor8_aab" ] && echo "  AAB (no-R8): $(ls -lh "$nor8_aab" | awk '{print $5}')" || echo "  AAB (no-R8): not built"
    [ -f "$nor8_apk" ] && echo "  APK (no-R8): $(ls -lh "$nor8_apk" | awk '{print $5}')" || echo "  APK (no-R8): not built"
    local debug_apk="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    [ -f "$debug_apk" ] && echo "  Debug APK:   $(ls -lh "$debug_apk" | awk '{print $5}')" || echo "  Debug APK:   not built"
}

do_version() {
    echo "==> Current version:"
    grep -E "versionCode|versionName" "$PROJECT_DIR/app/build.gradle.kts" | sed 's/^[ \t]*/  /'
}

CMD="${1:-publish}"
if [ $# -gt 0 ]; then
    shift
fi

case "$CMD" in
    publish) do_publish ;;
    bundle)  do_bundle ;;
    apk)     do_apk ;;
    clean)   do_clean ;;
    verify)  do_verify ;;
    info)    do_info ;;
    version) do_version ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $CMD"; usage ;;
esac
