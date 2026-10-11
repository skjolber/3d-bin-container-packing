package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;

/**
 * Brute force with full support required: boxes rest completely on the floor or on the boxes below.
 */
public class BruteForceFullSupportTest {

	private static final int SEEDS = 20;

	private static List<Supplier<AbstractPackager<?>>> packagers(boolean requireFullSupport) {
		return List.of(
				() -> BruteForcePackager.newBuilder().withRequireFullSupport(requireFullSupport).build(),
				() -> FastBruteForcePackager.newBuilder().withRequireFullSupport(requireFullSupport).build(),
				() -> ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).withRequireFullSupport(requireFullSupport).build());
	}

	//
	//  side view (y = 1), container 5 x 1 x 2 with an obstacle O on the floor at x = 1; boxes in order A, C, B.
	//  The free point on top of A starts at x = 0 and reaches over O and C, and hides the point on top of C.
	//  B at that point would rest partly on the obstacle, so it is shifted onto C's corner:
	//
	//   z
	//   2       [   B   ]
	//   1 [A][O][   C   ]
	//   0
	//     0  1  2       5  x
	//
	@ParameterizedTest
	@EnumSource(value = Order.class, names = { "CHRONOLOGICAL", "CHRONOLOGICAL_ALLOW_SKIPPING" })
	public void shiftsBoxesOntoTheCornerOfABoxBelow(Order order) {
		for (Supplier<AbstractPackager<?>> supplier : packagers(true)) {
			try (AbstractPackager<?> packager = supplier.get()) {
				List<BoxItem> items = List.of(
						new BoxItem(Box.newBuilder().withId("A").withSize(1, 1, 1).withRotate2D().withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("C").withSize(3, 1, 1).withRotate2D().withWeight(1).build(), 1),
						new BoxItem(Box.newBuilder().withId("B").withSize(3, 1, 1).withRotate2D().withWeight(1).build(), 1));
				Container container = Container.newBuilder().withId("c").withSize(5, 1, 2).withMaxLoadWeight(100).build();
				PackagerResult result = packager.newResultBuilder()
						.withContainerItem(b -> b
								.withContainerItem(container, 2)
								.withObstacles(o -> o.withObstacle(1, 0, 0, 1, 1, 1)))
						.withBoxItems(items)
						.withOrder(order)
						.withMaxContainerCount(2)
						.withInterruptDuration(10_000)
						.build();
				// <figure>
				//   z                                                 z
				//                                                     2                 +-----------------------+
				//   |                 /-----------------------|                         |                       |
				//   |                /                       /|                         |           B           |
				// 2 |               |-----------------------| |                         |                       |
				//   |               |                       | |       1 +-------+-------+-----------------------+
				//   | /-------/-----|           B           | |   y     |       |#######|                       |
				//   |/       /######|                       |/|         |   A   |#######|           C           |
				// 1 |-------|-------|-----------------------| | /       |       |#######|                       |
				//   |       |#######|                       | |/      0 +-------+-------+-----------------------+
				//   |   A   |#######|           C           | | 1       0       1       2                       5   x
				//   |       |#######|                       |/
				// 0 |-------|-------|-----------------------|-- x
				//   0       1       2                       5
				//
				// y                                                 z
				// 1 +-------+-------+-----------------------+       2 +-------+
				//   |       |#######|                       |         |       |
				//   |   A   |#######|           B           |         |   B   |
				//   |       |#######|                       |         |       |
				// 0 +-------+-------+-----------------------+       1 +-------+
				//   0       1       2                       5   x     |       |
				//                                                     |   C   |
				//                                                     |       |
				//                                                   0 +-------+
				//                                                     0       1   y
				// </figure>
				figure(result);

				String name = packager.getClass().getSimpleName() + " " + order;
				assertThat(result.isSuccess()).as(name).isTrue();
				assertThat(result.getContainers()).as(name).hasSize(1);
				assertThat(result.get(0).getStack().getPlacements()).as(name)
						.extracting(p -> p.getStackValue().getBox().getId() + "@" + p.getAbsoluteX() + "," + p.getAbsoluteY() + "," + p.getAbsoluteZ())
						.containsExactly("A@0,0,0", "C@2,0,0", "B@2,0,1");
			}
		}
	}

	@Test
	public void resultsAreFullySupported() {
		FullySupportedStabilityValidator validator = new FullySupportedStabilityValidator();
		try (PlainPackager plain = PlainPackager.newBuilder().withRequireFullSupport(true).build()) {
			for (Supplier<AbstractPackager<?>> supplier : packagers(true)) {
				try (AbstractPackager<?> packager = supplier.get()) {
					for (String variant : List.of("items", "chronological", "allowSkipping", "groups", "loadLimits")) {
						for (int seed = 0; seed < SEEDS; seed++) {
							PackagerResult result = pack(packager, variant, seed);
							String name = packager.getClass().getSimpleName() + " " + variant + " seed " + seed;
							if(!result.isSuccess()) {
								// only a box item group can be unpackable: when it has no fully supported arrangement in a
								// container. The exhaustive searches try every position the plain packager tries.
								assertThat(variant).as(name).isEqualTo("groups");
								if(!(packager instanceof FastBruteForcePackager)) {
									assertThat(pack(plain, variant, seed).isSuccess()).as(name).isFalse();
								}
								continue;
							}
							for (Container container : result.getContainers()) {
								List<ValidatorResultReason> reasons = new ArrayList<>();
								assertThat(validator.isValid(container.getStack().getPlacements(), reasons)).as("%s: %s", name, reasons).isTrue();
							}
						}
					}
				}
			}
		}
	}

	/** Without full support, the same inputs give boxes which are not fully supported. */
	@Test
	public void resultsAreNotFullySupportedWithoutTheOption() {
		FullySupportedStabilityValidator validator = new FullySupportedStabilityValidator();
		for (Supplier<AbstractPackager<?>> supplier : packagers(false)) {
			try (AbstractPackager<?> packager = supplier.get()) {
				int unsupported = 0;
				for (int seed = 0; seed < SEEDS; seed++) {
					for (Container container : pack(packager, "items", seed).getContainers()) {
						if(!validator.isValid(container.getStack().getPlacements(), new ArrayList<>())) {
							unsupported++;
						}
					}
				}
				assertThat(unsupported).as(packager.getClass().getSimpleName()).isPositive();
			}
		}
	}

	private static PackagerResult pack(AbstractPackager<?> packager, String variant, int seed) {
		Random random = new Random(seed);
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Box.Builder builder = Box.newBuilder().withId("b" + i).withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2)).withRotate2D();
			if(variant.equals("loadLimits")) {
				builder.withWeight(1 + random.nextInt(3)).withMaxLoadWeight(1 + random.nextInt(4));
			} else {
				builder.withWeight(1);
			}
			items.add(new BoxItem(builder.build(), 1));
		}
		Container container = Container.newBuilder().withId("c").withSize(4, 3, 3).withMaxLoadWeight(100).build();
		var builder = packager.newResultBuilder()
				.withContainerItem(b -> b.withContainerItem(container, 6))
				.withMaxContainerCount(6)
				.withInterruptDuration(10_000);
		switch (variant) {
			case "chronological" -> builder.withOrder(Order.CHRONOLOGICAL);
			case "allowSkipping" -> builder.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING);
			default -> {
			}
		}
		if(variant.equals("groups")) {
			List<BoxItemGroup> groups = new ArrayList<>();
			groups.add(new BoxItemGroup("g0", new ArrayList<>(items.subList(0, 2))));
			groups.add(new BoxItemGroup("g1", new ArrayList<>(items.subList(2, 5))));
			builder.withBoxItemGroups(groups);
		} else {
			builder.withBoxItems(items);
		}
		return builder.build();
	}

	/** @return the placements of a container as id@x,y,z */
	static List<String> positions(List<Placement> placements) {
		List<String> positions = new ArrayList<>();
		for (Placement placement : placements) {
			positions.add(placement.getStackValue().getBox().getId() + "@" + placement.getAbsoluteX() + "," + placement.getAbsoluteY() + "," + placement.getAbsoluteZ());
		}
		return positions;
	}
}
