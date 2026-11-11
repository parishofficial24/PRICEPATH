@echo off
setlocal
cd /d "%~dp0"
if "%PRICEPATH_DB_PASSWORD%"=="" (
  echo Please set PRICEPATH_DB_PASSWORD to your MySQL root password before running.
  echo Example in this terminal: set PRICEPATH_DB_PASSWORD=your_password
  exit /b 1
)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d out @sources.txt
if errorlevel 1 (
  echo.
  echo Build failed. Read the compiler messages above.
  exit /b 1
)
java -cp "out;lib\*" app.Main
