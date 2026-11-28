package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.accelerator.test.FlatTestInstances.Instance;

/**
 * The tree search of an item ({@link FlatTree}) must find the winner of the loop over the orders ({@link FlatSubtree}) for the loop's winner, and an
 * arrangement of the same value, confirmed by the packager's search of its order, when any best arrangement will do.
 */
public abstract class AbstractFlatTreeTest {

	/** @return the engine under test, with the options of a {@linkplain FlatEngine} */
	protected abstract BruteForceAccelerator engine(FlatEngineOptions options);


	private final FlatSearchOracle oracle = new FlatSearchOracle();

	private static DefaultBoxItemPermutationRotationIterator iterator(Instance instance) {
		Container container = instance.container;
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
	}

	private static FlatProblem problem(BoxItemPermutationRotationIterator iterator, Container container, int limit, boolean anyBest) {
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), limit, anyBest);
	}

	private BruteForceAccelerator engine(int threads, boolean tree, int nodeBudget) {
		return engine(new FlatEngineOptions().withThreads(threads).withMinBoxes(1).withTree(tree).withNodeBudget(nodeBudget));
	}

	private static String describe(AcceleratorWinner winner) {
		return Arrays.toString(winner.getPermutation()) + " " + Arrays.toString(Arrays.copyOf(winner.getRotations(), winner.getCount())) + " " + winner.getCount() + " "
				+ winner.getLoadVolume() + " " + winner.getLoadWeight() + " " + winner.isFullyPacked();
	}

	private static String value(AcceleratorWinner winner) {
		return winner.getCount() + " " + winner.getLoadVolume() + " " + winner.getLoadWeight();
	}

	@Test
	void sameWinnerAsTheLoopOverTheOrders() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator tree = engine(1, true, 20_000);
		BruteForceAccelerator threads = engine(3, true, 500);
		try {
			for (int seed = 0; seed < 150; seed++) {
				Instance instance = FlatTestInstances.random(seed, 6, 10);
				BoxItemPermutationRotationIterator iterator = iterator(instance);
				FlatProblem problem = problem(iterator, instance.container, iterator.length(), false);
				Optional<AcceleratorWinner> expected = loop.search(problem, 0L, () -> false);
				assertThat(expected).isPresent();
				assertThat(describe(tree.search(problem, 0L, () -> false).get())).as("seed " + seed).isEqualTo(describe(expected.get()));
				assertThat(describe(threads.search(problem, 0L, () -> false).get())).as("seed " + seed + " on threads").isEqualTo(describe(expected.get()));
			}
		} finally {
			loop.close();
			tree.close();
			threads.close();
		}
	}

	/** The later containers of a packing keep every type of the packing, with no box left of some: a type without boxes, and more types than boxes. */
	@Test
	void sameWinnerWithTypesWithoutBoxes() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator tree = engine(2, true, 100);
		try {
			for (int seed = 0; seed < 100; seed++) {
				Instance instance = FlatTestInstances.random(seed, 4, 8);
				DefaultBoxItemPermutationRotationIterator iterator = iterator(instance);
				// the boxes of the first type are placed: the type stays, without boxes
				int[] permutations = iterator.getPermutations();
				int placed = 1;
				while (placed < permutations.length && permutations[placed] == permutations[0]) {
					placed++;
				}
				if(placed == permutations.length) {
					continue;
				}
				iterator.removePermutations(placed);
				FlatProblem problem = problem(iterator, instance.container, iterator.length(), false);
				assertThat(problem.ints()[FlatProblem.HEADER + permutations[0]]).isZero();
				Optional<AcceleratorWinner> expected = loop.search(problem, 0L, () -> false);
				assertThat(describe(tree.search(problem, 0L, () -> false).get())).as("seed " + seed).isEqualTo(describe(expected.get()));
				assertThat(value(tree.search(problem(iterator, instance.container, iterator.length(), true), 0L, () -> false).get())).as("seed " + seed).isEqualTo(value(expected.get()));
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void sameWinnerWithABoundAndALimit() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator tree = engine(1, true, 1);
		try {
			for (int seed = 0; seed < 100; seed++) {
				Instance instance = FlatTestInstances.random(seed, 6, 9);
				BoxItemPermutationRotationIterator iterator = iterator(instance);
				int limit = Math.max(1, iterator.length() - 2);
				FlatProblem problem = problem(iterator, instance.container, limit, false);
				AcceleratorWinner unbounded = loop.search(problem, 0L, () -> false).get();
				long bound = unbounded.getLoadVolume() / 2;
				Optional<AcceleratorWinner> expected = loop.search(problem, bound, () -> false);
				assertThat(describe(tree.search(problem, bound, () -> false).get())).as("seed " + seed).isEqualTo(describe(expected.get()));
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void anyBestHasTheSameValueAndAnArrangementThePackagerConfirms() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator tree = engine(2, true, 300);
		try {
			for (int seed = 0; seed < 150; seed++) {
				Instance instance = FlatTestInstances.random(seed, 6, 10);
				DefaultBoxItemPermutationRotationIterator iterator = iterator(instance);
				AcceleratorWinner expected = loop.search(problem(iterator, instance.container, iterator.length(), false), 0L, () -> false).get();
				AcceleratorWinner winner = tree.search(problem(iterator, instance.container, iterator.length(), true), 0L, () -> false).get();
				assertThat(value(winner)).as("seed " + seed).isEqualTo(value(expected));
				if(winner.getCount() > 0) {
					// the packager places the winner's order itself and confirms the count, the volume and the rotations
					iterator.setPermutation(winner.getPermutation());
					int maxPackableCount = Math.min(iterator.length(), FlatSearchOracle.maxPackableCount(iterator, instance.container));
					FlatSearchOracle.Search search = oracle.search(iterator, instance.container, maxPackableCount);
					assertThat(search.count).as("seed " + seed).isEqualTo(winner.getCount());
					assertThat(search.loadVolume).as("seed " + seed).isEqualTo(winner.getLoadVolume());
					assertThat(search.rotations).as("seed " + seed).isEqualTo(Arrays.copyOf(winner.getRotations(), winner.getCount()));
				}
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	/**
	 * A search which does not search a state twice ({@link FlatStateCache}) finds the value of the loop's winner when any best arrangement will do, also
	 * with a table so small that it forgets, and on threads; and the packager's search of the winner's order places as many boxes with the same volume.
	 */
	@Test
	void stateCacheKeepsTheValueWhenAnyBestWillDo() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator plain = engine(new FlatEngineOptions().withThreads(1).withMinBoxes(1).withTree(true).withStateCache(0));
		// the entries of the table, the threads and the node budget
		int[][] variants = { { 16, 1, 20_000 }, { 4096, 1, 20_000 }, { 4096, 1, 1 }, { 16, 2, 300 }, { 4096, 3, 300 } };
		BruteForceAccelerator[] cached = new BruteForceAccelerator[variants.length];
		for (int i = 0; i < variants.length; i++) {
			cached[i] = engine(new FlatEngineOptions().withThreads(variants[i][1]).withMinBoxes(1).withTree(true).withNodeBudget(variants[i][2]).withStateCache(variants[i][0]));
		}
		long plainNodes = 0L;
		long[] cachedNodes = new long[variants.length];
		try {
			for (int seed = 0; seed < 150; seed++) {
				Instance instance = FlatTestInstances.random(seed, 6, 10);
				DefaultBoxItemPermutationRotationIterator iterator = iterator(instance);
				AcceleratorWinner expected = loop.search(problem(iterator, instance.container, iterator.length(), false), 0L, () -> false).get();
				FlatProblem problem = problem(iterator, instance.container, iterator.length(), true);
				plainNodes += plain.search(problem, 0L, () -> false).get().getNodes();
				for (int i = 0; i < variants.length; i++) {
					String message = "seed " + seed + ", " + Arrays.toString(variants[i]);
					AcceleratorWinner winner = cached[i].search(problem, 0L, () -> false).get();
					cachedNodes[i] += winner.getNodes();
					assertThat(value(winner)).as(message).isEqualTo(value(expected));
					if(winner.getCount() > 0) {
						iterator.setPermutation(winner.getPermutation());
						int maxPackableCount = Math.min(iterator.length(), FlatSearchOracle.maxPackableCount(iterator, instance.container));
						FlatSearchOracle.Search search = oracle.search(iterator, instance.container, maxPackableCount);
						assertThat(search.count).as(message).isEqualTo(winner.getCount());
						assertThat(search.loadVolume).as(message).isEqualTo(winner.getLoadVolume());
						iterator.reset();
					}
				}
			}
			// on one thread the cache is all which differs: fewer nodes, and fewer still with a table which does not forget
			assertThat(cachedNodes[1]).isLessThan(plainNodes * 9 / 10);
			assertThat(cachedNodes[1]).isLessThan(cachedNodes[0]);
			assertThat(cachedNodes[0]).isLessThan(plainNodes);
			// and the turns of the search do not change what it searches
			assertThat(cachedNodes[2]).isEqualTo(cachedNodes[1]);
		} finally {
			loop.close();
			plain.close();
			for (BruteForceAccelerator engine : cached) {
				engine.close();
			}
		}
	}

	/** The cache keeps the value of the winner, not the arrangement, so it is not used for the loop's winner: the same arrangement with and without. */
	@Test
	void stateCacheIsNotUsedForTheLoopsWinner() throws Exception {
		BruteForceAccelerator loop = engine(1, false, 20_000);
		BruteForceAccelerator plain = engine(new FlatEngineOptions().withThreads(1).withMinBoxes(1).withTree(true).withStateCache(0));
		BruteForceAccelerator cached = engine(new FlatEngineOptions().withThreads(1).withMinBoxes(1).withTree(true).withStateCache(4096));
		BruteForceAccelerator threads = engine(new FlatEngineOptions().withThreads(3).withMinBoxes(1).withTree(true).withNodeBudget(500).withStateCache(4096));
		try {
			for (int seed = 0; seed < 150; seed++) {
				Instance instance = FlatTestInstances.random(seed, 6, 10);
				BoxItemPermutationRotationIterator iterator = iterator(instance);
				FlatProblem problem = problem(iterator, instance.container, iterator.length(), false);
				AcceleratorWinner expected = loop.search(problem, 0L, () -> false).get();
				AcceleratorWinner winner = cached.search(problem, 0L, () -> false).get();
				assertThat(describe(winner)).as("seed " + seed).isEqualTo(describe(expected));
				assertThat(winner.getNodes()).as("seed " + seed).isEqualTo(plain.search(problem, 0L, () -> false).get().getNodes());
				assertThat(describe(threads.search(problem, 0L, () -> false).get())).as("seed " + seed + " on threads").isEqualTo(describe(expected));
			}
		} finally {
			loop.close();
			plain.close();
			cached.close();
			threads.close();
		}
	}
}
