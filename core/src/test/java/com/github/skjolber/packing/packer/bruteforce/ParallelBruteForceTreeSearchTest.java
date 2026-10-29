package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.TreeTestInstances.Instance;

/**
 * The parallel brute force packager with the tree search (the orders split into units by their first types, searched on the threads) must return
 * exactly the placements of the single-threaded loop over the permutations: the units' results compare in the order of the orders.
 */
class ParallelBruteForceTreeSearchTest {

	private static final long INTERRUPT_MILLIS = 60_000L;

	/** few permutations per unit, so that every container is split between the threads */
	private static ParallelBruteForcePackager parallel() {
		return ParallelBruteForcePackager.newBuilder().withThreads(3).withParallelizationCount(2).withTreeSearchMinBoxes(1).build();
	}

	private static PackagerResult pack(AbstractPackager<?> packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	private static void compare(int fromSeed, int toSeed, int minBoxes, int maxBoxes, boolean tight) {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		ParallelBruteForcePackager tree = parallel();
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
		compare(0, 60, 4, 7, false);
	}

	@Test
	void samePlacementsOnTightProblems() {
		compare(1000, 1040, 7, 8, true);
	}

	@Test
	void samePlacementsWithBoxItemGroups() {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		ParallelBruteForcePackager tree = parallel();
		try {
			for (int seed = 400; seed < 460; seed++) {
				Instance instance = seed % 2 == 0 ? TreeTestInstances.random(seed, 4, 7) : TreeTestInstances.tight(seed, 6, 8);
				int groupCount = 2 + seed % 2;
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				List<BoxItemGroup> groups = BruteForceTreeSearchTest.groups(instance, groupCount);
				String expected = BruteForceTreeSearchTest.describe(loop.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(groups).withMaxContainerCount(8)
						.withInterruptDuration(INTERRUPT_MILLIS).build());
				List<BoxItemGroup> again = BruteForceTreeSearchTest.groups(instance, groupCount);
				assertThat(BruteForceTreeSearchTest.describe(tree.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(again).withMaxContainerCount(8)
						.withInterruptDuration(INTERRUPT_MILLIS).build())).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void samePlacementsWithBoxLoadLimits() {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		ParallelBruteForcePackager tree = parallel();
		try {
			for (int seed = 300; seed < 340; seed++) {
				Instance instance = TreeTestInstances.random(seed, 4, 8);
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
