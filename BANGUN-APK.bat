@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo ================================================
echo             KALAORDER - BANGUN APK
echo ================================================
echo.

REM 1. Deteksi JAVA_HOME otomatis (JDK 17 / 21 atau JBR Android Studio)
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    ) else if exist "C:\Users\tabim\.gemini\antigravity\scratch\jdk17\jdk-17.0.21+9\bin\java.exe" (
        set "JAVA_HOME=C:\Users\tabim\.gemini\antigravity\scratch\jdk17\jdk-17.0.21+9"
    ) else (
        for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*" "C:\Program Files\Eclipse Adoptium\jdk-17*" "C:\Program Files\Java\jdk-21*" "C:\Program Files\Java\jdk-17*") do (
            if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
        )
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [OK] JAVA_HOME: %JAVA_HOME%
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo [GAGAL] Java JDK 17/21 tidak ditemukan.
        echo Pasang JDK 17+ ^(misalnya Eclipse Temurin^) atau Android Studio terlebih dahulu.
        goto :failed
    )
    echo [OK] Menggunakan Java dari PATH sistem.
)

REM 2. Deteksi Android SDK otomatis
if not defined ANDROID_SDK_ROOT (
    if defined ANDROID_HOME (
        set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    )
)

if defined ANDROID_SDK_ROOT (
    echo [OK] Android SDK: %ANDROID_SDK_ROOT%
) else (
    echo [PERINGATAN] ANDROID_SDK_ROOT belum terdeteksi otomatis.
    echo Pastikan Android SDK sudah terpasang di %LOCALAPPDATA%\Android\Sdk.
)

REM 3. Siapkan debug.keystore (decode dari base64 atau buat otomatis dengan keytool)
if not exist "debug.keystore" (
    if exist "debug.keystore.base64" (
        echo [INFO] Menyiapkan debug.keystore dari base64...
        certutil -decode "debug.keystore.base64" "debug.keystore" >nul 2>&1
        if errorlevel 1 (
            powershell -NoProfile -Command "[IO.File]::WriteAllBytes('debug.keystore', [Convert]::FromBase64String((Get-Content 'debug.keystore.base64' -Raw)))"
        )
    ) else (
        echo [INFO] Membuat debug.keystore otomatis dengan keytool...
        keytool -genkey -v -keystore "debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
        if errorlevel 1 (
            echo [GAGAL] Tidak dapat membuat debug.keystore dengan keytool.
            goto :failed
        )
    )
)

REM 4. Cari Gradle: gradlew.bat -> gradle global -> auto-download Gradle 9.3.1
set "GRADLE_CMD="
if exist "gradlew.bat" (
    set "GRADLE_CMD=gradlew.bat"
) else (
    where gradle >nul 2>&1
    if not errorlevel 1 (
        set "GRADLE_CMD=gradle"
    ) else (
        set "LOCAL_GRADLE_BIN=%CD%\.gradle-local\gradle-9.3.1\bin\gradle.bat"
        if not exist "!LOCAL_GRADLE_BIN!" (
            echo [INFO] Gradle belum terpasang. Mengunduh Gradle 9.3.1 otomatis...
            if not exist ".gradle-local" mkdir ".gradle-local"
            powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip' -OutFile '.gradle-local\gradle.zip'; Expand-Archive -Path '.gradle-local\gradle.zip' -DestinationPath '.gradle-local' -Force; Remove-Item '.gradle-local\gradle.zip' -Force"
            if errorlevel 1 (
                echo [GAGAL] Tidak dapat mengunduh Gradle 9.3.1. Periksa koneksi internet.
                goto :failed
            )
        )
        set "GRADLE_CMD=!LOCAL_GRADLE_BIN!"
    )
)

echo [INFO] Menggunakan Gradle: %GRADLE_CMD%
echo [INFO] Memulai proses build APK KalaOrder...
echo.

call "%GRADLE_CMD%" :app:assembleDebug --stacktrace
if errorlevel 1 goto :failed

if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo [GAGAL] File app-debug.apk tidak ditemukan setelah proses build.
    goto :failed
)

if not exist "dist" mkdir "dist"
copy /y "app\build\outputs\apk\debug\app-debug.apk" "dist\KalaOrder-debug.apk" >nul
if errorlevel 1 goto :failed

echo.
echo ================================================
echo BUILD BERHASIL!
echo File APK: %CD%\dist\KalaOrder-debug.apk
echo ================================================
echo.
echo Langkah pemasangan di HP:
echo 1. Salin dist\KalaOrder-debug.apk ke HP Android.
echo 2. Buka file APK dari File Manager untuk memasang.
echo 3. Aktifkan Izin Akses Notifikasi di tab Bot WA.
echo.
pause
exit /b 0

:failed
echo.
echo ================================================
echo BUILD GAGAL. Periksa pesan error di atas.
echo Tips: Pastikan koneksi internet aktif, JDK 17/21,
echo dan Android SDK terpasang di PC ini.
echo ================================================
echo.
pause
exit /b 1
