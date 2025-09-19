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

echo Building for release ...
mvn -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] Build failed.
  echo.
  pause
  exit /b 1
)

set "JAR="
for %%f in (target\*-shaded.jar) do set "JAR=%%f"
if "%JAR%"=="" (
  echo [ERROR] Could not find shaded jar in target\
  echo.
  pause
  exit /b 1
)

echo Creating and pushing tag %TAG% ...
git tag -a "%TAG%" -m "Release %TAG%"
if errorlevel 1 (
  echo [ERROR] git tag failed (maybe tag exists?).
  echo.
  pause
  exit /b 1
)
git push && git push origin "%TAG%"
if errorlevel 1 (
  echo [ERROR] Pushing tag failed.
  echo.
  pause
  exit /b 1
)

where gh >nul 2>nul
if errorlevel 1 (
  echo 'gh' not found. You can publish the tag as a GitHub Release in the UI; the workflow will upload the JAR.
) else (
  echo Creating GitHub release %TAG% ...
  gh release create "%TAG%" "%JAR%" --title "%TAG%" --notes "Automated release"
)

echo Done.
endlocal
pause
