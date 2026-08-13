#!/usr/bin/env bash
#
# Runs the cross-feature workflow tests on an emulator.
#
# These are deliberately not part of CI: they need a device, and CI is kept fast for the
# per-push loop. Run this before starting a big feature and before cutting a release.
#
#   ./scripts/workflow-tests.sh            # headless workflow tests + the Compose UI journeys
#   ./scripts/workflow-tests.sh --fast     # headless only, no UI tests (the usual case)
#   ./scripts/workflow-tests.sh --shutdown # power the emulator down afterwards
#
set -euo pipefail

cd "$(dirname "$0")/.."

FAST=0
SHUTDOWN=0
for arg in "$@"; do
  case "$arg" in
    --fast) FAST=1 ;;
    --shutdown) SHUTDOWN=1 ;;
    -h|--help) sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option: $arg (try --help)" >&2; exit 2 ;;
  esac
done

AVD_NAME="${AVD_NAME:-Pixel_9}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export PATH="$PATH:$SDK/platform-tools:$SDK/emulator"

HEADLESS_PACKAGE="io.github.benji377.timety.workflow"
UI_PACKAGE="io.github.benji377.timety.uiflow"

if [ "$FAST" -eq 1 ]; then
  PACKAGES="$HEADLESS_PACKAGE"
  echo "==> Running headless workflow tests only"
else
  PACKAGES="$HEADLESS_PACKAGE,$UI_PACKAGE"
  echo "==> Running headless workflow tests and Compose UI journeys"
fi

# A previous `adb emu kill` can leave an instance that still answers on the adb port, and booting
# the same AVD twice fails hard while adb quietly targets the stale one. Reuse whatever is already
# attached rather than starting a second copy.
BOOTED_HERE=0
if adb devices | grep -qw "device"; then
  echo "==> Reusing the device already attached:"
  adb devices | sed '1d;/^$/d;s/^/    /'
else
  echo "==> Booting $AVD_NAME headless (this takes a while)"
  nohup emulator -avd "$AVD_NAME" -no-window -no-audio -no-boot-anim \
    -gpu swiftshader_indirect >/tmp/timety-emulator.log 2>&1 &
  BOOTED_HERE=1
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 5
  done
  echo "==> Booted"
fi

STATUS=0
./gradlew connectedDebugAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.package=$PACKAGES" || STATUS=$?

REPORT="app/build/reports/androidTests/connected/debug/index.html"
if [ -f "$REPORT" ]; then
  echo
  echo "==> Report: file://$(pwd)/$REPORT"
fi

if [ "$SHUTDOWN" -eq 1 ] && [ "$BOOTED_HERE" -eq 1 ]; then
  echo "==> Shutting the emulator down"
  adb emu kill || true
  # `adb emu kill` reports success while the instance lingers; wait it out so the next run does
  # not find a half-dead device and reuse it.
  for _ in $(seq 1 20); do
    adb devices | grep -qw "device" || break
    sleep 1
  done
fi

# connectedAndroidTest can uninstall the app when it finishes, so `run-as` against the app's data
# directory may fail right after a run. Reinstall with `./gradlew installDebug` before poking at
# the on-device database.
exit "$STATUS"
