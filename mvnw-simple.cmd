@echo off
setlocal enabledelayedexpansion

REM Set JAVA_HOME if not already set
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
)

REM Check if wrapper jar exists, if not download it
if not exist ".mvn\wrapper\maven-wrapper.jar" (
    echo Downloading Maven Wrapper...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '.mvn\wrapper\maven-wrapper.jar'}"
)

REM Run Maven Wrapper with proper quoting
"%JAVA_HOME%\bin\java.exe" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%CD%" org.apache.maven.wrapper.MavenWrapperMain %*
