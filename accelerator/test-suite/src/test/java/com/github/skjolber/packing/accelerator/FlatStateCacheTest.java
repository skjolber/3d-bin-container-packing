package com.github.skjolber.packing.accelerator;

import com.github.skjolber.packing.accelerator.test.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.accelerator.test.FlatTestInstances.Instance;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

/**
 * What the cache of searched states ({@link FlatStateCache}) rests on, checked node by node: a node with the state of a node which was searched is
 * searched all the same, and must not find an arrangement better than the best to beat when it was entered. The value of the winner alone does not
 * tell: on these instances many arrangements have the best value, so a search which skips nodes at random finds it too.
 */
class FlatStateCacheTest {

	@AfterEach
	void off() {
		FlatStateCache.verify = false;
	}

	private static FlatProblem problem(Instance instance, boolean allPoints) {
		Container container = instance.container;
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), iterator.length(), true,
				allPoints);
	}

	private static long[] verify(boolean allPoints, int threads, int entries, int nodeBudget, int fromSeed, int toSeed, int minBoxes, int maxBoxes) throws Exception {
		FlatStateCache.VERIFIED.set(0L);
		FlatStateCache.VIOLATED.set(0L);
		FlatStateCache.verify = true;
		try (FlatEngine engine = FlatEngine.newBuilder().withThreads(threads).withMinBoxes(1).withTree(true).withNodeBudget(nodeBudget).withStateCache(entries).build()) {
			for (int seed = fromSeed; seed < toSeed; seed++) {
				assertThat(engine.search(problem(FlatTestInstances.random(seed, minBoxes, maxBoxes), allPoints), 0L, () -> false)).isPresent();
			}
		} finally {
			FlatStateCache.verify = false;
		}
		return new long[] { FlatStateCache.VERIFIED.get(), FlatStateCache.VIOLATED.get() };
	}

	@Test
	void aNodeWhichIsFoundCannotBeatTheBest() throws Exception {
		long[] result = verify(false, 1, 1 << 14, 20_000, 0, 300, 6, 11);
		System.out.println("state cache: " + result[0] + " nodes found and searched all the same, " + result[1] + " of them found better than the best to beat");
		assertThat(result[0]).isGreaterThan(50_000L);
		assertThat(result[1]).isZero();
	}

	@Test
	void aNodeWhichIsFoundCannotBeatTheBestWithAllPoints() throws Exception {
		long[] result = verify(true, 1, 1 << 14, 20_000, 0, 200, 5, 8);
		assertThat(result[0]).isGreaterThan(5_000L);
		assertThat(result[1]).isZero();
	}

	@Test
	void aNodeWhichIsFoundCannotBeatTheBestOnThreadsAndWithASmallTable() throws Exception {
		long[] result = verify(false, 3, 64, 200, 0, 200, 6, 10);
		assertThat(result[0]).isGreaterThan(5_000L);
		assertThat(result[1]).isZero();
		result = verify(true, 2, 64, 200, 0, 100, 5, 8);
		assertThat(result[0]).isGreaterThan(300L);
		assertThat(result[1]).isZero();
	}
}
