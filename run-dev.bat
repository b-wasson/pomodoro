@echo off
REM Quick dev run on Windows. Run from cmd/PowerShell:  .\run-dev.bat
setlocal
set "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Closing any previous Pomodoro instance so the build isn't locked...
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*pomodoro-1.0.0.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }" 2>nul

REM Use the Maven Wrapper (auto-downloads Maven; no separate install needed).
echo Building...
call "%~dp0mvnw.cmd" clean package
if errorlevel 1 (
    echo.
    echo *** BUILD FAILED ***
    echo If the error mentions it cannot delete pomodoro-1.0.0.jar, a Pomodoro
    echo window is still open - close it and run this again.
    exit /b 1
)

echo Launching...
java -jar "%~dp0target\pomodoro-1.0.0.jar"
endlocal
