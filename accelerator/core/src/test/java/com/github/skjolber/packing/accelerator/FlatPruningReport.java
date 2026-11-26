package com.github.skjolber.packing.accelerator;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.github.skjolber.packing.accelerator.FlatTestInstances.Instance;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * The experiment of {@link FlatPruning}: the nodes and the time of the tree search of one thread with and without the cache of searched states and
 * the bound by the union of the free points, on random instances, and whether the value of the winner is the same. Run with
 * <code>-Dflat.pruning=true</code> (and <code>-Dflat.pruning.seeds</code>, <code>-Dflat.pruning.min</code>, <code>-Dflat.pruning.max</code>).
 */
@EnabledIfSystemProperty(named = "flat.pruning", matches = "true")
class FlatPruningReport {

	private static final String[] VARIANTS = { "baseline", "cache", "union", "cache+union" };

	private static FlatProblem problem(Instance instance, boolean anyBest, boolean allPoints) {
		Container container = instance.container;
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), iterator.length(),
				anyBest, allPoints);
	}

	@Test
	void report() throws Exception {
		int seeds = Integer.getInteger("flat.pruning.seeds", 40);
		int min = Integer.getInteger("flat.pruning.min", 9);
		int max = Integer.getInteger("flat.pruning.max", 12);
		int passes = 3;
		// mode: tree, any best, all points, free space
		Object[][] modes = { { "loop", false, false, false, null }, { "tree exact", true, false, false, null }, { "tree exact, no free space bound", true, false, false, Boolean.FALSE },
				{ "tree any best", true, true, false, null }, { "tree all points exact", true, false, true, null }, { "tree all points any best", true, true, true, null } };
		for (Object[] mode : modes) {
			boolean tree = (Boolean)mode[1];
			boolean anyBest = (Boolean)mode[2];
			boolean allPoints = (Boolean)mode[3];
			FlatEngine.Builder builder = FlatEngine.newBuilder().withThreads(1).withMinBoxes(1).withTree(tree);
			if(mode[4] != null) {
				builder.withFreeSpace((Boolean)mode[4]);
			}
			// all points: fewer boxes, as the tree is much larger
			int less = allPoints ? 4 : 0;
			FlatProblem[] problems = new FlatProblem[seeds];
			for (int i = 0; i < seeds; i++) {
				problems[i] = problem(FlatTestInstances.random(1000 + i, min - less, max - less), anyBest, allPoints);
			}
			long[] baselineNodes = new long[seeds];
			long[][] baselineValue = new long[seeds][];
			int[][] baselineOrder = new int[seeds][];
			long baselineTotal = 0L;
			double baselineTime = 0;
			try (FlatEngine engine = builder.build()) {
				for (int variant = 0; variant < (tree ? VARIANTS.length : 1); variant++) {
					long nodes = 0L;
					int valueDifferences = 0;
					int orderDifferences = 0;
					int gaveUp = 0;
					long lookups = 0L;
					long hits = 0L;
					long checks = 0L;
					long cuts = 0L;
					double[] times = new double[passes];
					// a pass to warm up, then the timed passes
					for (int pass = -1; pass < passes; pass++) {
						long start = System.nanoTime();
						nodes = 0L;
						for (int i = 0; i < seeds; i++) {
							FlatPruning.clear();
							FlatPruning.cache = variant == 1 || variant == 3;
							FlatPruning.union = variant == 2 || variant == 3;
							Optional<AcceleratorWinner> winner = engine.search(problems[i], 0L, () -> false);
							FlatPruning.cache = false;
							FlatPruning.union = false;
							if(pass != passes - 1) {
								if(winner.isPresent()) {
									nodes += winner.get().getNodes();
								}
								continue;
							}
							lookups += FlatPruning.lookups;
							hits += FlatPruning.hits;
							checks += FlatPruning.unionChecks;
							cuts += FlatPruning.unionCuts;
							if(winner.isEmpty()) {
								gaveUp++;
								if(variant == 0) {
									baselineValue[i] = null;
								} else if(baselineValue[i] != null) {
									valueDifferences++;
								}
								continue;
							}
							AcceleratorWinner w = winner.get();
							nodes += w.getNodes();
							long[] value = { w.getCount(), w.getLoadVolume(), w.getLoadWeight(), w.isFullyPacked() ? 1 : 0 };
							if(variant == 0) {
								baselineNodes[i] = w.getNodes();
								baselineValue[i] = value;
								baselineOrder[i] = w.getPermutation();
							} else {
								if(!Arrays.equals(value, baselineValue[i])) {
									valueDifferences++;
									System.out.println("  VALUE DIFFERS " + mode[0] + " " + VARIANTS[variant] + " seed " + (1000 + i) + ": " + Arrays.toString(baselineValue[i]) + " and "
											+ Arrays.toString(value));
								} else if(!Arrays.equals(Arrays.copyOf(w.getPermutation(), w.getCount()), Arrays.copyOf(baselineOrder[i], w.getCount()))) {
									orderDifferences++;
								}
							}
						}
						if(pass >= 0) {
							times[pass] = (System.nanoTime() - start) / 1e6;
						}
					}
					Arrays.sort(times);
					double time = times[passes / 2];
					if(variant == 0) {
						baselineTotal = nodes;
						baselineTime = time;
					}
					System.out.println(String.format(
							"%-32s %-12s nodes %,14d (%.3f) time %9.1f ms (%.3f) value differs %d, same value other order %d, gave up %d, cache hits %,d of %,d, union cuts %,d of %,d",
							mode[0], VARIANTS[variant], nodes, baselineTotal == 0 ? 1.0 : (double)nodes / baselineTotal, time, time / baselineTime, valueDifferences, orderDifferences, gaveUp,
							hits, lookups, cuts, checks));
				}
			}
		}
	}
}
