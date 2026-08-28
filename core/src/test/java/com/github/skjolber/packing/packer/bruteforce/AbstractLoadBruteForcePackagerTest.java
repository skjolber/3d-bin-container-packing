package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;

abstract class AbstractLoadBruteForcePackagerTest extends AbstractBruteForcePackagerTest {

	@Test
	void packsBoxesWithoutLoadConstraints() {
		/*
		 * +-----+
		 * |  B  |
		 * +-----+
		 * |  B  |
		 * +-----+
		 */
		Box box = box("box", 10, 10, 1, 1);
		PackagerResult result = pack(container(2), 1, 1, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
		assertThat(result.getContainers().get(0).getStack()).hasSize(2);
	}

	@Test
	void rejectsStackWhichExceedsMaxLoadWeight() {
		/*
		 * +-------+  weight 10
		 * | upper |
		 * +-------+
		 * | lower |  max load 5 -> rejected
		 * +-------+
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(10)
				.withMaxLoadWeight(5).build();
		PackagerResult result = pack(container(2), 2, 2, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void acceptsExactMaxLoadWeightBoundary() {
		/*
		 * +-------+  weight 5
		 * | upper |
		 * +-------+
		 * | lower |  max load 5 -> accepted
		 * +-------+
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(5)
				.withMaxLoadWeight(5).build();
		PackagerResult result = pack(container(2), 1, 1, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
	}

	@Test
	void rejectsStackWhichExceedsMaxLoadPressure() {
		/*
		 * +-----+  weight 11
		 * | top |
		 * +-----+  area 10, pressure 1.1
		 * | base|  max pressure 1 -> rejected
		 * +-----+
		 */
		Box box = Box.newBuilder().withId("box").withSize(2, 5, 1).withWeight(11)
				.withMaxLoadPressure(1).build();
		PackagerResult result = pack(container(2, 5, 2), 2, 2, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void acceptsExactMaxLoadPressureBoundary() {
		/*
		 * +-----+  weight 10
		 * | top |
		 * +-----+  area 10, pressure 1.0
		 * | base|  max pressure 1 -> accepted
		 * +-----+
		 */
		Box box = Box.newBuilder().withId("box").withSize(2, 5, 1).withWeight(10)
				.withMaxLoadPressure(1).build();
		PackagerResult result = pack(container(2, 5, 2), 1, 1, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
	}

	@Test
	void enforcesMaxLoadBoxCountAcrossLevels() {
		/*
		 * +---+ #2
		 * +---+ #1
		 * +---+ max one box above -> third needs another container
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadBoxCount(1).build();
		PackagerResult result = pack(container(3), 2, 2, new BoxItem(box, 3));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
		assertThat(result.getContainers().get(0).getStack()).hasSize(2);
	}

	@Test
	void acceptsExactMaxLoadBoxCountBoundary() {
		/*
		 * +---+ #2
		 * +---+ #1
		 * +---+ max two boxes above -> accepted
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadBoxCount(2).build();
		PackagerResult result = pack(container(3), 1, 1, new BoxItem(box, 3));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
	}

	@Test
	void rejectsDifferentTypesForIdenticalOnlyStacking() {
		/*
		 * +-----+
		 * |  B  | different type
		 * +-----+
		 * |  A  | identical-only -> rejected
		 * +-----+
		 */
		Box first = Box.newBuilder().withId("first").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadIdenticalBoxCount(1).build();
		Box second = Box.newBuilder().withId("second").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadIdenticalBoxCount(1).build();
		PackagerResult result = pack(container(2), 2, 2, new BoxItem(first), new BoxItem(second));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
	}

	@Test
	void acceptsIdenticalTypeWithinCountLimit() {
		/*
		 * +---+ A
		 * +---+ A
		 * +---+ A  two identical boxes above -> accepted
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadIdenticalBoxCount(2).build();
		PackagerResult result = pack(container(3), 1, 1, new BoxItem(box, 3));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(1);
	}

	@Test
	void recordsLoadAcrossFourLevels() {
		/*
		 * +---+ load 0
		 * +---+ load 3
		 * +---+ load 6
		 * +---+ load 9
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(3)
				.withMaxLoadWeight(20).build();
		PackagerResult result = pack(container(4), 1, 1, new BoxItem(box, 4));
		assertThat(placementsByHeight(result)).extracting(Placement::getLoadWeight)
				.containsExactly(9L, 6L, 3L, 0L);
	}

	@Test
	void distributesBridgeLoadBetweenTwoSupporters() {
		/*
		 * +-----------------+ top, weight 10
		 * |       TOP       |
		 * +--------+--------+
		 * |  LEFT  | RIGHT  | each carries load 5
		 * +--------+--------+
		 */
		Box support = Box.newBuilder().withId("support").withSize(5, 10, 1).withWeight(1)
				.withMaxLoadWeight(5).build();
		Box top = Box.newBuilder().withId("top").withSize(10, 10, 1).withWeight(10)
				.withMaxLoadWeight(0).build();
		PackagerResult result = pack(container(10, 10, 2), 1, 1,
				new BoxItem(support, 2), new BoxItem(top));
		assertThat(result.isSuccess()).isTrue();
		List<Placement> placements = result.getContainers().get(0).getStack().getPlacements();
		assertThat(placements).filteredOn(p -> p.getBox().getId().equals("support"))
				.extracting(Placement::getLoadWeight).containsOnly(5L);
		assertThat(placements).filteredOn(p -> p.getBox().getId().equals("top"))
				.singleElement().extracting(Placement::getLoadWeight).isEqualTo(0L);
	}

	@Test
	void sideBySideBoxesDoNotLoadEachOther() {
		/*
		 * +-------+-------+
		 * |   A   |   B   | same floor level: no carried load
		 * +-------+-------+
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(7)
				.withMaxLoadWeight(0).build();
		PackagerResult result = pack(container(20, 10, 1), 1, 1, new BoxItem(box, 2));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers().get(0).getStack().getPlacements())
				.extracting(Placement::getLoadWeight).containsOnly(0L);
	}

	@Test
	void findsValidOrderingAfterRejectingOverloadedPermutation() {
		/*
		 * Rejected candidate:       examples of accepted permutations:
		 * +-------+ heavy(6)        +-------+ light(4)  +-------+ base(max 5)
		 * +-------+ base(max 5)     +-------+ base(5)   +-------+ light
		 * +-------+ light           +-------+ heavy     +-------+ heavy
		 */
		Box base = Box.newBuilder().withId("base").withSize(10, 10, 1).withWeight(1)
				.withMaxLoadWeight(5).build();
		Box heavy = box("heavy", 10, 10, 1, 6);
		Box light = box("light", 10, 10, 1, 4);
		PackagerResult result = pack(container(3), 1, 1,
				new BoxItem(base), new BoxItem(heavy), new BoxItem(light));
		List<Placement> placements = placementsByHeight(result);
		assertThat(placements).extracting(p -> p.getBox().getId())
				.containsExactlyInAnyOrder("heavy", "base", "light");
		Placement basePlacement = placements.stream()
				.filter(p -> p.getBox().getId().equals("base")).findFirst().orElseThrow();
		assertThat(basePlacement.getLoadWeight()).isLessThanOrEqualTo(5L);
		long weightAbove = 0;
		for(int i = placements.size() - 1; i >= 0; i--) {
			Placement placement = placements.get(i);
			assertThat(placement.getLoadWeight()).isEqualTo(weightAbove);
			weightAbove += placement.getWeight();
		}
	}

	@Test
	void clearsLoadStateBetweenContainers() {
		/*
		 * container 1       container 2
		 * +-------+          +-------+
		 * |   B   |          |   B   | both bases carry exactly one box
		 * +-------+          +-------+
		 * |   B   |          |   B   |
		 * +-------+          +-------+
		 */
		Box box = Box.newBuilder().withId("box").withSize(10, 10, 1).withWeight(2)
				.withMaxLoadWeight(2).build();
		PackagerResult result = pack(container(2), 2, 2, new BoxItem(box, 4));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(2);
		for (Container packed : result.getContainers()) {
			assertThat(packed.getStack().getPlacements().stream()
					.sorted(Comparator.comparingInt(Placement::getAbsoluteZ))
					.map(Placement::getLoadWeight)).containsExactly(2L, 0L);
		}
	}

	protected PackagerResult pack(Container container, int containerCount, int maxContainerCount,
			BoxItem... items) {
		try(AbstractBruteForcePackager packager = createPackager()) {
			return packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, containerCount))
					.withMaxContainerCount(maxContainerCount)
					.withBoxItems(items)
					.build();
		}
	}

	private static List<Placement> placementsByHeight(PackagerResult result) {
		assertThat(result.isSuccess()).isTrue();
		return result.getContainers().get(0).getStack().getPlacements().stream()
				.sorted(Comparator.comparingInt(Placement::getAbsoluteZ)).toList();
	}

	private static Box box(String id, int dx, int dy, int dz, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).build();
	}

	protected static Container container(int dz) {
		return container(10, 10, dz);
	}

	protected static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}
}
