package com.github.skjolber.packing.packer.bruteforce.flat;

import java.util.Optional;

/**
 * Run by hand: the nodes and the time of the permutation loop against the joint tree, both as {@linkplain FlatEngine} with the given threads, on the tight
 * random problems of the parity tests. Arguments: first seed, number of seeds, fewest boxes, most boxes, repeats, threads.
 */
public class FlatJointStats {

	public static void main(String[] args) throws Exception {
		long firstSeed = args.length > 0 ? Long.parseLong(args[0]) : 109;
		int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 1;
		int minBoxes = args.length > 2 ? Integer.parseInt(args[2]) : 11;
		int maxBoxes = args.length > 3 ? Integer.parseInt(args[3]) : 12;
		int repeats = args.length > 4 ? Integer.parseInt(args[4]) : 2;
		int threads = args.length > 5 ? Integer.parseInt(args[5]) : 1;

		FlatEngine engine = FlatEngine.newBuilder().withThreads(threads).withMinBoxes(1).build();
		FlatEngine tree = FlatEngine.newBuilder().withThreads(threads).withMinBoxes(1).withJointTree(true).build();
		try {
			for (int i = 0; i < seeds; i++) {
				long seed = firstSeed + i;
				FlatProblem problem = FlatJointSearchTest.tight(seed, minBoxes, maxBoxes);
				long loopBest = Long.MAX_VALUE;
				Optional<AcceleratorWinner> expected = Optional.empty();
				for (int r = 0; r < repeats; r++) {
					long start = System.nanoTime();
					expected = engine.search(problem, 0L, () -> false);
					loopBest = Math.min(loopBest, System.nanoTime() - start);
				}
				long treeBest = Long.MAX_VALUE;
				AcceleratorWinner actual = null;
				for (int r = 0; r < repeats; r++) {
					long start = System.nanoTime();
					actual = tree.search(problem, 0L, () -> false).orElse(null);
					treeBest = Math.min(treeBest, System.nanoTime() - start);
				}
				String same;
				if(actual == null) {
					same = "tree gave up";
				} else {
					AcceleratorWinner e = expected.get();
					boolean equal = actual.getCount() == e.getCount() && (e.getCount() == 0 || (actual.getLoadVolume() == e.getLoadVolume() && actual.getLoadWeight() == e.getLoadWeight()
							&& java.util.Arrays.equals(actual.getPermutation(), e.getPermutation())
							&& java.util.Arrays.equals(java.util.Arrays.copyOf(actual.getRotations(), e.getCount()), java.util.Arrays.copyOf(e.getRotations(), e.getCount()))));
					same = equal ? "same" : "MISMATCH: loop " + FlatJointSearchTest.describe(e) + ", tree " + FlatJointSearchTest.describe(actual);
				}
				System.out.println("seed " + seed + " n=" + problem.n() + ": loop " + expected.get().getNodes() + " nodes " + loopBest / 1_000_000 + " ms, tree "
						+ (actual == null ? "-" : actual.getNodes()) + " nodes " + treeBest / 1_000_000 + " ms, " + same);
			}
		} finally {
			engine.close();
			tree.close();
		}
	}
}
