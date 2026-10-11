package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;

/**
 * The support options of the plain and LAFF packagers, on random orders: with full support required, every box rests
 * completely on the floor or on the boxes below; with support calculated, the supported area of each placement is the
 * area where it rests on the boxes below.
 */
public class SupportOptionsResultsTest {

	private static final int SEEDS = 30;

	@Test
	public void resultsAreFullySupported() {
		FullySupportedStabilityValidator validator = new FullySupportedStabilityValidator();
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().withRequireFullSupport(true).build(),
				() -> LargestAreaFitFirstPackager.newBuilder().withRequireFullSupport(true).build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().withRequireFullSupport(true).build());
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				for (String variant : List.of("items", "chronological", "groups", "loadLimits")) {
					for (int seed = 0; seed < SEEDS; seed++) {
						PackagerResult result = pack(packager, variant, seed);
						String name = packager.getClass().getSimpleName() + " " + variant + " seed " + seed;
						// a box item group may have no fully supported arrangement in a container
						assertThat(result.isSuccess() || variant.equals("groups")).as(name).isTrue();
						for (Container container : result.getContainers()) {
							List<ValidatorResultReason> reasons = new ArrayList<>();
							assertThat(validator.isValid(container.getStack().getPlacements(), reasons)).as("%s: %s", name, reasons).isTrue();
						}
					}
				}
			}
		}
	}

	@Test
	public void supportedAreasAreCalculated() {
		List<Supplier<AbstractPackager<?>>> packagers = List.of(
				() -> PlainPackager.newBuilder().withCalculateSupport(true).build(),
				() -> LargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true).build(),
				() -> FastLargestAreaFitFirstPackager.newBuilder().withCalculateSupport(true).build());
		for (Supplier<AbstractPackager<?>> supplier : packagers) {
			try (AbstractPackager<?> packager = supplier.get()) {
				int stacked = 0;
				for (String variant : List.of("items", "chronological", "groups")) {
					for (int seed = 0; seed < SEEDS; seed++) {
						PackagerResult result = pack(packager, variant, seed);
						String name = packager.getClass().getSimpleName() + " " + variant + " seed " + seed;
						// a box item group may not fit a container
						assertThat(result.isSuccess() || variant.equals("groups")).as(name).isTrue();
						for (Container container : result.getContainers()) {
							List<Placement> placements = container.getStack().getPlacements();
							SupportGraph graph = new SupportGraph(placements);
							for (Placement placement : placements) {
								if(placement.getAbsoluteZ() == 0) {
									continue;
								}
								stacked++;
								assertThat(placement.getSupportedArea()).as("%s: %s", name, placement).isEqualTo(graph.getSupportedArea(placement));
							}
						}
					}
				}
				// boxes are stacked on each other
				assertThat(stacked).as(packager.getClass().getSimpleName()).isGreaterThan(SEEDS);
			}
		}
	}

	private static PackagerResult pack(AbstractPackager<?> packager, String variant, int seed) {
		Random random = new Random(seed);
		List<BoxItem> items = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			Box.Builder builder = Box.newBuilder().withId("b" + i).withSize(1 + random.nextInt(4), 1 + random.nextInt(4), 1 + random.nextInt(3)).withRotate3D();
			if(variant.equals("loadLimits")) {
				builder.withWeight(1 + random.nextInt(3)).withMaxLoadWeight(2 + random.nextInt(8));
			} else {
				builder.withWeight(1);
			}
			items.add(new BoxItem(builder.build(), 1));
		}
		Container container = Container.newBuilder().withId("c").withSize(6, 5, 5).withMaxLoadWeight(1000).build();
		var builder = packager.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 12)))
				.withMaxContainerCount(12);
		if(variant.equals("chronological")) {
			builder.withOrder(Order.CHRONOLOGICAL);
		}
		if(variant.equals("groups")) {
			List<BoxItemGroup> groups = new ArrayList<>();
			for (int g = 0; g < 4; g++) {
				groups.add(new BoxItemGroup("g" + g, new ArrayList<>(items.subList(g * 3, g * 3 + 3))));
			}
			builder.withBoxItemGroups(groups);
		} else {
			builder.withBoxItems(items);
		}
		return builder.build();
	}
}
