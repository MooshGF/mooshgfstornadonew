@echo off
setlocal
cd /d "%~dp0"
if not exist gradlew.bat (
  echo First, download and extract the official Forge 1.20.1 MDK into this folder.
  echo https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
  echo Choose MDK, then copy this project's src folder and build.gradle over the extracted MDK.
  pause
  exit /b 1
)
call gradlew.bat build
if errorlevel 1 (echo BUILD FAILED. See errors above.& pause & exit /b 1)
echo Built JAR is in build\libs\MooshGFsTornado-1.0.0.jar
pause
