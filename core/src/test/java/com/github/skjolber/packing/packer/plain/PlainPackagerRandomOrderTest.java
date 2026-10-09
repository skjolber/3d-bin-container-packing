package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;

/**
 * Packs random orders, i.e. a few container types with counts and load weight limits, and a few box items which may be rotated.
 */

public class PlainPackagerRandomOrderTest {

	/**
	 * @param rotation 0 for none, 1 for two dimensions, 2 for three dimensions
	 */
	protected record BoxSpec(String id, int dx, int dy, int dz, int rotation, int weight, int count) {
	}

	protected record ContainerSpec(String id, int dx, int dy, int dz, int maxLoadWeight, int count) {
	}

	protected record Order(List<ContainerSpec> containers, List<BoxSpec> boxes, int maxContainerCount) {
	}

	/**
	 * @return one to three container types of 5 to 24 units, a third with a load weight limit, and one to ten box items of one to four
	 *         boxes of 1 to 8 units, which may be rotated in two or three dimensions
	 */
	protected static Order random(long seed) {
		Random random = new Random(seed);
		List<ContainerSpec> containers = new ArrayList<>();
		int types = 1 + random.nextInt(3);
		for (int i = 0; i < types; i++) {
			int maxLoadWeight = random.nextInt(3) == 0 ? 20 + random.nextInt(80) : 100_000;
			containers.add(new ContainerSpec("c" + i, 5 + random.nextInt(20), 5 + random.nextInt(20), 5 + random.nextInt(20), maxLoadWeight, 1 + random.nextInt(3)));
		}
		List<BoxSpec> boxes = new ArrayList<>();
		int items = 1 + random.nextInt(10);
		for (int i = 0; i < items; i++) {
			boxes.add(new BoxSpec("b" + i, 1 + random.nextInt(8), 1 + random.nextInt(8), 1 + random.nextInt(8), random.nextInt(3), 1 + random.nextInt(10), 1 + random.nextInt(4)));
		}
		return new Order(containers, boxes, 1 + random.nextInt(4));
	}

	protected static PackagerResult pack(PlainPackager packager, Order order) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (BoxSpec b : order.boxes()) {
			Box.Builder builder = Box.newBuilder().withId(b.id()).withSize(b.dx(), b.dy(), b.dz()).withWeight(b.weight());
			switch (b.rotation()) {
				case 0 -> builder.withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build());
				case 1 -> builder.withRotate2D();
				default -> builder.withRotate3D();
			}
			boxItems.add(new BoxItem(builder.build(), b.count()));
		}
		List<ContainerItem> containerItems = new ArrayList<>();
		for (ContainerSpec c : order.containers()) {
			Container container = Container.newBuilder().withId(c.id()).withSize(c.dx(), c.dy(), c.dz()).withMaxLoadWeight(c.maxLoadWeight()).withEmptyWeight(0).build();
			containerItems.add(new ContainerItem(container, c.count()));
		}
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(order.maxContainerCount()).build();
	}

	/**
	 * Check that all placements are within their container, and that none of them overlap.
	 */
	protected static void assertValid(PackagerResult result) {
		for (Container container : result.getContainers()) {
			List<Placement> placements = container.getStack().getPlacements();
			for (int i = 0; i < placements.size(); i++) {
				Placement a = placements.get(i);
				assertThat(a.getAbsoluteX()).isGreaterThanOrEqualTo(0);
				assertThat(a.getAbsoluteY()).isGreaterThanOrEqualTo(0);
				assertThat(a.getAbsoluteZ()).isGreaterThanOrEqualTo(0);
				assertThat(a.getAbsoluteEndX()).isLessThan(container.getLoadDx());
				assertThat(a.getAbsoluteEndY()).isLessThan(container.getLoadDy());
				assertThat(a.getAbsoluteEndZ()).isLessThan(container.getLoadDz());
				for (int j = i + 1; j < placements.size(); j++) {
					assertThat(a.intersects3D(placements.get(j))).as("%s and %s overlap in %s", a, placements.get(j), container.getId()).isFalse();
				}
			}
		}
	}

	@Test
	void testRandomOrdersPackWithoutException() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			for (long seed = 0; seed < 300; seed++) {
				PackagerResult result;
				try {
					result = pack(packager, random(seed));
				} catch (RuntimeException e) {
					throw new AssertionError("Seed " + seed + " failed", e);
				}
				try {
					assertValid(result);
				} catch (AssertionError e) {
					throw new AssertionError("Seed " + seed + " is invalid", e);
				}
			}
		} finally {
			packager.close();
		}
	}
}
