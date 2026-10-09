# Repository map

| Module (`-pl`) | Contents |
|---|---|
| `api` | Public model and interfaces: `Box`, `BoxItem`, `BoxStackValue`, `Container`, `Placement`, `Packager`, interrupts, controls, container packing strategies |
| `points` | Extreme-point / free-space tracking (`ep.points2d`, `ep.points3d`) |
| `validators` | Result and load validators |
| `core` | Packagers (`packer.plain`, `packer.laff`, `packer.bruteforce`, `packer.composite`), permutation iterators (`iterator`), container packing strategy implementations (`packer.strategy`), virtual-box preprocessing (`virtualbox`) |
| `test` | Shared test utilities (assertj extensions, generators, Bouwkamp data, api-only extension examples) |
| `jmh` | JMH benchmarks; datasets in `jmh/src/main/resources` |
| `visualizer/*` | Visualizer applications |

Documentation: `README.md` (usage), `FEATURES.md` (feature summary), `DEVELOPER.md`
(writing controls), `skills/maven/SKILL.md` (detailed Maven usage: modules, plugins,
JMH, releases). When changing user-visible behaviour or defaults, update the
matching `README.md` and `FEATURES.md` sections in the same change.

# Source code

## Formatting
Line length 200. Tabs for indentation (see `eclipse-formatter.xml`).

## Java code

 * Improved performance translates to better results in real life
   * Do not use `Comparator`. Rather create custom `XXXComparator` interfaces.
     * Do not inline optimizations that use instance checks on known static comparators.
   * Do not use streams instead of a for-loop.
   * Check arguments in builders etc, not in constructors etc on the critical path.
 * Use one line per builder method.
 * Library sources target Java 17; do not use newer language features or APIs.

## Tests

 * Use ASCII art to explain stacking unit tests. Prefer generated figures (`ContainerAsciiArt` and
   `FigureRecorder` in the `test` module; `PackagerResultFigures.figure(result)` for packager results) to hand-drawn ones.
 * Generated figures sit between `// <figure>` and `// </figure>` lines. Do not edit them by hand: run the
   tests with `-Dfigures.record=true` to write or refresh them. They are for humans, so read test files
   without them, for example `sed '/<figure>/,/<\/figure>/d' SomeTest.java`.
 * Tests live in the same package as the code under test and may use protected members.
 * Keep brute-force test inputs small (it is exponential; about 10 boxes or fewer per
   container) and give packagers an interrupt duration so a regression cannot hang the build.
 * When a test encodes behaviour that a change intentionally alters, update or rename it
   to describe the new behaviour rather than weakening its assertions.

# Building and testing

Run commands from the repository root. Use JDK 25, matching CI and the
Copilot setup; library sources target Java 17. Use the checked-in Maven
wrapper (`./mvnw`, or `mvnw.cmd` on Windows), which pins Maven 3.9.16.
The first run needs network access to download Maven and dependencies.

## Iteration

Test core and its dependencies without building unrelated applications:

```sh
./mvnw -B -ntp -Pdev -pl core -am test
```

Use `-pl points -am` for extreme-point changes, or `-pl api -am` for API
changes. These selections test dependencies, not downstream consumers;
run the core selection after changing shared library code.
The `test` module provides shared test utilities; it is not the whole test suite.

Run individual tests, including compilation of required modules. `-Dtest`
accepts comma-separated names and wildcards:

```sh
./mvnw -B -ntp -Pdev -pl core -am -Dtest=LargestAreaFitFirstPackagerTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -Pdev -pl core -am -Dtest='VirtualBox*Test,GridVirtualBoxLayoutGeneratorTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

The no-matching-tests option is needed for upstream modules. It also permits
misspelled test names: confirm the named test ran and its test count is nonzero.
Do not set this option globally.

The opt-in `dev` profile skips coverage, source JARs, and Javadoc generation;
it does not skip tests. Tests use one fork by default. Override with
`-Dtest.forkCount=1.5C` when additional CPU and memory are available.
Avoid `clean` during ordinary iteration to preserve build outputs.

## Build cache

The root POM enables `maven-build-cache-extension`. When inputs are unchanged,
Maven replays cached results and logs `Skipping plugin execution (cached)`:
tests are then *not* executed, and per-module times are unusually short.
When a run must prove that tests executed (final verification, or after
suspicious results), add `-Dmaven.build.cache.enabled=false` and confirm that
each module reports `Tests run:` counts.

## Final verification

```sh
./mvnw -B -ntp -Dmaven.build.cache.enabled=false verify
```

This builds all modules, runs tests, and retains coverage and packaging checks,
including Javadoc and Moditect. Do not use `-Pdev` or test-skipping flags for this check.
Use `clean verify` when a fresh build is needed or stale outputs are suspected,
for example a Javadoc error `No source files for package ...` in a module you did not change.
Report which checks ran, per-module test counts, and any failures or checks that could not run.

## Diagnosing failures

Read the Maven reactor summary and the failing module's
`target/surefire-reports/` directory. XML reports contain test results;
text reports contain failure details, and `*-output.txt` contains captured
test output. Confirm reports are from the current run.
Keep test summaries visible: use `-B -ntp`, not `-q`.
If capturing output in a script, preserve Maven's exit status.

Before attributing a failure to your change, check whether it also fails on the
unchanged code (for example `git stash`, rerun the single test, `git stash pop`),
and report pre-existing failures separately.
