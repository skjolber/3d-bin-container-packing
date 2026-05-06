---
name: maven
description: 'Maven build expertise for this multi-module Java project. Use when working with pom.xml files, managing dependencies, running builds or tests for specific modules, configuring or troubleshooting plugins (surefire, jacoco, shade, spotless, pitest, owasp), regenerating OpenAPI sources, building the JMH benchmark JAR, or releasing to Maven Central.'
---

# Maven Multi-Module Build

This project is a Maven multi-module build. The root `pom.xml` is the parent; all dependency versions and plugin versions are declared there as properties.

## Module Names (for `-pl`)

| `-pl` value | Description |
|---|---|
| `api` | Public interfaces and data model |
| `points` | Free-space point tracking |
| `core` | Packager algorithm implementations |
| `test` | Shared test utilities |
| `jmh` | JMH benchmark suite |
| `open-api/open-api-model` | Generated Jackson model |
| `open-api/open-api-server` | Generated Spring server stubs |
| `open-api/open-api-client` | Generated Apache HttpClient 5 stubs |
| `open-api/open-api-test` | Shared open-api test utilities |
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

### OpenAPI Generator — regenerate model/server/client
```bash
# All three at once
mvn generate-sources -pl open-api/open-api-model,open-api/open-api-server,open-api/open-api-client

# Individual module
mvn generate-sources -pl open-api/open-api-model
```
Source of truth is `open-api/3d-api.yaml`. Never hand-edit generated sources.

### PiTest — mutation testing
```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage -pl <module> -am
```

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

## Release to Maven Central

Uses `maven-release-plugin` + `maven-gpg-plugin` + Sonatype Central Portal plugin.

```bash
mvn release:prepare
mvn release:perform
```
