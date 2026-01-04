\
#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Build an XCFramework and leave ONLY the final .xcframework
# in the project root. All intermediate build artifacts
# (DerivedData, Archives, temp build folders) are deleted.
#
# Supported:
#   - iOS (device)
#   - iOS Simulator
#   - macOS
#   - Mac Catalyst
#
# Usage:
#   ./build_xcframework.sh PROJECT_NAME SCHEME PROJECT_PATH CONFIGURATION INCLUDE_MAC INCLUDE_CATALYST
#
# ============================================================

# env variables
# PROJECT_NAME="LibraryFramework"
# SCHEME="LibraryFramework"
# PROJECT_PATH="library/library/iosXCFramework/LibraryFramework.xcodeproj"
# CONFIGURATION="Release"
# INCLUDE_MAC=0 # or 1 to include
# INCLUDE_CATALYST=0 # or 1 to include

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "ERROR: xcodebuild not found (Xcode required)."
  exit 1
fi

if [[ ! -e "$PROJECT_PATH" ]]; then
  echo "ERROR: PROJECT_PATH not found: $PROJECT_PATH"
  exit 1
fi

ROOT="$(cd "$(dirname "$PROJECT_PATH")" && pwd)"
TMP_BUILD="$ROOT/.xcframework_build_tmp"
DERIVED_DATA="$TMP_BUILD/DerivedData"
ARCHIVES="$TMP_BUILD/Archives"
FINAL_XCFRAMEWORK="$ROOT/${PROJECT_NAME}.xcframework"

rm -rf "$TMP_BUILD" "$FINAL_XCFRAMEWORK"
mkdir -p "$ARCHIVES"

# project / workspace switch
XCB_ARGS=()
if [[ "$PROJECT_PATH" == *.xcworkspace ]]; then
  XCB_ARGS=(-workspace "$PROJECT_PATH")
else
  XCB_ARGS=(-project "$PROJECT_PATH")
fi

COMMON_ARGS=(
  "${XCB_ARGS[@]}"
  -scheme "$SCHEME"
  -configuration "$CONFIGURATION"
  -derivedDataPath "$DERIVED_DATA"
  SKIP_INSTALL=NO
  BUILD_LIBRARY_FOR_DISTRIBUTION=YES
)

echo "==> iOS (device)"
IOS_ARCHIVE="$ARCHIVES/iOS.xcarchive"
xcodebuild archive \
  "${COMMON_ARGS[@]}" \
  -destination "generic/platform=iOS" \
  -archivePath "$IOS_ARCHIVE" \
  >/dev/null

echo "==> iOS Simulator"
IOS_SIM_ARCHIVE="$ARCHIVES/iOS-Simulator.xcarchive"
xcodebuild archive \
  "${COMMON_ARGS[@]}" \
  -destination "generic/platform=iOS Simulator" \
  -archivePath "$IOS_SIM_ARCHIVE" \
  >/dev/null

if [[ "$INCLUDE_MAC" == "1" ]]; then

  echo "==> macOS"
  MAC_ARCHIVE="$ARCHIVES/macOS.xcarchive"
  xcodebuild archive \
    "${COMMON_ARGS[@]}" \
    -destination "generic/platform=macOS" \
    -archivePath "$MAC_ARCHIVE" \
    >/dev/null

    if [[ -d "$MAC_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework" ]]; then
      XC_ARGS+=(-framework "$MAC_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework")
    fi
fi

XC_ARGS=(
  -framework "$IOS_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework"
  -framework "$IOS_SIM_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework"
)

if [[ "$INCLUDE_CATALYST" == "1" ]]; then
  echo "==> Mac Catalyst"
  CAT_ARCHIVE="$ARCHIVES/MacCatalyst.xcarchive"
  xcodebuild archive \
    "${COMMON_ARGS[@]}" \
    -destination "generic/platform=macOS,variant=Mac Catalyst" \
    -archivePath "$CAT_ARCHIVE" \
    >/dev/null

  if [[ -d "$CAT_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework" ]]; then
    XC_ARGS+=(-framework "$CAT_ARCHIVE/Products/Library/Frameworks/${PROJECT_NAME}.framework")
  fi
fi

echo "==> Creating XCFramework in project root"
xcodebuild -create-xcframework \
  "${XC_ARGS[@]}" \
  -output "$FINAL_XCFRAMEWORK"

echo "==> Cleaning temporary build artifacts"
rm -rf "$TMP_BUILD"

echo
echo "✅ Done"
echo "📦 ${FINAL_XCFRAMEWORK}"
