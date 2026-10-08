package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsBestBoxItemComparator;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsOnGroundLevelPlacementComparator;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsOnGroundLevelPointControls;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

public class PlainPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 1);
		
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder()
					.withContainerItem(containerItem)
					.withBoxItems(products)
					.build();
			// <figure>
			//   z   /-------|           y   z                         y                         z
			//      /   B   /|               1 +-------+-------+       2 +-------+               1 +-------+-------+
			//   | /-------/-------|   /       |       |       |         |       |                 |       |       |
			//   |/       /       /|  /        |   A   |   C   |         |   B   |                 |   C   |   B   |
			// 1 |-------|-------| | / 2       |       |       |         |       |                 |       |       |
			//   |       |       | |/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
			//   |   A   |   C   | | 1         0       1       2   x     |       |       |         0       1       2   y
			//   |       |       |/                                      |   A   |   C   |
			// 0 |-------|-------|-- x                                   |       |       |
			//   0       1       2                                     0 +-------+-------+
			//                                                           0       1       2   x
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingSquaresOnSquareTwoLevels() {
		
		int factor = 100;

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2 * factor, 2 * factor, 2 * factor).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 1);
		
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(factor, factor, factor).withWeight(1).build(), 8));
	
			PackagerResult build = packager.newResultBuilder()
					.withContainerItem(containerItem)
					.withBoxItems(products)
					.build();
			// <figure>
			//                  /------------------------/-------------------------|       z
			//                 /                        /                         /|       200 +---------------------------------+--------------------------------+
			//                /                        /                         / |           |                                 |                                |
			//               /           A            /            A            /  |           |                                 |                                |
			//              /                        /                         /   |           |                                 |                                |
			//             /                        /                         /    |           |                                 |                                |
			//            /                        /                         /     |           |                                 |                                |
			//           /------------------------/-------------------------|      |           |                                 |                                |
			//          /                        /                         /|      |           |                                 |                                |
			//     z   /                        /                         / |      |           |                A                |               A                |
			//        /                        /                         /  |      |           |                                 |                                |
			//     | /                        /                         /   |      |           |                                 |                                |
			//     |/                        /                         /    |      |           |                                 |                                |
			// 200 |------------------------|-------------------------|     |     /|           |                                 |                                |
			//     |                        |                         |     |    / |           |                                 |                                |
			//     |                        |                         |     |   /  |           |                                 |                                |
			//     |                        |                         |     |  /   |           |                                 |                                |
			//     |                        |                         |     | /    |           |                                 |                                |
			//     |                        |                         |     |/     |       100 +---------------------------------+--------------------------------+
			//     |           A            |            A            |     |      |           |                                 |                                |
			//     |                        |                         |    /|      |           |                                 |                                |
			//     |                        |                         |   / |      |   y       |                                 |                                |
			//     |                        |                         |  /  |  A   |           |                                 |                                |
			//     |                        |                         | /   |      | /         |                                 |                                |
			//     |                        |                         |/    |      |/          |                                 |                                |
			// 100 |------------------------|-------------------------|     |      | 200       |                                 |                                |
			//     |                        |                         |     |     /            |                A                |               A                |
			//     |                        |                         |     |    /             |                                 |                                |
			//     |                        |                         |     |   /              |                                 |                                |
			//     |                        |                         |     |  /               |                                 |                                |
			//     |                        |                         |     | /                |                                 |                                |
			//     |           A            |            A            |     |/                 |                                 |                                |
			//     |                        |                         |     | 100              |                                 |                                |
			//     |                        |                         |    /                   |                                 |                                |
			//     |                        |                         |   /                    |                                 |                                |
			//     |                        |                         |  /                   0 +---------------------------------+--------------------------------+
			//     |                        |                         | /                      0                                 100                              200   x
			//     |                        |                         |/
			//   0 |------------------------|-------------------------|-- x
			//     0                       100                       200
			//
			// y                                                                                z
			// 200 +---------------------------------+--------------------------------+         200 +---------------------------------+--------------------------------+
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                A                |               A                |             |                A                |               A                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			// 100 +---------------------------------+--------------------------------+         100 +---------------------------------+--------------------------------+
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                A                |               A                |             |                A                |               A                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//     |                                 |                                |             |                                 |                                |
			//   0 +---------------------------------+--------------------------------+           0 +---------------------------------+--------------------------------+
			//     0                                 100                              200   x       0                                 100                              200   y
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}

	}


	@Test
	void testStackingRectangles() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z   /---------------/-------|   y   z                                 y                                 z
			//      /       B       /       /|       1 +---------------+-------+       2 +---------------+-------+       1 +---------------+
			//   | /---------------/       / | /       |               |       |         |               |       |         |               |
			//   |/               /       /  |/        |       A       |   C   |         |       B       |       |         |       C       |
			// 1 |---------------|-------|   | 2       |               |       |         |               |       |         |               |
			//   |               |       |  /        0 +---------------+-------+       1 +---------------+   C   |       0 +---------------+
			//   |       A       |   C   | / 1         0               2       3   x     |               |       |         0       1       2   y
			//   |               |       |/                                              |       A       |       |
			// 0 |---------------|-------|-- x                                           |               |       |
			//   0               2       3                                             0 +---------------+-------+
			//                                                                           0               2       3   x
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingSquaresAndRectangle() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 10, 10).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//    z                                             z
			//                                                  10 +------------------------------------+
			//    | /-------------------------------|              |                                    |
			//    |/                               /|              |                                    |
			// 10 |-------------------------------| |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |              |                                    |
			//    |                               | |---|          |                                    |
			//    |                               | |  /|          |                                    |
			//    |                               | |-| |          |                                    |
			//    |                               | |/| |          |                                    |
			//    |                               | | | |          |                                    |
			//    |                               | | | |        5 |                 A                  |
			//    |               A               | | | |          |                                    |
			//    |                               | | | |          |                                    |
			//    |                               | | | |          |                                    |
			//    |                               | | |C|          |                                    |
			//    |                               | | | |          |                                    |
			//    |                               | |B| |          |                                    |
			//    |                               | | | |   y      |                                    |
			//    |                               | | | |          |                                    |
			//    |                               | | | | /        |                                    |
			//    |                               | | | |/         |                                    |
			//    |                               | | | | 3        |                                    |
			//    |                               | | |/           |                                    |
			//    |                               | | | 2          |                                    |
			//    |                               | |/             |                                    |
			//    |                               | | 1            |                                    |
			//    |                               |/               |                                    |
			//  0 |-------------------------------|-- x            |                                    |
			//    0                               5                |                                    |
			//                                                   0 +------------------------------------+
			//                                                     0                                    5   x
			//
			// y                                                 z
			// 3 +---------------------------------------+       10 +------+
			//   |                                       |          |      |
			//   |                   C                   |          |      |
			//   |                                       |          |      |
			// 2 +---------------------------------------+          |      |
			//   |                                       |          |      |
			//   |                   B                   |          |      |
			//   |                                       |          |      |
			// 1 +---------------------------------------+          |      |
			//   |                                       |          |      |
			//   |                   A                   |          |      |
			//   |                                       |          |      |
			// 0 +---------------------------------------+          |      |
			//   0                                       5   x      |      |
			//                                                      |      |
			//                                                      |      |
			//                                                      |      |
			//                                                      |      |
			//                                                    5 |  A   +-------+------+
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |   B   |  C   |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                      |      |       |      |
			//                                                    0 +------+-------+------+
			//                                                      0      1       2      3   y
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingDecreasingRectangles() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z                                                         z
			//                                                             1 +-----------------------+---------------+-------+
			//   | /-----------------------/---------------/-------|   y     |                       |               |       |
			//   |/                       /               /       /|         |           A           |       B       |   C   |
			// 1 |-----------------------|---------------|-------| | /       |                       |               |       |
			//   |                       |               |       | |/      0 +-----------------------+---------------+-------+
			//   |           A           |       B       |   C   | | 1       0                       3               5       6   x
			//   |                       |               |       |/
			// 0 |-----------------------|---------------|-------|-- x
			//   0                       3               5       6
			//
			// y                                                         z
			// 1 +-----------------------+---------------+-------+       1 +-------+
			//   |                       |               |       |         |       |
			//   |           A           |       B       |   C   |         |   C   |
			//   |                       |               |       |         |       |
			// 0 +-----------------------+---------------+-------+       0 +-------+
			//   0                       3               5       6   x     0       1   y
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingRectanglesTwoLevels() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z   /-------/-------/-------|       z                                 y                                 z
			//      /   A   /   B   /   C   /|       2 +-------+-------+-------+       2 +-------+-------+-------+       2 +-------+-------+
			//   | /-------/-------/-------| |         |       |       |       |         |       |       |       |         |       |       |
			//   |/       /       /       /| |         |       |       |       |         |   A   |   B   |   C   |         |       |       |
			// 2 |-------|-------|-------| | |   y     |       |       |       |         |       |       |       |         |       |       |
			//   |       |       |       | | |         |   A   |   B   |   C   |       1 +-------+-------+-------+         |   C   |   C   |
			//   |       |       |       | | | /       |       |       |       |         |       |       |       |         |       |       |
			//   |       |       |       | | |/        |       |       |       |         |   A   |   B   |   C   |         |       |       |
			//   |   A   |   B   |   C   | | | 2       |       |       |       |         |       |       |       |         |       |       |
			//   |       |       |       | |/        0 +-------+-------+-------+       0 +-------+-------+-------+       0 +-------+-------+
			//   |       |       |       | | 1         0       1       2       3   x     0       1       2       3   x     0       1       2   y
			//   |       |       |       |/
			// 0 |-------|-------|-------|-- x
			//   0       1       2       3
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}

	}

	@Test
	void testStackingRectanglesThreeLevels() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 3));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z   /---------------/-------|       z                                 y                                 z
			//      /       A       /       /|       2 +---------------+-------+       2 +---------------+-------+       2 +---------------+
			//   | /---------------/       / |         |               |       |         |               |       |         |               |
			//   |/               /       /  |         |               |       |         |       A       |       |         |               |
			// 2 |---------------|-------|   |   y     |               |       |         |               |       |         |               |
			//   |               |       |   |         |       A       |   A   |       1 +---------------+   A   |         |       A       |
			//   |               |       |   | /       |               |       |         |               |       |         |               |
			//   |               |       |   |/        |               |       |         |       A       |       |         |               |
			//   |       A       |   A   |   | 2       |               |       |         |               |       |         |               |
			//   |               |       |  /        0 +---------------+-------+       0 +---------------+-------+       0 +---------------+
			//   |               |       | / 1         0               2       3   x     0               2       3   x     0       1       2   y
			//   |               |       |/
			// 0 |---------------|-------|-- x
			//   0               2       3
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingNotPossible() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				// capacity is 3*2*3 = 18
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			assertThat(build.getContainers()).isEmpty();
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingMultipleContainersSingleContainerResult() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(1, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("3").withEmptyWeight(1).withSize(1, 3, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("4").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainers(containers, 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//         /-------|   y   z                 y                 z
			//        /   C   /|       1 +-------+       3 +-------+       1 +-------+-------+-------+
			//   z   /-------| | /       |       |         |       |         |       |       |       |
			//      /   B   /| |/        |   A   |         |   C   |         |   A   |   B   |   C   |
			//   | /-------| | | 3       |       |         |       |         |       |       |       |
			//   |/       /| |/        0 +-------+       2 +-------+       0 +-------+-------+-------+
			// 1 |-------| | | 2         0       1   x     |       |         0       1       2       3   y
			//   |       | |/                              |   B   |
			//   |   A   | | 1                             |       |
			//   |       |/                              1 +-------+
			// 0 |-------|-- x                             |       |
			//   0       1                                 |   A   |
			//                                             |       |
			//                                           0 +-------+
			//                                             0       1   x
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
	
			Container fits = build.get(0);
			assertEquals(fits.getVolume(), containers.get(2).getVolume());
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingMultipleContainersMultiContainerResult() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(1, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("3").withEmptyWeight(1).withSize(1, 3, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("4").withEmptyWeight(1).withSize(1, 4, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("5").withEmptyWeight(1).withSize(1, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("6").withEmptyWeight(1).withSize(1, 6, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("7").withEmptyWeight(1).withSize(1, 7, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainers(containers)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 3));
			products.add(new BoxItem(Box.newBuilder().withDescription("D").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 4));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(5).withBoxItems(products).build();
			// <figure>
			// container 1 of 2: 7
			//                 /-------|   y   z                 y                 z
			//                /   D   /|       1 +-------+       7 +-------+       1 +-------+-------+-------+-------+-------+-------+-------+
			//               /-------| | /       |       |         |       |         |       |       |       |       |       |       |       |
			//              /   C   /| |/        |   A   |         |   D   |         |   A   |   B   |   B   |   C   |   C   |   C   |   D   |
			//             /-------| | | 7       |       |         |       |         |       |       |       |       |       |       |       |
			//            /   C   /| |/        0 +-------+       6 +-------+       0 +-------+-------+-------+-------+-------+-------+-------+
			//           /-------| | | 6         0       1   x     |       |         0       1       2       3       4       5       6       7   y
			//          /   C   /| |/                              |   C   |
			//         /-------| | | 5                             |       |
			//        /   B   /| |/                              5 +-------+
			//   z   /-------| | | 4                               |       |
			//      /   B   /| |/                                  |   C   |
			//   | /-------| | | 3                                 |       |
			//   |/       /| |/                                  4 +-------+
			// 1 |-------| | | 2                                   |       |
			//   |       | |/                                      |   C   |
			//   |   A   | | 1                                     |       |
			//   |       |/                                      3 +-------+
			// 0 |-------|-- x                                     |       |
			//   0       1                                         |   B   |
			//                                                     |       |
			//                                                   2 +-------+
			//                                                     |       |
			//                                                     |   B   |
			//                                                     |       |
			//                                                   1 +-------+
			//                                                     |       |
			//                                                     |   A   |
			//                                                     |       |
			//                                                   0 +-------+
			//                                                     0       1   x
			//
			// container 2 of 2: 3
			//         /-------|   y   z                 y                 z
			//        /   D   /|       1 +-------+       3 +-------+       1 +-------+-------+-------+
			//   z   /-------| | /       |       |         |       |         |       |       |       |
			//      /   D   /| |/        |   D   |         |   D   |         |   D   |   D   |   D   |
			//   | /-------| | | 3       |       |         |       |         |       |       |       |
			//   |/       /| |/        0 +-------+       2 +-------+       0 +-------+-------+-------+
			// 1 |-------| | | 2         0       1   x     |       |         0       1       2       3   y
			//   |       | |/                              |   D   |
			//   |   D   | | 1                             |       |
			//   |       |/                              1 +-------+
			// 0 |-------|-- x                             |       |
			//   0       1                                 |   D   |
			//                                             |       |
			//                                           0 +-------+
			//                                             0       1   x
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
	
			assertEquals(result.size(), 2);
			assertEquals(result.get(0).getVolume(), 7);
			assertEquals(result.get(1).getVolume(), 3);
		} finally {
			packager.close();
		}
	}

	@Test
	void issue440() {
		Container build = Container.newBuilder()
				.withDescription("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		PlainPackager packager = PlainPackager.newBuilder()
				.build();

		try {
			for (int i = 1; i <= 1; i++) {
				int boxCountPerStackableItem = i;
	
				List<ContainerItem> containerItems = ContainerItem
						.newListBuilder()
						.withContainer(build, i + 2)
						.build();
	
				List<BoxItem> stackableItems = Arrays.asList(
						createStackableItem("1", 1200, 750, 2280, 285, boxCountPerStackableItem),
						createStackableItem("2", 1200, 450, 2280, 155, boxCountPerStackableItem),
						createStackableItem("3", 360, 360, 570, 20, boxCountPerStackableItem),
						createStackableItem("4", 2250, 1200, 2250, 900, boxCountPerStackableItem),
						createStackableItem("5", 1140, 750, 1450, 395, boxCountPerStackableItem),
						createStackableItem("6", 1130, 1500, 3100, 800, boxCountPerStackableItem),
						createStackableItem("7", 800, 490, 1140, 156, boxCountPerStackableItem),
						createStackableItem("8", 800, 2100, 1200, 135, boxCountPerStackableItem),
						createStackableItem("9", 1120, 1700, 2120, 160, boxCountPerStackableItem),
						createStackableItem("10", 1200, 1050, 2280, 390, boxCountPerStackableItem));
	
				PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(stackableItems).build();
				// <figure>
				//                  /---/--|-|                 z
				//                 /   /  /|/|                 6810              +----+
				//                / 1 /  / | |  /-----|        6470 +------------+    |
				//      z        /   /  /  | | /     /|             |            |    |
				//          /-------/  /   | |/     / |             |            |    |
				//      |  /       /  /    | /     /  |             |            |    |
				//      | /       /  /     |/     /   |             |            |    |
				// 6810 |/       |--|      |     /    |             |            | 2  |
				// 6470 |--------|  |      |    /     |        5380 |     8      |    |
				//      |        |  |     /----|      |             |            |    |
				//      |        |  |    /-|   |      |             |            |    |
				//      |        |2 |   / /|   |      |             |            |    |
				//      |   8    |  |  / / | 1 |     /              |            |    |
				//      |        |  | / /  |   |    /          4530 |            +----++-----+
				//      |        |  |/ /   |   |   /--|        4370 +------------+----++     |
				// 4530 |        |--| /  /--|  |  /  /|   y         |                  |     |
				// 4370 |--------|---|  /  /|  | /  / |             |                  |  1  |
				//      |            | /  / |  ||  /  | /           |                  |     |
				//      |            |/  /  |--|| /   |/            |                  |     |
				// 3390 |            |--|   | / |/ 5  | 2340   3390 |                  +----++
				//      |     9      |  |   |/  |    / 2170         |        9         |    ||
				//      |            |7 |  //   |   /          3100 |                  |    ||
				//      |            |  | //    |  /                |                  | 7  ||
				//      |            |  |//     | / 1560            |                  |    ||
				// 2250 |------------|--||      |/                  |                  |    ||
				//      |                |      | 1200              |                  |    ||
				//      |                |     / 1120          2250 +------------------+----++
				//      |                |    /                2020 |                        |
				//      |       4        |   / 800                  |                        |
				//      |                |  /                       |                        |
				//      |                | /                   1450 |                        |
				//      |                |/                         |                        |
				//    0 |----------------|-- x                      |           4            |
				//      0      1200    2250                         |                        |
				//                                                  |                        |
				//                                                  |                        |
				//                                                  |                        |
				//                                                  |                        |
				//                                                0 +------------------------+
				//                                                  0            1200 1650   2250   x
				//
				// y                                                                                  z
				// 2340 +--------------------------------------------+---------------------+          6810 +------------+
				//      |                     6                      |                     |          6470 |            |
				// 2170 +-----------------------------------+        |                     |               |            |
				//      |                                   |        |                     |               |            |
				//      |                                   |        |                     |               |            |
				//      |                                   |        |                     |               |            |
				//      |                                   |        |                     |               |     2      |
				//      |                                   |        |                     |          5380 |            +----------+
				//      |                                   |        |          1          |               |            |          |
				//      |                                   |        |                     |               |            |    10    |
				//      |                10                 |        |                     |               |            |          |
				//      |                                   |        |                     |               |            |          |
				// 1560 |                                   |        |                     |          4530 +-----------++----------+-+
				//      |                                   |        |                     |          4370 +-----------+             |
				//      |                                   |        |                     |               |           |             |
				//      |                                   |        |                     |               |     9     |             |
				//      |                                   |        |                     |               |           |             |
				// 1200 |                                   +--------+---+                 |               |           |             |
				// 1120 +-----------------------------------+            +-+---------------+          3390 +--------+  |      1      |
				//      |                                   |            | |               |               |        |  |             |
				//      |                 9                 |            | |       4       |          3100 |        |  |             |
				//      |                                   |            | |               |               |   7    |  |             |
				//      |                                   |            | |               |               |        |  |             |
				//  800 +-----------------------------------+            | +--------------++               |        |  |             |
				//      |                                   |            | |              ||               |        |  |             |
				//      |                                   |            | |              ||          2250 +--------+--++---+--------+
				//      |                                   |     2      | |              ||          2020 |            +---+        |
				//      |                                   |            | |              ||               |            | 3 |   6    |
				//      |                                   |            | |              ||               |            |   |        |
				//      |                 8                 |            | |      7       ||          1450 |            +---+--------+
				//      |                                   |            | |              ||               |            |            |
				//      |                                   |            | |              ||               |     4      |            |
				//      |                                   |            | |              ||               |            |            |
				//      |                                   |            | |              ||               |            |     5      |
				//      |                                   |            | |              ||               |            |            |
				//    0 +-----------------------------------+------------+-+--------------++               |            |            |
				//      0                                   1200     1500  1700 1860       2250   x        |            |            |
				//                                                                                       0 +------------+------------+
				//                                                                                         0        800 1200         2340   y
				// </figure>
				figure(result);
				List<Container> packList = result.getContainers();
	
				assertNotNull(packList);
				assertTrue(i >= packList.size());
			}
		} finally {
			packager.close();
		}
	}

	private BoxItem createStackableItem(String id, int width, int height, int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new BoxItem(box, boxCountPerStackableItem);
	}

	@Test
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(PlainPackager.newBuilder().build());
	}

	@Test
	void testStackingSpecificMultipleContainers() {
		// just all for one big container
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("big").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.withContainer(Container.newBuilder().withId("small").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 4)
				.withContainer(Container.newBuilder().withId("other").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 4));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(5).withBoxItems(products).build();
			// <figure>
			// container 1 of 3: big
			//   z   /-------/-------|   y   z                         y                         z
			//      /   A   /   A   /|       1 +-------+-------+       2 +-------+-------+       1 +-------+-------+
			//   | /-------/-------| | /       |       |       |         |       |       |         |       |       |
			//   |/       /       /| |/        |   A   |   A   |         |   A   |   A   |         |   A   |   A   |
			// 1 |-------|-------| | | 2       |       |       |         |       |       |         |       |       |
			//   |       |       | |/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
			//   |   A   |   A   | | 1         0       1       2   x     |       |       |         0       1       2   y
			//   |       |       |/                                      |   A   |   A   |
			// 0 |-------|-------|-- x                                   |       |       |
			//   0       1       2                                     0 +-------+-------+
			//                                                           0       1       2   x
			//
			// container 2 of 3: small
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |   B   |         |   B   |         |   B   |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |   B   | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			//
			// container 3 of 3: small
			//   z                 z                 y                 z
			//                     1 +-------+       1 +-------+       1 +-------+
			//   | /-------|   y     |       |         |       |         |       |
			//   |/       /|         |   C   |         |   C   |         |   C   |
			// 1 |-------| | /       |       |         |       |         |       |
			//   |       | |/      0 +-------+       0 +-------+       0 +-------+
			//   |   C   | | 1       0       1   x     0       1   x     0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			figure(build);
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
			
			assertEquals(build.get(0).getId(), "big");
			assertEquals(build.get(1).getId(), "small");
			assertEquals(build.get(2).getId(), "small");
		} finally {
			packager.close();
		}
	}

	@Test
	void testDoNotStackMatchesWithPetrol() {
		Container container = Container.newBuilder()
				.withId("my-container")
				.withEmptyWeight(1)
				.withSize(2, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("petrol-1").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("lighter-2").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withManifestControlsBuilderFactory(NoLightersWithPetrolManifestControls.newFactory());
			})
					.withMaxContainerCount(5)
					.withBoxItems(products)
					.build();
			// <figure>
			// container 1 of 2: my-container
			//   z   /-----------|   y   z                     y                     z
			//      /           /|       1 +-----------+       2 +-----------+       1 +-----------------------+
			//   | /           / | /       |           |         |           |         |                       |
			//   |/           /  |/        | lighter-2 |         |           |         |       lighter-2       |
			// 1 |-----------|   | 2       |           |         |           |         |                       |
			//   |           |  /        0 +-----------+         | lighter-2 |       0 +-----------------------+
			//   | lighter-2 | /           0           1   x     |           |         0                       2   y
			//   |           |/                                  |           |
			// 0 |-----------|-- x                               |           |
			//   0           1                                 0 +-----------+
			//                                                   0           1   x
			//
			// container 2 of 2: my-container
			//   z                    z                    y                    z
			//                        1 +----------+       1 +----------+       1 +----------+
			//   | /----------|   y     |          |         |          |         |          |
			//   |/          /|         | petrol-1 |         | petrol-1 |         | petrol-1 |
			// 1 |----------| | /       |          |         |          |         |          |
			//   |          | |/      0 +----------+       0 +----------+       0 +----------+
			//   | petrol-1 | | 1       0          1   x     0          1   x     0          1   y
			//   |          |/
			// 0 |----------|-- x
			//   0          1
			// </figure>
			figure(build);
			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);
			
			for(Container c : containers) {
				assertEquals(c.getStack().size(), 1);
			}
			
			assertEquals(containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "lighter-2");
			assertEquals(containers.get(1).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "petrol-1");
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testDoNotStackMatchesWithPetrolForGroups() {
		Container container = Container.newBuilder()
				.withId("my-container")
				.withEmptyWeight(1)
				.withSize(2, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			BoxItem boxItem1 = new BoxItem(Box.newBuilder().withId("petrol-1").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1);
			BoxItem boxItem2 = new BoxItem(Box.newBuilder().withId("lighter-2").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1);
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem1));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem2));
			
			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withManifestControlsBuilderFactory(NoLighterWithPetrolManifestControls.newFactory());
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(Arrays.asList(boxItemGroup1, boxItemGroup2))
					.build();
			// <figure>
			// container 1 of 2: my-container
			//   z   /-----------|   y   z                     y                     z
			//      /           /|       1 +-----------+       2 +-----------+       1 +-----------------------+
			//   | /           / | /       |           |         |           |         |                       |
			//   |/           /  |/        | lighter-2 |         |           |         |       lighter-2       |
			// 1 |-----------|   | 2       |           |         |           |         |                       |
			//   |           |  /        0 +-----------+         | lighter-2 |       0 +-----------------------+
			//   | lighter-2 | /           0           1   x     |           |         0                       2   y
			//   |           |/                                  |           |
			// 0 |-----------|-- x                               |           |
			//   0           1                                 0 +-----------+
			//                                                   0           1   x
			//
			// container 2 of 2: my-container
			//   z                    z                    y                    z
			//                        1 +----------+       1 +----------+       1 +----------+
			//   | /----------|   y     |          |         |          |         |          |
			//   |/          /|         | petrol-1 |         | petrol-1 |         | petrol-1 |
			// 1 |----------| | /       |          |         |          |         |          |
			//   |          | |/      0 +----------+       0 +----------+       0 +----------+
			//   | petrol-1 | | 1       0          1   x     0          1   x     0          1   y
			//   |          |/
			// 0 |----------|-- x
			//   0          1
			// </figure>
			figure(build);
			assertTrue(build.isSuccess());
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);
			
			for(Container c : containers) {
				assertEquals(c.getStack().size(), 1);
			}
			
			// the larger group first
			assertEquals("lighter-2", containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId());
			assertEquals("petrol-1", containers.get(1).getStack().getPlacements().get(0).getStackValue().getBox().getId());
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testMaxFireHazardsPerContainer() {
		Container container = Container.newBuilder()
				.withId("my-container")
				.withEmptyWeight(1)
				.withSize(2, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			BoxItem boxItem1 = new BoxItem(Box.newBuilder()
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);
			
			BoxItem boxItem2 = new BoxItem(Box.newBuilder()
					.withId("firehazard")
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.withProperty(MaxFireHazardBoxItemGroupsPerContainerManifestControls.KEY, Boolean.TRUE)
					.build(), 2);
	
			List<BoxItem> products = new ArrayList<>();
			products.add(boxItem1);
			products.add(boxItem2);
	
			PackagerResult build = packager.newResultBuilder()
				.withContainerItem( b -> {
					b.withContainerItem(new ContainerItem(container, 5));
					b.withManifestControlsBuilderFactory(MaxFireHazardBoxItemPerContainerManifestControls.newFactory(1));
				})
				.withMaxContainerCount(5)
				.withBoxItems(products)
				.build();
			// <figure>
			// container 1 of 2: my-container
			//   z   /------------|   y   z                      y                      z
			//      / firehazard /|       1 +------------+       2 +------------+       1 +------------+------------+
			//   | /------------| | /       |            |         |            |         |            |            |
			//   |/            /| |/        |     A      |         | firehazard |         |     A      | firehazard |
			// 1 |------------| | | 2       |            |         |            |         |            |            |
			//   |            | |/        0 +------------+       1 +------------+       0 +------------+------------+
			//   |     A      | | 1         0            1   x     |            |         0            1            2   y
			//   |            |/                                   |     A      |
			// 0 |------------|-- x                                |            |
			//   0            1                                  0 +------------+
			//                                                     0            1   x
			//
			// container 2 of 2: my-container
			//   z                      z                      y                      z
			//                          1 +------------+       1 +------------+       1 +------------+
			//   | /------------|   y     |            |         |            |         |            |
			//   |/            /|         | firehazard |         | firehazard |         | firehazard |
			// 1 |------------| | /       |            |         |            |         |            |
			//   |            | |/      0 +------------+       0 +------------+       0 +------------+
			//   | firehazard | | 1       0            1   x     0            1   x     0            1   y
			//   |            |/
			// 0 |------------|-- x
			//   0            1
			// </figure>
			figure(build);

			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);

			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}	

	@Test
	void testMaxFireHazardsPerContainerGroups() {
		Container container = Container.newBuilder()
				.withId("my-container")
				.withEmptyWeight(1)
				.withSize(2, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			BoxItem boxItem1 = new BoxItem(Box.newBuilder()
					.withId("id")
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);
			
			BoxItem boxItem2 = new BoxItem(Box.newBuilder()
					.withId("firehazard1")
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.withProperty(MaxFireHazardBoxItemGroupsPerContainerManifestControls.KEY, Boolean.TRUE)
					.build(), 1);

			BoxItem boxItem3 = new BoxItem(Box.newBuilder()
					.withId("firehazard2")
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.withProperty(MaxFireHazardBoxItemGroupsPerContainerManifestControls.KEY, Boolean.TRUE)
					.build(), 1);

			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem1));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem2));
			BoxItemGroup boxItemGroup3 = new BoxItemGroup("c", Arrays.asList(boxItem3));
			
			List<BoxItemGroup> groups = Arrays.asList(boxItemGroup1, boxItemGroup2, boxItemGroup3);
			
			PackagerResult result = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withManifestControlsBuilderFactory(MaxFireHazardBoxItemGroupsPerContainerManifestControls.newFactory(1));
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(copyGroups(groups))
					.withOrder(Order.CHRONOLOGICAL)
					.build();
			// <figure>
			// container 1 of 2: my-container
			//   z   /-------------|   y   z                       y                       z
			//      / firehazard1 /|       1 +-------------+       2 +-------------+       1 +-------------+-------------+
			//   | /-------------| | /       |             |         |             |         |             |             |
			//   |/             /| |/        |     id      |         | firehazard1 |         |     id      | firehazard1 |
			// 1 |-------------| | | 2       |             |         |             |         |             |             |
			//   |             | |/        0 +-------------+       1 +-------------+       0 +-------------+-------------+
			//   |     id      | | 1         0             1   x     |             |         0             1             2   y
			//   |             |/                                    |     id      |
			// 0 |-------------|-- x                                 |             |
			//   0             1                                   0 +-------------+
			//                                                       0             1   x
			//
			// container 2 of 2: my-container
			//   z                       z                       y                       z
			//                           1 +-------------+       1 +-------------+       1 +-------------+
			//   | /-------------|   y     |             |         |             |         |             |
			//   |/             /|         | firehazard2 |         | firehazard2 |         | firehazard2 |
			// 1 |-------------| | /       |             |         |             |         |             |
			//   |             | |/      0 +-------------+       0 +-------------+       0 +-------------+
			//   | firehazard2 | | 1       0             1   x     0             1   x     0             1   y
			//   |             |/
			// 0 |-------------|-- x
			//   0             1
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(Arrays.asList(new ContainerItem(container, 5)))
					.withMaxContainerCount(5)
					.withBoxItemGroups(groups)
					.withOrder(Order.CHRONOLOGICAL));
		} finally {
			packager.close();
		}
	}	

	@Test
	void testStackingSquaresOnSquareWithPointConstraints() {
		
		int maxWeight = 2;

		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(2, 1, 3)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory( (c) -> {
			c.withPlacementComparator(new HeavyItemsOnGroundLevelPlacementComparator(maxWeight));
			c.withBoxItemComparator(new HeavyItemsBestBoxItemComparator(maxWeight));
		}).build();
		
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(3).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				
				// strictly not necessary but included
				b.withPointControlsBuilderFactory(HeavyItemsOnGroundLevelPointControls.newFactory(maxWeight));
			}).withBoxItems(products).build();
			// <figure>
			//   z                         z                         y                         z
			//                             3 +---------------+       1 +---------------+       3 +-------+
			//   | /---------------|         |               |         |               |         |       |
			//   |/               /|         |       C       |         |       C       |         |   C   |
			// 3 |---------------| |         |               |         |               |         |       |
			//   |               | |       2 +---------------+       0 +---------------+       2 +-------+
			//   |       C       | |         |               |         0       1       2   x     |       |
			//   |               |/|         |       B       |                                   |   B   |
			// 2 |---------------| |         |               |                                   |       |
			//   |               | |       1 +-------+-------+                                 1 +-------+
			//   |       B       | |   y     |       |                                           |       |
			//   |               |/          |   A   |                                           |   A   |
			// 1 |-------|-------|   /       |       |                                           |       |
			//   |       | |        /      0 +-------+                                         0 +-------+
			//   |   A   | |       / 1       0       1       2   x                               0       1   y
			//   |       |/       /
			// 0 |-------|---------- x
			//   0       1       2
			// </figure>
			figure(build);
			
			assertTrue(build.isSuccess());
			
			List<Container> containers = build.getContainers();
			
			assertEquals("A", containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId());
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testStackingOrder1() {

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 10);
		
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("0").withRotate3D().withSize(1, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("1").withRotate3D().withSize(1, 3, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("2").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("3").withRotate3D().withSize(1, 4, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("4").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem( b -> {
						b.withContainerItem(containerItem);
					})
					.withBoxItems(products)
					.withOrder(Order.CHRONOLOGICAL)
					.withMaxContainerCount(10)
					.build();
			// <figure>
			// container 1 of 2: 1
			//             /-------/-------|   y   z                         y                         z
			//            /       /       /|       1 +-------+-------+       5 +-------+-------+       1 +-----------------------+---------------+
			//           /       /   2   / | /       |       |       |         |       |       |         |                       |               |
			//          /       /       /  |/        |   0   |   1   |         |       |       |         |           1           |       2       |
			//         /       /-------|   | 5       |       |       |         |       |       |         |                       |               |
			//        /       /       /|  /        0 +-------+-------+         |       |   2   |       0 +-----------------------+---------------+
			//   z   /       /       / | /           0       1       2   x     |       |       |         0                       3               5   y
			//      /       /       /  |/                                      |       |       |
			//   | /       /       /   | 3                                     |       |       |
			//   |/       /       /   /                                      3 |       +-------+
			// 1 |-------|-------|   /                                         |       |       |
			//   |       |       |  /                                          |   0   |       |
			//   |   0   |   1   | /                                           |       |       |
			//   |       |       |/                                            |       |       |
			// 0 |-------|-------|-- x                                         |       |       |
			//   0       1       2                                             |       |   1   |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                               0 +-------+-------+
			//                                                                 0       1       2   x
			//
			// container 2 of 2: 1
			//             /-------|   y   z                 y                 z
			//            /   4   /|       1 +-------+       5 +-------+       1 +-------------------------------+-------+
			//           /-------| | /       |       |         |       |         |                               |       |
			//          /       /| |/        |   3   |         |   4   |         |               3               |   4   |
			//         /       / | | 5       |       |         |       |         |                               |       |
			//        /       /  |/        0 +-------+       4 +-------+       0 +-------------------------------+-------+
			//   z   /       /   | 4         0       1   x     |       |         0                               4       5   y
			//      /       /   /                              |       |
			//   | /       /   /                               |       |
			//   |/       /   /                                |       |
			// 1 |-------|   /                                 |       |
			//   |       |  /                                  |       |
			//   |   3   | /                                   |       |
			//   |       |/                                    |   3   |
			// 0 |-------|-- x                                 |       |
			//   0       1                                     |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                               0 +-------+
			//                                                 0       1   x
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			
			List<Container> containers = result.getContainers();
			assertEquals(containers.size(), 2);
			
			int index = 0;
			for (Container c : containers) {
				for (Placement stackPlacement : c.getStack().getPlacements()) {
					assertEquals(stackPlacement.getStackValue().getBox().getId(), Integer.toString(index));
					index++;
				}
			}
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(Arrays.asList(containerItem))
					.withMaxContainerCount(10)
					.withBoxItems(products)
					.withOrder(Order.CHRONOLOGICAL));
		} finally {
			packager.close();
		}

	}

	@Test
	void testStackingOrder2() {

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 10);
		
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("0").withRotate3D().withSize(1, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("1").withRotate3D().withSize(1, 3, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("2").withRotate3D().withSize(1, 4, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("3").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("4").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem( b -> {
						b.withContainerItem(containerItem);
					})
					.withBoxItems(products)
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
					.withMaxContainerCount(10)
					.build();
			// <figure>
			// container 1 of 2: 1
			//             /-------/-------|   y   z                         y                         z
			//            /       /       /|       1 +-------+-------+       5 +-------+-------+       1 +-----------------------+---------------+
			//           /       /   3   / | /       |       |       |         |       |       |         |                       |               |
			//          /       /       /  |/        |   0   |   1   |         |       |       |         |           1           |       3       |
			//         /       /-------|   | 5       |       |       |         |       |       |         |                       |               |
			//        /       /       /|  /        0 +-------+-------+         |       |   3   |       0 +-----------------------+---------------+
			//   z   /       /       / | /           0       1       2   x     |       |       |         0                       3               5   y
			//      /       /       /  |/                                      |       |       |
			//   | /       /       /   | 3                                     |       |       |
			//   |/       /       /   /                                      3 |       +-------+
			// 1 |-------|-------|   /                                         |       |       |
			//   |       |       |  /                                          |   0   |       |
			//   |   0   |   1   | /                                           |       |       |
			//   |       |       |/                                            |       |       |
			// 0 |-------|-------|-- x                                         |       |       |
			//   0       1       2                                             |       |   1   |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                                 |       |       |
			//                                                               0 +-------+-------+
			//                                                                 0       1       2   x
			//
			// container 2 of 2: 1
			//             /-------|   y   z                 y                 z
			//            /   4   /|       1 +-------+       5 +-------+       1 +-------------------------------+-------+
			//           /-------| | /       |       |         |       |         |                               |       |
			//          /       /| |/        |   2   |         |   4   |         |               2               |   4   |
			//         /       / | | 5       |       |         |       |         |                               |       |
			//        /       /  |/        0 +-------+       4 +-------+       0 +-------------------------------+-------+
			//   z   /       /   | 4         0       1   x     |       |         0                               4       5   y
			//      /       /   /                              |       |
			//   | /       /   /                               |       |
			//   |/       /   /                                |       |
			// 1 |-------|   /                                 |       |
			//   |       |  /                                  |       |
			//   |   2   | /                                   |       |
			//   |       |/                                    |   2   |
			// 0 |-------|-- x                                 |       |
			//   0       1                                     |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                                 |       |
			//                                               0 +-------+
			//                                                 0       1   x
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			
			List<Container> containers = result.getContainers();
			assertEquals(containers.size(), 2);
			
			for (Container c : containers) {
				int index = 0;
				for (Placement stackPlacement : c.getStack().getPlacements()) {
					int n = Integer.parseInt(stackPlacement.getStackValue().getBox().getId());
					assertTrue(n >= index);
					index = n;
				}
			}
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(Arrays.asList(containerItem))
					.withMaxContainerCount(10)
					.withBoxItems(products)
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING));

		} finally {
			packager.close();
		}

	}

	@Test
	void testRequireFullSupport() {

		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 3, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 10);
		
		PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory( (c) -> {
			c.withRequireFullSupport(true);
		}).build();
		
		try {
			List<BoxItem> products = new ArrayList<>();
	
			// neither can be stacked on the other without leaving something in the air
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(2, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(1, 3, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder()
					.withContainerItem(containerItem)
					.withBoxItems(products)
					.withMaxContainerCount(10)
					.build();
			// <figure>
			// container 1 of 2: 1
			//   z   /---------------|   y   z                         y                         z
			//      /               /|       1 +---------------+       2 +---------------+       1 +---------------+
			//   | /               / | /       |               |         |               |         |               |
			//   |/               /  |/        |       A       |         |               |         |       A       |
			// 1 |---------------|   | 2       |               |         |               |         |               |
			//   |               |  /        0 +---------------+         |       A       |       0 +---------------+
			//   |       A       | /           0               2   x     |               |         0               2   y
			//   |               |/                                      |               |
			// 0 |---------------|-- x                                   |               |
			//   0               2                                     0 +---------------+
			//                                                           0               2   x
			//
			// container 2 of 2: 1
			//         /-------|   y   z                 y                 z
			//        /       /|       1 +-------+       3 +-------+       1 +-----------------------+
			//   z   /       / | /       |       |         |       |         |                       |
			//      /       /  |/        |   B   |         |       |         |           B           |
			//   | /       /   | 3       |       |         |       |         |                       |
			//   |/       /   /        0 +-------+         |       |       0 +-----------------------+
			// 1 |-------|   /           0       1   x     |       |         0                       3   y
			//   |       |  /                              |   B   |
			//   |   B   | /                               |       |
			//   |       |/                                |       |
			// 0 |-------|-- x                             |       |
			//   0       1                                 |       |
			//                                             |       |
			//                                           0 +-------+
			//                                             0       1   x
			// </figure>
			figure(build);
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
			
			assertEquals(2, build.getContainers().size());
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testStackingSquaresOnSquareWithPredefinedPoints() {
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(false, 16);
		calculator.clearToSize(2, 2, 1);
		Box box = Box.newBuilder().withDescription("0").withSize(1, 1, 1).withWeight(1).build();
		Placement pillar = new Placement(box.getStackValue(0), calculator.get(0));
		calculator.add(0, pillar);
		
		List<Point> all = calculator.getAll();
		assertEquals(all.size(), 2);
		
		Container container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 3, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();
		
		ContainerItem containerItem = new ContainerItem(container, 1);
		
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder()
					.withContainerItem( (c) -> {
						c.withContainerItem(containerItem);
						c.withPoints(all);
					})
					.withBoxItems(products)
					.build();
			// <figure>
			//   z   /-------/-------|   y   z                         y                         z
			//      /       /   C   /|       1 +-------+-------+       2 +-------+-------+       1 +-------+-------+
			//   | |-------/-------| | /       |       |       |         |       |       |         |       |       |
			//   | |  A   /       /| |/        |   A   |   B   |         |   A   |   C   |         |   B   |   C   |
			// 1 | |     |-------| | | 2       |       |       |         |       |       |         |       |       |
			//   | |     |       | |/        0 +-------+-------+       1 +-------+-------+       0 +-------+-------+
			//   | |-----|   B   | | 1         0       1       2   x             |       |         0       1       2   y
			//   |       |       |/                                              |   B   |
			// 0 |-------|-------|-- x                                           |       |
			//   0       1       2                                     0         +-------+
			//                                                           0       1       2   x
			// </figure>
			figure(build);
			
			List<Placement> placements = build.getContainers().get(0).getStack().getPlacements();
			for(Placement placement : placements) {
				assertFalse(placement.getAbsoluteX() == 0 && placement.getAbsoluteY() == 0);
			}
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}

	@Test
	void testUndoGroupsOrder1() {
		Container container1 = Container.newBuilder()
				.withId("my-container1")
				.withEmptyWeight(1)
				.withSize(4, 1, 2)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			BoxItem boxItem0 = new BoxItem(Box.newBuilder()
					.withId("id0")
					.withSize(1, 1, 2)
					.withWeight(1)
					.build(), 1);
					
			BoxItem boxItem1 = new BoxItem(Box.newBuilder()
					.withId("id1")
					.withSize(1, 1, 2)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem2 = new BoxItem(Box.newBuilder()
					.withId("id2")
					.withSize(3, 1, 1)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem3 = new BoxItem(Box.newBuilder()
					.withId("id3")
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem4 = new BoxItem(Box.newBuilder()
					.withId("id4")
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);

			// group 1 make group 2 not fit anymore in the same container
			// so needs a new container

			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem0));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem1, boxItem2));
			BoxItemGroup boxItemGroup3 = new BoxItemGroup("c", Arrays.asList(boxItem3, boxItem4));
			
			List<BoxItemGroup> groups = Arrays.asList(boxItemGroup1, boxItemGroup2, boxItemGroup3);
			
			PackagerResult result = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container1, 5));
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(copyGroups(groups))
					.withOrder(Order.CHRONOLOGICAL)
					.build();
			// <figure>
			// container 1 of 2: my-container1
			//   z                 z                 y                 z
			//                     2 +-------+       1 +-------+       2 +-------+
			//   | /-------|         |       |         |       |         |       |
			//   |/       /|         |       |         |  id0  |         |       |
			// 2 |-------| |         |       |         |       |         |       |
			//   |       | |         |  id0  |       0 +-------+         |  id0  |
			//   |       | |   y     |       |         0       1   x     |       |
			//   |       | |         |       |                           |       |
			//   |  id0  | | /       |       |                           |       |
			//   |       | |/      0 +-------+                         0 +-------+
			//   |       | | 1       0       1   x                       0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			//
			// container 2 of 2: my-container1
			//   z                                         z                                         y                                         z
			//                                             2 +-------+-------+-------+               1 +-------+-------+-------+-------+       2 +-------+
			//   | /-------/-------/-------|                 |       |       |       |                 |       |       |       |       |         |       |
			//   |/       /       /       /|                 |       |  id3  |  id4  |                 |  id1  |  id3  |  id4  |  id2  |         |  id4  |
			// 2 |-------|-------|-------| |                 |       |       |       |                 |       |       |       |       |         |       |
			//   |       |       |       | |               1 |  id1  +-------+-------+-------+       0 +-------+-------+-------+-------+       1 +-------+
			//   |       |  id3  |  id4  | |-------|   y     |       |                       |         0       1       2       3       4   x     |       |
			//   |       |       |       |/       /|         |       |          id2          |                                                   |  id2  |
			// 1 |  id1  |-------|-------|-------| | /       |       |                       |                                                   |       |
			//   |       |                       | |/      0 +-------+-----------------------+                                                 0 +-------+
			//   |       |          id2          | | 1       0       1       2       3       4   x                                               0       1   y
			//   |       |                       |/
			// 0 |-------|-----------------------|-- x
			//   0       1       2       3       4
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(Arrays.asList(new ContainerItem(container1, 5)))
					.withMaxContainerCount(5)
					.withBoxItemGroups(groups)
					.withOrder(Order.CHRONOLOGICAL));
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testUndoGroupsOrder2() {
		Container container1 = Container.newBuilder()
				.withId("my-container1")
				.withEmptyWeight(1)
				.withSize(4, 1, 2)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			BoxItem boxItem0 = new BoxItem(Box.newBuilder()
					.withId("id0")
					.withSize(1, 1, 2)
					.withWeight(1)
					.build(), 1);
					
			BoxItem boxItem1 = new BoxItem(Box.newBuilder()
					.withId("id1")
					.withSize(1, 1, 2)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem2 = new BoxItem(Box.newBuilder()
					.withId("id2")
					.withSize(3, 1, 1)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem3 = new BoxItem(Box.newBuilder()
					.withId("id3")
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);

			BoxItem boxItem4 = new BoxItem(Box.newBuilder()
					.withId("id4")
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);

			// group 1 make group 2 not fit anymore in the same container
			// so needs a new container

			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem0));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem1, boxItem2));
			BoxItemGroup boxItemGroup3 = new BoxItemGroup("c", Arrays.asList(boxItem3, boxItem4));
			
			List<BoxItemGroup> groups = Arrays.asList(boxItemGroup1, boxItemGroup2, boxItemGroup3);
			
			PackagerResult result = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container1, 5));
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(copyGroups(groups))
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
					.build();
			// <figure>
			// container 1 of 2: my-container1
			//   z                                 z                                 y                                 z
			//                                     2 +-------+                       1 +-------+-------+-------+       2 +-------+
			//   | /-------|                         |       |                         |       |       |       |         |       |
			//   |/       /|                         |       |                         |  id0  |  id3  |  id4  |         |  id0  |
			// 2 |-------| |                         |       |                         |       |       |       |         |       |
			//   |       | |                       1 |  id0  +-------+-------+       0 +-------+-------+-------+       1 +-------+
			//   |       | |-------/-------|   y     |       |       |       |         0       1       2       3   x     |       |
			//   |       |/       /       /|         |       |  id3  |  id4  |                                           |  id4  |
			// 1 |  id0  |-------|-------| | /       |       |       |       |                                           |       |
			//   |       |       |       | |/      0 +-------+-------+-------+                                         0 +-------+
			//   |       |  id3  |  id4  | | 1       0       1       2       3   x                                       0       1   y
			//   |       |       |       |/
			// 0 |-------|-------|-------|-- x
			//   0       1       2       3
			//
			// container 2 of 2: my-container1
			//   z                                         z                                         y                                         z
			//                                             2 +-------+                               1 +-------+-----------------------+       2 +-------+
			//   | /-------|                                 |       |                                 |       |                       |         |       |
			//   |/       /|                                 |       |                                 |  id1  |          id2          |         |  id1  |
			// 2 |-------| |                                 |       |                                 |       |                       |         |       |
			//   |       | |                               1 |  id1  +-----------------------+       0 +-------+-----------------------+       1 +-------+
			//   |       | |-----------------------|   y     |       |                       |         0       1                       4   x     |       |
			//   |       |/                       /|         |       |          id2          |                                                   |  id2  |
			// 1 |  id1  |-----------------------| | /       |       |                       |                                                   |       |
			//   |       |                       | |/      0 +-------+-----------------------+                                                 0 +-------+
			//   |       |          id2          | | 1       0       1                       4   x                                               0       1   y
			//   |       |                       |/
			// 0 |-------|-----------------------|-- x
			//   0       1                       4
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(Arrays.asList(new ContainerItem(container1, 5)))
					.withMaxContainerCount(5)
					.withBoxItemGroups(groups)
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING));
		} finally {
			packager.close();
		}
	}	

	@Test
	void testStackingRectanglesWithObstacles() {
		PlainPackager packager = PlainPackager.newBuilder().build();

		try {
			Container container = Container.newBuilder()
					.withDescription("1")
					.withEmptyWeight(1)
					.withSize(3, 3, 3)
					.withMaxLoadWeight(100)
					.withStack(new ValidatingStack())
					.build();
	
			List<BoxItem> products9 = new ArrayList<>();
			for(int i = 0; i < 9; i++) {
				products9.add(new BoxItem(Box.newBuilder().withId("" + (char)(i + 'A')).withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			
			PackagerResult build9 = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
			}).withBoxItems(products9).build();
			// <figure>
			//         /-------/-------/-------|   y   z                                 y                                 z
			//        /   C   /   F   /   I   /|       1 +-------+-------+-------+       3 +-------+-------+-------+       1 +-------+-------+-------+
			//   z   /-------/-------/-------| | /       |       |       |       |         |       |       |       |         |       |       |       |
			//      /   B   /   E   /   H   /| |/        |   A   |   D   |   G   |         |   C   |   F   |   I   |         |   G   |   H   |   I   |
			//   | /-------/-------/-------| | | 3       |       |       |       |         |       |       |       |         |       |       |       |
			//   |/       /       /       /| |/        0 +-------+-------+-------+       2 +-------+-------+-------+       0 +-------+-------+-------+
			// 1 |-------|-------|-------| | | 2         0       1       2       3   x     |       |       |       |         0       1       2       3   y
			//   |       |       |       | |/                                              |   B   |   E   |   H   |
			//   |   A   |   D   |   G   | | 1                                             |       |       |       |
			//   |       |       |       |/                                              1 +-------+-------+-------+
			// 0 |-------|-------|-------|-- x                                             |       |       |       |
			//   0       1       2       3                                                 |   A   |   D   |   G   |
			//                                                                             |       |       |       |
			//                                                                           0 +-------+-------+-------+
			//                                                                             0       1       2       3   x
			// </figure>
			figure(build9);
			
			List<Placement> placements = build9.getContainers().get(0).getStack().getPlacements();
			
			for(int obstacleIndex = 0; obstacleIndex < 9; obstacleIndex++) {
				
				Placement obstacle = placements.get(obstacleIndex);
				
				List<BoxItem> products = new ArrayList<>();
				
				for(int i = 0; i < 8; i++) {
					products.add(new BoxItem(Box.newBuilder().withId("" + (char)(i + 'A')).withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
				}
	
				PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
					b.withContainerItem(new ContainerItem(container, 1));
					b.withObstacles( o -> {
						o.withObstacle(
								obstacle.getAbsoluteX(), obstacle.getAbsoluteY(), obstacle.getAbsoluteZ(),
								obstacle.getAbsoluteEndX() - obstacle.getAbsoluteX() + 1, obstacle.getAbsoluteEndY() - obstacle.getAbsoluteY() + 1, obstacle.getAbsoluteEndZ() - obstacle.getAbsoluteZ() + 1 
							);
					});
				}).withBoxItems(products).build();
				
				PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
				
				List<Placement> buildPlacements = build.getContainers().get(0).getStack().getPlacements();
				for (Placement placement : buildPlacements) {
					assertFalse(placement.intersects3D(obstacle));
				}
			}
		} finally {
			packager.close();
		}
	}

	
}
