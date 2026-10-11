package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

/**
 * Support areas beyond the int range, as with fine-grained units (for example 1/10000 inch, see
 * GitHub issue #1158): contact areas must not overflow.
 *
 * <pre>
 *  z
 *  |
 *  40 +-------------------+
 *     |        top        |   200000 x 80000, rests on both bases
 *  30 +---------+---------+
 *     |  base   |  base   |   100000 x 80000 each: contact 8e9 per base
 *     |         |         |
 *   0 +---------+---------+---- x
 *     0      100000     200000
 * </pre>
 */
public class LargeUnitsSupportTest {

	private static final int SCALE = 2000;

	@Test
	public void supportedAreaBeyondIntRange() {
		Placement left = placement(100 * SCALE, 40 * SCALE, 30 * SCALE, 0);
		Placement right = placement(100 * SCALE, 40 * SCALE, 30 * SCALE, 100 * SCALE);
		BoxStackValue top = Box.newBuilder().withSize(200 * SCALE, 40 * SCALE, 10 * SCALE).withWeight(1).build().getStackValue(0);

		long expected = (long) (200 * SCALE) * (40 * SCALE);
		assertThat(AbstractPlacementControls.calculateAreaSupport(List.of(left, right), 0, 0, 30 * SCALE, top)).isEqualTo(expected);
		assertThat(new StackSupportIndex().calculateAreaSupport(List.of(left, right), 0, 0, 30 * SCALE, top)).isEqualTo(expected);
	}

	@Test
	public void fullSupportStacksOnTwoBoxes() {
		int dx = 200 * SCALE;
		int dy = 40 * SCALE;
		int h = 10 * SCALE;
		List<ContainerItem> containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("c").withSize(dx, dy, 4 * h).withEmptyWeight(0).withMaxLoadWeight(1000).build(), 2)
				.build();
		List<BoxItem> items = List.of(
				new BoxItem(Box.newBuilder().withId("base").withSize(dx / 2, dy, 3 * h).withWeight(2).build(), 2),
				new BoxItem(Box.newBuilder().withId("top").withSize(dx, dy, h).withWeight(1).build(), 1));

		try (PlainPackager packager = PlainPackager.newBuilder().withRequireFullSupport(true).build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(items)
					.withMaxContainerCount(2)
					.build();

			PackagerResultAssert.assertThat(result).isSuccess().hasContainerCount(1).hasStackSize(0, 3);
		}
	}

	private static Placement placement(int dx, int dy, int dz, int x) {
		Box box = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build();
		return new Placement(box.getStackValue(0), 0, x, 0, 0);
	}
}
