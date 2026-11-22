#!/usr/bin/env bash
# Linux/macOS-এ APK বানানোর স্ক্রিপ্ট
set -e
flutter create --platforms=android --org com.niramoy --project-name niramoy_pharmacy .
cp android_overrides/AndroidManifest.xml android/app/src/main/AndroidManifest.xml
# secure storage ও ক্যামেরা স্ক্যানারের জন্য minSdk ২৪
for f in android/app/build.gradle android/app/build.gradle.kts; do
  if [ -f "$f" ]; then sed -i 's/flutter\.minSdkVersion/24/' "$f"; fi
done
flutter pub get
flutter build apk --release
echo "APK: build/app/outputs/flutter-apk/app-release.apk"
