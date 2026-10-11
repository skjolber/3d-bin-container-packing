package com.github.skjolber.packing.packer.laff;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.test.assertj.StackAssert;

public class LargestAreaFitFirstPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z                                 z                                 y                                 z
			//                                     1 +-------+-------+-------+       1 +-------+-------+-------+       1 +-------+
			//   | /-------/-------/-------|   y     |       |       |       |         |       |       |       |         |       |
			//   |/       /       /       /|         |   A   |   B   |   C   |         |   A   |   B   |   C   |         |   C   |
			// 1 |-------|-------|-------| | /       |       |       |       |         |       |       |       |         |       |
			//   |       |       |       | |/      0 +-------+-------+-------+       0 +-------+-------+-------+       0 +-------+
			//   |   A   |   B   |   C   | | 1       0       1       2       3   x     0       1       2       3   x     0       1   y
			//   |       |       |       |/
			// 0 |-------|-------|-------|-- x
			//   0       1       2       3
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}
	

	@Test
	void testStackingSquaresOnSquareForGroup() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", products);

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(Arrays.asList(boxItemGroup1)).build();
			// <figure>
			//   z                                 z                                 y                                 z
			//                                     1 +-------+-------+-------+       1 +-------+-------+-------+       1 +-------+
			//   | /-------/-------/-------|   y     |       |       |       |         |       |       |       |         |       |
			//   |/       /       /       /|         |   A   |   B   |   C   |         |   A   |   B   |   C   |         |   C   |
			// 1 |-------|-------|-------| | /       |       |       |       |         |       |       |       |         |       |
			//   |       |       |       | |/      0 +-------+-------+-------+       0 +-------+-------+-------+       0 +-------+
			//   |   A   |   B   |   C   | | 1       0       1       2       3   x     0       1       2       3   x     0       1   y
			//   |       |       |       |/
			// 0 |-------|-------|-------|-- x
			//   0       1       2       3
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<Placement> placements = fits.getStack().getPlacements();

			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}


	@Test
	void testStackingRectangles() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
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
			figure(result);
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(0, 1, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}

	}

	@Test
	void testStackingSquaresAndRectangle() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(6, 10, 10).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//                       /---------------------------------------|       z
			//                      /                                       /|       2 +---------------------------------------+
			//                     /                                       / |         |                                       |
			//                    /                                       /  |         |                   B                   |
			//                   /                                       /   |   y     |                                       |
			//                  /                   C                   /   /|       1 +---------------------------------------+
			//                 /                                       /   / | /       |                                       |
			//                /                                       /   /  |/        |                   A                   |
			//               /                                       /   /   | 10      |                                       |
			//              /                                       /   /   /        0 +---------------------------------------+
			//             /---------------------------------------|   /   /           0                                       5   x
			//            /                                       /|  /   /
			//           /                                       / | /   /
			//          /                                       /  |/   /
			//         /                                       /   |   /
			//        /                                       /   /   /
			//   z   /                                       /   /   /
			//      /                                       /   /   /
			//   | /                                       /   /   / 5
			//   |/                                       /   /   /
			// 2 |---------------------------------------|   /   /
			//   |                                       |  /   /
			//   |                   B                   | /   /
			//   |                                       |/   /
			// 1 |---------------------------------------|   /
			//   |                                       |  /
			//   |                   A                   | /
			//   |                                       |/
			// 0 |---------------------------------------|-- x
			//   0                                       5
			//
			// y                                               z
			// 10 +------------------------------------+       2 +-----------------------------------+-----------------------------------+
			//    |                                    |         |                 B                 |                 C                 |
			//    |                                    |         |                                   |                                   |
			//    |                                    |       1 +-----------------------------------+-----------------------------------+
			//    |                                    |         |                                                                       |
			//    |                                    |         |                                   A                                   |
			//    |                                    |         |                                                                       |
			//    |                                    |       0 +-----------------------------------------------------------------------+
			//    |                                    |         0                                   5                                   10   y
			//    |                 C                  |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//  5 +------------------------------------+
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                 B                  |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//    |                                    |
			//  0 +------------------------------------+
			//    0                                    5   x
			// </figure>
			figure(result);
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingDecreasingRectangles() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(6, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
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
			figure(result);
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(3, 0, 0).hasBoxItemId("B"); // point with lowest x is selected first
			assertThat(placements.get(2)).isAt(5, 0, 0).hasBoxItemId("C");
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}

	}

	@Test
	void testStackingRectanglesTwoLevels() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z   /---------------/-------|       z                                 y                                 z
			//      /       C       /       /|       2 +---------------+-------+       2 +---------------+-------+       2 +---------------+
			//   | /---------------/       / |         |               |       |         |               |       |         |               |
			//   |/               /       /  |         |       B       |   C   |         |       C       |       |         |       C       |
			// 2 |---------------|-------|   |   y     |               |       |         |               |       |         |               |
			//   |               |       |  /|       1 +---------------+-------+       1 +---------------+   C   |       1 +---------------+
			//   |       B       |   C   | / | /       |               |       |         |               |       |         |               |
			//   |               |       |/  |/        |       A       |   B   |         |       B       |       |         |       B       |
			// 1 |---------------|-------|   | 2       |               |       |         |               |       |         |               |
			//   |               |       |  /        0 +---------------+-------+       0 +---------------+-------+       0 +---------------+
			//   |       A       |   B   | / 1         0               2       3   x     0               2       3   x     0       1       2   y
			//   |               |       |/
			// 0 |---------------|-------|-- x
			//   0               2       3
			// </figure>
			figure(result);
			Container fits = result.get(0);
	
			assertNotNull(fits);
	
			validate(fits);

			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(0, 1, 0).hasBoxItemId("A");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("B");
	
			assertThat(placements.get(3)).isAt(0, 0, 1).hasBoxItemId("B");
			assertThat(placements.get(4)).isAt(0, 1, 1).hasBoxItemId("C");
			assertThat(placements.get(5)).isAt(2, 0, 1).hasBoxItemId("C");
			
			assertEquals(2, countLevels(fits));
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}
	

	@Test
	void testStackingRectanglesTwoLevelsForGroups() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", products);

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(Arrays.asList(boxItemGroup1)).build();
			// <figure>
			//   z   /---------------/-------|       z                                 y                                 z
			//      /       C       /       /|       2 +---------------+-------+       2 +---------------+-------+       2 +---------------+
			//   | /---------------/       / |         |               |       |         |               |       |         |               |
			//   |/               /       /  |         |       B       |   C   |         |       C       |       |         |       C       |
			// 2 |---------------|-------|   |   y     |               |       |         |               |       |         |               |
			//   |               |       |  /|       1 +---------------+-------+       1 +---------------+   C   |       1 +---------------+
			//   |       B       |   C   | / | /       |               |       |         |               |       |         |               |
			//   |               |       |/  |/        |       A       |   B   |         |       B       |       |         |       B       |
			// 1 |---------------|-------|   | 2       |               |       |         |               |       |         |               |
			//   |               |       |  /        0 +---------------+-------+       0 +---------------+-------+       0 +---------------+
			//   |       A       |   B   | / 1         0               2       3   x     0               2       3   x     0       1       2   y
			//   |               |       |/
			// 0 |---------------|-------|-- x
			//   0               2       3
			// </figure>
			figure(result);
			Container fits = result.get(0);
	
			assertNotNull(fits);

			validate(fits);

			List<Placement> placements = fits.getStack().getPlacements();
			
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(0, 1, 0).hasBoxItemId("A");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("B");
	
			assertThat(placements.get(3)).isAt(0, 0, 1).hasBoxItemId("B");
			assertThat(placements.get(4)).isAt(0, 1, 1).hasBoxItemId("C");
			assertThat(placements.get(5)).isAt(2, 0, 1).hasBoxItemId("C");
			
			assertEquals(2, countLevels(fits));
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingRectanglesThreeLevels() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//   z   /---------------|       z                         y                         z
			//      /               /|       3 +---------------+       2 +---------------+       3 +---------------+
			//   | /               / |         |               |         |               |         |               |
			//   |/               /  |         |       C       |         |               |         |       C       |
			// 3 |---------------|   |         |               |         |               |         |               |
			//   |               |  /|       2 +---------------+         |       C       |       2 +---------------+
			//   |       C       | / |         |               |         |               |         |               |
			//   |               |/  |         |       B       |         |               |         |       B       |
			// 2 |---------------|   |   y     |               |         |               |         |               |
			//   |               |  /|       1 +---------------+       0 +---------------+       1 +---------------+
			//   |       B       | / | /       |               |         0               2   x     |               |
			//   |               |/  |/        |       A       |                                   |       A       |
			// 1 |---------------|   | 2       |               |                                   |               |
			//   |               |  /        0 +---------------+                                 0 +---------------+
			//   |       A       | /           0               2   x                               0               2   y
			//   |               |/
			// 0 |---------------|-- x
			//   0               2
			// </figure>
			figure(result);
	
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
			
			assertEquals(3, countLevels(fits));
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingNotPossible() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				// capacity is 3*2*3 = 18
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			assertEquals(result.size(), 0);
		} finally {
			packager.close();
		}
	}

	@Test
	void issue433() {
		Container container = Container
				.newBuilder()
				.withId("1")
				.withSize(14, 195, 74)
				.withEmptyWeight(0)
				.withMaxLoadWeight(100)
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container, 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
				.newBuilder()
				.build();

		try {
			List<BoxItem> products = Arrays.asList(
					new BoxItem(Box.newBuilder().withId("Foot").withSize(7, 37, 39).withRotate3D().withWeight(0).build(), 20));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//                          /--/--|       z
			//                         /  /  /|       74 +------+------+
			//                        /Fo/  / |          |      |      |
			//                       /  /  /  |          |      |      |
			//                      /  /  /   |          |      |      |
			//                     /--/--|    |          |      |      |
			//                    /  /  /| Fo |          |      |      |
			//                   /Fo/  / |    |          |      |      |
			//                  /  /  /Fo|    |          |      |      |
			//                 /--/--|   |   /|          |      |      |
			//                /  /  /|   |  / |          | Foot | Foot |
			//               /Fo/  / |   | /  |          |      |      |
			//              /  /  /Fo|   |/   |   y      |      |      |
			//             /--/--|   |   |    |          |      |      |
			//            /  /  /|   |  /| Fo | /        |      |      |
			//           /Fo/  / |   | / |    |/         |      |      |
			//          /  /  /  |   |/Fo|    | 195      |      |      |
			//         /  /  /   |   |   |   /           |      |      |
			//    z   /--/--|    |  /|   |  /            |      |      |
			//       /  /  /| Fo | / |   | /          37 +------+------+
			//    | /  /  / |    |/Fo|   |/              |      |      |
			//    |/  /  /Fo|    |   |   | 156           |      |      |
			// 74 |--|--|   |   /|   |  /                |      |      |
			//    |  |  |   |  / |   | /                 |      |      |
			//    |  |  |   | /  |   |/                  |      |      |
			//    |  |  |   |/   |   | 117               |      |      |
			//    |Fo|  |   |    |  /                    |      |      |
			//    |  |  |  /| Fo | /                     |      |      |
			//    |  |  | / |    |/                      | Foot | Foot |
			//    |  |  |/Fo|    | 78                    |      |      |
			// 37 |--|--|   |   /                        |      |      |
			//    |  |  |   |  /                         |      |      |
			//    |  |  |   | /                          |      |      |
			//    |  |  |   |/                           |      |      |
			//    |Fo|  |   | 39                         |      |      |
			//    |  |  |  /                             |      |      |
			//    |  |  | /                              |      |      |
			//    |  |  |/                               |      |      |
			//  0 |--|--|-- x                          0 +------+------+
			//    0  7 14                                0      7      14   x
			//
			// y                 z
			// 195 +--+-+        74 +-------------+-------------+-------------+-------------+-------------+
			//     |  | |           |             |             |             |             |             |
			//     |  | |           |             |             |             |             |             |
			//     |Fo|F|           |    Foot     |    Foot     |    Foot     |    Foot     |    Foot     |
			//     |  | |           |             |             |             |             |             |
			//     |  | |           |             |             |             |             |             |
			//     |  | |        37 +-------------+-------------+-------------+-------------+-------------+
			// 156 +--+-+           |             |             |             |             |             |
			//     |  | |           |             |             |             |             |             |
			//     |  | |           |    Foot     |    Foot     |    Foot     |    Foot     |    Foot     |
			//     |  | |           |             |             |             |             |             |
			//     |Fo|F|           |             |             |             |             |             |
			//     |  | |           |             |             |             |             |             |
			//     |  | |         0 +-------------+-------------+-------------+-------------+-------------+
			//     |  | |           0             39            78            117           156           195   y
			// 117 +--+-+
			//     |  | |
			//     |  | |
			//     |Fo|F|
			//     |  | |
			//     |  | |
			//     |  | |
			//  78 +--+-+
			//     |  | |
			//     |  | |
			//     |  | |
			//     |Fo|F|
			//     |  | |
			//     |  | |
			//     |  | |
			//  39 +--+-+
			//     |  | |
			//     |  | |
			//     |Fo|F|
			//     |  | |
			//     |  | |
			//     |  | |
			//   0 +--+-+
			//     0  7 14   x
			// </figure>
			figure(result);
			PackagerResultAssert.assertThat(result).isSuccess();
			Container pack = result.get(0);
			assertNotNull(pack);
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}

	}

	@Test
	void issue440() {
		Container build = Container.newBuilder()
				.withId("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(build)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.build();

		try {
			for (int i = 1; i <= 10; i++) {
				int boxCountPerStackableItem = i;
	
				List<BoxItem> products = Arrays.asList(
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
	
				PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(copy(products)).build();
				// <figure>
				//              /-------|                     z
				//             /       /|                     7670 +----------+
				//            /       / |                          |          |
				//           /       /  |                          |          |
				//      z   /       /  /----|                      |          |
				//         /       /  /    /||                6670 |          +-------+
				//      | /       /  /    / ||                     |          |       |
				//      |/       /  /    /  ||                     |          |       |
				// 7670 |-------|  /    /   ||                     |    6     |       |
				//      |       | /    /    ||                     |          |       |
				//      |       |/    /     ||                     |          |   8   |
				// 6670 |       |----|      ||                     |          |       |
				//      |       |    |     / |                     |          |       |
				//      |   6   |    |    / /                      |          |       |
				//      |       | 8  |   / //                      |          |       |
				//      |       |    |  /-------|                  |          |       |
				//      |       |    | /       /|---|    y    4570 +----------++------+
				//      |       |    |/       / |  /|              |     2     |
				// 4570 |---2---|----|       / /  / |  /      4120 +-----------+
				// 4120 |-------|  //       / /| /  | /            |           |
				//      |   1   | //       / / |/   |/ 2280        |     1     |
				//      |       |//       / / //   // 2190         |           |
				// 3370 |-------|/       / / //   // 2060     3370 +-----------+
				// 3070 |  10   |-------| / //   //           3070 |           +----------+
				//      |       |   5   |/ //   // 1700            |    10     |          |
				// 2320 |-------|-----|-| //   // 1450             |           |    5     |
				//      |             |  //   // 1200              |           |          |
				//      |      9      | //   //               2320 +-----------+--------+-+
				//      |             |//   //                2000 |                    |
				// 1200 |-------------||   //                 1770 |         9          |
				//      |              |  //                       |                    |
				//      |      4       | //                        |                    |
				//      |              |//                    1200 +--------------------++
				//    0 |--------------|--- x                      |                     |
				//      0     1130    2340                         |                     |
				//                                                 |          4          |
				//                                                 |                     |
				//                                                 |                     |
				//                                               0 +---------------------+
				//                                                 0          1130        2340   x
				//
				// y                                                                                  z
				// 2280 +----------------------------------+                                          7670 +--------------+
				// 2190 |                                  +-----------------------------+                 |              |
				//      |                                  |              4              |                 |      6       |
				// 2060 |                                  +-------+                     |                 |              |
				//      |                                  |       |                     |            6670 +-----------+  |
				//      |                2                 |   3   |                     |                 |           |  |
				//      |                                  |       |                     |                 |           |  |
				//      |                                  |       |                     |                 |           |  |
				// 1700 |                                  +-------+-----------------+   |                 |           |  |
				//      |                                  |                         |   |                 |     8     |  |
				//      |                                  |            9            |   |                 |           |  |
				// 1500 +--------------------------------+ |                         |   |                 |           |  |
				// 1450 |                                | +-------------------------+---+-+               |           |  |
				//      |                                | |                               |               |           |  |
				//      |                                | |               5               |               |           |  |
				//      |                                | |                               |          4570 +-----------+--+------+
				// 1200 |                                +-+--------------------+          |               |          2          |
				//      |                                |                      |          |          4120 +---------------------+
				//      |                                |                      |          |               |                     |
				//      |                                |                      |          |               |          1          |
				//      |                                |                      |          |               |                     |
				//      |                                |                      |          |          3370 +-------------+-------+
				//      |               6                |                      |          |          3070 +-------------+       |
				//      |                                |                      |          |               |             |  10   |
				//      |                                |          8           |          |               |      5      |       |
				//      |                                |                      |          |               |             |       |
				//      |                                |                      |          |          2320 +-------------+--+---++
				//      |                                |                      |          |          2000 |                +-7++
				//      |                                |                      |          |          1770 |       9        +--++
				//      |                                |                      |          |               |                |3 ||
				//      |                                |                      |          |               |                |  ||
				//      |                                |                      |          |          1200 +----------------+--+++
				//      |                                |                      |          |               |                     |
				//    0 +--------------------------------+----------------------+----------+               |                     |
				//      0                                1130      1500         1930 2120  2340   x        |          4          |
				//                                                                                         |                     |
				//                                                                                         |                     |
				//                                                                                       0 +---------------------+
				//                                                                                         0           1200 1700 2280   y
				// </figure>
				figure(result);
				if(result.isSuccess()) {
					List<Container> packList = result.getContainers();
					assertTrue(i >= packList.size());
					
					validate(packList);
					PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
							.withContainerItems(containerItems)
							.withMaxContainerCount(packList.size())
							.withBoxItems(products));
				}
			}
		} finally {
			packager.close();
		}

	}

	@Test
	void testCorrectLevelZOffsetAdjustments() { // issue 450
		Container build = Container.newBuilder()
				.withId("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(build)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.build();

		try {
			int boxCountPerStackableItem = 1;
	
			List<BoxItem> products = Arrays.asList(
					createStackableItem("1", 1200, 750, 2280, 285, boxCountPerStackableItem),
					createStackableItem("2", 1200, 450, 2280, 155, boxCountPerStackableItem),
					createStackableItem("3", 360, 360, 570, 20, boxCountPerStackableItem),
					createStackableItem("4", 2250, 1200, 2250, 900, boxCountPerStackableItem),
					createStackableItem("5", 1140, 750, 1450, 395, boxCountPerStackableItem),
					createStackableItem("6", 1130, 1500, 3100, 800, boxCountPerStackableItem),
					createStackableItem("7", 800, 490, 1140, 156, boxCountPerStackableItem));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			// <figure>
			//           /------------|                        z
			//      z   /            /|                        5500 +--------------+
			//         /            / |                             |              |
			//      | /            /  |                             |              |
			//      |/            /   |                             |              |
			// 5500 |------------|    |                             |              |
			//      |            |    |                             |              |
			//      |            |    |                             |              |
			//      |            |    |                             |              |
			//      |            |    |                             |              |
			//      |            |    |                             |              |
			//      |            |    |                             |      6       |
			//      |            |    |                             |              |
			//      |     6      |    |                             |              |
			//      |            |    |--|                          |              |
			//      |            |    | /|                          |              |
			//      |            |    |/ |                          |              |
			//      |            |    | /|                          |              |
			//      |            |   / / |------------|             |              |
			//      |            |  / /------------| /|             |              |
			//      |            | /---|          /|/7|             |              |
			//      |            |/   /|         / | /              |              |
			// 2310 |------------|---| |        /  |/|         2310 +--------------++----+
			//      |     2      | 3 |/        /   | |    y         |       2       | 3  |
			// 1950 |------------|---|--------|   /  |              |               |    |
			//      |            |            |  /   |  /      1950 +---------------+----+----------+
			//      |     1      |     5      | /    | /       1690 |               |               |
			//      |            |            |/     |/ 2280        |       1       |       5       |
			// 1200 |------------|-----------||     //              |               |               |
			//      |                        |     //               |               |               |
			//      |                        |    // 1450      1200 +---------------+--------------++
			//      |           4            |   //                 |                              |
			//      |                        |  //                  |                              |
			//      |                        | // 570               |                              |
			//      |                        |//                    |              4               |
			//    0 |------------------------|--- x                 |                              |
			//      0          1130         2340                    |                              |
			//                                                      |                              |
			//                                                    0 +------------------------------+
			//                                                      0              1130  1560       2340   x
			//
			// y                                                                                  z
			// 2280 +----------------------------------+                                          5500 +-------------------+
			// 2250 |                                  +-------------------------------+               |                   |
			//      |                                  |                               |               |                   |
			//      |                                  |                               |               |                   |
			//      |                                  |                               |               |                   |
			//      |                2                 |                               |               |                   |
			//      |                                  |               7               |               |                   |
			//      |                                  |                               |               |                   |
			//      |                                  |                               |               |                   |
			//      |                                  |                               |               |                   |
			//      |                                  |                               |               |         6         |
			// 1500 +--------------------------------+ |                               |               |                   |
			// 1450 |                                | +-------------------------------+               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |               5               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |               |                   |
			//      |                                | |                               |          2310 +-------+-----------+----------+
			//      |               6                | |                               |               |   3   |          2           |
			//      |                                | |                               |               |       |                      |
			//      |                                | |                               |          1950 +-------+-----------+----1-----+
			//  570 |                                | +---------+                     |          1690 |                   +----------+
			//      |                                | |         |                     |               |         5         |          |
			//      |                                | |         |                     |               |                   |    7     |
			//      |                                | |         |                     |               |                   |          |
			//      |                                | |    3    |                     |          1200 +-------------------+----------+
			//      |                                | |         |                     |               |                              |
			//      |                                | |         |                     |               |                              |
			//      |                                | |         |                     |               |                              |
			//    0 +--------------------------------+-+---------+---------------------+               |              4               |
			//      0                                1130        1560                  2340   x        |                              |
			//                                                                                         |                              |
			//                                                                                         |                              |
			//                                                                                       0 +------------------------------+
			//                                                                                         0       570         1450       2280   y
			// </figure>
			figure(result);
			List<Container> packList = result.getContainers();
			validate(packList);
			
			PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
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

	void validate(Container container) {
		StackAssert.assertThat(container.getStack()).placementsDoNotIntersect();
	}

	void validate(List<Container> list) {
		for (Container container : list) {
			StackAssert.assertThat(container.getStack()).placementsDoNotIntersect();
		}
	}

	@Test
	@Disabled
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(LargestAreaFitFirstPackager.newBuilder().build());
	}

	public static int countLevels(Container container) {
		int count = 0;
		List<Placement> placements = container.getStack().getPlacements();
		for (Placement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteX() == 0 && stackPlacement.getAbsoluteY() == 0) {
				count++;
			}
		}
		
		return count;
	}

	@Test
	void testStackingRectanglesWithObstacles() {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
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
				// <figure>
				//         /-------/-------/-------|   y   z                                 y                                 z
				//        /   B   /   E   /   H   /|       1 +-------+-------+-------+       3 +-------+-------+-------+       1 +-------+-------+-------+
				//   z   /-------/-------/-------| | /       |#######|       |       |         |       |       |       |         |       |       |       |
				//      /   A   /   D   /   G   /| |/        |#######|   C   |   F   |         |   B   |   E   |   H   |         |   F   |   G   |   H   |
				//   | /-------/-------/-------| | | 3       |#######|       |       |         |       |       |       |         |       |       |       |
				//   |/#######/       /       /| |/        0 +-------+-------+-------+       2 +-------+-------+-------+       0 +-------+-------+-------+
				// 1 |-------|-------|-------| | | 2         0       1       2       3   x     |       |       |       |         0       1       2       3   y
				//   |#######|       |       | |/                                              |   A   |   D   |   G   |
				//   |#######|   C   |   F   | | 1                                             |       |       |       |
				//   |#######|       |       |/                                              1 +-------+-------+-------+
				// 0 |-------|-------|-------|-- x                                             |#######|       |       |
				//   0       1       2       3                                                 |#######|   C   |   F   |
				//                                                                             |#######|       |       |
				//                                                                           0 +-------+-------+-------+
				//                                                                             0       1       2       3   x
				// </figure>
				figure(build);
				
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

	/**
	 * Container 2 x 2 x 2 with a unit cube obstacle in a corner of the floor, and groups of 3 (a) and 4 (b) unit cubes: all 7 boxes fit around the
	 * obstacle.
	 */
	@Test
	void testStackingGroupsWithObstacles() {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Container container = Container.newBuilder()
					.withDescription("1")
					.withEmptyWeight(1)
					.withSize(2, 2, 2)
					.withMaxLoadWeight(100)
					.build();

			List<BoxItemGroup> groups = new ArrayList<>();
			for (String id : List.of("a", "b")) {
				List<BoxItem> products = new ArrayList<>();
				int count = id.equals("a") ? 3 : 4;
				for(int i = 0; i < count; i++) {
					products.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(1, 1, 1).withWeight(1).build(), 1));
				}
				groups.add(new BoxItemGroup(id, products));
			}

			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				b.withObstacles(o -> o.withObstacle(0, 0, 0, 1, 1, 1));
			}).withBoxItemGroups(groups).build();
			// <figure>
			//   z   /-------/-------|       z                         y                         z
			//      /  a0   /  a2   /|       2 +-------+-------+       2 +-------+-------+       2 +-------+-------+
			//   | /-------/-------| |         |       |       |         |       |       |         |       |       |
			//   |/       /       /| |         |  b3   |  a1   |         |  a0   |  a2   |         |  a1   |  a2   |
			// 2 |-------|-------| | |   y     |       |       |         |       |       |         |       |       |
			//   |       |       | |/|       1 +-------+-------+       1 +-------+-------+       1 +-------+-------+
			//   |  b3   |  a1   | | | /       |#######|       |         |       |       |         |       |       |
			//   |       |       |/|b|/        |#######|  b1   |         |  b3   |  a1   |         |  b1   |  b2   |
			// 1 |-------|-------| | | 2       |#######|       |         |       |       |         |       |       |
			//   |#######|       | |/        0 +-------+-------+       0 +-------+-------+       0 +-------+-------+
			//   |#######|  b1   | | 1         0       1       2   x     0       1       2   x     0       1       2   y
			//   |#######|       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			// </figure>
			figure(build);

			assertTrue(build.isSuccess());
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();

			Container packed = build.getContainers().get(0);
			Placement obstacle = packed.getObstacles().get(0);
			List<Placement> placements = packed.getStack().getPlacements();
			assertEquals(7, placements.size());
			for (Placement placement : placements) {
				assertFalse(placement.intersects3D(obstacle));
			}
		} finally {
			packager.close();
		}
	}

	//
	//  container 2 x 1 x 3; boxes in order a (1 x 1 x 1), b (1 x 1 x 2), c (1 x 1 x 1). The first level is as high as
	//  a, so b is skipped, and c is placed. Then b waits for the next container: on the next level of the first
	//  container, it would be after c.
	//
	@Test
	void testSkippedBoxWaitsForTheNextContainer() {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			Container container = Container.newBuilder()
					.withDescription("1")
					.withEmptyWeight(1)
					.withSize(2, 1, 3)
					.withMaxLoadWeight(100)
					.build();

			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withRotate2D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 2).withRotate2D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("c").withSize(1, 1, 1).withRotate2D().withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder()
					.withContainerItems(new ContainerItem(container, 2))
					.withBoxItems(products)
					.withOrder(Order.CHRONOLOGICAL_ALLOW_SKIPPING)
					.withMaxContainerCount(2)
					.build();
			// <figure>
			// container 1 of 2: 1
			//   z                         z                         y                         z
			//                             1 +-------+-------+       1 +-------+-------+       1 +-------+
			//   | /-------/-------|   y     |       |       |         |       |       |         |       |
			//   |/       /       /|         |   a   |   c   |         |   a   |   c   |         |   c   |
			// 1 |-------|-------| | /       |       |       |         |       |       |         |       |
			//   |       |       | |/      0 +-------+-------+       0 +-------+-------+       0 +-------+
			//   |   a   |   c   | | 1       0       1       2   x     0       1       2   x     0       1   y
			//   |       |       |/
			// 0 |-------|-------|-- x
			//   0       1       2
			//
			// container 2 of 2: 1
			//   z                 z                 y                 z
			//                     2 +-------+       1 +-------+       2 +-------+
			//   | /-------|         |       |         |       |         |       |
			//   |/       /|         |       |         |   b   |         |       |
			// 2 |-------| |         |       |         |       |         |       |
			//   |       | |         |   b   |       0 +-------+         |   b   |
			//   |       | |   y     |       |         0       1   x     |       |
			//   |       | |         |       |                           |       |
			//   |   b   | | /       |       |                           |       |
			//   |       | |/      0 +-------+                         0 +-------+
			//   |       | | 1       0       1   x                       0       1   y
			//   |       |/
			// 0 |-------|-- x
			//   0       1
			// </figure>
			figure(build);

			assertTrue(build.isSuccess());
			assertEquals(2, build.size());
			org.assertj.core.api.Assertions.assertThat(build.get(0).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("a", "c");
			org.assertj.core.api.Assertions.assertThat(build.get(1).getStack().getPlacements()).extracting(p -> p.getStackValue().getBox().getId()).containsExactly("b");
		} finally {
			packager.close();
		}
	}

	/**
	 * Container 6 x 2 x 2: box A is 2 high and sets the height of the level, boxes B are 1 high. A second box B fits on the floor
	 * beside the first one or on top of it; the two candidates have the same volume, weight and area, so only their height differs.
	 * The floor is used before boxes are stacked.
	 *
	 * <pre>
	 * Side view, as expected:                 Not like this:
	 *
	 *   z=1 |   A   |       |       |           z=1 |   A   |   B   |       |
	 *   z=0 |   A   |   B   |   B   |           z=0 |   A   |   B   |       |
	 *       x=0     x=2     x=4     x=6           x=0     x=2     x=4     x=6
	 * </pre>
	 */
	@Test
	void testSpreadOnTheFloorBeforeStackingWithinALevel() {
		Container container = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(6, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withSize(2, 2, 2).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withSize(2, 2, 1).withWeight(1).build(), 2));

			PackagerResult build = packager.newResultBuilder().withContainerItems(new ContainerItem(container, 1)).withBoxItems(products).build();
			assertTrue(build.isSuccess());
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();

			List<Placement> placements = build.getContainers().get(0).getStack().getPlacements();
			assertEquals(3, placements.size());
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(2, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(4, 0, 0).hasBoxItemId("B");
		} finally {
			packager.close();
		}
	}

	/**
	 * Container 4 x 2 x 2 with a platform (X) of 2 x 2 x 1 on the floor. The first box of a level fits on the floor beside the platform or on top of it;
	 * the two candidates have the same volume, weight and area, so only their height differs. The floor is preferred.
	 *
	 * <pre>
	 * Side view, as expected:                 Not like this:
	 *
	 *   z=1 |       |       |                   z=1 |   B   |       |
	 *   z=0 |   X   |   B   |                   z=0 |   X   |       |
	 *       x=0     x=2     x=4                     x=0     x=2     x=4
	 * </pre>
	 */
	@Test
	void testFirstPlacementOnTheFloorBeforeOnAnObstacle() {
		Container container = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(4, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withSize(2, 2, 1).withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder().withContainerItem(b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				b.withObstacles(o -> o.withObstacle(0, 0, 0, 2, 2, 1));
			}).withBoxItems(products).build();
			assertTrue(build.isSuccess());
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();

			List<Placement> placements = build.getContainers().get(0).getStack().getPlacements();
			assertEquals(1, placements.size());
			assertThat(placements.get(0)).isAt(2, 0, 0).hasBoxItemId("B");
		} finally {
			packager.close();
		}
	}

	/**
	 * Container 6 x 2 x 2 with a platform (X) of 2 x 2 x 1 on the floor. Box A is 2 high and sets the height of the level. Box B fits on the floor beside the platform
	 * or on top of it; the two candidates have the same volume, weight and area, so only their height differs. The floor is preferred.
	 *
	 * <pre>
	 * Side view, as expected:                 Not like this:
	 *
	 *   z=1 |   A   |       |       |           z=1 |   A   |   B   |       |
	 *   z=0 |   A   |   X   |   B   |           z=0 |   A   |   X   |       |
	 *       x=0     x=2     x=4     x=6           x=0     x=2     x=4     x=6
	 * </pre>
	 */
	@Test
	void testNextPlacementOnTheFloorBeforeOnAnObstacle() {
		Container container = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(6, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withSize(2, 2, 2).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).withSize(2, 2, 1).withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder().withContainerItem(b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				b.withObstacles(o -> o.withObstacle(2, 0, 0, 2, 2, 1));
			}).withBoxItems(products).build();
			assertTrue(build.isSuccess());
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();

			List<Placement> placements = build.getContainers().get(0).getStack().getPlacements();
			assertEquals(2, placements.size());
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(4, 0, 0).hasBoxItemId("B");
		} finally {
			packager.close();
		}
	}
}
