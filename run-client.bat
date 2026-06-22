@echo off
setlocal
cd /d "%~dp0"

echo [CLIENT] Starting TaskManager Client...
mvn javafx:run
pause
