package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.bruteforce.TreeTestInstances.Instance;

/**
 * With the tree search the fast brute force packager must return exactly the placements of its loop over the permutations (one position per
 * rotation), for random and tight problems, with full support, box item groups and box load limits.
 */
class FastBruteForceTreeSearchTest {

	private static final long INTERRUPT_MILLIS = 60_000L;

	@org.junit.jupiter.api.BeforeAll
	static void treeForEveryContainer() {
		AbstractBruteForcePackager.treeSearchMinPrefixes = 1;
	}

	@org.junit.jupiter.api.AfterAll
	static void restore() {
		AbstractBruteForcePackager.treeSearchMinPrefixes = 64;
	}

	private static PackagerResult pack(FastBruteForcePackager packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	static void compare(int fromSeed, int toSeed, int minBoxes, int maxBoxes, boolean tight, UnaryOperator<FastBruteForcePackager.Builder> configure) {
		FastBruteForcePackager loop = configure.apply(FastBruteForcePackager.newBuilder()).withTreeSearch(false).build();
		FastBruteForcePackager tree = configure.apply(FastBruteForcePackager.newBuilder()).withTreeSearchMinBoxes(1).build();
		try {
			for (int seed = fromSeed; seed < toSeed; seed++) {
				Instance instance = tight ? TreeTestInstances.tight(seed, minBoxes, maxBoxes) : TreeTestInstances.random(seed, minBoxes, maxBoxes);
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				String expected = BruteForceTreeSearchTest.describe(pack(loop, containerItems, instance.copyBoxItems()));
				assertThat(expected).as("seed " + seed + " packs all boxes").isNotEmpty();
				assertThat(BruteForceTreeSearchTest.describe(pack(tree, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void samePlacementsAsTheLoop() {
		compare(0, 80, 4, 8, false, b -> b);
	}

	@Test
	void samePlacementsOnTightProblems() {
		compare(1000, 1060, 7, 10, true, b -> b);
	}

	@Test
	void samePlacementsOnTightProblemsWithFullSupport() {
		compare(1100, 1140, 7, 9, true, b -> b.withRequireFullSupport(true));
	}

	@Test
	void samePlacementsWithBoxItemGroups() {
		FastBruteForcePackager loop = FastBruteForcePackager.newBuilder().withTreeSearch(false).build();
		FastBruteForcePackager tree = FastBruteForcePackager.newBuilder().withTreeSearchMinBoxes(1).build();
		try {
			for (int seed = 400; seed < 460; seed++) {
				Instance instance = seed % 2 == 0 ? TreeTestInstances.random(seed, 4, 8) : TreeTestInstances.tight(seed, 6, 9);
				int groupCount = 2 + seed % 2;
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				String expected = BruteForceTreeSearchTest.describe(loop.newResultBuilder().withContainerItems(containerItems)
						.withBoxItemGroups(BruteForceTreeSearchTest.groups(instance, groupCount)).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build());
				assertThat(BruteForceTreeSearchTest.describe(tree.newResultBuilder().withContainerItems(containerItems)
						.withBoxItemGroups(BruteForceTreeSearchTest.groups(instance, groupCount)).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build())).as("seed " + seed)
						.isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void samePlacementsWithBoxLoadLimits() {
		FastBruteForcePackager loop = FastBruteForcePackager.newBuilder().withTreeSearch(false).build();
		FastBruteForcePackager tree = FastBruteForcePackager.newBuilder().withTreeSearchMinBoxes(1).build();
		try {
			for (int seed = 300; seed < 340; seed++) {
				Instance instance = TreeTestInstances.random(seed, 4, 8);
				// each box can carry about its own weight
				List<BoxItem> boxItems = new ArrayList<>();
				for (BoxItem boxItem : instance.boxItems) {
					Box box = boxItem.getBox();
					BoxStackValue first = box.getStackValue(0);
					Box limited = Box.newBuilder().withId(box.getId()).withSize(first.getDx(), first.getDy(), first.getDz()).withWeight(box.getWeight()).withMaxLoadWeight(box.getWeight() + seed % 3)
							.withRotate3D().build();
					boxItems.add(new BoxItem(limited, boxItem.getCount()));
				}
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				String expected = BruteForceTreeSearchTest.describe(pack(loop, containerItems, copy(boxItems)));
				assertThat(BruteForceTreeSearchTest.describe(pack(tree, containerItems, copy(boxItems)))).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	private static List<BoxItem> copy(List<BoxItem> boxItems) {
		List<BoxItem> result = new ArrayList<>(boxItems.size());
		for (BoxItem boxItem : boxItems) {
			result.add(new BoxItem(boxItem.getBox(), boxItem.getCount()));
		}
		return result;
	}
}
