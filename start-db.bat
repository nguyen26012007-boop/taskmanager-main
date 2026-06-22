@echo off
setlocal EnableDelayedExpansion

set MYSQL_HOME=C:\laragon\bin\mysql\mysql-8.4.3-winx64
set MYSQLD=%MYSQL_HOME%\bin\mysqld.exe
set MYSQLADMIN=%MYSQL_HOME%\bin\mysqladmin.exe
set MYSQL_INI=%MYSQL_HOME%\my.ini

if exist "%MYSQLADMIN%" (
    "%MYSQLADMIN%" -uroot ping >nul 2>nul
    if %errorlevel%==0 (
        echo [DB] MySQL/MariaDB is already running on port 3306.
        exit /b 0
    )
)

netstat -ano | findstr ":3306 .*LISTENING" >nul
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

for /l %%i in (1,1,20) do (
    timeout /t 1 /nobreak >nul
    if exist "%MYSQLADMIN%" (
        "%MYSQLADMIN%" -uroot ping >nul 2>nul
        if !errorlevel!==0 (
            echo [DB] MySQL started on port 3306.
            exit /b 0
        )
    )
    netstat -ano | findstr ":3306 .*LISTENING" >nul
    if !errorlevel!==0 (
        echo [DB] MySQL started on port 3306.
        exit /b 0
    )
)

echo [DB] MySQL may not have started. Check Laragon MySQL data/config.
pause
exit /b 1
