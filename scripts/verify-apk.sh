#!/usr/bin/env bash
#
# Checks that a built release APK is the artifact it claims to be: right package, right version,
# and signed with the release key. A build that silently picks up the debug keystore, or that ships
# a versionCode nobody bumped, still installs fine locally, so nothing else catches this.
#
#   ./scripts/verify-apk.sh timety-v2.3.0.apk --version 2.3.0 --version-code 243
#   ./scripts/verify-apk.sh app.apk --version 2.3.0 --version-code 243 --cert-sha256 ab:cd:...
#
# Options:
#   --version X.Y.Z      versionName the APK must declare. Required.
#   --version-code N     versionCode the APK must declare. Required.
#   --cert-sha256 HEX    Expected signing certificate SHA-256 digest. Skipped when empty.
#   --package NAME       Expected applicationId. Defaults to io.github.benji377.timety.
#
# Needs the Android SDK build-tools (aapt2, apksigner) under $ANDROID_HOME.
set -euo pipefail

APK="${1:-}"
if [ -z "$APK" ] || [ "$APK" = "-h" ] || [ "$APK" = "--help" ]; then
  sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'
  [ -n "$APK" ] && exit 0 || exit 2
fi
shift

VERSION=""
VERSION_CODE=""
CERT_SHA256=""
PACKAGE="io.github.benji377.timety"

while [ $# -gt 0 ]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --version-code) VERSION_CODE="$2"; shift 2 ;;
    --cert-sha256) CERT_SHA256="$2"; shift 2 ;;
    --package) PACKAGE="$2"; shift 2 ;;
    *) echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
  esac
done

if [ -z "$VERSION" ] || [ -z "$VERSION_CODE" ]; then
  echo "--version and --version-code are required" >&2
  exit 2
fi

if [ ! -f "$APK" ]; then
  echo "no such APK: $APK" >&2
  exit 1
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BUILD_TOOLS=$(find "$SDK/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)
if [ -z "$BUILD_TOOLS" ]; then
  echo "no build-tools found under $SDK" >&2
  exit 1
fi

FAILED=0
fail() {
  echo "FAIL: $*" >&2
  FAILED=1
}

badging=$("$BUILD_TOOLS/aapt2" dump badging "$APK")

# A field that is absent comes back empty and is reported below; without the `|| true` the
# non-matching grep would take the whole script down before printing anything.
field() {
  printf '%s' "$badging" | grep -oP "$1" | head -1 || true
}

got_package=$(field "^package: name='\K[^']+")
got_code=$(field "^package:.* versionCode='\K[^']+")
got_name=$(field "^package:.* versionName='\K[^']+")
min_sdk=$(field "^minSdkVersion:'\K[^']+")
target_sdk=$(field "^targetSdkVersion:'\K[^']+")

echo "package:     $got_package"
echo "versionName: $got_name"
echo "versionCode: $got_code"
echo "minSdk:      $min_sdk"
echo "targetSdk:   $target_sdk"

[ "$got_package" = "$PACKAGE" ] || fail "package is $got_package, expected $PACKAGE"
[ "$got_name" = "$VERSION" ] || fail "versionName is $got_name, expected $VERSION"
[ "$got_code" = "$VERSION_CODE" ] || fail "versionCode is $got_code, expected $VERSION_CODE"

signing=$("$BUILD_TOOLS/apksigner" verify --print-certs --verbose "$APK" 2>&1) || {
  echo "$signing" >&2
  fail "apksigner could not verify the APK"
  exit 1
}

# v1 is optional on modern minSdk levels, but shipping an APK that only carries the legacy JAR
# signature would mean the newer schemes were switched off by accident.
if ! printf '%s' "$signing" | grep -qE "^Verified using v[23] scheme.*: true"; then
  echo "$signing" >&2
  fail "APK is not signed with the v2 or v3 signature scheme"
fi

# apksigner labels the line "Signer #1 ..." or "V2 Signer: ..." depending on which schemes signed
# the APK, so match on the part both spellings share.
got_cert=$(printf '%s' "$signing" | { grep -oiP "certificate SHA-256 digest: \K[0-9a-f]+" || true; } \
  | head -1 | tr '[:upper:]' '[:lower:]')
echo "signer:      $got_cert"

if [ -n "$CERT_SHA256" ]; then
  expected=$(printf '%s' "$CERT_SHA256" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')
  if [ -z "$got_cert" ]; then
    echo "$signing" >&2
    fail "no certificate digest in the apksigner output"
  elif [ "$got_cert" != "$expected" ]; then
    fail "signing certificate is $got_cert, expected $expected"
  fi
else
  echo "note: no expected certificate digest given, signer identity not checked" >&2
fi

if [ "$FAILED" -ne 0 ]; then
  exit 1
fi

echo "OK: $APK"
