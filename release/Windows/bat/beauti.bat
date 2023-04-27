@echo off

REM Check whether the JRE is included
IF EXIST "%~dp0\..\jre" (

REM for BEAST version that includes JRE
    "%~dp0\..\jre\bin\java" -Xss256m -Xmx8g -Dfile.encoding=UTF-8 -cp "%~dp0\..\lib\launcher.jar" beast.pkgmgmt.launcher.BeautiLauncher %*

) ELSE (
REM for version that does not include JRE
    java -Xss256m -Xmx8g -Dfile.encoding=UTF-8 -cp "%~dp0\..\lib\launcher.jar" beast.pkgmgmt.launcher.BeautiLauncher %*
)