#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/docs/screenshots"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
ACTIVITY="com.ergonomic.mountainweather/.MainActivity"

declare -A PROFILES=(
    [phone]="1080x2400:420"
    [tablet7]="1200x1920:213"
    [tablet10]="1600x2560:320"
)

ICON_COORDS_SETTINGS=()
ICON_COORDS_LOCATION=()

WAIT_APP=4
WAIT_NAV=2

usage() {
    cat <<EOF
Usage: $(basename "$0") [command] [options]

Capture Google Play Store screenshots from the running emulator.

Commands:
  phone         Capture phone screenshots (1080x2400, 420dpi)
  tablet7       Capture 7-inch tablet screenshots (1200x1920, 213dpi)
  tablet10      Capture 10-inch tablet screenshots (1600x2560, 320dpi)
  all           Capture screenshots for all device profiles
  list          Show available profiles and output directories
  clean         Remove all generated screenshots

Options:
  --no-crop     Skip cropping (phone screenshots are cropped to 2:1 by default)
  --delay N     Wait N seconds after app launch (default: $WAIT_APP)

Requires:
  - Running Android emulator (adb devices)
  - App installed on the emulator
  - ImageMagick (convert) for cropping

EOF
    exit 0
}

check_deps() {
    if ! "$ADB" get-state &>/dev/null; then
        echo "ERROR: No emulator/device connected. Start one first."
        exit 1
    fi
    if ! command -v convert &>/dev/null; then
        echo "WARNING: ImageMagick not found. Phone screenshots won't be cropped."
    fi
}

get_physical_display() {
    "$ADB" shell wm size | grep "Physical" | awk '{print $3}'
}

get_physical_density() {
    "$ADB" shell wm density | grep "Physical" | awk '{print $3}'
}

set_display() {
    local profile="$1"
    local spec="${PROFILES[$profile]}"
    local size="${spec%%:*}"
    local dpi="${spec##*:}"

    local phys_size
    phys_size=$(get_physical_display)

    if [ "$size" = "$phys_size" ]; then
        "$ADB" shell wm size reset 2>/dev/null || true
        "$ADB" shell wm density reset 2>/dev/null || true
    else
        "$ADB" shell wm size "$size"
        "$ADB" shell wm density "$dpi"
    fi
    sleep 2
}

reset_display() {
    "$ADB" shell wm size reset 2>/dev/null || true
    "$ADB" shell wm density reset 2>/dev/null || true
    sleep 1
}

screencap() {
    local out_file="$1"
    "$ADB" exec-out screencap -p > "$out_file"
}

launch_app() {
    "$ADB" shell am force-stop com.ergonomic.mountainweather 2>/dev/null || true
    sleep 1
    "$ADB" shell am start -n "$ACTIVITY"
    sleep "$WAIT_APP"
}

tap() {
    "$ADB" shell input tap "$1" "$2"
    sleep "$WAIT_NAV"
}

back() {
    "$ADB" shell input keyevent KEYCODE_BACK
    sleep "$WAIT_NAV"
}

get_screen_width() {
    local profile="$1"
    local spec="${PROFILES[$profile]}"
    local size="${spec%%:*}"
    echo "${size%%x*}"
}

get_screen_height() {
    local profile="$1"
    local spec="${PROFILES[$profile]}"
    local size="${spec%%:*}"
    echo "${size##*x}"
}

calc_icon_x() {
    local width
    width=$(get_screen_width "$1")
    local icon="$2"
    case "$icon" in
        settings) echo $(( width - width / 16 )) ;;
        location) echo $(( width - width / 10 )) ;;
        star)     echo $(( width - width / 7 )) ;;
    esac
}

calc_icon_y() {
    local profile="$1"
    local spec="${PROFILES[$profile]}"
    local dpi="${spec##*:}"
    if [ "$dpi" -ge 400 ]; then
        echo 220
    elif [ "$dpi" -ge 300 ]; then
        echo 130
    else
        echo 110
    fi
}

crop_to_2_1() {
    local file="$1"
    if ! command -v convert &>/dev/null; then
        return
    fi
    local dims
    dims=$(identify -format '%wx%h' "$file")
    local w="${dims%%x*}"
    local h="${dims##*x}"
    local max_h=$(( w * 2 ))
    if [ "$h" -gt "$max_h" ]; then
        convert "$file" -gravity center -crop "${w}x${max_h}+0+0" +repage "$file"
    fi
}

capture_profile() {
    local profile="$1"
    local do_crop="$2"

    local dir="$OUT_DIR"
    case "$profile" in
        phone)    dir="$OUT_DIR" ;;
        tablet7)  dir="$OUT_DIR/tablet" ;;
        tablet10) dir="$OUT_DIR/tablet10" ;;
    esac
    mkdir -p "$dir"

    local settings_x settings_y location_x
    settings_x=$(calc_icon_x "$profile" settings)
    location_x=$(calc_icon_x "$profile" location)
    settings_y=$(calc_icon_y "$profile")

    echo ""
    echo "==> Capturing [$profile] screenshots..."
    echo "    Display: ${PROFILES[$profile]}"
    echo "    Output:  $dir/"
    echo ""

    set_display "$profile"

    # 1) Home screen
    echo "  [1/3] Home screen..."
    launch_app
    sleep 2
    screencap "$dir/01_home.png"
    echo "        saved: 01_home.png"

    # 2) Settings screen
    echo "  [2/3] Settings screen..."
    tap "$settings_x" "$settings_y"
    sleep 1
    screencap "$dir/02_settings.png"
    echo "        saved: 02_settings.png"

    # 3) Search / locations screen
    echo "  [3/3] Search screen..."
    back
    sleep 1
    tap "$location_x" "$settings_y"
    sleep 1
    screencap "$dir/03_search.png"
    echo "        saved: 03_search.png"
    back

    # Crop phone screenshots to meet Google Play 2:1 ratio
    if [ "$profile" = "phone" ] && [ "$do_crop" = "yes" ]; then
        echo ""
        echo "  Cropping phone screenshots to max 2:1 ratio..."
        for f in "$dir"/0*.png; do
            crop_to_2_1 "$f"
        done
        echo "        done"
    fi

    echo ""
    echo "  Results:"
    for f in "$dir"/0*.png; do
        local dims
        dims=$(identify -format '%wx%h' "$f" 2>/dev/null || echo "?")
        local size
        size=$(ls -lh "$f" | awk '{print $5}')
        echo "    $(basename "$f"): ${dims}  ${size}"
    done
}

do_capture() {
    local profile="$1"
    local do_crop="$2"
    check_deps
    capture_profile "$profile" "$do_crop"
    if [ "$profile" != "phone" ]; then
        reset_display
        echo "  Display restored to defaults."
    fi
}

do_all() {
    local do_crop="$1"
    check_deps
    for p in phone tablet7 tablet10; do
        capture_profile "$p" "$do_crop"
    done
    reset_display
    echo ""
    echo "==> All screenshots captured. Display restored."
}

do_list() {
    echo "Available profiles:"
    echo ""
    printf "  %-10s %-14s %-6s %s\n" "Profile" "Resolution" "DPI" "Output"
    printf "  %-10s %-14s %-6s %s\n" "-------" "----------" "---" "------"
    printf "  %-10s %-14s %-6s %s\n" "phone"   "1080x2400" "420" "docs/screenshots/"
    printf "  %-10s %-14s %-6s %s\n" "tablet7" "1200x1920" "213" "docs/screenshots/tablet/"
    printf "  %-10s %-14s %-6s %s\n" "tablet10" "1600x2560" "320" "docs/screenshots/tablet10/"
    echo ""
    echo "Google Play requirements:"
    echo "  Phone:     min 320px, max 3840px, ratio max 2:1"
    echo "  Tablet 7\": min 320px, max 3840px, ratio max 2:1"
    echo "  Tablet 10\": min 320px, max 3840px, ratio max 2:1"
}

do_clean() {
    echo "==> Removing generated screenshots..."
    rm -f "$OUT_DIR"/0*.png
    rm -rf "$OUT_DIR/tablet" "$OUT_DIR/tablet10"
    echo "    done"
}

DO_CROP="yes"
CMD="${1:-help}"
shift 2>/dev/null || true

while [ $# -gt 0 ]; do
    case "$1" in
        --no-crop) DO_CROP="no" ;;
        --delay)   shift; WAIT_APP="$1" ;;
    esac
    shift
done

case "$CMD" in
    phone)    do_capture phone "$DO_CROP" ;;
    tablet7)  do_capture tablet7 "$DO_CROP" ;;
    tablet10) do_capture tablet10 "$DO_CROP" ;;
    all)      do_all "$DO_CROP" ;;
    list)     do_list ;;
    clean)    do_clean ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $CMD"; usage ;;
esac
