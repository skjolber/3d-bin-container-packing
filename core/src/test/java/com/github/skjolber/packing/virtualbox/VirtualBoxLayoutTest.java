package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

class VirtualBoxLayoutTest {
	/*
	 * Relative coordinates stay unchanged when validating a shared layout:
	 *
	 *       +---------+
	 *       |    A    |    z = 1
	 *       +---------+
	 *       |    A    |    z = 0
	 *       +---------+
	 *
	 * Geometry preparation must not mutate the reusable placements.
	 */
	@Test
	void retainsPhysicalPlacementsWithoutMutatingThemDuringPreparation() {
		BoxItem item = item(1, 1, 1, 2);
		BoxStackValue value = item.getBox().getStackValue(0);
		Placement lower = new Placement(item, value, -1, 0, 0, 0, false);
		Placement upper = new Placement(item, value, -1, 0, 0, 1, false);
		List<Placement> placements = new ArrayList<>(List.of(lower, upper));
		VirtualBoxLayout layout = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 2), placements);
		assertThat(layout.getPlacements()).isSameAs(placements);
		layout.prepare();
		layout.prepare();
		assertThat(layout.getPlacements().get(0)).isSameAs(lower);
		assertThat(layout.getPlacements().get(1)).isSameAs(upper);
		assertThat(lower.getAbsoluteZ()).isZero();
		assertThat(upper.getAbsoluteZ()).isEqualTo(1);
		for(Placement placement : placements) {
			assertThat(placement.getBoxItem()).isSameAs(item);
			assertThat(placement.getStackValue()).isSameAs(value);
			StackPlacementAssert.assertThat(placement).hasLoadWeight(0);
			StackPlacementAssert.assertThat(placement).isUnsupported().supportsNothing();
		}
		assertThat(VirtualBox.of(List.of(layout)).getWeight()).isEqualTo(2);
	}

	static BoxItem item(int x, int y, int z, int count) {
		return new BoxItem(Box.newBuilder().withSize(x, y, z).withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withWeight(1).build(), count);
	}

	static Container container(int x, int y, int z) {
		return Container.newBuilder().withSize(x, y, z).withMaxLoadWeight(100_000).build();
	}

	/*
	 * Six cubes become one filled rectangle, without enumerating permutations:
	 *
	 *       +-----+-----+-----+
	 *       |  A  |  A  |  A  |
	 *       +-----+-----+-----+
	 *       |  A  |  A  |  A  |
	 *       +-----+-----+-----+
	 */
	@Test
	void gridContainsEntireInventoryAndMatchesContainer() {
		BoxItem item = item(1, 1, 1, 6);
		var layouts = new GridVirtualBoxLayoutGenerator().generate(item, List.of(container(3, 2, 1)), 8, () -> false);
		assertThat(layouts).hasSize(1);
		assertThat(layouts.get(0).getBounds()).isEqualTo(new VirtualBoxBounds(3, 2, 1));
		assertThat(layouts.get(0).getPlacements()).hasSize(6).allMatch(p -> p.getBoxItem() == item);
		assertFilled(layouts);
		VirtualBox virtual = VirtualBox.of(layouts);
		assertThat(virtual.toBoxItem(12).getCount()).isEqualTo(1);
		assertThat(virtual.toBoxItem(12).getGlobalIndex()).isEqualTo(12);
		assertThat(virtual.getWeight()).isEqualTo(6);
		assertThat(item.getCount()).isEqualTo(6);
		assertThat(item.getBox().getStackValue(0).getBox()).isSameAs(item.getBox());
	}

	/*
	 * The alternatives consume the SAME six cubes:
	 *
	 *     [ A A A ]          [ A A ]
	 *     [ A A A ]    or    [ A A ]
	 *                        [ A A ]
	 *
	 * They are stack values of one count-one item, not two separate items.
	 */
	@Test
	void alternativesRemainExclusiveAndBounded() {
		BoxItem item = item(1, 1, 1, 6);
		var layouts = new GridVirtualBoxLayoutGenerator().generate(item, List.of(container(3, 3, 2)), 2, () -> false);
		assertThat(layouts).hasSize(2);
		BoxItem virtual = VirtualBox.of(layouts).toBoxItem(0);
		assertThat(virtual.getCount()).isEqualTo(1);
		assertThat(virtual.getBox().getStackValues()).hasSize(2);
		assertThat(virtual.getVolume()).isEqualTo(6);
		assertFilled(layouts);
	}

	/*
	 * Five cubes have only line grids; a 3 x 2 floor cannot hold a whole line.
	 * No count is rounded up and no sixth cube is invented.
	 */
	@Test
	void primeCountDoesNotInventBoxes() {
		assertThat(new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 5), List.of(container(3, 2, 1)), 8, () -> false)).isEmpty();
	}

	/*
	 * Two fixed 2 x 1 x 1 boxes fit a 4 x 1 x 1 line.
	 * Turning that line into 1 x 4 x 1 would rotate its contents illegally.
	 */
	@Test
	void doesNotInventRotationsAndHonorsLoadWeightLimits() {
		BoxItem item = item(2, 1, 1, 2);
		var generator = new GridVirtualBoxLayoutGenerator();
		assertThat(generator.generate(item, List.of(container(1, 4, 1)), 8, () -> false)).isEmpty();
		Container light = Container.newBuilder().withSize(4, 1, 1).withMaxLoadWeight(1).build();
		assertThat(generator.generate(item, List.of(light), 8, () -> false)).isEmpty();
	}

	/*
	 * Expired deadline / interrupt
	 *                      |
	 *                      v
	 *              no partial virtual box
	 */
	@Test
	void respectsDeadlineAndCancellation() {
		var generator = new GridVirtualBoxLayoutGenerator();
		BoxItem small = item(1, 1, 1, 2);
		try(var expired = PackagerInterruptSupplierBuilder.newBuilder().withDeadline(0).build()) {
			assertThat(generator.generate(small, List.of(container(2, 1, 1)), 8, expired::getAsBoolean)).isEmpty();
		}
		assertThat(generator.generate(small, List.of(container(2, 1, 1)), 8, () -> true)).isEmpty();
		AtomicInteger checks = new AtomicInteger();
		var grids = new GridVirtualBoxLayoutGenerator().generate(item(1, 1, 1, 1000), List.of(container(10, 10, 10)), 8,
				() -> checks.incrementAndGet() > 10);
		assertThat(grids).isEmpty();
	}

	/*
	 * Identical dimensions do not make A and B the same inventory.
	 */
	@Test
	void rejectsAlternativesWithDifferentOriginalInventories() {
		var generator = new GridVirtualBoxLayoutGenerator();
		var a = generator.generate(item(1, 1, 1, 2), List.of(container(2, 1, 1)), 1, () -> false);
		var b = generator.generate(item(1, 1, 1, 2), List.of(container(2, 1, 1)), 1, () -> false);
		assertThatThrownBy(() -> VirtualBox.of(List.of(a.get(0), b.get(0)))).isInstanceOf(IllegalArgumentException.class);
	}

	static void assertFilled(List<VirtualBoxLayout> layouts) {
		for(var layout : layouts) {
			long volume = 0;
			List<Placement> placements = new ArrayList<>();
			for(var child : layout.getPlacements()) {
				assertThat(placements).noneMatch(child::intersects);
				placements.add(child);
				volume += child.getStackValue().getVolume();
			}
			assertThat(volume).isEqualTo(layout.getBounds().getVolume());
		}
	}
}
