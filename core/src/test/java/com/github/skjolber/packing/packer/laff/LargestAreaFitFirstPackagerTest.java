package com.github.skjolber.packing.packer.laff;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.StackAssert;

public class LargestAreaFitFirstPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			assertTrue(result.isSuccess());
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
		} finally {
			packager.close();
		}
	}
	

	@Test
	void testStackingSquaresOnSquareForGroup() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", products);

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(Arrays.asList(boxItemGroup1)).build();
			assertTrue(result.isSuccess());
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
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

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(2, 0, 0).hasStackableName("B");
			assertThat(placements.get(2)).isAt(0, 1, 0).hasStackableName("C");
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

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
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

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
	
			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(3, 0, 0).hasStackableName("B"); // point with lowest x is selected first
			assertThat(placements.get(2)).isAt(5, 0, 0).hasStackableName("C");
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

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			Container fits = result.get(0);
	
			assertNotNull(fits);
	
			validate(fits);

			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(2, 0, 0).hasStackableName("A");
			assertThat(placements.get(2)).isAt(0, 1, 0).hasStackableName("B");
	
			assertThat(placements.get(3)).isAt(0, 0, 1).hasStackableName("B");
			assertThat(placements.get(4)).isAt(2, 0, 1).hasStackableName("C");
			assertThat(placements.get(5)).isAt(0, 1, 1).hasStackableName("C");
			
			assertEquals(2, countLevels(fits));
		} finally {
			packager.close();
		}
	}
	

	@Test
	void testStackingRectanglesTwoLevelsForGroups() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", products);

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(Arrays.asList(boxItemGroup1)).build();
			Container fits = result.get(0);
	
			assertNotNull(fits);

			validate(fits);

			List<StackPlacement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
			assertThat(placements.get(1)).isAt(2, 0, 0).hasStackableName("A");
			assertThat(placements.get(2)).isAt(0, 1, 0).hasStackableName("B");
	
			assertThat(placements.get(3)).isAt(0, 0, 1).hasStackableName("B");
			assertThat(placements.get(4)).isAt(2, 0, 1).hasStackableName("C");
			assertThat(placements.get(5)).isAt(0, 1, 1).hasStackableName("C");
			
			assertEquals(2, countLevels(fits));
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

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
	
			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
	
			Container fits = result.get(0);
	
			assertNotNull(fits);
			validate(fits);
			
			assertEquals(3, countLevels(fits));
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

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1
	
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
				.withDescription("1")
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
			assertTrue(result.isSuccess());
			Container pack = result.get(0);
			assertNotNull(pack);
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
	
				PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
				if(result != null) {
					System.out.println(result.isSuccess() + " " + i);
				} else {
					
				}
				if(true) continue;
				List<Container> packList = result.getContainers();
	
				assertNotNull(packList);
				assertTrue(i >= packList.size());
				validate(packList);
			}
		} finally {
			packager.close();
		}

	}

	@Test
	void testCorrectLevelZOffsetAdjustments() { // issue 450
		Container build = Container.newBuilder()
				.withDescription("1")
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
			List<Container> packList = result.getContainers();
			validate(packList);
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
		assertDeadlineRespected(LargestAreaFitFirstPackager.newBuilder());
	}

	public static int countLevels(Container container) {
		int count = 0;
		List<StackPlacement> placements = container.getStack().getPlacements();
		for (StackPlacement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteX() == 0 && stackPlacement.getAbsoluteY() == 0) {
				count++;
			}
		}
		
		return count;
	}

}
