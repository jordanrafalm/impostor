# Build environment

The Gradle build must run on a supported JDK, not the JDK bundled with an Android Studio preview if that JDK is Java 25.

## Android Studio

In **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle**, set **Gradle JDK** to JDK 19 or JDK 21. Do not select JDK 25 for this project. The repository includes `gradle/gradle-daemon-jvm.properties`, which requests JDK 19 for the Gradle daemon because it is the compatible JDK available in the development environment.

The error `Unsupported class file major version 69` means Gradle or a build plugin was run with Java 25. It is an environment mismatch, not an application bytecode issue.

## Terminal

Use a JDK 21 installation without hardcoding a machine-specific path:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew build --no-daemon
```

Verify the selected runtime with:

```bash
java -version
./gradlew --version
```

Do not commit `JAVA_HOME`, SDK paths, or other user-specific paths to Gradle configuration.
