@echo off
setlocal ENABLEDELAYEDEXPANSION

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven (mvn) not found in PATH.
  exit /b 1
)
where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git not found in PATH.
  exit /b 1
)

for /f "delims=" %%v in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.version') do set VERSION=%%v
if "%VERSION%"=="" (
  echo [ERROR] Unable to read project version.
  exit /b 1
)
set TAG=v%VERSION%

echo Building for release ...
mvn -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] Build failed.
  exit /b 1
)

set JAR=
for %%f in (target\*-shaded.jar) do set JAR=%%f
if "%JAR%"=="" (
  echo [ERROR] Could not find shaded jar in target\
  exit /b 1
)

echo Creating and pushing tag %TAG% ...
git tag -a %TAG% -m "Release %TAG%"
if errorlevel 1 (
  echo [ERROR] git tag failed (maybe tag exists?).
  exit /b 1
)
git push && git push origin %TAG%
if errorlevel 1 (
  echo [ERROR] Pushing tag failed.
  exit /b 1
)

where gh >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo Creating GitHub release %TAG% ...
  gh release create %TAG% "%JAR%" --title "%TAG%" --notes "Automated release"
) else (
  echo 'gh' not found. You can publish the tag as a GitHub Release in the UI; the workflow will upload the JAR.
)

echo Done.
endlocal
