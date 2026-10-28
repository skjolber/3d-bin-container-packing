package com.github.skjolber.packing.packer.bruteforce.flat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.FlatTestInstances;

/**
 * The winner of the joint tree must be the winner of the permutation loop ({@linkplain FlatEngine} on one thread, which the other tests pin to the
 * packager's loop), on random problems, with and without a bound.
 */
class FlatJointSearchTest {

	private static int size(Random random, int max) {
		return Math.max(1 + random.nextInt(max), 1 + random.nextInt(max));
	}

	/** tight containers of 4 to 9 units, 3 to 8 box types, so that not all boxes fit; every third limited by weight */
	static FlatProblem tight(long seed, int minBoxes, int maxBoxes) {
		Random random = new Random(seed);
		int cx = 4 + random.nextInt(6);
		int cy = 4 + random.nextInt(6);
		int cz = 4 + random.nextInt(6);
		int max = Math.min(cx, Math.min(cy, cz));
		List<BoxItem> boxItems = new ArrayList<>();
		int boxes = minBoxes + random.nextInt(maxBoxes - minBoxes + 1);
		int remaining = boxes;
		int itemCount = 3 + random.nextInt(6);
		int totalWeight = 0;
		for (int i = 0; i < itemCount && remaining > 0; i++) {
			int count = i == itemCount - 1 ? remaining : Math.min(remaining, 1 + random.nextInt(2));
			remaining -= count;
			int weight = 1 + random.nextInt(4);
			totalWeight += count * weight;
			Box.Builder builder = Box.newBuilder().withId(Integer.toString(i)).withSize(size(random, max), size(random, max), size(random, max)).withWeight(weight);
			int rotation = random.nextInt(3);
			if(rotation == 1) {
				builder.withRotate2D();
			} else if(rotation == 2) {
				builder.withRotate3D();
			}
			boxItems.add(new BoxItem(builder.build(), count));
		}
		int maxLoadWeight = seed % 3 == 0 ? Math.max(4, totalWeight * (50 + random.nextInt(40)) / 100) : 100_000;
		Container container = Container.newBuilder().withId("c" + seed).withEmptyWeight(1).withSize(cx, cy, cz).withMaxLoadWeight(maxLoadWeight).build();
		return problem(container, boxItems);
	}

	static FlatProblem problem(Container container, List<BoxItem> boxItems) {
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(boxItems)
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), iterator.length());
	}

	static String describe(AcceleratorWinner w) {
		return "count " + w.getCount() + " volume " + w.getLoadVolume() + " weight " + w.getLoadWeight() + " full " + w.isFullyPacked() + " permutation " + Arrays.toString(w.getPermutation())
				+ " rotations " + Arrays.toString(Arrays.copyOf(w.getRotations(), w.getCount())) + " nodes " + w.getNodes();
	}

	private static void assertSame(AcceleratorWinner actual, AcceleratorWinner expected, String message) {
		String detail = message + ": expected " + describe(expected) + ", got " + describe(actual);
		assertThat(actual.getCount()).as(detail).isEqualTo(expected.getCount());
		if(expected.getCount() == 0) {
			return;
		}
		assertThat(actual.getLoadVolume()).as(detail).isEqualTo(expected.getLoadVolume());
		assertThat(actual.getLoadWeight()).as(detail).isEqualTo(expected.getLoadWeight());
		assertThat(actual.isFullyPacked()).as(detail).isEqualTo(expected.isFullyPacked());
		assertThat(actual.getPermutation()).as(detail).containsExactly(expected.getPermutation());
		assertThat(Arrays.copyOf(actual.getRotations(), expected.getCount())).as(detail).containsExactly(Arrays.copyOf(expected.getRotations(), expected.getCount()));
	}

	private static void compare(FlatEngine engine, FlatProblem problem, long bound, String message) throws PackagerInterruptedException {
		Optional<AcceleratorWinner> expected = engine.search(problem, bound, () -> false);
		AcceleratorWinner actual = FlatJointSearch.search(problem, bound, 128);
		assertThat(expected).as(message).isPresent();
		assertThat(actual).as(message).isNotNull();
		if(expected.get().getLoadVolume() < bound) {
			// nothing reaches the bound: the loop returns the best of the permutations it searched, which depends on its skipping; the packager discards both
			assertThat(actual.getLoadVolume()).as(message + ": expected " + describe(expected.get()) + ", got " + describe(actual)).isLessThan(bound);
			return;
		}
		assertSame(actual, expected.get(), message);
	}

	@Test
	void winnerIsTheWinnerOfThePermutationLoop() throws PackagerInterruptedException {
		FlatEngine engine = FlatEngine.newBuilder().withJointTree(false).withThreads(1).withMinBoxes(1).build();
		try {
			for (long seed = 0; seed < 200; seed++) {
				FlatTestInstances.Instance instance = FlatTestInstances.random(seed, 4, 9);
				compare(engine, problem(instance.container, instance.copyBoxItems()), 0L, "instance seed " + seed);
			}
			for (long seed = 1; seed <= 60; seed++) {
				compare(engine, tight(seed, 8, 10), 0L, "tight seed " + seed);
			}
		} finally {
			engine.close();
		}
	}

	@Test
	void winnerWithABoundIsTheWinnerOfThePermutationLoop() throws PackagerInterruptedException {
		FlatEngine engine = FlatEngine.newBuilder().withJointTree(false).withThreads(1).withMinBoxes(1).build();
		try {
			for (long seed = 1; seed <= 40; seed++) {
				FlatProblem problem = tight(seed, 8, 9);
				long volume = engine.search(problem, 0L, () -> false).get().getLoadVolume();
				compare(engine, problem, volume / 2, "tight seed " + seed + " half bound");
				compare(engine, problem, volume, "tight seed " + seed + " bound at the winner");
				compare(engine, problem, volume + 1, "tight seed " + seed + " bound above the winner");
			}
		} finally {
			engine.close();
		}
	}

	@Test
	void givesUpWhenTheFreePointsDoNotFit() {
		int gaveUp = 0;
		for (long seed = 1; seed <= 20; seed++) {
			if(FlatJointSearch.search(tight(seed, 8, 11), 0L, 12) == null) {
				gaveUp++;
			}
		}
		assertThat(gaveUp).isGreaterThan(0);
	}
}
