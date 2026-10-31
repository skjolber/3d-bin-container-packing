package com.github.skjolber.packing.accelerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * Prints figures for judging whether a device is worth using for the flat search: free points needed, nodes per permutation and their tail, and the
 * single-thread speed relative to the object search. Does not assert. Run with <code>-Dflat.stats=true</code>.
 */
@EnabledIfSystemProperty(named = "flat.stats", matches = "true")
class FlatStatsReport {

	private static final int P_MAX = 512;

	private static final int WARMUP_MILLIS = 1500;
	private static final int MEASURE_MILLIS = 3000;
	private static final int MAX_SEARCHES = 3_000_000;

	private interface Searcher {

		int search(int maxPackableCount);

		long loadVolume();
	}

	private static final class Input {

		final String name;
		final List<BoxItem> boxItems;
		final Container container;

		Input(String name, List<BoxItem> boxItems, Container container) {
			this.name = name;
			this.boxItems = boxItems;
			this.container = container;
		}
	}

	private static final class Run {

		int permutations;
		int searches;
		int skipped;
		int fullyPlaced;
		int overflows;
		int maxPoints;
		long nanos;
		int[] nodes = new int[MAX_SEARCHES];
		int[] points = new int[MAX_SEARCHES];
		long[] times = new long[MAX_SEARCHES];
	}

	@Test
	void report() {
		List<Input> inputs = new ArrayList<>();
		for (int side : new int[] { 54, 58, 64, 89 }) {
			inputs.add(egy(side));
		}
		inputs.add(random(12, 1.3, 1));
		inputs.add(random(12, 1.6, 2));
		inputs.add(random(14, 1.4, 3));

		StringBuilder summary = new StringBuilder();
		for (Input input : inputs) {
			summary.append(report(input));
		}
		System.out.println("=== G0 summary ===");
		System.out.print(summary);
	}

	private static Input egy(int side) {
		List<BoxItem> items = new ArrayList<>();
		int[][] dims = { { 31, 14, 19, 2 }, { 21, 15, 23, 1 }, { 22, 20, 30, 2 }, { 27, 26, 24, 4 }, { 39, 25, 22, 1 } };
		for (int i = 0; i < dims.length; i++) {
			items.add(new BoxItem(Box.newBuilder().withId("e" + i).withSize(dims[i][0], dims[i][1], dims[i][2]).withRotate3D().withWeight(1).build(), dims[i][3]));
		}
		return new Input("egy-" + side + " (10 boxes)", items, container(side, side, side));
	}

	private static Input random(int boxes, double volumeFactor, int seed) {
		Random random = new Random(seed);
		List<BoxItem> items = new ArrayList<>();
		long volume = 0;
		for (int i = 0; i < boxes; i++) {
			int dx = 15 + random.nextInt(46);
			int dy = 15 + random.nextInt(46);
			int dz = 15 + random.nextInt(46);
			volume += (long)dx * dy * dz;
			items.add(new BoxItem(Box.newBuilder().withId("r" + i).withSize(dx, dy, dz).withRotate3D().withWeight(1).build(), 1));
		}
		int side = (int)Math.ceil(Math.cbrt(volume * volumeFactor));
		return new Input("random-" + boxes + " x" + volumeFactor + " seed " + seed, items, container(side, side, side));
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withId("c").withEmptyWeight(1).withSize(dx, dy, dz).withMaxLoadWeight(1_000_000_000).build();
	}

	private String report(Input input) {
		DefaultBoxItemPermutationRotationIterator probe = iterator(input);
		int n = probe.length();
		System.out.println("--- " + input.name + ": container " + input.container.getLoadDx() + "x" + input.container.getLoadDy() + "x" + input.container.getLoadDz() + ", " + n
				+ " boxes");

		FlatProblem problem = FlatProblem.of(probe, input.container.getLoadDx(), input.container.getLoadDy(), input.container.getLoadDz());
		FlatLayout layout = new FlatLayout(n, P_MAX);
		int[] s = layout.newScratch();
		long[] sl = new long[layout.longSize];

		Run run = new Run();
		for (int phase = 0; phase < 2; phase++) {
			run = new Run();
			DefaultBoxItemPermutationRotationIterator iterator = iterator(input);
			Searcher flat = new Searcher() {
				@Override
				public int search(int maxPackableCount) {
					int[] permutation = iterator.getPermutations();
					for (int i = 0; i < n; i++) {
						s[layout.perm + i] = permutation[i];
					}
					s[layout.control + FlatLayout.C_NODES] = 0;
					s[layout.control + FlatLayout.C_MAX_POINTS] = 0;
					return FlatSearch.searchPermutation(layout, problem.ints(), problem.longs(), s, sl, 0, 0, maxPackableCount);
				}

				@Override
				public long loadVolume() {
					return sl[layout.bestVolume];
				}
			};
			loop(iterator, input.container, flat, run, phase == 0 ? WARMUP_MILLIS : MEASURE_MILLIS, layout, s, true);
		}

		Run objectRun = new Run();
		FlatSearchOracle oracle = new FlatSearchOracle();
		for (int phase = 0; phase < 2; phase++) {
			objectRun = new Run();
			DefaultBoxItemPermutationRotationIterator iterator = iterator(input);
			long[] volume = new long[1];
			Searcher object = new Searcher() {
				@Override
				public int search(int maxPackableCount) {
					FlatSearchOracle.Search search = oracle.search(iterator, input.container, maxPackableCount);
					volume[0] = search.loadVolume;
					return search.count;
				}

				@Override
				public long loadVolume() {
					return volume[0];
				}
			};
			// the same number of searches as the flat run
			loop(iterator, input.container, object, objectRun, phase == 0 ? WARMUP_MILLIS : Integer.MAX_VALUE, null, null, false, run.searches);
		}

		return summarize(input, n, run, objectRun);
	}

	private void loop(DefaultBoxItemPermutationRotationIterator iterator, Container container, Searcher searcher, Run run, int budgetMillis, FlatLayout layout, int[] s,
			boolean record) {
		loop(iterator, container, searcher, run, budgetMillis, layout, s, record, MAX_SEARCHES);
	}

	/** Mimics the permutation loop of the packager: bound skip, first fully placed permutation, skipping by the placed count and now and then a jump. */
	private void loop(DefaultBoxItemPermutationRotationIterator iterator, Container container, Searcher searcher, Run run, int budgetMillis, FlatLayout layout, int[] s,
			boolean record, int maxSearches) {
		Random random = new Random(42);
		int n = iterator.length();
		long bestVolume = 0;
		long start = System.nanoTime();
		long deadline = start + budgetMillis * 1_000_000L;
		long totalNanos = 0;
		while (true) {
			run.permutations++;
			int mpc = FlatSearchOracle.maxPackableCount(iterator, container);
			boolean allItemsFit = mpc == n;
			long first = 0;
			for (int i = 0; i < mpc; i++) {
				first += iterator.getStackValue(i).getBox().getVolume();
			}
			int size;
			if(!allItemsFit && first < bestVolume) {
				size = mpc;
				run.skipped++;
			} else {
				long t0 = System.nanoTime();
				size = searcher.search(mpc);
				long t1 = System.nanoTime();
				totalNanos += t1 - t0;
				if(size < 0) {
					run.overflows++;
					size = 0;
				} else {
					if(size == mpc) {
						run.fullyPlaced++;
					}
					bestVolume = Math.max(bestVolume, searcher.loadVolume());
				}
				if(record && run.searches < MAX_SEARCHES) {
					run.nodes[run.searches] = s[layout.control + FlatLayout.C_NODES];
					run.points[run.searches] = s[layout.control + FlatLayout.C_MAX_POINTS];
					run.times[run.searches] = t1 - t0;
					run.maxPoints = Math.max(run.maxPoints, run.points[run.searches]);
				}
				run.searches++;
			}
			if(run.searches >= maxSearches || run.searches >= MAX_SEARCHES) {
				break;
			}
			if((run.permutations & 63) == 0 && System.nanoTime() > deadline) {
				break;
			}
			int next;
			if(size >= n) {
				next = iterator.nextPermutation();
			} else if(random.nextInt(64) == 0) {
				next = iterator.nextPermutation(random.nextInt(Math.min(4, n)));
			} else {
				next = iterator.nextPermutation(size);
			}
			if(next == -1) {
				break;
			}
		}
		run.nanos = totalNanos;
	}

	private static DefaultBoxItemPermutationRotationIterator iterator(Input input) {
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(input.container.getLoadDx(), input.container.getLoadDy(), input.container.getLoadDz())
				.withBoxItems(input.boxItems)
				.withMaxLoadWeight(input.container.getMaxLoadWeight())
				.build();
	}

	private static int percentile(int[] values, int count, double p) {
		if(count == 0) {
			return 0;
		}
		int[] copy = Arrays.copyOf(values, count);
		Arrays.sort(copy);
		return copy[(int)(p * (count - 1))];
	}

	private static long percentile(long[] values, int count, double p) {
		if(count == 0) {
			return 0;
		}
		long[] copy = Arrays.copyOf(values, count);
		Arrays.sort(copy);
		return copy[(int)(p * (count - 1))];
	}

	private String summarize(Input input, int n, Run flat, Run object) {
		int c = flat.searches;
		long nodeSum = 0;
		for (int i = 0; i < c; i++) {
			nodeSum += flat.nodes[i];
		}
		long timeSum = 0;
		for (int i = 0; i < c; i++) {
			timeSum += flat.times[i];
		}
		// share of the search time spent in the slowest 1 % of the searches
		long[] sortedTimes = Arrays.copyOf(flat.times, c);
		Arrays.sort(sortedTimes);
		long tail = 0;
		for (int i = c - Math.max(1, c / 100); i < c; i++) {
			tail += sortedTimes[i];
		}

		double flatRate = c / (flat.nanos / 1e9);
		double objectRate = object.searches / (object.nanos / 1e9);

		StringBuilder b = new StringBuilder();
		b.append(String.format("%-34s n=%2d searches=%8d skipped=%8d full=%6d overflow=%d%n", input.name, n, c, flat.skipped, flat.fullyPlaced, flat.overflows));
		b.append(String.format("    nodes/search avg %.1f p50 %d p99 %d p99.9 %d max %d | max free points %d (p99 %d)%n", nodeSum / (double)Math.max(1, c),
				percentile(flat.nodes, c, 0.5), percentile(flat.nodes, c, 0.99), percentile(flat.nodes, c, 0.999), percentile(flat.nodes, c, 1.0), flat.maxPoints,
				percentile(flat.points, c, 0.99)));
		b.append(String.format("    time/search p50 %d ns p99 %d ns max %d ns | slowest 1%% of searches = %.1f%% of the time%n", percentile(flat.times, c, 0.5),
				percentile(flat.times, c, 0.99), percentile(flat.times, c, 1.0), 100.0 * tail / Math.max(1, timeSum)));
		b.append(String.format("    single thread: flat %.0f searches/s, object %.0f searches/s, ratio %.2f, %.0f ns/node%n", flatRate, objectRate, flatRate / objectRate,
				timeSum / (double)Math.max(1, nodeSum)));
		System.out.print(b);
		return b.toString();
	}
}
