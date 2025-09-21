@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Ultra-simple release: build and attach shaded JAR to GitHub Release
rem Prereqs (one-time):
rem   - Install Maven: https://maven.apache.org
rem   - Install GitHub CLI: https://cli.github.com
rem   - Authenticate: gh auth login
rem Tip: Make sure your latest commits are pushed before running this script.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%.."

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven ^(mvn^) not found in PATH.
  echo Install from https://maven.apache.org and try again.
  pause
  exit /b 1
)

where gh >nul 2>nul
if errorlevel 1 (
  echo [ERROR] GitHub CLI ^(gh^) not found in PATH.
  echo Install from https://cli.github.com and run: gh auth login
  pause
  exit /b 1
)

echo Building plugin (skip tests)...
mvn -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] Build failed.
  pause
  exit /b 1
)

set "JAR="
for %%f in (target\*-shaded.jar) do set "JAR=%%f"
if "%JAR%"=="" (
  echo [ERROR] Could not find shaded jar in target\
  pause
  exit /b 1
)

for /f "delims=" %%v in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.version') do set "VERSION=%%v"
if "%VERSION%"=="" (
  echo [ERROR] Unable to read project version.
  pause
  exit /b 1
)
set "TAG=v%VERSION%"

echo Creating or updating GitHub release %TAG% ...
gh release view "%TAG%" >nul 2>nul
if errorlevel 1 (
  rem Create new release (also creates tag on GitHub at current default branch tip unless --target is specified)
  gh release create "%TAG%" "%JAR%" --title "%TAG%" --notes "Automated release"
) else (
  rem Update existing release
  gh release upload "%TAG%" "%JAR%" --clobber
)
if errorlevel 1 (
  echo [ERROR] GitHub release step failed.
  pause
  exit /b 1
)

echo Done. Release %TAG% updated with %JAR%.
endlocal
pause
