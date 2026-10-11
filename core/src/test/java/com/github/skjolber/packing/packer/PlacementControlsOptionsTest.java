package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Options which custom placement controls cannot honour are rejected, instead of ignored.
 */
public class PlacementControlsOptionsTest {

	/** placement controls which do not respect load limits */
	private static PlacementControlsBuilderFactory comparatorControls() {
		return new ComparatorPlacementControlsBuilderFactory((a, b) -> 0, new VolumeThenWeightBoxItemComparator());
	}

	@Test
	public void supportOptionsAreRejectedWithCustomPlacementControls() {
		assertThatThrownBy(() -> PlainPackager.newBuilder().withPlacementControlsBuilderFactory(comparatorControls()).withCalculateSupport(true).build())
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> PlainPackager.newBuilder().withPlacementControlsBuilderFactory(comparatorControls()).withRequireFullSupport(true).build())
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> LargestAreaFitFirstPackager.newBuilder().withPlacementControlsBuilderFactory(comparatorControls()).withCalculateSupport(true).build())
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> FastLargestAreaFitFirstPackager.newBuilder().withFirstPlacementControlsBuilderFactory(comparatorControls()).withRequireFullSupport(true).build())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void loadLimitsAreRejectedWhenThePlacementControlsDoNotRespectThem() {
		BoxItem loaded = new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1).build(), 1);
		BoxItem free = new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).build(), 1);
		List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(1, 1, 2).withMaxLoadWeight(10).build(), 1));

		List<AbstractPackager<?>> custom = List.of(
				PlainPackager.newBuilder().withPlacementControlsBuilderFactory(comparatorControls()).build(),
				LargestAreaFitFirstPackager.newBuilder().withPlacementControlsBuilderFactory(comparatorControls()).build(),
				FastLargestAreaFitFirstPackager.newBuilder().withFirstPlacementControlsBuilderFactory(comparatorControls()).build());
		for (AbstractPackager<?> packager : custom) {
			try {
				assertThat(packager.getUnsupportedReason(new PackagerInput(List.of(loaded), null, containers, 1, Order.NONE))).as(packager.getClass().getSimpleName()).isNotNull();
				assertThat(packager.getUnsupportedReason(new PackagerInput(List.of(free), null, containers, 1, Order.NONE))).as(packager.getClass().getSimpleName()).isNull();
			} finally {
				packager.close();
			}
		}

		// the default placement controls respect load limits
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			assertThat(packager.getUnsupportedReason(new PackagerInput(List.of(loaded), null, containers, 1, Order.NONE))).isNull();
		}
	}
}
