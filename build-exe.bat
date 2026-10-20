@echo off
REM ============================================================
REM  Build Pomodoro into a standalone Windows app (with its own
REM  bundled Java runtime) using jpackage. Run from PowerShell or
REM  cmd on WINDOWS (not WSL).
REM ============================================================

setlocal

REM --- Point at your JDK 21 (has jpackage + supports JavaFX) ---
set "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using JDK at: %JAVA_HOME%
echo.

echo [1/3] Building the fat jar with the Maven Wrapper...
call "%~dp0mvnw.cmd" -q clean package
if errorlevel 1 (
    echo Maven build failed.
    exit /b 1
)

echo [2/3] Staging the jar...
if exist build-input rmdir /s /q build-input
mkdir build-input
copy /Y target\pomodoro-1.0.0.jar build-input\ >nul

echo [3/3] Running jpackage (app-image)...
if exist dist rmdir /s /q dist
"%JAVA_HOME%\bin\jpackage" ^
    --type app-image ^
    --name Pomodoro ^
    --app-version 1.0.0 ^
    --input build-input ^
    --main-jar pomodoro-1.0.0.jar ^
    --main-class com.pomodoro.Launcher ^
    --icon "%~dp0icon.ico" ^
    --java-options "-Xmx256m" ^
    --dest dist

if errorlevel 1 (
    echo jpackage failed.
    exit /b 1
)

echo.
echo Done!  Launch:  dist\Pomodoro\Pomodoro.exe
endlocal
