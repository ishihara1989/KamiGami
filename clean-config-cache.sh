#!/bin/bash
# Clean Gradle configuration cache
# This script removes the Gradle configuration cache to prevent "Spotless JVM-local cache is stale" errors
# Run this when:
# - Spotless version is upgraded
# - Gradle version is upgraded
# - build.gradle or spotless configuration is modified
# - "Spotless JVM-local cache is stale" error occurs

echo "Cleaning Gradle configuration cache..."

if [ -d ".gradle/configuration-cache" ]; then
    rm -rf .gradle/configuration-cache
    echo "Configuration cache deleted successfully."
else
    echo "Configuration cache directory does not exist. Nothing to clean."
fi

echo "Done."
