@echo off
setlocal
cd /d "%~dp0"

echo [SERVER] Starting TaskManager Server...
set TASKMANAGER_DB_HOST=127.0.0.1
set TASKMANAGER_DB_PORT=3306
set TASKMANAGER_DB_NAME=taskmanager
set TASKMANAGER_DB_USER=root
set TASKMANAGER_DB_PASSWORD=

mvn exec:java -Dexec.mainClass="com.taskmanager.server.ServerMain"
pause
