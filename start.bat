@echo off
setlocal EnableExtensions EnableDelayedExpansion

if /I "%~1"=="--help" goto :help
if /I "%~1"=="-h" goto :help

set "PROJECT_ROOT=%~dp0"
set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "TIEBA_DIR=%PROJECT_ROOT%\tieba"
set "MVNW=%TIEBA_DIR%\mvnw.cmd"
set "WAR_FILE=%TIEBA_DIR%\target\tieba.war"
set "APP_URL=http://localhost:8080/tieba"

echo [INFO] Project root: %PROJECT_ROOT%
echo [INFO] Backend module: %TIEBA_DIR%

if not exist "%TIEBA_DIR%" (
  echo [ERROR] Backend directory not found: %TIEBA_DIR%
  exit /b 1
)

if not exist "%MVNW%" (
  echo [ERROR] Maven wrapper not found: %MVNW%
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java is not available in PATH.
  echo [HINT] Install JDK 21+ and make sure "java -version" works.
  exit /b 1
)

echo [INFO] Building WAR package with Maven Wrapper...
pushd "%TIEBA_DIR%"
call "%MVNW%" clean package
if errorlevel 1 (
  popd
  echo [ERROR] Build failed.
  exit /b 1
)
popd

if not exist "%WAR_FILE%" (
  echo [ERROR] WAR package not found: %WAR_FILE%
  exit /b 1
)

set "TOMCAT_HOME="
if defined TOMCAT_HOME if exist "%TOMCAT_HOME%\bin\startup.bat" set "TOMCAT_HOME=%TOMCAT_HOME%"
if not defined TOMCAT_HOME if defined CATALINA_HOME if exist "%CATALINA_HOME%\bin\startup.bat" set "TOMCAT_HOME=%CATALINA_HOME%"
if not defined TOMCAT_HOME if exist "%PROJECT_ROOT%\tomcat\bin\startup.bat" set "TOMCAT_HOME=%PROJECT_ROOT%\tomcat"
if not defined TOMCAT_HOME if exist "%PROJECT_ROOT%\apache-tomcat\bin\startup.bat" set "TOMCAT_HOME=%PROJECT_ROOT%\apache-tomcat"
if not defined TOMCAT_HOME (
  for /d %%D in ("%PROJECT_ROOT%\apache-tomcat*") do (
    if exist "%%~fD\bin\startup.bat" (
      set "TOMCAT_HOME=%%~fD"
      goto :tomcat_found
    )
  )
)

:tomcat_found
if not defined TOMCAT_HOME (
  echo [ERROR] Tomcat was not found.
  echo [HINT] Set TOMCAT_HOME or CATALINA_HOME, or place Tomcat under:
  echo [HINT]   %PROJECT_ROOT%\tomcat
  echo [HINT]   %PROJECT_ROOT%\apache-tomcat*
  exit /b 1
)

echo [INFO] Tomcat home: %TOMCAT_HOME%
echo [INFO] Deploying WAR to Tomcat webapps...

if exist "%TOMCAT_HOME%\webapps\tieba" rd /s /q "%TOMCAT_HOME%\webapps\tieba"
copy /y "%WAR_FILE%" "%TOMCAT_HOME%\webapps\tieba.war" >nul
if errorlevel 1 (
  echo [ERROR] Failed to copy WAR to Tomcat webapps.
  exit /b 1
)

echo [INFO] Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"
if errorlevel 1 (
  echo [ERROR] Tomcat failed to start.
  exit /b 1
)

echo [INFO] Waiting for application readiness: %APP_URL%
set /a READY_ATTEMPTS=0

:wait_ready
powershell -NoLogo -NoProfile -Command "try { $resp = Invoke-WebRequest -Uri '%APP_URL%' -UseBasicParsing -TimeoutSec 5; if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) { exit 0 } else { exit 1 } } catch { exit 1 }"
if not errorlevel 1 goto :open_browser
set /a READY_ATTEMPTS+=1
if %READY_ATTEMPTS% GEQ 30 goto :ready_timeout
timeout /t 2 /nobreak >nul
goto :wait_ready

:open_browser
echo [INFO] Opening browser: %APP_URL%
start "" "%APP_URL%"
echo [SUCCESS] Application startup process completed.
echo [INFO] Opened: %APP_URL%
exit /b 0

:ready_timeout
echo [WARN] Application is still starting. Open manually after deployment finishes: %APP_URL%
echo [SUCCESS] Application startup process completed.
exit /b 0

:help
echo Usage: start.bat
echo.
echo This script builds the tieba WAR and starts Tomcat.
echo It auto-detects Tomcat in this order:
echo   1. TOMCAT_HOME
echo   2. CATALINA_HOME
echo   3. %~dp0tomcat
echo   4. %~dp0apache-tomcat
echo   5. %~dp0apache-tomcat*
exit /b 0
