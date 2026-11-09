@echo off
rem Gets the newest code, then builds and starts the test game.
rem  - in a git clone: downloads the latest commits from GitHub (git pull)
rem  - otherwise: unpacks celltostars-project.zip from Claude (the old src folder is removed first)
cd /d "%~dp0"
if exist .git (
    git pull --ff-only
) else if exist celltostars-project.zip (
    if exist src rmdir /s /q src
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force celltostars-project.zip ."
    del celltostars-project.zip
)
call run.bat
