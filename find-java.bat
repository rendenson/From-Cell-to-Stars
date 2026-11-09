@echo off
rem Picks a Java that Gradle can run on (17-24); downloads a portable Java 21 into .\jdk if there is none
echo Looking for Java... (if none fits, Java 21 is downloaded once, ~200 MB - please wait)
for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find-java.ps1"`) do set "JAVA_HOME=%%J"
echo Using Java: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"
