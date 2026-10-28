package com.github.skjolber.packing.packer.bruteforce.flat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.FlatSearchOracle;
import com.github.skjolber.packing.packer.bruteforce.FlatTestInstances;
import com.github.skjolber.packing.packer.bruteforce.FlatTestInstances.Instance;

/**
 * The winner of the engine must be the arrangement which the loop over the permutations of the fast brute force packager finds, whatever the number of
 * threads or the node budget.
 */
class FlatEngineTest {

	/** the best arrangement of a loop over the permutations, as the packager runs it */
	private static final class Reference {

		int[] permutation;
		int[] rotations;
		int count;
		long volume;
		long weight;
		boolean full;
	}

	private final FlatSearchOracle oracle = new FlatSearchOracle();

	private static BoxItemPermutationRotationIterator iterator(Instance instance) {
		Container container = instance.container;
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
	}

	private static FlatProblem problem(BoxItemPermutationRotationIterator iterator, Container container, int limit) {
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), limit);
	}

	private Reference reference(BoxItemPermutationRotationIterator iterator, Container container, int limit, long boundVolume) {
		iterator.reset();
		int n = iterator.length();
		long totalVolume = 0;
		long totalWeight = 0;
		for (int i = 0; i < n; i++) {
			totalVolume += iterator.getStackValue(i).getBox().getVolume();
			totalWeight += iterator.getStackValue(i).getBox().getWeight();
		}
		boolean allItemsFit = limit >= n && totalVolume <= container.getMaxLoadVolume() && totalWeight <= container.getMaxLoadWeight();

		Reference best = new Reference();
		int size;
		do {
			int maxPackableCount = allItemsFit ? n : Math.min(limit, FlatSearchOracle.maxPackableCount(iterator, container));
			long firstVolume = 0;
			for (int i = 0; i < maxPackableCount; i++) {
				firstVolume += iterator.getStackValue(i).getBox().getVolume();
			}
			if(!allItemsFit && firstVolume < Math.max(boundVolume, best.volume)) {
				size = maxPackableCount;
			} else {
				FlatSearchOracle.Search search = oracle.search(iterator, container, maxPackableCount);
				size = search.count;
				if(size > 0) {
					long weight = 0;
					for (int i = 0; i < size; i++) {
						weight += iterator.getStackValue(i).getBox().getWeight();
					}
					boolean better = best.count == 0 || search.loadVolume > best.volume || (search.loadVolume == best.volume && (weight > best.weight || (weight == best.weight && size > best.count)));
					if(better) {
						best.permutation = iterator.getPermutations();
						best.rotations = search.rotations;
						best.count = size;
						best.volume = search.loadVolume;
						best.weight = weight;
					}
					if(size == n) {
						best.full = true;
						return best;
					}
				}
			}
		} while (iterator.nextPermutation(size) != -1);
		return best;
	}

	private void assertWinner(Optional<AcceleratorWinner> actual, Reference expected, String message) {
		assertThat(actual).as(message).isPresent();
		AcceleratorWinner winner = actual.get();
		assertThat(winner.getCount()).as(message).isEqualTo(expected.count);
		if(expected.count == 0) {
			return;
		}
		assertThat(winner.getLoadVolume()).as(message).isEqualTo(expected.volume);
		assertThat(winner.getLoadWeight()).as(message).isEqualTo(expected.weight);
		assertThat(winner.isFullyPacked()).as(message).isEqualTo(expected.full);
		assertThat(winner.getPermutation()).as(message).containsExactly(expected.permutation);
		assertThat(Arrays.copyOf(winner.getRotations(), expected.count)).as(message).containsExactly(expected.rotations);
	}

	/** the engines with the loop come first; the tree engines follow */
	private static final int LOOP_ENGINES = 4;

	private static FlatEngine[] engines() {
		return new FlatEngine[] {
				FlatEngine.newBuilder().withJointTree(false).withThreads(1).withMinBoxes(1).build(),
				FlatEngine.newBuilder().withJointTree(false).withThreads(4).withMinBoxes(1).build(),
				// many small items, paused after every permutation
				FlatEngine.newBuilder().withJointTree(false).withThreads(3).withItemsPerThread(1).withNodeBudget(1).withMinBoxes(1).build(),
				FlatEngine.newBuilder().withJointTree(false).withThreads(1).withItemsPerThread(1024).withNodeBudget(1).withMinBoxes(1).build(),
				// the tree, the default
				FlatEngine.newBuilder().withThreads(4).withMinBoxes(1).build(),
				FlatEngine.newBuilder().withThreads(1).withItemsPerThread(1024).withNodeBudget(1).withMinBoxes(1).build(),
		};
	}

	@Test
	void winnerIsTheArrangementOfThePackagersLoop() throws PackagerInterruptedException {
		FlatEngine[] engines = engines();
		try {
			int searched = 0;
			int full = 0;
			int partial = 0;
			for (int seed = 0; seed < 120; seed++) {
				Instance instance = FlatTestInstances.random(seed, 4, 8);
				Container container = instance.container;
				BoxItemPermutationRotationIterator iterator = iterator(instance);
				int n = iterator.length();
				int limit = seed % 4 == 3 ? 1 + (seed / 4) % n : n;

				FlatProblem problem = problem(iterator, container, limit);
				Reference expected = reference(iterator, container, limit, 0L);
				for (int e = 0; e < engines.length; e++) {
					Optional<AcceleratorWinner> actual = engines[e].search(problem, 0L, () -> false);
					assertWinner(actual, expected, "seed " + seed + ", engine " + e + ", limit " + limit);
				}
				searched++;
				if(expected.full) {
					full++;
				} else if(expected.count > 0) {
					partial++;
				}
			}
			assertThat(searched).isEqualTo(120);
			// both the problems where all boxes fit, and those where the permutations must be compared, must be covered
			assertThat(full).isGreaterThan(10);
			assertThat(partial).isGreaterThan(30);
		} finally {
			for (FlatEngine engine : engines) {
				engine.close();
			}
		}
	}

	@Test
	void winnerWithABoundIsTheArrangementOfThePackagersLoop() throws PackagerInterruptedException {
		FlatEngine[] engines = engines();
		try {
			int empty = 0;
			int found = 0;
			for (int seed = 200; seed < 320; seed++) {
				Instance instance = FlatTestInstances.random(seed, 5, 8);
				Container container = instance.container;
				BoxItemPermutationRotationIterator iterator = iterator(instance);
				int n = iterator.length();

				// the bound of another container's result: from a few boxes to more than the container holds
				long bound = container.getMaxLoadVolume() * (seed % 12) / 10;
				FlatProblem problem = problem(iterator, container, n);
				Reference expected = reference(iterator, container, n, bound);
				for (int e = 0; e < engines.length; e++) {
					Optional<AcceleratorWinner> actual = engines[e].search(problem, bound, () -> false);
					if(e >= LOOP_ENGINES && expected.count > 0 && expected.volume < bound) {
						// nothing reaches the bound: the loop returns the best of the orders it searched, which depends on its skipping, the tree prunes them
						// all; the packager discards both
						assertThat(actual).as("seed " + seed + ", engine " + e + ", bound " + bound).isPresent();
						assertThat(actual.get().getLoadVolume()).as("seed " + seed + ", engine " + e + ", bound " + bound).isLessThan(bound);
						continue;
					}
					assertWinner(actual, expected, "seed " + seed + ", engine " + e + ", bound " + bound);
				}
				if(expected.count == 0) {
					empty++;
				} else {
					found++;
				}
			}
			assertThat(empty).isGreaterThan(0);
			assertThat(found).isGreaterThan(10);
		} finally {
			for (FlatEngine engine : engines) {
				engine.close();
			}
		}
	}

	@Test
	void givesUpWhenTheFreePointsDoNotFit() throws PackagerInterruptedException {
		FlatEngine engine = FlatEngine.newBuilder().withThreads(2).withMinBoxes(1).withMaxPoints(1).build();
		FlatEngine inline = FlatEngine.newBuilder().withThreads(1).withMinBoxes(1).withMaxPoints(1).build();
		try {
			Instance instance = FlatTestInstances.random(1, 8, 8);
			BoxItemPermutationRotationIterator iterator = iterator(instance);
			FlatProblem problem = problem(iterator, instance.container, iterator.length());
			assertThat(engine.search(problem, 0L, () -> false)).isEmpty();
			assertThat(inline.search(problem, 0L, () -> false)).isEmpty();
		} finally {
			engine.close();
			inline.close();
		}
	}

	@Test
	void supportsTheConfiguredNumberOfBoxes() {
		FlatEngine engine = FlatEngine.newBuilder().withThreads(1).withMinBoxes(5).withMaxBoxes(6).build();
		for (int boxes = 3; boxes <= 8; boxes++) {
			Instance instance = FlatTestInstances.random(boxes, boxes, boxes);
			BoxItemPermutationRotationIterator iterator = iterator(instance);
			assertThat(engine.supports(problem(iterator, instance.container, iterator.length()))).as("boxes " + iterator.length()).isEqualTo(iterator.length() >= 5 && iterator.length() <= 6);
		}
	}

	@Test
	void interruptIsHonoured() {
		FlatEngine[] engines = engines();
		try {
			Instance instance = FlatTestInstances.random(7, 8, 8);
			BoxItemPermutationRotationIterator iterator = iterator(instance);
			FlatProblem problem = problem(iterator, instance.container, iterator.length());
			for (FlatEngine engine : engines) {
				assertThatThrownBy(() -> engine.search(problem, 0L, () -> true)).isInstanceOf(PackagerInterruptedException.class);
			}
		} finally {
			for (FlatEngine engine : engines) {
				engine.close();
			}
		}
	}

	@Test
	void interruptDuringTheSearchIsHonoured() {
		// not before the first poll: the search must be running; the engines which run on the calling thread poll before every item
		FlatEngine[] engines = engines();
		try {
			Instance instance = FlatTestInstances.random(11, 8, 8);
			BoxItemPermutationRotationIterator iterator = iterator(instance);
			FlatProblem problem = problem(iterator, instance.container, iterator.length());
			for (int e : new int[] { 0, 3 }) {
				FlatEngine engine = engines[e];
				int[] polls = new int[1];
				assertThatThrownBy(() -> engine.search(problem, 0L, () -> ++polls[0] > 2)).isInstanceOf(PackagerInterruptedException.class);
				assertThat(polls[0]).isEqualTo(3);
			}
		} finally {
			for (FlatEngine engine : engines) {
				engine.close();
			}
		}
	}

	@Test
	void closedEngineCannotSearchOnThreads() {
		FlatEngine engine = FlatEngine.newBuilder().withThreads(2).withMinBoxes(1).build();
		engine.close();
		Instance instance = FlatTestInstances.random(3, 8, 8);
		BoxItemPermutationRotationIterator iterator = iterator(instance);
		FlatProblem problem = problem(iterator, instance.container, iterator.length());
		assertThatThrownBy(() -> engine.search(problem, 0L, () -> false)).isInstanceOf(IllegalStateException.class);
	}
}
