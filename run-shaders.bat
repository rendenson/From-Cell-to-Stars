@echo off
rem Like run.bat, but with Sodium + Iris so shader packs work. Put packs into run\shaderpacks
cd /d "%~dp0"
call find-java.bat
powershell -NoProfile -ExecutionPolicy Bypass -Command "& .\gradlew.bat runClient -Pshaders=true --console=plain 2>&1 | Tee-Object -FilePath build.log"
echo.
echo Done. You can close this window.
pause
