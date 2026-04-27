@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAR_FILE=%APP_HOME%english-chinese-dictionary.jar"
set "LIB_DIR=%APP_HOME%lib"
set "RUNTIME_HOME=%APP_HOME%runtime-home"

if not exist "%JAR_FILE%" (
    echo Missing file: %JAR_FILE%
    echo Build the project in the code folder and copy the generated jar into bin.
    pause
    exit /b 1
)

if not exist "%LIB_DIR%" (
    echo Missing directory: %LIB_DIR%
    echo Copy code\target\lib into bin\lib before running.
    pause
    exit /b 1
)

if not exist "%RUNTIME_HOME%" mkdir "%RUNTIME_HOME%"

java -Duser.home="%RUNTIME_HOME%" --module-path "%LIB_DIR%" --add-modules javafx.controls,javafx.graphics -cp "%JAR_FILE%;%LIB_DIR%\*" com.ecdictionary.DictionaryApplication
if errorlevel 1 pause
