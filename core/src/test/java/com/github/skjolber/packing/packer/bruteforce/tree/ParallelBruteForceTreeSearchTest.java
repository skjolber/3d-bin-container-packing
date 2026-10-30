package com.github.skjolber.packing.packer.bruteforce.tree;

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
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.tree.TreeTestInstances.Instance;

/**
 * The parallel brute force packager with the tree search (one tree on all the threads, with work stealing) must return results as good as the
 * single-threaded loop over the permutations, container by container (the same boxes), and the same placements where no container holds all its
 * boxes: a complete result of any thread stops the others, so among several complete arrangements the threads' timing decides, as for the
 * permutation work units.
 */
class ParallelBruteForceTreeSearchTest {

	private static final long INTERRUPT_MILLIS = 60_000L;

	/** few permutations per thread, so that every container is split between the threads */
	private static ParallelBruteForcePackager parallel() {
		return ParallelBruteForcePackager.newBuilder().withThreads(3).withParallelizationCount(48).withTreeSearchMinBoxes(1).withTreeSearchStealing(true).withTreeSearchAnyBest(false).build();
	}

	@Test
	void sameValuesWithStealingAndAnyBest() {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		ParallelBruteForcePackager tree = ParallelBruteForcePackager.newBuilder().withThreads(3).withParallelizationCount(48).withTreeSearchMinBoxes(1).withTreeSearchStealing(true)
				.withTreeSearchAnyBest(true).build();
		try {
			for (int seed = 0; seed < 80; seed++) {
				Instance instance = seed % 2 == 0 ? TreeTestInstances.random(seed, 4, 7) : TreeTestInstances.tight(1000 + seed, 7, 8);
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				String expected = BruteForceTreeSearchTest.describeValues(pack(loop, containerItems, instance.copyBoxItems()));
				assertThat(BruteForceTreeSearchTest.describeValues(pack(tree, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	private static PackagerResult pack(AbstractPackager<?> packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	/** @return the containers with their box ids, in order: the boxes of each container, which is what the result comparator decides */
	static String describeBoxes(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (com.github.skjolber.packing.api.Container container : result.getContainers()) {
			List<String> ids = new ArrayList<>();
			for (com.github.skjolber.packing.api.Placement placement : container.getStack().getPlacements()) {
				ids.add(placement.getBoxItem().getBox().getId());
			}
			java.util.Collections.sort(ids);
			builder.append(container.getId()).append(ids).append(' ');
		}
		return builder.toString();
	}

	/** whether a container of the result holds all the boxes it was offered (the result comparator's best possible result for it) */
	private static boolean anyComplete(PackagerResult result, int boxes) {
		if(boxes < 0) {
			// box item groups: a container holding its first groups whole is complete, too
			return true;
		}
		int placed = 0;
		for (com.github.skjolber.packing.api.Container container : result.getContainers()) {
			placed += container.getStack().getPlacements().size();
		}
		// the last container holds the rest; a container holding every box offered to it is complete
		return result.getContainers().size() == 1 || placed == boxes;
	}

	static void assertSame(PackagerResult expected, PackagerResult actual, String message, int boxes) {
		assertThat(describeBoxes(actual)).as(message).isEqualTo(describeBoxes(expected));
		if(!anyComplete(expected, boxes)) {
			assertThat(BruteForceTreeSearchTest.describe(actual)).as(message).isEqualTo(BruteForceTreeSearchTest.describe(expected));
		}
	}

	private static void compare(int fromSeed, int toSeed, int minBoxes, int maxBoxes, boolean tight) {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		ParallelBruteForcePackager tree = parallel();
		try {
			for (int seed = fromSeed; seed < toSeed; seed++) {
				Instance instance = tight ? TreeTestInstances.tight(seed, minBoxes, maxBoxes) : TreeTestInstances.random(seed, minBoxes, maxBoxes);
				List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
				PackagerResult expected = pack(loop, containerItems, instance.copyBoxItems());
				assertThat(expected.getContainers()).as("seed " + seed + " packs all boxes").isNotEmpty();
				assertSame(expected, pack(tree, containerItems, instance.copyBoxItems()), "seed " + seed, instance.boxes);
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
				PackagerResult expected = loop.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(groups).withMaxContainerCount(8)
						.withInterruptDuration(INTERRUPT_MILLIS).build();
				List<BoxItemGroup> again = BruteForceTreeSearchTest.groups(instance, groupCount);
				assertSame(expected, tree.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(again).withMaxContainerCount(8)
						.withInterruptDuration(INTERRUPT_MILLIS).build(), "seed " + seed, -1);
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
				PackagerResult expected = pack(loop, containerItems, copy(boxItems));
				assertSame(expected, pack(tree, containerItems, copy(boxItems)), "seed " + seed, instance.boxes);
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
