package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.TreeTestInstances.Instance;

/**
 * With the tree search the brute force packager must return exactly the placements of the loop over the permutations, for random problems and for the
 * features which change the candidates of a box: a point filter, full support, obstacles, load limits and weight limits.
 */
class BruteForceTreeSearchTest {

	private static final long INTERRUPT_MILLIS = 60_000L;

	static String describe(PackagerResult result) {
		StringBuilder builder = new StringBuilder();
		for (Container container : result.getContainers()) {
			builder.append(container.getId()).append('[');
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getBoxItem().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',')
						.append(placement.getAbsoluteZ()).append('+').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append('x')
						.append(placement.getStackValue().getDz()).append(' ');
			}
			builder.append("] ");
		}
		return builder.toString();
	}

	private static PackagerResult pack(BruteForcePackager packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(INTERRUPT_MILLIS).build();
	}

	static List<ContainerItem> containerItems(Instance instance, long seed) {
		// every box fits a container alone: enough containers for all boxes
		ContainerItem.Builder builder = ContainerItem.newListBuilder().withContainer(instance.container, 8);
		if(seed % 2 == 0) {
			// a smaller second container: the result of the first is a bound for the second
			Container second = Container.newBuilder().withId("second" + seed)
					.withEmptyWeight(1)
					.withSize(Math.max(3, instance.container.getLoadDx() - 1), instance.container.getLoadDy(), Math.max(3, instance.container.getLoadDz() - 1))
					.withMaxLoadWeight(100_000)
					.build();
			builder.withContainer(second, 8);
		}
		return builder.build();
	}

	/** Pack random instances with the loop and with the tree, and compare the placements. */
	static void compare(int fromSeed, int toSeed, int minBoxes, int maxBoxes, UnaryOperator<BruteForcePackager.Builder> configure) {
		compare(fromSeed, toSeed, minBoxes, maxBoxes, false, configure);
	}

	static void compare(int fromSeed, int toSeed, int minBoxes, int maxBoxes, boolean tight, UnaryOperator<BruteForcePackager.Builder> configure) {
		BruteForcePackager loop = configure.apply(BruteForcePackager.newBuilder()).withTreeSearch(false).build();
		BruteForcePackager tree = configure.apply(BruteForcePackager.newBuilder()).withTreeSearchMinBoxes(1).build();
		try {
			for (int seed = fromSeed; seed < toSeed; seed++) {
				Instance instance = tight ? TreeTestInstances.tight(seed, minBoxes, maxBoxes) : TreeTestInstances.random(seed, minBoxes, maxBoxes);
				List<ContainerItem> containerItems = containerItems(instance, seed);
				String expected = describe(pack(loop, containerItems, instance.copyBoxItems()));
				assertThat(expected).as("seed " + seed + " packs all boxes").isNotEmpty();
				assertThat(describe(pack(tree, containerItems, instance.copyBoxItems()))).as("seed " + seed).isEqualTo(expected);
			}
		} finally {
			loop.close();
			tree.close();
		}
	}

	@Test
	void samePlacementsAsTheLoop() {
		compare(0, 60, 4, 7, b -> b);
	}

	@Test
	void samePlacementsOnTightProblems() {
		// many box types which do not all fit: partial arrangements, bounds and the lexicographic tie rule decide
		compare(1000, 1040, 7, 8, true, b -> b);
	}

	@Test
	void samePlacementsOnTightProblemsWithoutSettledOrders() {
		// too many prefixes to keep the settled orders: the fallback settles the other candidates of a box only when every order through one settled
		BruteForceTreeSearch.SettledOrders.maxPrefixes = 0;
		try {
			compare(1000, 1040, 7, 8, true, b -> b);
		} finally {
			BruteForceTreeSearch.SettledOrders.maxPrefixes = BruteForceTreeSearch.SettledOrders.MAX_PREFIXES;
		}
	}

	@Test
	void samePlacementsOnTightProblemsWithFullSupport() {
		compare(1100, 1130, 6, 7, true, b -> b.withRequireFullSupport(true));
	}

	@Test
	void samePlacementsWithFullSupport() {
		compare(100, 130, 4, 6, b -> b.withRequireFullSupport(true));
	}

	@Test
	void samePlacementsWithAPointFilter() {
		// the first three fitting points only, in reverse order
		compare(200, 230, 4, 6, b -> b.withPointFilter((calculator, stackValue) -> {
			List<Integer> indexes = new ArrayList<>();
			for (int k = calculator.size() - 1; k >= 0 && indexes.size() < 3; k--) {
				if(calculator.get(k).fits3D(stackValue)) {
					indexes.add(k);
				}
			}
			return new org.eclipse.collections.api.iterator.IntIterator() {
				int i = 0;

				@Override
				public boolean hasNext() {
					return i < indexes.size();
				}

				@Override
				public int next() {
					return indexes.get(i++);
				}
			};
		}));
	}

	@Test
	void samePlacementsWithBoxLoadLimits() {
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		BruteForcePackager tree = BruteForcePackager.newBuilder().withTreeSearchMinBoxes(1).build();
		try {
			for (int seed = 300; seed < 330; seed++) {
				Instance instance = TreeTestInstances.random(seed, 4, 6);
				// each box can carry about its own weight
				List<BoxItem> boxItems = new ArrayList<>();
				for (BoxItem boxItem : instance.boxItems) {
					Box box = boxItem.getBox();
					com.github.skjolber.packing.api.BoxStackValue first = box.getStackValue(0);
					Box limited = Box.newBuilder().withId(box.getId()).withSize(first.getDx(), first.getDy(), first.getDz()).withWeight(box.getWeight()).withMaxLoadWeight(box.getWeight() + seed % 3)
							.withRotate3D().build();
					boxItems.add(new BoxItem(limited, boxItem.getCount()));
				}
				List<ContainerItem> containerItems = containerItems(instance, seed);
				String expected = describe(pack(loop, containerItems, copy(boxItems)));
				assertThat(describe(pack(tree, containerItems, copy(boxItems)))).as("seed " + seed).isEqualTo(expected);
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
