@echo off
REM Clean Gradle configuration cache
REM This script removes the Gradle configuration cache to prevent "Spotless JVM-local cache is stale" errors
REM Run this when:
REM - Spotless version is upgraded
REM - Gradle version is upgraded
REM - build.gradle or spotless configuration is modified
REM - "Spotless JVM-local cache is stale" error occurs

echo Cleaning Gradle configuration cache...

if exist .gradle\configuration-cache (
    rmdir /q /s .gradle\configuration-cache
    echo Configuration cache deleted successfully.
) else (
    echo Configuration cache directory does not exist. Nothing to clean.
)

echo Done.
pause
