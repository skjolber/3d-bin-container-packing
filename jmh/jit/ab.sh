#!/usr/bin/env bash
set -euo pipefail

main() {
usage() { echo "Usage: $0 <name> <base-ref> <regex> [JMH args...]" >&2; exit 2; }
[[ $# -ge 3 ]] || usage
NAME=$1 BASE_REF=$2 REGEX=$3; shift 3
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
CPUS=${CPUS-0,1,16,17}
[[ -n "$CPUS" ]] || { echo "CPUS must name one or more CPUs; unpinned runs are not supported" >&2; exit 2; }
FORKS=${FORKS:-3}
[[ "$FORKS" =~ ^[1-9][0-9]*$ ]] || { echo "FORKS must be a positive integer" >&2; exit 2; }
HEAP=${HEAP:-4g}
PROF=${PROF:-}
LOG_COMPILATION=${LOG_COMPILATION:-0}
ORDER=${ORDER:-alternating}
OUT=${OUT:-${TMPDIR:-/tmp}/jit-ab-$NAME-$(date +%Y%m%d-%H%M%S)-$$}
CACHE=${CACHE:-${TMPDIR:-/tmp}/jit-ab-cache}
mkdir -p "$OUT" "$CACHE"
OUT=$(realpath "$OUT")
CACHE=$(realpath "$CACHE")
if [[ -n "$(find "$OUT" -mindepth 1 -maxdepth 1 -print -quit)" ]]; then
	echo "Output directory must be empty: $OUT" >&2
	exit 2
fi
BASE=$(git rev-parse "$BASE_REF^{commit}")
if [[ -n "${JAVA:-}" ]]; then
	JAVA=$(command -v "$JAVA" 2>/dev/null || printf '%s' "$JAVA")
	JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$JAVA")")")
else
	JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}
	JAVA="$JAVA_HOME/bin/java"
fi
[[ -x "$JAVA_HOME/bin/java" ]] || { echo "JAVA_HOME does not contain bin/java: $JAVA_HOME" >&2; exit 2; }
JAVA=$(readlink -f "$JAVA")
JAVA_ID=$(taskset -c "$CPUS" "$JAVA" -XshowSettings:properties -version 2>&1)
JAVA_MAJOR=$(sed -n 's/^[[:space:]]*java.specification.version = //p' <<<"$JAVA_ID" | head -n 1)
[[ "$JAVA_MAJOR" == 25 || "$JAVA_MAJOR" == 27 ]] || { echo "JDK 25 or 27 is required (found ${JAVA_MAJOR:-unknown} at $JAVA_HOME)" >&2; exit 2; }

SOURCE_HASH=$(find jmh/src/main/java jmh/src/main/resources -type f -print0 | sort -z | xargs -0 -r sha256sum | sha256sum | cut -d' ' -f1)
JDK_HASH=$(printf '%s\n%s\n' "$JAVA" "$JAVA_ID" | sha256sum | cut -d' ' -f1)
KEY="$BASE-$SOURCE_HASH-$JDK_HASH"
BASE_JAR="$CACHE/$KEY/benchmark.jar"
BASE_DIR="$CACHE/$KEY/worktree"
if [[ ! -f "$BASE_JAR" ]]; then
  mkdir -p "$CACHE/$KEY"
  if [[ ! -e "$BASE_DIR/.git" ]]; then git worktree add --detach "$BASE_DIR" "$BASE"; fi
  [[ "$(git -C "$BASE_DIR" rev-parse HEAD)" == "$BASE" ]] || { echo "Cached worktree does not match base commit: $BASE_DIR" >&2; exit 1; }
  mkdir -p "$BASE_DIR/jmh/src/main/java" "$BASE_DIR/jmh/src/main/resources"
  cp -a jmh/src/main/java/. "$BASE_DIR/jmh/src/main/java/"
  cp -a jmh/src/main/resources/. "$BASE_DIR/jmh/src/main/resources/"
  (cd "$BASE_DIR" && taskset -c "$CPUS" env JAVA_HOME="$JAVA_HOME" ./mvnw -B -ntp -Pdev -DskipTests -Dmaven.build.cache.enabled=false -Dmaven.jar.forceCreation=true clean package -pl jmh -am) >"$OUT/base-build.log" 2>&1 || { cat "$OUT/base-build.log" >&2; exit 1; }
  mkdir -p "$(dirname "$BASE_JAR")"
  cp "$BASE_DIR/jmh/target/benchmark.jar" "$BASE_JAR"
fi

export JAVA_HOME
# Stage all tracked changes and untracked workspace inputs in a temporary worktree so
# neither the shared checkout nor its target directories are written by Maven.
CANDIDATE="$OUT/candidate.jar"
BUILD="$OUT/candidate-build"
git worktree add --detach "$BUILD" HEAD
git diff --binary HEAD | (cd "$BUILD" && git apply --index --allow-empty)
while IFS= read -r -d '' file; do
  mkdir -p "$BUILD/$(dirname "$file")"
  cp "$ROOT/$file" "$BUILD/$file"
done < <(git ls-files --others --exclude-standard -z)
(cd "$BUILD" && taskset -c "$CPUS" env JAVA_HOME="$JAVA_HOME" ./mvnw -B -ntp -Pdev -DskipTests -Dmaven.build.cache.enabled=false -Dmaven.jar.forceCreation=true clean package -pl jmh -am) >"$OUT/candidate-build.log" 2>&1 || { cat "$OUT/candidate-build.log" >&2; exit 1; }
cp "$BUILD/jmh/target/benchmark.jar" "$CANDIDATE"

ARGS=("$REGEX" -wi 3 -wf 0 -w 3s -i 5 -r 3s -f 1 -foe true "$@")
[[ -n "$PROF" ]] && ARGS+=( -prof "$PROF" )
FORK_JVM_ARGS="-Xms$HEAP -Xmx$HEAP -XX:+AlwaysPreTouch"
[[ -n "${JVM_ARGS:-}" ]] && FORK_JVM_ARGS+=" $JVM_ARGS"
run_one() {
  local side=$1 fork=$2 jar=$3
  local json="$OUT/$side-$fork.json" log="$OUT/$side-$fork.log"
  local fork_args=$FORK_JVM_ARGS
  local java_env=()
  [[ "$LOG_COMPILATION" == 1 ]] && fork_args+=" -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=$OUT/$side-$fork-%p-compilation.xml"
  if [[ "$PROF" == perfasm* ]]; then
    local hsdis_dir=${HSDIS_DIR:-${HOME:-}/.local/share/hsdis}
    [[ -d "$hsdis_dir" ]] || { echo "hsdis directory not found: $hsdis_dir (set HSDIS_DIR)" >&2; return 2; }
    java_env=(env "LD_LIBRARY_PATH=$hsdis_dir${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}")
  fi
  (cd "$OUT" && taskset -c "$CPUS" "${java_env[@]}" "$JAVA" -Djmh.ignoreLock=true -jar "$jar" "${ARGS[@]}" -jvmArgsAppend "$fork_args" -rf json -rff "$json") >"$log" 2>&1
}
{
  printf 'name=%s\nbase_ref=%s\nbase_commit=%s\n' "$NAME" "$BASE_REF" "$BASE"
  printf 'candidate_commit=%s\n' "$(git rev-parse HEAD)"
  printf 'working_tree_hash=%s\n' "$( { git diff --binary HEAD; git ls-files --others --exclude-standard -z | xargs -0 -r sha256sum; } | sha256sum | cut -d' ' -f1)"
  printf 'jdk_major=%s\njava=%s\njdk_identity_hash=%s\ncpus=%s\nforks_per_side=%s\nheap=%s\n' "$JAVA_MAJOR" "$JAVA" "$JDK_HASH" "$CPUS" "$FORKS" "$HEAP"
  printf 'order=%s\nprofiler=%s\nlog_compilation=%s\n' "$ORDER" "${PROF:-none}" "$LOG_COMPILATION"
  printf 'jmh_args='; printf '%q ' "${ARGS[@]}"; printf '\n'
  printf 'fork_jvm_args=%s\n' "$FORK_JVM_ARGS"
  printf 'started_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >"$OUT/run-metadata.txt"
for ((i=1; i<=FORKS; i++)); do
  case "$ORDER" in
    alternating) if (( i % 2 )); then run_one base "$i" "$BASE_JAR"; run_one candidate "$i" "$CANDIDATE"; else run_one candidate "$i" "$CANDIDATE"; run_one base "$i" "$BASE_JAR"; fi ;;
    BC) run_one base "$i" "$BASE_JAR"; run_one candidate "$i" "$CANDIDATE" ;;
    CB) run_one candidate "$i" "$CANDIDATE"; run_one base "$i" "$BASE_JAR" ;;
    *) echo "ORDER must be alternating, BC, or CB" >&2; exit 2 ;;
  esac
done
python3 "$ROOT/jmh/jit/compare.py" --base "$OUT"/base-*.json --candidate "$OUT"/candidate-*.json --output "$OUT/comparison.md" --merged-base "$OUT/base-merged.json" --merged-candidate "$OUT/candidate-merged.json" --java "$JAVA" --classpath "$BASE_JAR" --cpus "$CPUS"
printf 'Artifacts: %s\n' "$OUT"

}

main "$@"
