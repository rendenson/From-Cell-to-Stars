@echo off
rem Only compiles the mod (no game). Output is also saved to build.log
cd /d "%~dp0"
call find-java.bat
powershell -NoProfile -ExecutionPolicy Bypass -Command "& .\gradlew.bat build --console=plain 2>&1 | Tee-Object -FilePath build.log"
echo.
echo Done. You can close this window.
pause
