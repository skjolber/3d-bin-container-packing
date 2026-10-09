package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.placement.DefaultPlacementComparatorFactory;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * The support options of the default placement controls have the same effect whether they are set on the packager builder,
 * or on the placement controls with the consumer overloads {@code withPlacementControlsBuilderFactory(b -> ..)}: with support
 * calculated and not required, positions with higher support are preferred.
 * <p>
 * The flat box "top" (2x3x1) can lie in two orientations on the box "base" (3x2x2): only turned to 3x2x1 does it rest completely
 * on the base. Choosing that orientation is a matter of ranking by support, which is not otherwise a preference (see the
 * figures).
 */
public class PlacementControlsSupportRankingTest {

	private static final long INTERRUPT_DURATION = 60_000L;

	private static List<BoxItem> boxItems() {
		List<BoxItem> boxItems = new ArrayList<>();
		boxItems.add(new BoxItem(Box.newBuilder().withId("base").withSize(3, 2, 2).withWeight(1).build(), 1));
		boxItems.add(new BoxItem(Box.newBuilder().withId("top").withSize(2, 3, 1).withWeight(1).build(), 1));
		return boxItems;
	}

	private static PackagerResult pack(AbstractPackager<?> packager) {
		Container container = Container.newBuilder().withId("container").withSize(4, 3, 3).withMaxLoadWeight(100).build();
		return packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 1)))
				.withBoxItems(boxItems())
				.withInterruptDuration(INTERRUPT_DURATION)
				.build();
	}

	/** The placements of the result, by box id, position and size, in placement order. */
	private static List<String> placements(PackagerResult result) {
		assertThat(result.isSuccess()).isTrue();

		List<String> placements = new ArrayList<>();
		for(Container container : result.getContainers()) {
			for(Placement placement : container.getStack().getPlacements()) {
				placements.add(placement.getBoxItem().getBox().getId()
						+ " at " + placement.getAbsoluteX() + "," + placement.getAbsoluteY() + "," + placement.getAbsoluteZ()
						+ " size " + placement.getStackValue().getDx() + "x" + placement.getStackValue().getDy() + "x" + placement.getStackValue().getDz());
			}
		}
		return placements;
	}

	@Test
	public void plainConsumerPathRanksBySupportLikeTheBuilderOption() {
		List<String> builderOption;
		try (PlainPackager packager = PlainPackager.newBuilder().withCalculateSupport(true).build()) {
			PackagerResult result = pack(packager);
			// <figure>
			//   z   /-----------------------|       z                                 y                                 z
			//      /                       /|       3 +-----------------------+       2 +-----------------------+       3 +---------------+
			//   | /                       / |         |                       |         |                       |         |               |
			//   |/                       /  |         |          top          |         |                       |         |      top      |
			// 3 |-----------------------|   |         |                       |         |                       |         |               |
			//   |                       |  /|       2 +-----------------------+         |          top          |       2 +---------------+
			//   |          top          | / |         |                       |         |                       |         |               |
			//   |                       |/  |         |                       |         |                       |         |               |
			// 2 |-----------------------|   |   y     |                       |         |                       |         |               |
			//   |                       |   |         |         base          |       0 +-----------------------+         |     base      |
			//   |                       |   | /       |                       |         0                       3   x     |               |
			//   |                       |   |/        |                       |                                           |               |
			//   |         base          |   | 2       |                       |                                           |               |
			//   |                       |  /        0 +-----------------------+                                         0 +---------------+
			//   |                       | /           0                       3   x                                       0               2   y
			//   |                       |/
			// 0 |-----------------------|-- x
			//   0                       3
			// </figure>
			figure(result);
			builderOption = placements(result);
		}
		// the top box is turned to rest completely on the base box
		assertThat(builderOption).containsExactly("base at 0,0,0 size 3x2x2", "top at 0,0,2 size 3x2x1");

		try (PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true)).build()) {
			assertThat(placements(pack(packager))).isEqualTo(builderOption);
		}
	}

	@Test
	public void plainWithoutSupportDoesNotRankBySupport() {
		// the baseline of the other test: no preference, so the top box is placed turned the other way
		for(boolean consumer : new boolean[] {false, true}) {
			PlainPackager.Builder builder = PlainPackager.newBuilder();
			if(consumer) {
				builder.withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(false));
			}
			try (PlainPackager packager = builder.build()) {
				assertThat(placements(pack(packager))).as("consumer: %s", consumer).containsExactly("base at 0,0,0 size 3x2x2", "top at 0,0,2 size 2x3x1");
			}
		}
	}

	@Test
	public void plainConsumerPathRequiringFullSupportMatchesTheBuilderOption() {
		List<String> builderOption;
		try (PlainPackager packager = PlainPackager.newBuilder().withRequireFullSupport(true).build()) {
			builderOption = placements(pack(packager));
		}
		try (PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withRequireFullSupport(true)).build()) {
			assertThat(placements(pack(packager))).isEqualTo(builderOption);
		}
	}

	@Test
	public void laffConsumerPathsRankBySupportLikeTheBuilderOption() {
		List<String> builderOption;
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true).build()) {
			PackagerResult result = pack(packager);
			// <figure>
			//   z   /-----------------------|       z                                 y                                 z
			//      /                       /|       3 +-----------------------+       2 +-----------------------+       3 +---------------+
			//   | /                       / |         |                       |         |                       |         |               |
			//   |/                       /  |         |          top          |         |                       |         |      top      |
			// 3 |-----------------------|   |         |                       |         |                       |         |               |
			//   |                       |  /|       2 +-----------------------+         |          top          |       2 +---------------+
			//   |          top          | / |         |                       |         |                       |         |               |
			//   |                       |/  |         |                       |         |                       |         |               |
			// 2 |-----------------------|   |   y     |                       |         |                       |         |               |
			//   |                       |   |         |         base          |       0 +-----------------------+         |     base      |
			//   |                       |   | /       |                       |         0                       3   x     |               |
			//   |                       |   |/        |                       |                                           |               |
			//   |         base          |   | 2       |                       |                                           |               |
			//   |                       |  /        0 +-----------------------+                                         0 +---------------+
			//   |                       | /           0                       3   x                                       0               2   y
			//   |                       |/
			// 0 |-----------------------|-- x
			//   0                       3
			// </figure>
			figure(result);
			builderOption = placements(result);
		}
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))
				.withFirstPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))
				.build()) {
			assertThat(placements(pack(packager))).isEqualTo(builderOption);
		}
	}

	@Test
	public void fastLaffConsumerPathsRankBySupportLikeTheBuilderOption() {
		List<String> builderOption;
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true).build()) {
			PackagerResult result = pack(packager);
			// <figure>
			//   z   /-----------------------|       z                                 y                                 z
			//      /                       /|       3 +-----------------------+       2 +-----------------------+       3 +---------------+
			//   | /                       / |         |                       |         |                       |         |               |
			//   |/                       /  |         |          top          |         |                       |         |      top      |
			// 3 |-----------------------|   |         |                       |         |                       |         |               |
			//   |                       |  /|       2 +-----------------------+         |          top          |       2 +---------------+
			//   |          top          | / |         |                       |         |                       |         |               |
			//   |                       |/  |         |                       |         |                       |         |               |
			// 2 |-----------------------|   |   y     |                       |         |                       |         |               |
			//   |                       |   |         |         base          |       0 +-----------------------+         |     base      |
			//   |                       |   | /       |                       |         0                       3   x     |               |
			//   |                       |   |/        |                       |                                           |               |
			//   |         base          |   | 2       |                       |                                           |               |
			//   |                       |  /        0 +-----------------------+                                         0 +---------------+
			//   |                       | /           0                       3   x                                       0               2   y
			//   |                       |/
			// 0 |-----------------------|-- x
			//   0                       3
			// </figure>
			figure(result);
			builderOption = placements(result);
		}
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder()
				.withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))
				.withFirstPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))
				.build()) {
			assertThat(placements(pack(packager))).isEqualTo(builderOption);
		}
	}

	@Test
	public void laffConsumerPathsMatchTheDefaultsWithoutOptions() {
		List<String> defaults;
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			defaults = placements(pack(packager));
		}
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.withPlacementControlsBuilderFactory(b -> { })
				.withFirstPlacementControlsBuilderFactory(b -> { })
				.build()) {
			assertThat(placements(pack(packager))).isEqualTo(defaults);
		}
	}

	@Test
	public void laffConsumerPathOptionsCannotBeCombinedWithTheBuilderOptions() {
		assertThatThrownBy(() -> LargestAreaFitFirstPackager.newBuilder()
				.withCalculateSupport(true)
				.withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true))
				.build()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void buildersCanBuildSeveralPackagers() {
		PlainPackager.Builder plain = PlainPackager.newBuilder().withCalculateSupport(true);
		LargestAreaFitFirstPackager.Builder laff = LargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true);
		FastLargestAreaFitFirstPackager.Builder fastLaff = FastLargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true);

		for(int i = 0; i < 2; i++) {
			try (PlainPackager a = plain.build(); LargestAreaFitFirstPackager b = laff.build(); FastLargestAreaFitFirstPackager c = fastLaff.build()) {
				assertThat(pack(a).isSuccess()).isTrue();
				assertThat(pack(b).isSuccess()).isTrue();
				assertThat(pack(c).isSuccess()).isTrue();
			}
		}
	}

	@Test
	public void placementComparatorFactoryOfTheConsumerIsCompiledOnce() {
		PlacementControlsBuilderFactory factory = new PlacementControlsBuilderFactoryBuilder()
				.withPlacementComparatorFactory((DefaultPlacementComparatorFactory.Builder ranking) -> ranking.lowerZIsBetter())
				.build();

		// not the uncompiled builder, which compiles for every container
		assertThat(((LoadAwarePlacementControlsBuilderFactory)factory).comparatorBuilderFactory).isInstanceOf(DefaultPlacementComparatorFactory.class);

		// also if the builder is passed in
		factory = new PlacementControlsBuilderFactoryBuilder()
				.withPlacementComparatorFactory(DefaultPlacementComparatorFactory.newFactory().lowerZIsBetter())
				.build();
		assertThat(((LoadAwarePlacementControlsBuilderFactory)factory).comparatorBuilderFactory).isInstanceOf(DefaultPlacementComparatorFactory.class);
	}
}
