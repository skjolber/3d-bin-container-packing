package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.container;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.item;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

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
			StackPlacementAssert.assertThat(placement).isUnsupported().supportsNothing();
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

	/* Placements are created once, on first access; unused alternatives allocate none. */
	@Test
	void placementsAreCreatedLazilyAndOnce() {
		var value = item(1, 1, 1, 100).getBox().getStackValue(0);
		var grid = new GridVirtualBoxLayoutGenerator.Grid(new VirtualBoxBounds(10, 10, 1), value, 10, 10, 1, 3);
		var layout = (GridVirtualBoxLayoutGenerator.GridLayout) GridVirtualBoxLayoutGenerator.materialize(List.of(grid)).get(0);
		assertThat(layout.gridPlacements).isNull();
		assertThat(layout.getPlacements()).hasSize(100);
		assertThat(layout.getPlacements()).isSameAs(layout.gridPlacements);
	}

	/*
	 * 40 boxes (2 x 1 x 1) in a 6 x 3 x 2 container, which holds 3 x 3 x 2 = 18:
	 *
	 *       +-----------+   +-----------+   +---+---+---+
	 *       |  3 x 3 x 2|   |  3 x 3 x 2|   | A   A   A |  one row of three
	 *       |   (18)    |   |   (18)    |   +---+---+---+
	 *       +-----------+   +-----------+   | A |          one loose box
	 *                                       +---+
	 *
	 * Full blocks first, then whole layers, whole rows and a line.
	 */
	@Test
	void partitionsIntoContainerSizedBlocks() {
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		BoxItem boxes = item(2, 1, 1, 40);
		assertThat(generator.generate(boxes, List.of(container(6, 3, 2)), 8, () -> false)).isEmpty();
		assertThat(generator.partition(boxes, 40, List.of(container(6, 3, 2)), 10_000)).containsExactly(18, 18, 3, 1);
		// 5 cubes in a 3 x 2 floor: one full row and a line of two
		assertThat(generator.partition(item(1, 1, 1, 5), 5, List.of(container(3, 2, 1)), 10_000)).containsExactly(3, 2);
		// 7 cubes in a 3 x 3 x 3 container: a 3 x 2 layer slice and one loose box
		assertThat(generator.partition(item(1, 1, 1, 7), 7, List.of(container(3, 3, 3)), 10_000)).containsExactly(6, 1);
		// grid limit of four: rows of four
		assertThat(generator.partition(item(1, 1, 1, 8), 8, List.of(container(4, 2, 1)), 4)).containsExactly(4, 4);
		// weight limit of three boxes per container
		BoxItem heavy = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(10)
				.build(), 7);
		Container light = Container.newBuilder()
				.withSize(4, 4, 4)
				.withMaxLoadWeight(30)
				.build();
		assertThat(generator.partition(heavy, 7, List.of(light), 10_000)).containsExactly(3, 3, 1);
		// nothing fits twice
		assertThat(generator.partition(item(2, 2, 2, 3), 3, List.of(container(3, 3, 3)), 10_000)).isEmpty();
	}

	/*
	 * Rotatable 2 x 1 x 1 boxes in a 20 x 1 x 1 container. Standing up (1 x 1 x 2)
	 * or sideways (1 x 2 x 1) does not fit at all, so only lying lengthwise counts:
	 *
	 *       +----+----+----+----+----+----+----+----+----+----+----+----+----+
	 *       |  A |  A |  A |  A |  A |  A |  A |  A |  A |  A |  A |  A |  A |
	 *       +----+----+----+----+----+----+----+----+----+----+----+----+----+
	 *       |<----------------- 10 (full) ------------------->|<--- 3 ----->|
	 *
	 * An orientation which does not fit must not yield a single 13-box "block".
	 */
	@Test
	void partitionIgnoresOrientationsWhichDoNotFit() {
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		BoxItem boxes = new BoxItem(Box.newBuilder()
				.withSize(2, 1, 1)
				.withRotate3D()
				.withWeight(1)
				.build(), 13);
		assertThat(generator.partition(boxes, 13, List.of(container(20, 1, 1)), 10_000)).containsExactly(10, 3);
	}

	/* Every partition block forms a fitting grid, also for rotatable boxes. */
	@Test
	void everyPartitionBlockHasLayouts() {
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		BoxItem fixed = item(2, 1, 1, 97);
		BoxItem rotatable = new BoxItem(Box.newBuilder()
				.withSize(3, 2, 1)
				.withRotate3D()
				.withWeight(1)
				.build(), 97);
		List<List<Container>> containerSets = List.of(List.of(container(7, 5, 3)), List.of(container(20, 1, 1)), List.of(container(7, 5, 3), container(20, 2, 1)));
		for(BoxItem boxes : List.of(fixed, rotatable)) {
			for(List<Container> containers : containerSets) {
				int total = 0;
				for(int block : generator.partition(boxes, 97, containers, 10_000)) {
					total += block;
					if(block > 1) {
						assertThat(generator.generate(boxes, block, containers, 8, () -> false)).as("block %s in %s", block, containers).isNotEmpty();
					}
				}
				assertThat(total == 0 || total == 97).isTrue();
			}
		}
	}

	/*
	 * 36 boxes (2 x 1 x 1). A large container (6 x 3 x 2) holds 18, a small one (4 x 3 x 2) holds 12.
	 *
	 *   one large available:   [18] [18]       needs two large containers  ✗
	 *                          [12] [12] [12]  fits small or large         ✓
	 *   two large available:   [18] [18]                                   ✓
	 */
	@Test
	void partitionRespectsAvailableContainers() {
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		BoxItem boxes = item(2, 1, 1, 36);
		Container large = container(6, 3, 2);
		Container small = container(4, 3, 2);
		assertThat(generator.partition(boxes, 36, List.of(new ContainerItem(large, 1), new ContainerItem(small, 3)), 4, 10_000)).containsExactly(12, 12, 12);
		assertThat(generator.partition(boxes, 36, List.of(new ContainerItem(large, 2), new ContainerItem(small, 3)), 4, 10_000)).containsExactly(18, 18);
		// the result may use at most two containers: three small blocks cannot be placed either
		assertThat(generator.partition(boxes, 36, List.of(new ContainerItem(large, 2), new ContainerItem(small, 3)), 2, 10_000)).containsExactly(18, 18);
	}

	/*
	 * With container costs, blocks which also fit the small container are preferred,
	 * so the delegate can still choose the cheaper container:
	 *
	 *   [18] [18]  fits only large      [12] [12] [12]  fits small and large  ✓
	 */
	@Test
	void partitionPrefersBlocksFittingMoreContainerTypesWithCosts() {
		GridVirtualBoxLayoutGenerator generator = new GridVirtualBoxLayoutGenerator();
		BoxItem boxes = item(2, 1, 1, 36);
		Container large = container(6, 3, 2);
		Container small = container(4, 3, 2);
		List<ContainerItem> containers = List.of(new ContainerItem(large, 2, new FixedContainerCostCalculator(10, large.getVolume(), "large", 0)),
				new ContainerItem(small, 3, new FixedContainerCostCalculator(5, small.getVolume(), "small", 0)));
		assertThat(generator.partition(boxes, 36, containers, 5, 10_000)).containsExactly(12, 12, 12);
	}

	/*
	 * Four cubes. The square container B ranks best, but the long container A must
	 * keep a layout of its own even with a limit of one layout:
	 *
	 *   A (4 x 1 x 1):  [A A A A]          B (2 x 2 x 1):  [A A]
	 *                                                      [A A]
	 */
	@Test
	void everyContainerKeepsALayout() {
		List<VirtualBoxLayout> layouts = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 4), List.of(container(4, 1, 1), container(2, 2, 1)), 1, () -> false);
		assertThat(layouts).extracting(VirtualBoxLayout::getBoundingBox).containsExactly(VirtualBoxBounds.of(2, 2, 1), VirtualBoxBounds.of(4, 1, 1));
	}

	/*
	 * Container 5 wide; a 4-wide grid leaves a 1-wide strip. If no box is narrower
	 * than 2, that strip is wasted and the axis counts as matched.
	 */
	@Test
	void leftoverNarrowerThanEveryBoxCountsAsMatched() {
		BoxItem boxes = item(2, 1, 1, 4);
		List<Container> containers = List.of(container(5, 2, 2));
		var exact = new GridVirtualBoxLayoutGenerator().generate(boxes, containers, 1, () -> false).get(0);
		var near = new GridVirtualBoxLayoutGenerator(2).generate(boxes, containers, 1, () -> false).get(0);
		assertThat(exact.getBoundingBox()).isEqualTo(VirtualBoxBounds.of(2, 2, 2));
		assertThat(near.getBoundingBox()).isEqualTo(VirtualBoxBounds.of(4, 2, 1));
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
