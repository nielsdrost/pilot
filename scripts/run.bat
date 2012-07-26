@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set HOME=%~dp0..

set ARGS=

:setupArgs
if ""%1""=="""" goto doneStart
set ARGS=%ARGS% %1
shift
goto setupArgs

:doneStart

java -classpath "%HOME%;%HOME%\lib\*" -Dgat.adaptor.path="%HOME%"\lib\adaptors %DEPLOY_ARGS%

if "%OS%"=="Windows_NT" @endlocal
