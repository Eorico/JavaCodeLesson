@echo off
REM Starts the DEIHS Library System. Run build.cmd first.
if not exist "out\Main.class" (
    echo Not built yet. Run build.cmd first.
    exit /b 1
)
java -cp "out;lib\mysql-connector-j-9.4.0.jar" Main
