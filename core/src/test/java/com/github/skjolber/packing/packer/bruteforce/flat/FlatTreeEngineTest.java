package com.github.skjolber.packing.packer.bruteforce.flat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.FlatTestInstances;

/**
 * The engine with the joint tree ({@linkplain FlatTreeItem}: items, threads, pausing) must give the winner of the engine with the permutation loop,
 * which the other tests pin to the packager's loop.
 */
class FlatTreeEngineTest {

	private static FlatEngine[] engines() {
		return new FlatEngine[] {
				FlatEngine.newBuilder().withJointTree(true).withThreads(1).withMinBoxes(1).build(),
				FlatEngine.newBuilder().withJointTree(true).withThreads(4).withMinBoxes(1).build(),
				// many small items, paused after every node
				FlatEngine.newBuilder().withJointTree(true).withThreads(3).withItemsPerThread(1).withNodeBudget(1).withMinBoxes(1).build(),
				FlatEngine.newBuilder().withJointTree(true).withThreads(1).withItemsPerThread(1024).withNodeBudget(7).withMinBoxes(1).build(),
		};
	}

	private static void assertSame(Optional<AcceleratorWinner> actual, Optional<AcceleratorWinner> expected, String message) {
		assertThat(expected).as(message).isPresent();
		assertThat(actual).as(message).isPresent();
		AcceleratorWinner a = actual.get();
		AcceleratorWinner e = expected.get();
		String detail = message + ": expected " + FlatJointSearchTest.describe(e) + ", got " + FlatJointSearchTest.describe(a);
		assertThat(a.getCount()).as(detail).isEqualTo(e.getCount());
		if(e.getCount() == 0) {
			return;
		}
		assertThat(a.getLoadVolume()).as(detail).isEqualTo(e.getLoadVolume());
		assertThat(a.getLoadWeight()).as(detail).isEqualTo(e.getLoadWeight());
		assertThat(a.isFullyPacked()).as(detail).isEqualTo(e.isFullyPacked());
		assertThat(a.getPermutation()).as(detail).containsExactly(e.getPermutation());
		assertThat(Arrays.copyOf(a.getRotations(), e.getCount())).as(detail).containsExactly(Arrays.copyOf(e.getRotations(), e.getCount()));
	}

	@Test
	void winnerIsTheWinnerOfThePermutationLoop() throws PackagerInterruptedException {
		FlatEngine loop = FlatEngine.newBuilder().withJointTree(false).withThreads(1).withMinBoxes(1).build();
		FlatEngine[] engines = engines();
		try {
			for (long seed = 0; seed < 120; seed++) {
				FlatTestInstances.Instance instance = FlatTestInstances.random(seed, 4, 9);
				FlatProblem problem = FlatJointSearchTest.problem(instance.container, instance.copyBoxItems());
				Optional<AcceleratorWinner> expected = loop.search(problem, 0L, () -> false);
				for (int i = 0; i < engines.length; i++) {
					assertSame(engines[i].search(problem, 0L, () -> false), expected, "instance seed " + seed + " engine " + i);
				}
			}
			for (long seed = 1; seed <= 40; seed++) {
				FlatProblem problem = FlatJointSearchTest.tight(seed, 8, 10);
				Optional<AcceleratorWinner> expected = loop.search(problem, 0L, () -> false);
				for (int i = 0; i < engines.length; i++) {
					assertSame(engines[i].search(problem, 0L, () -> false), expected, "tight seed " + seed + " engine " + i);
				}
				// a bound at the winner: still found
				long volume = expected.get().getLoadVolume();
				Optional<AcceleratorWinner> bounded = loop.search(problem, volume, () -> false);
				for (int i = 0; i < engines.length; i++) {
					assertSame(engines[i].search(problem, volume, () -> false), bounded, "tight seed " + seed + " engine " + i + " bound at the winner");
				}
			}
		} finally {
			loop.close();
			for (FlatEngine engine : engines) {
				engine.close();
			}
		}
	}

	@Test
	void givesUpWhenTheFreePointsDoNotFit() throws PackagerInterruptedException {
		FlatEngine engine = FlatEngine.newBuilder().withJointTree(true).withThreads(2).withMaxPoints(12).withMinBoxes(1).build();
		try {
			int gaveUp = 0;
			for (long seed = 1; seed <= 20; seed++) {
				if(engine.search(FlatJointSearchTest.tight(seed, 8, 11), 0L, () -> false).isEmpty()) {
					gaveUp++;
				}
			}
			assertThat(gaveUp).isGreaterThan(0);
		} finally {
			engine.close();
		}
	}
}
