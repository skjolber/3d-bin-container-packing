								package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
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
			assertValid(build);
	
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
			assertValid(result);
	
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
				System.out.println("Attempt " + i);
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
			assertValid(build);
	
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
				b.withBoxItemControlsBuilderFactory(NoLightersWithPetrolManifestControls.newFactory());
			})
					.withMaxContainerCount(5)
					.withBoxItems(products)
					.build();
			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);
			
			for(Container c : containers) {
				assertEquals(c.getStack().size(), 1);
			}
			
			assertEquals(containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "lighter-2");
			assertEquals(containers.get(1).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "petrol-1");
			
			assertValid(build);
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
				b.withBoxItemControlsBuilderFactory(NoLighterWithPetrolManifestControls.newFactory());
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(Arrays.asList(boxItemGroup1, boxItemGroup2))
					.build();
			assertTrue(build.isSuccess());
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);
			
			for(Container c : containers) {
				assertEquals(c.getStack().size(), 1);
			}
			
			assertEquals(containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "petrol-1");
			assertEquals(containers.get(1).getStack().getPlacements().get(0).getStackValue().getBox().getId(), "lighter-2");
			
			assertValid(build);
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
					b.withBoxItemControlsBuilderFactory(MaxFireHazardBoxItemPerContainerManifestControls.newFactory(1));
				})
				.withMaxContainerCount(5)
				.withBoxItems(products)
				.build();

			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);

			assertValid(build);
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
				b.withBoxItemControlsBuilderFactory(MaxFireHazardBoxItemGroupsPerContainerManifestControls.newFactory(1));
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(cloneGroups(groups))
					.withOrder(Order.CRONOLOGICAL)
					.build();
			assertTrue(result.isSuccess());

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			assertValid(result);
			
			assertValidUsingValidatorForGroups(Arrays.asList(new ContainerItem(container, 5)), 5, result, groups, Order.CRONOLOGICAL);
		} finally {
			packager.close();
		}
	}	

	@Test
	void testStackingSquaresOnSquareWithPointConstraints() {

		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(2, 1, 3)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(3).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				b.withPointControlsBuilderFactory(HeavyItemsOnGroundLevelPointControls.newFactory(2));
			}).withBoxItems(products).build();
			
			List<Container> containers = build.getContainers();
			
			assertEquals("A", containers.get(0).getStack().getPlacements().get(0).getStackValue().getBox().getId());
			
			assertValid(build);
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
					.withOrder(Order.CRONOLOGICAL)
					.withMaxContainerCount(10)
					.build();
			assertValid(result);
			
			List<Container> containers = result.getContainers();
			assertEquals(containers.size(), 2);
			
			int index = 0;
			for (Container c : containers) {
				for (Placement stackPlacement : c.getStack().getPlacements()) {
					assertEquals(stackPlacement.getStackValue().getBox().getId(), Integer.toString(index));
					index++;
				}
			}
			
			assertValidUsingValidator(Arrays.asList(containerItem), 10, result, products, Order.CRONOLOGICAL);
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
					.withOrder(Order.CRONOLOGICAL_ALLOW_SKIPPING)
					.withMaxContainerCount(10)
					.build();
			assertValid(result);
			
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
			
			assertValidUsingValidator(Arrays.asList(containerItem), 10, result, products, Order.CRONOLOGICAL_ALLOW_SKIPPING);

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
			
			assertValid(build);
			
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
			
			List<Placement> placements = build.getContainers().get(0).getStack().getPlacements();
			for(Placement placement : placements) {
				assertFalse(placement.getAbsoluteX() == 0 && placement.getAbsoluteY() == 0);
			}
			
			assertValid(build);
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
					.withBoxItemGroups(cloneGroups(groups))
					.withOrder(Order.CRONOLOGICAL)
					.build();
			assertTrue(result.isSuccess());

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			assertValid(result);
			
			assertValidUsingValidatorForGroups(Arrays.asList(new ContainerItem(container1, 5)), 5, result, groups, Order.CRONOLOGICAL);
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
					.withBoxItemGroups(cloneGroups(groups))
					.withOrder(Order.CRONOLOGICAL_ALLOW_SKIPPING)
					.build();
			assertTrue(result.isSuccess());

			List<Container> containers = result.getContainers();
			assertEquals(2, containers.size());

			assertValid(result);
			
			assertValidUsingValidatorForGroups(Arrays.asList(new ContainerItem(container1, 5)), 5, result, groups, Order.CRONOLOGICAL_ALLOW_SKIPPING);
		} finally {
			packager.close();
		}
	}	

}
