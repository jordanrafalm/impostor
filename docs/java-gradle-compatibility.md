# Java and Gradle compatibility

The checked-in wrapper is Gradle 8.11.1. Use JDK 21 for Android Studio and terminal builds. JDK 19 is also verified for the current terminal build. Do not run this project with Java 25: Gradle/Kotlin tooling can fail while loading build scripts with `Unsupported class file major version 69`.

## Android Studio

Open **Settings/Preferences > Build, Execution, Deployment > Build Tools > Gradle**, set **Gradle JDK** to a JDK 21 installation, and restart the Gradle sync. The setting must be applied to the project, not only to a separate terminal.

## Terminal

Select a compatible JDK without hardcoding a user-specific path, then verify:

```bash
java -version
./gradlew --version
./gradlew build
```

The Gradle wrapper is intentionally pinned in `gradle/wrapper/gradle-wrapper.properties`. Kotlin compilation remains multiplatform and uses the repository's Gradle/Kotlin versions; a toolchain cannot change the JVM that launches Gradle itself, so Android Studio's Gradle JDK selection is required to prevent the Java 25 error.