@echo off
REM Windows-এ এক ক্লিকে APK বানানোর স্ক্রিপ্ট (Flutter আগে থেকে ইনস্টল ও PATH-এ থাকতে হবে)
echo [1/4] Android ফাইল তৈরি হচ্ছে...
call flutter create --platforms=android --org com.niramoy --project-name niramoy_pharmacy .
if errorlevel 1 goto :fail
echo [2/4] Manifest বসানো হচ্ছে...
copy /Y android_overrides\AndroidManifest.xml android\app\src\main\AndroidManifest.xml
if errorlevel 1 goto :fail
for %%F in (android\app\build.gradle android\app\build.gradle.kts) do if exist %%F powershell -NoProfile -Command "(Get-Content '%%F') -replace 'flutter\.minSdkVersion','24' | Set-Content '%%F'"
echo [3/4] প্যাকেজ নামানো হচ্ছে...
call flutter pub get
if errorlevel 1 goto :fail
echo [4/4] APK বানানো হচ্ছে (কয়েক মিনিট লাগতে পারে)...
call flutter build apk --release
if errorlevel 1 goto :fail
echo.
echo সফল! APK: build\app\outputs\flutter-apk\app-release.apk
goto :eof
:fail
echo.
echo ব্যর্থ হয়েছে। ওপরের ত্রুটির বার্তা দেখে আমাকে পাঠান।
exit /b 1
