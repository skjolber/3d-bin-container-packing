package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.container;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.item;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;

class GridVirtualBoxLayoutGeneratorTest {

	/*
	 * Four identical layers, each containing six boxes:
	 *
	 *       +---------+---------+
	 *       |    A    |    A    |
	 *       +---------+---------+
	 *       |    A    |    A    |
	 *       +---------+---------+
	 *       |    A    |    A    |
	 *       +---------+---------+
	 *
	 * Each bottom box carries three units. Generation itself needs no load graph.
	 */
	@Test
	void gridChecksLoadsWithoutGeometrySweep() {
		BoxItem original = new BoxItem(Box.newBuilder()
				.withSize(2, 3, 4)
				.withWeight(1)
				.withMaxLoadWeight(3)
				.withMaxLoadPressure(0.5)
				.withMaxLoadIdenticalBoxCount(3)
				.build(), 24);
		List<VirtualBoxLayout> layouts = new GridVirtualBoxLayoutGenerator().generate(original, List.of(container(4, 9, 16)), 8, () -> false);
		assertThat(layouts).hasSize(1);
		VirtualBox virtual = VirtualBox.of(layouts);
		assertThat(virtual.getLayouts()).isSameAs(layouts);
		VirtualBoxLayout layout = layouts.get(0);
		assertThat(layout.prepared).isTrue();
		for(Placement placement : layout.getPlacements()) {
			assertThat(placement.getSupporters()).isEmpty();
			assertThat(placement.getSupportees()).isEmpty();
		}
	}

	/*
	 *       +---------------+       weight above = 0
	 *       |       A       |
	 *       +---------------+       weight above = 1
	 *       |       A       |
	 *       +---------------+       weight above = 2, area = 4, pressure = 0.5
	 *       |       A       |
	 *       +---------------+
	 */
	@Test
	void fractionalPressureBoundaryIsCheckedBeforeMaterializing() {
		for(double pressure : new double[] {0.49, 0.5}) {
			BoxItem original = new BoxItem(Box.newBuilder()
					.withSize(2, 2, 1)
					.withWeight(1)
					.withMaxLoadPressure(pressure)
					.build(), 3);
			var layouts = new GridVirtualBoxLayoutGenerator().generate(original, List.of(container(2, 2, 3)), 8, () -> false);
			assertThat(layouts).hasSize(pressure < 0.5 ? 0 : 1);
		}
	}

	/*
	 *       [ A ] [ A ] [ A ]       zero-weight boxes still count towards stack depth.
	 *
	 * A three-high tower is forbidden; a single-layer row is permitted.
	 */
	@Test
	void zeroWeightDoesNotBypassDepthOrIdenticalItemConstraints() {
		BoxItem original = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(0)
				.withMaxLoadWeight(0)
				.withMaxLoadPressure(0)
				.withMaxLoadIdenticalBoxCount(1)
				.build(), 3);
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		assertThat(generator.generate(original, List.of(container(1, 1, 3)), 8, () -> false)).isEmpty();
		VirtualBoxLayout floor = generator.generate(original, List.of(container(3, 1, 1)), 8, () -> false).get(0);
		assertThat(floor.getPlacements()).hasSize(3).allSatisfy(p -> assertThat(p.getAbsoluteZ()).isZero());
	}

	/* Refinement: original A x 6 -> grid A x 2, without changing the six-item inventory. */
	@Test
	void subsetUsesItsOwnWeightAndKeepsOriginalOrientations() {
		BoxItem original = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(500_000_000)
				.build(), 6);
		original.setGlobalIndex(37);
		original.setLocalIndex(12);
		Container container = Container.newBuilder()
				.withSize(2, 1, 1)
				.withMaxLoadWeight(1_000_000_000)
				.build();
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		assertThat(generator.generate(original, List.of(container), 8, () -> false)).isEmpty();
		List<VirtualBoxLayout> layouts = generator.generate(original, 2, List.of(container), 8, () -> false);
		assertThat(layouts).hasSize(1);
		assertThat(VirtualBox.of(layouts).getWeight()).isEqualTo(1_000_000_000);
		assertThat(layouts.get(0).getPlacements()).hasSize(2).allSatisfy(p -> {
			assertThat(p.getBoxItem()).isSameAs(original);
			assertThat(p.getStackValue()).isSameAs(original.getBox().getStackValue(0));
		});
		assertThat(original.getCount()).isEqualTo(6);
		assertThat(original.getGlobalIndex()).isEqualTo(37);
		assertThat(original.getLocalIndex()).isEqualTo(12);
		assertThat(generator.generate(original, 3, List.of(container), 8, () -> false)).isEmpty();
		assertThatThrownBy(() -> generator.generate(original, 7, List.of(container), 8, () -> false)).isInstanceOf(IllegalArgumentException.class);
	}

	/* A cancelled materialization must not expose a partly constructed rectangle. */
	@Test
	void interruptionDuringMaterializationDiscardsPartialLayout() {
		AtomicInteger checks = new AtomicInteger();
		var value = item(1, 1, 1, 100).getBox().getStackValue(0);
		var grid = new GridVirtualBoxLayoutGenerator.Grid(new VirtualBoxBounds(10, 10, 1), value, 10, 10, 1, 3);
		assertThat(GridVirtualBoxLayoutGenerator.materialize(100, List.of(grid),
				() -> checks.incrementAndGet() > 10)).isEmpty();
		assertThat(checks.get()).isEqualTo(11);
	}

	/* Every retained alternative is a complete grid, not just the first best envelope. */
	@Test
	void everyAlternativePassesGenericGeometryValidation() {
		BoxItem original = new BoxItem(Box.newBuilder()
				.withSize(1, 2, 3)
				.withRotate3D()
				.withWeight(1)
				.build(), 12);
		List<VirtualBoxLayout> layouts = new GridVirtualBoxLayoutGenerator().generate(original, List.of(container(12, 12, 12)), 100, () -> false);
		assertThat(layouts.size()).isGreaterThan(8);
		for(VirtualBoxLayout layout : layouts) {
			new VirtualBoxLayout(layout.getBoundingBox(), layout.getPlacements()).prepare();
		}
	}

	@Test
	void generalLayoutsStillValidateGeometryWhenMixedWithGrids() {
		BoxItem original = item(1, 1, 1, 2);
		VirtualBoxLayout grid = new GridVirtualBoxLayoutGenerator().generate(original, List.of(container(2, 1, 1)), 1, () -> false).get(0);
		VirtualBoxLayout overlap = new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1), List.of(grid.getPlacements().get(0), grid.getPlacements().get(0)));
		assertThatThrownBy(() -> VirtualBox.of(List.of(grid, overlap))).isInstanceOf(IllegalArgumentException.class);
	}
}
