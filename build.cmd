@echo off
REM Compiles the DEIHS Library System into the out\ folder.
REM The sources now live in several packages under app\, so the file list is
REM gathered rather than spelled out.
setlocal
if exist sources.txt del sources.txt
dir /s /B app\*.java ui\*.java Main.java > sources.txt
javac -cp "lib\mysql-connector-j-9.4.0.jar" -d out @sources.txt
set RESULT=%ERRORLEVEL%
del sources.txt
if not "%RESULT%"=="0" (
    echo.
    echo Build FAILED.
    exit /b 1
)
echo Build OK. Run the program with run.cmd
