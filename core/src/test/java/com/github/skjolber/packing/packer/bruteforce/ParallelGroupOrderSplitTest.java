package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * The parallel brute-force packager splits the orders of the box item groups between its threads, by the first groups
 * of the orders (prefixes), and gives the same results as on one thread:
 *
 * <pre>
 *   orders:   [a b c] [a c b] | [b a c] [b c a] | [c a b] [c b a]
 *   units:        prefix a    |    prefix b     |    prefix c        (taken by the threads in turn)
 * </pre>
 */
public class ParallelGroupOrderSplitTest {

	private static final int SEEDS = 30;

	@Test
	public void splitsGroupOrdersWithTheResultsOfOneThread() {
		// units of one group (parallelization count 4) and of two groups (64)
		for (int parallelizationCount : new int[] { 4, 64 }) {
			for (boolean fullSupport : new boolean[] { false, true }) {
				try (BruteForcePackager single = BruteForcePackager.newBuilder().withRequireFullSupport(fullSupport).build();
						ParallelBoxItemBruteForcePackager parallel = ParallelBoxItemBruteForcePackager.newBuilder()
								.withThreads(4)
								.withParallelizationCount(parallelizationCount)
								.withRequireFullSupport(fullSupport)
								.build()) {
					for (long seed = 0; seed < SEEDS; seed++) {
						for (boolean priorities : new boolean[] { false, true }) {
							for (boolean load : new boolean[] { false, true }) {
								List<BoxItemGroup> groups = createGroups(seed, priorities, load);
								assertThat(pack(parallel, groups))
										.as("parallelization count %d, full support %s, seed %d, priorities %s, load %s", parallelizationCount, fullSupport, seed, priorities, load)
										.isEqualTo(pack(single, groups));
							}
						}
					}
					assertThat(parallel.groupOrderSplits.get()).isGreaterThan(SEEDS);
				}
			}
		}
	}

	@Test
	public void splitGroupOrdersRespectTheDeadline() {
		List<BoxItemGroup> groups = new ArrayList<>();
		Random random = new Random(42);
		for (int g = 0; g < 12; g++) {
			List<BoxItem> items = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				items.add(new BoxItem(Box.newBuilder().withId("b" + g + "-" + i).withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2)).withRotate2D().withWeight(1).build(), 1));
			}
			groups.add(new BoxItemGroup("g" + g, items));
		}
		Container container = Container.newBuilder().withId("c").withSize(6, 3, 2).withMaxLoadWeight(100).build();
		try (ParallelBoxItemBruteForcePackager parallel = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(4).withParallelizationCount(64).build()) {
			long start = System.currentTimeMillis();
			parallel.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 12)))
					.withBoxItemGroups(groups)
					.withMaxContainerCount(12)
					.withInterruptDuration(200)
					.build();
			assertThat(System.currentTimeMillis() - start).isLessThan(5_000);
			assertThat(parallel.groupOrderSplits.get()).isGreaterThan(0);
		}
	}

	/**
	 * @return 3 to 5 groups of 1 or 2 boxes
	 */
	private static List<BoxItemGroup> createGroups(long seed, boolean priorities, boolean load) {
		Random random = new Random(seed);
		List<BoxItemGroup> groups = new ArrayList<>();
		int index = 0;
		int groupCount = 3 + random.nextInt(3);
		for (int g = 0; g < groupCount; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			int count = 1 + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				Box.Builder builder = Box.newBuilder()
						.withId("b" + index++)
						.withSize(1 + random.nextInt(2), 1 + random.nextInt(2), 1 + random.nextInt(2))
						.withRotate2D()
						.withWeight(1 + random.nextInt(3));
				if(load) {
					builder.withMaxLoadWeight(random.nextInt(4));
				}
				boxItems.add(new BoxItem(builder.build(), 1));
			}
			BoxItemGroup group = new BoxItemGroup("g" + g, boxItems);
			if(priorities) {
				group.withContainerPriority(random.nextInt(2));
			}
			groups.add(group);
		}
		return groups;
	}

	/** @return the placements of each container (box id, position and size) */
	private static List<String> pack(AbstractPackager<?> packager, List<BoxItemGroup> groups) {
		PackagerResult result = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(4, 2, 2).withMaxLoadWeight(100).build(), 10)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(10)
				.withInterruptDuration(10_000)
				.build();
		assertThat(result.isSuccess()).isTrue();
		List<String> containers = new ArrayList<>();
		for (Container container : result.getContainers()) {
			StringBuilder builder = new StringBuilder();
			for (Placement placement : container.getStack().getPlacements()) {
				builder.append(placement.getStackValue().getBox().getId()).append('@').append(placement.getAbsoluteX()).append(',').append(placement.getAbsoluteY()).append(',')
						.append(placement.getAbsoluteZ()).append(' ').append(placement.getStackValue().getDx()).append('x').append(placement.getStackValue().getDy()).append('x')
						.append(placement.getStackValue().getDz()).append(' ');
			}
			containers.add(builder.toString());
		}
		return containers;
	}
}
