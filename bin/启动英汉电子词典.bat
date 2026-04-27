@echo off
setlocal

set "ROOT_DIR=%~dp0.."
set "CODE_DIR=%ROOT_DIR%\code"
set "TARGET_DIR=%CODE_DIR%\target"
set "APP_JAR=%TARGET_DIR%\english-chinese-dictionary-1.0.0.jar"
set "LIB_DIR=%TARGET_DIR%\lib"
set "APP_DATA_DIR=%ROOT_DIR%\app-data"
set "JAVAFX_CACHE_DIR=%APP_DATA_DIR%\javafx-cache"

if not exist "%APP_JAR%" (
    echo [INFO] Build output not found. Running Maven package...
    pushd "%CODE_DIR%"
    call mvn "-Dmaven.repo.local=.m2" package
    if errorlevel 1 (
        echo [ERROR] Maven package failed.
        echo [ERROR] Please check JDK 21 and Maven first.
        popd
        pause
        exit /b 1
    )
    popd
)

if not exist "%APP_JAR%" (
    echo [ERROR] Main jar not found:
    echo %APP_JAR%
    pause
    exit /b 1
)

if not exist "%LIB_DIR%" (
    echo [ERROR] Runtime library folder not found:
    echo %LIB_DIR%
    pause
    exit /b 1
)

if not exist "%APP_DATA_DIR%" mkdir "%APP_DATA_DIR%"
if not exist "%JAVAFX_CACHE_DIR%" mkdir "%JAVAFX_CACHE_DIR%"

pushd "%CODE_DIR%"
java -Djavafx.cachedir="%JAVAFX_CACHE_DIR%" --module-path "%LIB_DIR%" --add-modules javafx.controls,javafx.graphics -jar "%APP_JAR%"
set "APP_EXIT=%ERRORLEVEL%"
popd

if not "%APP_EXIT%"=="0" (
    echo [ERROR] Application exited with code %APP_EXIT%.
    pause
    exit /b %APP_EXIT%
)

endlocal
