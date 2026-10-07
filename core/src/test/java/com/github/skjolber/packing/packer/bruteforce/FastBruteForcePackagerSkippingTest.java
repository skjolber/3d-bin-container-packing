package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fast brute force with {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}: each box is placed in one of its rotations, at the
 * best point for the rotation, or skipped.
 */
public class FastBruteForcePackagerSkippingTest {

	private static final int SEEDS = 40;

	//
	//  container 3 x 2 x 1 (top view); boxes in order a (2 x 1, rotates), c (3 x 1, rotates), b (2 x 2).
	//  With a as given, c fits beside it and loads 5 of 6; with a rotated, c is skipped and b fills the container:
	//
	//   y                        y
	//   2 +-----------+          2 +---+-------+
	//     |     c     |            |   |       |
	//   1 +-------+---+          1 | a |   b   |
	//     |   a   |   |            |   |       |
	//   0 +-------+---+          0 +---+-------+
	//     0   1   2   3 x          0   1   2   3 x
	//
	//   container 1: a, b    container 2: c
	//
	@Test
	public void searchesRotationsAndSkipsTogether() {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			List<BoxItem> items = List.of(
					new BoxItem(Box.newBuilder().withId("a").withSize(2, 1, 1).withRotate2D().withWeight(1).build(), 1),
					new BoxItem(Box.newBuilder().withId("c").withSize(3, 1, 1).withRotate2D().withWeight(1).build(), 1),
					new BoxItem(Box.newBuilder().withId("b").withSize(2, 2, 1).withRotate2D().withWeight(1).build(), 1));
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(3, 2, 1).withMaxLoadWeight(100).build(), 2)))
					.withBoxItems(items)
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
					.withMaxContainerCount(2)
					.withInterruptDuration(10_000)
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).hasSize(2);
			assertThat(result.get(0).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("a", "b");
			assertThat(result.get(1).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("c");
		}
	}

	/**
	 * The bounded search finds the same arrangements as an exhaustive search: packing results are identical for
	 * random orders, with load limits, groups and container priorities.
	 */
	@Test
	public void matchesExhaustiveReference() {
		int skipped = 0;
		try (FastBruteForcePackager packager = new FastBruteForcePackager(new BruteForceIntermediatePackagerResultComparator(), FastBruteForcePackager.DEFAULT_POINT_COMPARATOR);
				FastBruteForcePackager reference = new ExhaustiveSkippingPackager()) {
			for (String variant : List.of("items", "loadLimits", "groups", "containerPriorities")) {
				for (int seed = 0; seed < SEEDS; seed++) {
					List<String> expected = describe(pack(reference, variant, seed));
					List<String> actual = describe(pack(packager, variant, seed));
					assertThat(actual).as("%s seed %d", variant, seed).isEqualTo(expected);
					if(!isPrefixPerContainer(actual)) {
						skipped++;
					}
				}
			}
		}
		// the inputs make the packager skip boxes
		assertThat(skipped).isGreaterThan(SEEDS);
	}

	private static PackagerResult pack(FastBruteForcePackager packager, String variant, int seed) {
		Random random = new Random(seed);
		// container priorities must not decrease in the box item order
		int firstLowPriority = 1 + random.nextInt(5);
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Box.Builder builder = Box.newBuilder().withId("b" + i).withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2)).withRotate2D();
			if(variant.equals("loadLimits")) {
				builder.withWeight(1 + random.nextInt(3)).withMaxLoadWeight(1 + random.nextInt(3));
			} else {
				builder.withWeight(1);
			}
			BoxItem item = new BoxItem(builder.build(), 1);
			if(variant.equals("containerPriorities")) {
				item.withContainerPriority(i < firstLowPriority ? 0 : 1);
			}
			items.add(item);
		}
		Container container = Container.newBuilder().withId("c").withSize(4, 3, 2).withMaxLoadWeight(100).build();
		var builder = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 6)))
				.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
				.withMaxContainerCount(6)
				.withInterruptDuration(10_000);
		if(variant.equals("groups")) {
			List<BoxItemGroup> groups = new ArrayList<>();
			for (int g = 0; g < 3; g++) {
				groups.add(new BoxItemGroup("g" + g, new ArrayList<>(items.subList(g * 2, g * 2 + 2))));
			}
			builder.withBoxItemGroups(groups);
		} else {
			builder.withBoxItems(items);
		}
		return builder.build();
	}

	/** @return per container: the box ids, positions and sizes of the placements */
	private static List<String> describe(PackagerResult result) {
		List<String> containers = new ArrayList<>();
		if(!result.isSuccess()) {
			containers.add("not packed");
			return containers;
		}
		for (Container container : result.getContainers()) {
			StringBuilder builder = new StringBuilder();
			for (Placement placement : container.getStack().getPlacements()) {
				BoxStackValue stackValue = placement.getStackValue();
				builder.append(stackValue.getBox().getId())
						.append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',').append(placement.getAbsoluteZ())
						.append(' ').append(stackValue.getDx()).append('x').append(stackValue.getDy()).append('x').append(stackValue.getDz())
						.append(' ');
			}
			containers.add(builder.toString());
		}
		return containers;
	}

	/** @return true if the containers hold the boxes in their order without skipping any */
	private static boolean isPrefixPerContainer(List<String> containers) {
		int next = 0;
		for (String container : containers) {
			for (String placement : container.split(" ")) {
				if(placement.startsWith("b")) {
					if(!placement.startsWith("b" + next + "@")) {
						return false;
					}
					next++;
				}
			}
		}
		return true;
	}

	/** Fast brute force with an exhaustive recursive search, without bounds, instead of the iterative one. */
	private static class ExhaustiveSkippingPackager extends FastBruteForcePackager {

		ExhaustiveSkippingPackager() {
			super(new BruteForceIntermediatePackagerResultComparator(), DEFAULT_POINT_COMPARATOR);
		}

		@Override
		protected void searchInOrderSkipping(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
				Container container, PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator,
				int[] skipEnds, int maxContainerPriority, SkippingBest best) throws PackagerInterruptedException {
			int[] permutations = iterator.getPermutations();
			BoxItem[] boxItems = iterator.getBoxItems();
			long[] minAreas = getMinAreas(boxItems, permutations);
			pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[0], iterator.getMinBoxVolume(0));
			new Search(pointCalculator, placements, boxItems, permutations, minAreas, skipEnds, iterator, stack, container, interrupt, utility, pointComparator, best)
					.search(0, 0, 0L, container.getMaxLoadWeight(), maxContainerPriority);
		}

		private class Search {

			private final FastPointCalculator3DStack pointCalculator;
			private final Placement[] placements;
			private final BoxItem[] boxItems;
			private final int[] permutations;
			private final long[] minAreas;
			private final int[] skipEnds;
			private final BoxItemPermutationRotationIterator iterator;
			private final Stack stack;
			private final Container container;
			private final PackagerInterruptSupplier interrupt;
			private final LoadPlacementUtility utility;
			private final FastBruteForceBoxStackValuePointComparator pointComparator;
			private final SkippingBest best;
			private final int[] placedPermutations;
			private final int[] placedRotations;

			Search(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItem[] boxItems, int[] permutations, long[] minAreas, int[] skipEnds,
					BoxItemPermutationRotationIterator iterator, Stack stack, Container container, PackagerInterruptSupplier interrupt, LoadPlacementUtility utility,
					FastBruteForceBoxStackValuePointComparator pointComparator, SkippingBest best) {
				this.pointCalculator = pointCalculator;
				this.placements = placements;
				this.boxItems = boxItems;
				this.permutations = permutations;
				this.minAreas = minAreas;
				this.skipEnds = skipEnds;
				this.iterator = iterator;
				this.stack = stack;
				this.container = container;
				this.interrupt = interrupt;
				this.utility = utility;
				this.pointComparator = pointComparator;
				this.best = best;
				this.placedPermutations = new int[permutations.length];
				this.placedRotations = new int[permutations.length];
			}

			void search(int level, int placedCount, long placedVolume, int freeLoadWeight, int maxContainerPriority) throws PackagerInterruptedException {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int length = permutations.length;
				if(level == length) {
					// every arrangement is compared: no bound
					if(placedCount > 0) {
						best.offer(pointCalculator.getPoints(), placedPermutations, placedRotations, placedCount);
					}
					return;
				}
				BoxItem boxItem = boxItems[permutations[level]];
				Box box = boxItem.getBox();
				if(boxItem.getContainerPriority() <= maxContainerPriority && box.getWeight() <= freeLoadWeight && placedVolume + box.getVolume() <= container.getMaxLoadVolume()) {
					BoxStackValue[] stackValues = box.getStackValues();
					for (int rotation = 0; rotation < stackValues.length; rotation++) {
						BoxStackValue stackValue = stackValues[rotation];
						boolean checkObstacles = !container.getObstacles().isEmpty();
						boolean checkExtraction = hasExtractionOrders(iterator);
						int pointIndex = utility == null ? getBestPoint(pointCalculator, boxItem, stackValue, stack, placedCount, container, checkObstacles, checkExtraction, pointComparator)
								: getBestPointWithLoad(pointCalculator, boxItem, stackValue, stack, placedCount, container, checkObstacles, checkExtraction, utility, pointComparator);
						if(pointIndex == -1) {
							continue;
						}
						Placement placement = placements[placedCount];
						SimplePoint3D point = pointCalculator.get(pointIndex);
						if(utility != null) {
							utility.populatePointSupporters(point);
							utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
							placement.clearLoad();
							placement.setIndex(stack.size());
							placement.setSupportedArea(utility.getSupportedAreaAtPoint(point, stackValue, false));
						}
						placement.setStackValue(stackValue);
						placement.setPoint(point);
						pointCalculator.add(pointIndex, placement);
						stack.add(placement);
						if(utility != null) {
							utility.addSupportersLoad(placement);
						}
						if(level + 1 < length) {
							if(minAreas[level + 1] != minAreas[level]) {
								pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[level + 1], iterator.getMinBoxVolume(level + 1));
							} else {
								pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(level + 1));
							}
						}
						placedPermutations[placedCount] = permutations[level];
						placedRotations[placedCount] = rotation;

						search(level + 1, placedCount + 1, placedVolume + box.getVolume(), freeLoadWeight - box.getWeight(), maxContainerPriority);

						if(utility != null) {
							placement.removeSupporteesAbove();
							for (PlacementLoad placementLoad : placement.getSupporters()) {
								placementLoad.getPlacement().removeLastSupportee();
							}
							placement.clearLoad();
						}
						stack.remove(stack.size() - 1);
						pointCalculator.setStackSize(placedCount);
					}
				}
				int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
				if(skipEnd != -1) {
					search(skipEnd, placedCount, placedVolume, freeLoadWeight, Math.min(maxContainerPriority, boxItem.getContainerPriority()));
				}
			}
		}
	}
}
