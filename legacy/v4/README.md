# shadowed-v4

Version 4 of this library (the `api`, `points` and `core` artifacts) with its packages moved from
`com.github.skjolber.packing` to `com.github.skjolber.packing.v4`, so that 4.x and 5.x can run side by side in one JVM.
Without the move, the two versions have the same class names and cannot be on the same classpath.

```java
// 5.x
com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D calculator;
// 4.x
com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D reference;
```

The module has no code of its own: the build takes the released 4.x jars from Maven Central and relocates them.

## Why

Tests compare 5.x with 4.x on the same input: where 5.x is meant to behave as 4.x did, 4.x is the reference.

- `PointCalculatorVersion4ComparisonTest` in `points` gives both point calculators the same random placements and requires,
  after every placement, the same free points in 2D, and in 3D that the union of the 5.x points covers every free point
  of 4.x (and `PointCalculatorVersion4ComparisonIT` many more). Exact 3D point parity ended when the moved points got a
  canonical total order (see `CustomIntXComparator`): 4.x left the order of equal keys to a quicksort. The free space is
  identical, but may be tiled into different maximal points; a 4.x point can be covered by several 5.x points, not by one.
  4.x points below the minimum area or volume limit in force are exempt, as no remaining box fits into them (4.x keeps some,
  and 5.x may have dropped them: in 4,000 random sequences, 28 checks in 2 sequences, always below the limits).
- The brute force differential tests in `core` (package `packer.bruteforce.reference`) use the 4.x brute force search for a
  single container and its permutation and rotation iterator as the reference, through `Version4Reference`: the 5.x
  iterators must enumerate the same states, and the 5.x searches must find packings as good. `ReferenceGoldenMasterTest`
  pins the reference to known 4.x results.

The `*IT` classes run with `mvn -P slow-tests verify`.

The module is not published. Use it as a `test` dependency only:

```xml
<dependency>
	<groupId>com.github.skjolber.3d-bin-container-packing</groupId>
	<artifactId>shadowed-v4</artifactId>
	<version>${project.version}</version>
	<scope>test</scope>
</dependency>
```

The two versions share no objects: build the input for each version with its own classes, for example a 4.x `Box` with
`com.github.skjolber.packing.v4.api.Box.newBuilder()`, and compare the results by their values.

## Version

The 4.x version is the property `v4.version` in `pom.xml`. To try another 4.x build, for example a snapshot installed
locally with `mvn install` on the 4.x branch, override it on the command line:

```sh
./mvnw -B -ntp -Dmaven.build.cache.enabled=false -Dv4.version=4.2.5-SNAPSHOT -pl points -am test
```

Do not commit a snapshot version: the CI build cannot resolve a snapshot which only exists in a local repository.
After changing the version, run the tests of `points` and `core` with the slow tests; `ReferenceGoldenMasterTest` shows whether
the brute force results of 4.x changed.

## How it is built

- `maven-shade-plugin` merges the 4.x `api`, `points` and `core` jars and relocates their packages. It leaves out their
  `module-info.class` (which names the 5.x modules) and does not include eclipse-collections, which both versions use in
  the same version.
- The 4.x artifacts are optional dependencies, so they are not passed on to the modules which use this one, where they
  would clash with the 5.x classes. eclipse-collections is a normal dependency.
- The relocation runs in the `process-classes` phase, and the relocated classes are unpacked into `target/classes`:
  in a build which stops before `package` (such as `mvn test`), Maven gives the modules which depend on this one
  `target/classes` instead of the jar. For the shade plugin, the (empty) jar is created early in the same phase.
- Javadoc, sources, Moditect and publishing are switched off.

Users who want to run 4.x next to 5.x while migrating can relocate 4.x the same way in their own build, with the shade
plugin configuration in `pom.xml`.
