# Source code

## Formatting
Line length 200.

# Building and testing

Run commands from the repository root. Use JDK 25, matching CI and the
Copilot setup; library sources target Java 17. Use the checked-in Maven
wrapper (`./mvnw`, or `mvnw.cmd` on Windows), which pins Maven 3.9.12.
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

Run an individual test, including compilation of required modules:

```sh
./mvnw -B -ntp -Pdev -pl core -am -Dtest=LargestAreaFitFirstPackagerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The no-matching-tests option is needed for upstream modules. It also permits
misspelled test names: confirm the named test ran and its test count is nonzero.
Do not set this option globally.

The opt-in `dev` profile skips coverage, source JARs, and Javadoc generation;
it does not skip tests. Tests use one fork by default. Override with
`-Dtest.forkCount=1.5C` when additional CPU and memory are available.
Avoid `clean` during ordinary iteration to preserve build outputs.

## Final verification

```sh
./mvnw -B -ntp verify
```

This builds all modules, runs tests, and retains coverage and packaging checks,
including Moditect. Do not use `-Pdev` or test-skipping flags for this check.
Use `clean verify` when a fresh build is needed or stale outputs are suspected.
Report which checks ran and any failures or checks that could not run.

## Diagnosing failures

Read the Maven reactor summary and the failing module's
`target/surefire-reports/` directory. XML reports contain test results;
text reports contain failure details, and `*-output.txt` contains captured
test output. Confirm reports are from the current run.
Keep test summaries visible: use `-B -ntp`, not `-q`.
If capturing output in a script, preserve Maven's exit status.
