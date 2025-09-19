@echo off
setlocal

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven (mvn) not found in PATH.
  echo.
  pause
  exit /b 1
)

echo Building plugin (skip tests)...
mvn -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] Build failed.
  echo.
  pause
  exit /b 1
)

echo Done. Artifacts in target\
endlocal
pause
