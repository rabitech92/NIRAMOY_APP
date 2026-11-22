Flutter App Run Guide (niramoy_app)
Ei doc e step-by-step deya ache kibhabe Windows e Android Studio diye Flutter app run korte hoy. Apnar project: C:\Users\Rabiul Islam\Downloads\niramoy_app
1. Ja ja lagbe (Prerequisites)
• Flutter SDK (apnar kache 3.47.9 install ache)
• Android Studio
• Android Studio te Flutter ar Dart plugin
• Android SDK Command-line Tools
• Ekta emulator ba USB diye connect kora real phone
• Internet connection (prothom build e Gradle download kore)
2. Ek bar setup (Shudhu prothom bar)
2.1 Flutter check korun
Android Studio er Terminal e likhun:
flutter --version
"not recognized" error ashle flutter.dev theke SDK download kore bin folder ta PATH e add korun.
2.2 Plugin install
File > Settings > Plugins e giye Flutter install korun (Dart auto install hobe). Tarpor Android Studio restart korun.
2.3 Android SDK Command-line Tools
1.  File > Settings > Languages & Frameworks > Android SDK
2.  SDK Tools tab e jan
3.  Android SDK Command-line Tools (latest) tick din
4.  Apply / OK chapun
2.4 License accept
flutter doctor --android-licenses
Shob prompt e y likhe Enter din.
2.5 Environment check
flutter doctor
Android toolchain e [v] howa uchit. Visual Studio er [X] ignore korun, eta shudhu Windows desktop app er jonno.
3. Emulator toiri o chalu kora
1.  Android Studio te Tools > Device Manager e jan
2.  + (Create Device) chapun, ekta phone (jemon Pixel) select korun
3.  System image download kore Finish korun
4.  Device er pashe Play button chapun
Ekta emulator diye jekono project run kora jay. Alada project er jonno notun emulator lage na.
Emulator boro korte: emulator panel er pop-out icon chapun, ba Settings > Tools > Emulator e "Launch in the Running Devices tool window" untick korun.
4. Project setup
Project folder e terminal khule:
flutter pub get
Jodi android folder na thake
Zip ba download kora project e platform folder thake na. Tokhon "No supported devices connected" error ashe. Fix:
flutter create . --platforms=android
Eta apnar lib, pubspec.yaml delete kore na. Tarpor abar flutter pub get din.
android_overrides folder
Ei folder e custom Android config thakte pare (permissions, AndroidManifest.xml, build.gradle). flutter create er por jodi internet, camera ba QR scanner er moto feature kaj na kore, tahole oi file gulo notun android/ folder e merge korte hobe. build_apk.bat ba build_apk.sh e hoyto eta automatic kora hoy, ota khule dekhun.
5. App run kora
Kon device ache dekhun:
flutter devices
Run korun:
flutter run
Ekadhik device thakle:
flutter run -d emulator-5556
(emulator-5556 er jaygay flutter devices e dekha id din.)
Chrome e quick test korte chaile:
flutter run -d chrome
Prothom build e 5 theke 15 minute lagte pare. Running Gradle task 'assembleDebug'... likha thakle bujhben kaj cholche. Lal WARNING (Java / Gradle) ignore korun.
Alternative: Android Studio er upore "Add Configuration" e Flutter run configuration set kore (entry point lib/main.dart) Play button chapun.
6. Cholar somoy kaj-er shortcut
Key
Kaj
r
Hot reload (code change shathe shathe dekha)
R
Hot restart
q
App bondho kora
7. APK build kora
Age cholte thaka flutter run bondho korun (q), tarpor:
flutter build apk --release
File paben: build\app\outputs\flutter-apk\app-release.apk
Folder khulte:
explorer build\app\outputs\flutter-apk
Choto size er jonno:
flutter build apk --split-per-abi
Beshirbhag phone er jonno app-arm64-v8a-release.apk use korun. Play Store e upload korte hole alada keystore diye sign korte hobe.
8. Common problem ar fix
Problem
Fix
flutter not recognized
Flutter bin folder PATH e add korun, terminal restart korun
No supported devices connected
flutter create . --platforms=android chalan
cmdline-tools component is missing
SDK Tools theke Command-line Tools install korun
License error
flutter doctor --android-licenses
Gradle lock / build atke jay
Onno flutter run bondho kore abar try korun
Emulator list e nai
Device Manager theke emulator Play kore chalu korun
A message on the flutter/lifecycle channel was discarded
Shadharon warning, ignore korun
Package version warning (newer versions available)
Ignore korun, app cholbe
Pura build bhenge gele
flutter clean tarpor flutter pub get ar flutter run
9. Quick checklist
1.  flutter doctor e Android toolchain [v]
2.  Emulator chalu ba phone connected
3.  flutter pub get
4.  android folder ache kina check
5.  flutter run
Kono error ashle terminal er shesh 20-30 line copy kore pathan.
