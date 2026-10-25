#!/bin/bash
set -e
echo "🚀 Building Zenith90 Installable Pixel APK..."
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug
elif command -v gradle >/dev/null 2>&1; then
    gradle assembleDebug
else
    echo "💡 Open ~/Downloads/MyHabbit in Android Studio or push to GitHub Actions to auto-build the APK."
    exit 1
fi
cp app/build/outputs/apk/debug/app-debug.apk ~/Downloads/Zenith90-Pixel.apk
echo "✅ Done! Your installable APK is ready at: ~/Downloads/Zenith90-Pixel.apk"
open -R ~/Downloads/Zenith90-Pixel.apk
