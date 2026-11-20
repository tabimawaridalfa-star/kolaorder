@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
    if exist "C:\Users\tabim\.gemini\antigravity\scratch\jdk17\jdk-17.0.21+9" (
        set "JAVA_HOME=C:\Users\tabim\.gemini\antigravity\scratch\jdk17\jdk-17.0.21+9"
    )
)

echo ================================================
echo                 STOA - BUILD APK
echo ================================================
echo.
echo Pastikan Android Studio dan Android SDK sudah terpasang.
echo Build ini menjalankan tes unit sebelum membuat APK release.
echo.

call gradlew.bat clean testDebugUnitTest assembleRelease --stacktrace
if errorlevel 1 goto :failed

if not exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo GAGAL: APK release tidak ditemukan setelah proses build.
    goto :failed
)

if not exist "dist" mkdir "dist"
copy /y "app\build\outputs\apk\release\app-release.apk" "dist\Stoa-release.apk" >nul
if errorlevel 1 goto :failed

echo.
echo ================================================
echo BUILD BERHASIL
echo APK: %CD%\dist\Stoa-release.apk
echo ================================================
echo.
echo Salin Stoa-release.apk ke HP Android, lalu buka untuk memasang.
echo.
pause
exit /b 0

:failed
echo.
echo BUILD GAGAL. Periksa pesan error di atas.
echo Pastikan Gradle dapat mengunduh dependensi dan Android SDK Platform 35 tersedia.
echo.
pause
exit /b 1
