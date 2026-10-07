package com.github.skjolber.packing.jmh;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GroupBruteForceBenchmarkTest {

	@Test
	public void sixGroups() {
		packagers(6);
	}

	@Test
	public void eightGroups() {
		packagers(8);
	}

	private void packagers(int groups) {
		GroupBruteForceBenchmarkState state = new GroupBruteForceBenchmarkState(groups);
		state.init();
		try {
			long start = System.nanoTime();
			int bruteForce = GroupBruteForceBenchmark.pack(state.getBruteForcePackager(), state);
			long middle = System.nanoTime();
			int parallel = GroupBruteForceBenchmark.pack(state.getParallelBruteForcePackager(), state);
			long end = System.nanoTime();
			System.out.println(groups + " groups: brute force " + bruteForce + " containers in " + (middle - start) / 1_000_000 + " ms, parallel " + parallel + " containers in " + (end - middle) / 1_000_000 + " ms");
			assertTrue(bruteForce > 1);
			assertTrue(parallel == bruteForce);
		} finally {
			state.shutdown();
		}
	}
}
