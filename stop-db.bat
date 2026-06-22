@echo off
setlocal

set MYSQL_HOME=C:\Program Files\MariaDB 12.3
set MYSQLADMIN=%MYSQL_HOME%\bin\mariadb-admin.exe
set MYSQL_SERVICE=TaskManagerMariaDB

sc query "%MYSQL_SERVICE%" >nul 2>nul
if %errorlevel%==0 (
    echo [DB] Stopping MariaDB service...
    net stop "%MYSQL_SERVICE%"
    exit /b %errorlevel%
)

if not exist "%MYSQLADMIN%" (
    echo [DB] Cannot find mysqladmin.exe at:
    echo %MYSQLADMIN%
    pause
    exit /b 1
)

echo [DB] Stopping MySQL...
"%MYSQLADMIN%" -uroot shutdown

if %errorlevel%==0 (
    echo [DB] MySQL stopped.
) else (
    echo [DB] Could not stop MySQL. It may already be stopped or root may need a password.
    pause
    exit /b 1
)
