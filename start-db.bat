@echo off
setlocal

set MYSQL_HOME=C:\laragon\bin\mysql\mysql-8.4.3-winx64
set MYSQLD=%MYSQL_HOME%\bin\mysqld.exe
set MYSQL_INI=%MYSQL_HOME%\my.ini

netstat -ano | findstr ":3306" >nul
if %errorlevel%==0 (
    echo [DB] MySQL/MariaDB is already running on port 3306.
    exit /b 0
)

if not exist "%MYSQLD%" (
    echo [DB] Cannot find mysqld.exe at:
    echo %MYSQLD%
    pause
    exit /b 1
)

echo [DB] Starting MySQL without opening Laragon...
start "TaskManager MySQL" /min "%MYSQLD%" --defaults-file="%MYSQL_INI%"

timeout /t 3 /nobreak >nul
netstat -ano | findstr ":3306" >nul
if %errorlevel%==0 (
    echo [DB] MySQL started on port 3306.
) else (
    echo [DB] MySQL may not have started. Check Laragon MySQL data/config.
    pause
    exit /b 1
)

