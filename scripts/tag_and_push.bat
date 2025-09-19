@echo off
setlocal ENABLEDELAYEDEXPANSION

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven (mvn) not found in PATH.
  echo.
  pause
  exit /b 1
)
where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git not found in PATH.
  echo.
  pause
  exit /b 1
)

for /f "delims=" %%v in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.version') do set VERSION=%%v

if "%VERSION%"=="" (
  echo [ERROR] Unable to read project version.
  echo.
  pause
  exit /b 1
)

set TAG=v%VERSION%
echo Will tag and push: %TAG%
set /p CONFIRM=Continue? (Y/N): 
if /i "%CONFIRM%"=="Y" (
  rem continue
) else (
  echo Aborted.
  echo.
  pause
  exit /b 1
)

echo Tagging %TAG% ...
git tag -a "%TAG%" -m "Release %TAG%"
if errorlevel 1 (
  echo [ERROR] git tag failed (maybe tag exists?).
  echo.
  pause
  exit /b 1
)

echo Pushing commits and tags ...
git push && git push origin "%TAG%"
if errorlevel 1 (
  echo [ERROR] git push failed.
  echo.
  pause
  exit /b 1
)

echo Done. Create a GitHub release from tag %TAG% (or run scripts\release.bat).
endlocal
pause
