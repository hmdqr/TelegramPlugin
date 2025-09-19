@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Move to repo root
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven (mvn) not found in PATH.
  exit /b 1
)

set NEW_VERSION=%~1
if "%NEW_VERSION%"=="" (
  set /p NEW_VERSION=Enter new version (e.g., 1.1.0): 
)
if "%NEW_VERSION%"=="" (
  echo [ERROR] No version provided.
  exit /b 1
)

echo Setting project version to %NEW_VERSION% ...
mvn -q versions:set -DnewVersion=%NEW_VERSION% -DgenerateBackupPoms=false
if errorlevel 1 (
  echo [ERROR] Failed to set version via Maven.
  exit /b 1
)

for /f "delims=" %%v in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.version') do set CURR_VERSION=%%v

echo Version is now %CURR_VERSION%
endlocal
