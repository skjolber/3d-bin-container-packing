package com.github.skjolber.packing.accelerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.accelerator.FlatTestInstances.Instance;

/**
 * The tree search of an item ({@link FlatTree}) must find the winner of the loop over the orders ({@link FlatSubtree}) for the loop's winner, and an
 * arrangement of the same value, confirmed by the packager's search of its order, when any best arrangement will do.
 */
class FlatTreeTest {

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

	private static FlatEngine engine(int threads, boolean tree, int nodeBudget) {
		return FlatEngine.newBuilder().withThreads(threads).withMinBoxes(1).withTree(tree).withNodeBudget(nodeBudget).build();
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
		FlatEngine loop = engine(1, false, 20_000);
		FlatEngine tree = engine(1, true, 20_000);
		FlatEngine threads = engine(3, true, 500);
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
		FlatEngine loop = engine(1, false, 20_000);
		FlatEngine tree = engine(2, true, 100);
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
		FlatEngine loop = engine(1, false, 20_000);
		FlatEngine tree = engine(1, true, 1);
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
		FlatEngine loop = engine(1, false, 20_000);
		FlatEngine tree = engine(2, true, 300);
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
}
