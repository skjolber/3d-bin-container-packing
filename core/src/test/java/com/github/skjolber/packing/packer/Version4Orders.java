package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;

/**
 * Random orders for comparing packagers with those of version 4 (module shadowed-v4 in legacy/v4, packages
 * {@code com.github.skjolber.packing.v4}): the same order is built with the classes of each version, and a result is summarized by its
 * containers and placements, so that the results of the two versions can be compared.
 * <p>
 * The orders use what both versions support: box items with or without rotation and with weights, several container types with
 * counts and load weight limits, and a maximum container count.
 */
public final class Version4Orders {

	/**
	 * @param rotation 0 for none, 1 for two dimensions, 2 for three dimensions
	 */
	public record BoxSpec(String id, int dx, int dy, int dz, int rotation, int weight, int count) {
	}

	public record ContainerSpec(String id, int dx, int dy, int dz, int maxLoadWeight, int count) {
	}

	public record Scenario(long seed, List<ContainerSpec> containers, List<BoxSpec> boxes, int maxContainerCount) {

		public List<BoxItem> boxItems() {
			List<BoxItem> items = new ArrayList<>(boxes.size());
			for (BoxSpec b : boxes) {
				Box.Builder builder = Box.newBuilder().withId(b.id()).withSize(b.dx(), b.dy(), b.dz()).withWeight(b.weight());
				switch (b.rotation()) {
					case 0 -> builder.withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build());
					case 1 -> builder.withRotate2D();
					default -> builder.withRotate3D();
				}
				items.add(new BoxItem(builder.build(), b.count()));
			}
			return items;
		}

		public List<ContainerItem> containerItems() {
			List<ContainerItem> items = new ArrayList<>(containers.size());
			for (ContainerSpec c : containers) {
				Container container = Container.newBuilder().withId(c.id()).withSize(c.dx(), c.dy(), c.dz()).withMaxLoadWeight(c.maxLoadWeight()).withEmptyWeight(0).build();
				items.add(new ContainerItem(container, c.count()));
			}
			return items;
		}

		public List<com.github.skjolber.packing.v4.api.BoxItem> version4BoxItems() {
			List<com.github.skjolber.packing.v4.api.BoxItem> items = new ArrayList<>(boxes.size());
			for (BoxSpec b : boxes) {
				com.github.skjolber.packing.v4.api.Box.Builder builder = com.github.skjolber.packing.v4.api.Box.newBuilder()
						.withId(b.id())
						.withSize(b.dx(), b.dy(), b.dz())
						.withWeight(b.weight());
				switch (b.rotation()) {
					case 0 -> builder.withRotation(com.github.skjolber.packing.v4.api.Rotation.newBuilder().withBottomAtZeroDegrees().build());
					case 1 -> builder.withRotate2D();
					default -> builder.withRotate3D();
				}
				items.add(new com.github.skjolber.packing.v4.api.BoxItem(builder.build(), b.count()));
			}
			return items;
		}

		public List<com.github.skjolber.packing.v4.api.ContainerItem> version4ContainerItems() {
			List<com.github.skjolber.packing.v4.api.ContainerItem> items = new ArrayList<>(containers.size());
			for (ContainerSpec c : containers) {
				com.github.skjolber.packing.v4.api.Container container = com.github.skjolber.packing.v4.api.Container.newBuilder()
						.withId(c.id())
						.withSize(c.dx(), c.dy(), c.dz())
						.withMaxLoadWeight(c.maxLoadWeight())
						.withEmptyWeight(0)
						.build();
				items.add(new com.github.skjolber.packing.v4.api.ContainerItem(container, c.count()));
			}
			return items;
		}
	}

	/**
	 * A result: whether all boxes were packed, and the containers with their placements in order, one line each.
	 *
	 * @param success whether all boxes were packed
	 * @param containerCount the number of containers
	 * @param lines a line per container ({@code container <id>}) followed by a line per placement ({@code <box id> <x>,<y>,<z> <dx>x<dy>x<dz>})
	 */
	public record Summary(boolean success, int containerCount, List<String> lines) {

		/**
		 * @return positive if this result is better: all boxes packed, then fewer containers
		 */
		public int compareTo(Summary other) {
			if(success != other.success) {
				return success ? 1 : -1;
			}
			return Integer.compare(other.containerCount, containerCount);
		}
	}

	private Version4Orders() {
	}

	/**
	 * @param seed the seed
	 * @return one to three container types of 5 to 24 units, a third with a load weight limit, and one to ten box items of one to four
	 *         boxes of 1 to 8 units, which may be rotated in two or three dimensions
	 */
	public static Scenario random(long seed) {
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
		return new Scenario(seed, containers, boxes, 1 + random.nextInt(4));
	}

	/**
	 * @param packager a packager of 5.x
	 * @param scenario the order
	 * @return the result of packing the order
	 */
	public static PackagerResult pack(Packager<?> packager, Scenario scenario) {
		return packager.newResultBuilder()
				.withContainerItems(scenario.containerItems())
				.withBoxItems(scenario.boxItems())
				.withMaxContainerCount(scenario.maxContainerCount())
				.build();
	}

	/**
	 * @param packager a packager of version 4
	 * @param scenario the order
	 * @return the result of packing the order
	 */
	public static com.github.skjolber.packing.v4.api.PackagerResult packVersion4(com.github.skjolber.packing.v4.api.Packager<?> packager, Scenario scenario) {
		return packager.newResultBuilder()
				.withContainerItems(scenario.version4ContainerItems())
				.withBoxItems(scenario.version4BoxItems())
				.withMaxContainerCount(scenario.maxContainerCount())
				.build();
	}

	public static Summary summary(PackagerResult result) {
		List<String> lines = new ArrayList<>();
		for (Container container : result.getContainers()) {
			lines.add("container " + container.getId());
			for (Placement p : container.getStack().getPlacements()) {
				lines.add(p.getStackValue().getBox().getId() + " " + p.getAbsoluteX() + "," + p.getAbsoluteY() + "," + p.getAbsoluteZ() + " " + p.getStackValue().getDx() + "x"
						+ p.getStackValue().getDy() + "x" + p.getStackValue().getDz());
			}
		}
		return new Summary(result.isSuccess(), result.getContainers().size(), lines);
	}

	public static Summary summary(com.github.skjolber.packing.v4.api.PackagerResult result) {
		List<String> lines = new ArrayList<>();
		for (com.github.skjolber.packing.v4.api.Container container : result.getContainers()) {
			lines.add("container " + container.getId());
			for (com.github.skjolber.packing.v4.api.Placement p : container.getStack().getPlacements()) {
				lines.add(p.getStackValue().getBox().getId() + " " + p.getAbsoluteX() + "," + p.getAbsoluteY() + "," + p.getAbsoluteZ() + " " + p.getStackValue().getDx() + "x"
						+ p.getStackValue().getDy() + "x" + p.getStackValue().getDz());
			}
		}
		return new Summary(result.isSuccess(), result.getContainers().size(), lines);
	}
}
