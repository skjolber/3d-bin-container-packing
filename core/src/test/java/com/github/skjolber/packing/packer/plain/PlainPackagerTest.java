package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackPlacement;
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
					.withContainerItem( b -> {
						b.withContainerItem(containerItem);
					})
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
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withMaxContainerCount(5).withBoxItems(products).build();
			assertValid(build);
	
			assertEquals(build.size(), 2);
			assertEquals(build.get(0).getVolume(), 7);
			assertEquals(build.get(1).getVolume(), 3);
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
		assertDeadlineRespected(PlainPackager.newBuilder());
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
			products.add(new BoxItem(Box.newBuilder().withId("matches-2").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withBoxItemListenerBuilderSupplier(NoMatchesWithPetrolBoxItemListener.newFactory());
			})
					.withMaxContainerCount(5)
					.withBoxItems(products)
					.build();
			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);
			
			for(Container c : containers) {
				assertEquals(c.getStack().size(), 1);
			}
			
			assertEquals(containers.get(0).getStack().getPlacements().get(0).getBoxItem().getBox().getId(), "matches-2");
			assertEquals(containers.get(1).getStack().getPlacements().get(0).getBoxItem().getBox().getId(), "petrol-1");
			
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
			BoxItem boxItem2 = new BoxItem(Box.newBuilder().withId("matches-2").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1);
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem1));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem2));
			
			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withBoxItemGroupListenerBuilderSupplier(NoMatchesWithPetrolBoxItemGroupListener.newFactory());
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
			
			assertEquals(containers.get(0).getStack().getPlacements().get(0).getBoxItem().getBox().getId(), "petrol-1");
			assertEquals(containers.get(1).getStack().getPlacements().get(0).getBoxItem().getBox().getId(), "matches-2");
			
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
					.withProperty(MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener.KEY, Boolean.TRUE)
					.build(), 2);
	
			List<BoxItem> products = new ArrayList<>();
			products.add(boxItem1);
			products.add(boxItem2);
	
			PackagerResult build = packager.newResultBuilder()
				.withContainerItem( b -> {
					b.withContainerItem(new ContainerItem(container, 5));
					b.withBoxItemListenerBuilderSupplier(MaxFireHazardBoxItemPerContainerBoxItemListener.newFactory(1));
				})
				.withMaxContainerCount(5)
				.withBoxItems(products)
				.build();

			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);

			for(Container c : containers) {
				System.out.println(c);
				
				for(StackPlacement s : c.getStack().getPlacements()) {
					System.out.println(" " + s);
				}
			}
			
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
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.build(), 1);
			
			BoxItem boxItem2 = new BoxItem(Box.newBuilder()
					.withId("firehazard")
					.withRotate3D()
					.withSize(1, 1, 1)
					.withWeight(1)
					.withProperty(MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener.KEY, Boolean.TRUE)
					.build(), 2);
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", Arrays.asList(boxItem1));
			BoxItemGroup boxItemGroup2 = new BoxItemGroup("b", Arrays.asList(boxItem2));
			
			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 5));
				b.withBoxItemGroupListenerBuilderSupplier(MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener.newFactory(1));
			})
					.withMaxContainerCount(5)
					.withBoxItemGroups(Arrays.asList(boxItemGroup1, boxItemGroup2))
					.build();
			assertTrue(build.isSuccess());

			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 2);

			for(Container c : containers) {
				System.out.println(c);
				
				for(StackPlacement s : c.getStack().getPlacements()) {
					System.out.println(" " + s);
				}
			}
			
			assertValid(build);
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
				b.withFilteredPointsBuilderSupplier(HeavyItemsOnGroundLevel.newFactory(2));
			}).withBoxItems(products).build();
			
			List<Container> containers = build.getContainers();
			for(Container c : containers) {
				System.out.println(c);
				
				for(StackPlacement s : c.getStack().getPlacements()) {
					System.out.println(" " + s);
				}
			}
			
			assertEquals("A", containers.get(0).getStack().getPlacements().get(0).getBoxItem().getBox().getId());
			
			assertValid(build);
		} finally {
			packager.close();
		}
	}
}
