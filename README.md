# VISUS Gradle Plugin

An Eclipse plugin to run standard Gradle tasks from context menu in a mixed Gradle - Eclipse project
environment.

## Issue

The standard Gradle plugin for Eclipse does not work properly in a mixed Gradle - Eclipse project
environment where some Eclipse files were generated using Gradle. It also does not work with the
Gradle "war" task and the "Run on server" action using a Tomcat due to missing / misplaced artifacts
in the WEB-INF/lib folder!

## Solution

This plugin replaces most of the content of the standard Gradle plugin for Eclipse used by us with a
small context menu plugin to run some actions.

- clean the Gradle output directory (build)
- generating JAR using Gradle
- generating WAR using Gradle
- fix the WEB-INF/lib situation for running the WAR inside Eclipse using the "Run on server" action
- fix the WEB-INF/lib situation for running the WAR using Gradle
- regenerate the Eclipse files when dependencies have changed in Gradle
- fix possible corrupt Eclipse files when working with Gradle
