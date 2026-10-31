#!/usr/bin/env python3
"""Merge JMH iteration samples and report JMH 99.9% and fork-level 95% intervals."""
import argparse
import json
import pathlib
import subprocess


def flatten(value):
	if isinstance(value, list):
		for child in value:
			yield from flatten(child)
	elif isinstance(value, (int, float)):
		yield float(value)


def read_results(paths):
	groups = {}
	for path in paths:
		for result in json.loads(path.read_text()):
			metric = result["primaryMetric"]
			key = (result["benchmark"], json.dumps(result.get("params", {}), sort_keys=True), metric.get("scoreUnit", ""))
			if not metric.get("rawData"):
				raise ValueError(f"{path}: {result['benchmark']} has no rawData; rerun with JMH JSON output")
			# the outer dimension of rawData is the fork: one entry per fork, from a file of one fork or of several
			for fork in metric["rawData"]:
				values = list(flatten(fork))
				if not values:
					raise ValueError(f"{path}: no iteration samples for {result['benchmark']}")
				groups.setdefault(key, []).append((result, values))
	return groups


def stats(args, confidence, values):
	output = subprocess.check_output(["taskset", "-c", args.cpus, args.java, "--class-path", args.classpath,
		str(pathlib.Path(__file__).with_name("Statistics.java")), str(confidence), *map(str, values)], text=True)
	return json.loads(output)


def merged_result(entries, args):
	first = entries[0][0]
	all_samples = [value for _, samples in entries for value in samples]
	merged = stats(args, 0.999, all_samples)
	metric = dict(first["primaryMetric"])
	metric.update(score=merged["mean"], scoreError=merged["error"], scoreConfidence=merged["ci"],
		scorePercentiles={"0.0": merged["min"], "50.0": merged["p50"], "90.0": merged["p90"], "99.0": merged["p99"], "100.0": merged["max"]},
		rawData=[samples for _, samples in entries])
	result = dict(first)
	result["primaryMetric"] = metric
	result["forks"] = len(entries)
	result.pop("secondaryMetrics", None)
	return result, merged


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("--base", nargs="+", type=pathlib.Path, required=True)
	parser.add_argument("--candidate", nargs="+", type=pathlib.Path, required=True)
	parser.add_argument("--output", type=pathlib.Path, required=True)
	parser.add_argument("--merged-base", type=pathlib.Path, required=True)
	parser.add_argument("--merged-candidate", type=pathlib.Path, required=True)
	parser.add_argument("--java", required=True)
	parser.add_argument("--classpath", required=True)
	parser.add_argument("--cpus", required=True)
	args = parser.parse_args()
	base, candidate = read_results(args.base), read_results(args.candidate)
	if base.keys() != candidate.keys():
		raise ValueError("base and candidate benchmark/parameter/score-unit sets differ")
	base_json, candidate_json, rows, fork_lines = [], [], [], []
	header = [
		"# JMH comparison", "", "Merged iteration samples use JMH `ListStatistics` and its 99.9% confidence interval. Lower is better for time/op; higher is better for throughput. The separate fork interval treats each fork mean as one independent observation and uses a 95% interval.", "",
		"| Benchmark / parameters | Base merged (99.9% CI) | Candidate merged (99.9% CI) | Change | CI overlap | Fork means (base / candidate, 95% CI) |",
		"|---|---:|---:|---:|:---:|---:|"]
	for key in sorted(base):
		br, bs = merged_result(base[key], args)
		cr, cs = merged_result(candidate[key], args)
		base_json.append(br); candidate_json.append(cr)
		base_fork = [sum(samples) / len(samples) for _, samples in base[key]]
		candidate_fork = [sum(samples) / len(samples) for _, samples in candidate[key]]
		bf, cf = stats(args, 0.95, base_fork), stats(args, 0.95, candidate_fork)
		lower = key[2] in ("ns/op", "us/op", "ms/op", "s/op")
		change = (bs["mean"] / cs["mean"] - 1) * 100 if lower else (cs["mean"] / bs["mean"] - 1) * 100
		overlap = not (bs["ci"][1] < cs["ci"][0] or cs["ci"][1] < bs["ci"][0])
		params = json.loads(key[1])
		name = key[0] + (" " + ", ".join(f"{k}={v}" for k, v in sorted(params.items())) if params else "")
		fmt = lambda x: f"{x['mean']:.6g} [{x['ci'][0]:.6g}, {x['ci'][1]:.6g}] {key[2]}"
		rows.append(f"| `{name}` | {fmt(bs)} | {fmt(cs)} | {change:+.2f}% | {'overlap' if overlap else '**no overlap**'} | {bf['mean']:.6g} [{bf['ci'][0]:.6g}, {bf['ci'][1]:.6g}] / {cf['mean']:.6g} [{cf['ci'][0]:.6g}, {cf['ci'][1]:.6g}] |")
		fork_lines.append(f"- `{name}`: base {', '.join(f'{v:.6g}' for v in base_fork)}; candidate {', '.join(f'{v:.6g}' for v in candidate_fork)} {key[2]}.")
	args.merged_base.write_text(json.dumps(base_json, indent=2) + "\n")
	args.merged_candidate.write_text(json.dumps(candidate_json, indent=2) + "\n")
	args.output.write_text("\n".join(header + rows + ["", "## Per-fork iteration means", ""] + fork_lines) + "\n")


if __name__ == "__main__":
	main()
