@echo off
echo Starting Tomasulo Simulator...
echo.

REM Set JAVA_HOME
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"

REM Run Maven with error output
"%JAVA_HOME%\bin\java.exe" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%CD%" org.apache.maven.wrapper.MavenWrapperMain javafx:run 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start
    echo.
)

pause
