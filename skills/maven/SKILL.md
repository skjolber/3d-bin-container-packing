---
name: maven
description: 'Maven build expertise for this multi-module Java project. Use when working with pom.xml files, managing dependencies, running builds or tests for specific modules, configuring or troubleshooting plugins (surefire, jacoco, shade, spotless, pitest, spotbugs, owasp), building the JMH benchmark JAR, or releasing to Maven Central.'
---

# Maven Multi-Module Build

This project is a Maven multi-module build. The root `pom.xml` is the parent; all dependency versions and plugin versions are declared there as properties.

## Module Names (for `-pl`)

| `-pl` value | Description |
|---|---|
| `api` | Public interfaces and data model |
| `points` | Free-space point tracking |
| `validators` | Result and load validators |
| `core` | Packager algorithm implementations |
| `test` | Shared test utilities |
| `jmh` | JMH benchmark suite |
| `visualizer/api` | Visualizer JSON contract types |
| `visualizer/algorithm` | Algorithm state capture |
| `visualizer/packaging` | Packing result → JSON conversion |

## Common Commands

```bash
# Build and test one module (and its dependencies)
mvn test -pl <module> -am

# Build without running tests
mvn package -pl <module> -am -DskipTests

# Install all modules
mvn install

# Install everything, skip tests
mvn install -DskipTests
```

## Plugin Reference

### Surefire — running tests
```bash
mvn test -pl <module> -am
```
Version is `maven-surefire-plugin.version` in root `pom.xml`.

### JaCoCo — code coverage
```bash
mvn test jacoco:report -pl <module> -am
# Report lands in target/site/jacoco/index.html
```

### Spotless — code formatting
```bash
mvn spotless:check          # fail if formatting is off
mvn spotless:apply          # auto-fix formatting
```

### Maven Shade — fat JAR (jmh module)
```bash
mvn package -pl jmh -am -DskipTests
java -jar jmh/target/benchmarks.jar
```

### PiTest — mutation testing
```bash
./mvnw -B -ntp -Pdev test-compile org.pitest:pitest-maven:mutationCoverage -pl <module> -am -DfailWhenNoMutations=false
```
`-DfailWhenNoMutations=false` is needed because `-am` also runs PIT on upstream modules.
Narrow a run with `-DtargetClasses=<pattern> -DtargetTests=<pattern>` (for example
`-DtargetClasses='com.github.skjolber.packing.ep.points2d.*'`); a whole module takes long.
JUnit 5 support comes from `pitest-junit5-plugin` in the root POM.

### SpotBugs — static analysis
```bash
./mvnw -B -ntp -Pdev,spotbugs -DskipTests -Dmaven.build.cache.enabled=false clean verify -pl api,points,validators,core -am
```
The `spotbugs` profile runs the `check` goal at `verify` (effort max, threshold low) and writes
`<module>/target/spotbugsXml.xml`; the build fails on findings. `spotbugs-exclude.xml` in the root excludes
findings which are by design or reviewed (mutable inputs and outputs are not copied, no serialization, extension points).
Use `clean`: Moditect fails on an already modular JAR otherwise.

### OWASP Dependency Check
```bash
mvn dependency-check:check -pl <module>
```

## Dependency Management Rules

- **All versions live in root `pom.xml` as `<properties>`** — never hardcode a version in a child POM.
- Use `${property.version}` references in child POMs.
- `<dependencyManagement>` in the root POM controls all third-party and inter-module versions.
- Inter-module dependencies use `${project.version}`.

## Key Version Properties

| Property | Controls |
|---|---|
| `java.version` | Java 17 source/target |
| `junit.version` | JUnit Jupiter |
| `mockito.version` | Mockito |
| `assertj.version` | AssertJ |
| `jmh.version` | JMH framework |
| `jacoco-maven-plugin.version` | JaCoCo |
| `maven-surefire-plugin.version` | Surefire |
| `spotless.version` | Spotless formatter |
| `pitest.version` | PiTest mutation testing |
| `pitest-junit5-plugin.version` | PiTest JUnit 5 test discovery |
| `spotbugs-maven-plugin.version` | SpotBugs (`spotbugs` profile) |

## Release to Maven Central

Uses `maven-release-plugin` + `maven-gpg-plugin` + Sonatype Central Portal plugin.

```bash
mvn release:prepare
mvn release:perform
```
