package com.github.skjolber.packing.packer.laff;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
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

public class FastLargestAreaFitFirstPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			BoxItemGroup boxItemGroup1 = new BoxItemGroup("a", products);

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItemGroups(Arrays.asList(boxItemGroup1)).build();
			assertTrue(result.isSuccess());
			Container fits = result.get(0);
	
			assertNotNull(fits);
	
			List<StackPlacement> placements = fits.getStack().getPlacements();
			
			for(StackPlacement p : placements) {
				System.out.println(p.getStackValue().getBox().getId() + " " + p.getAbsoluteX() + "x" + p.getAbsoluteY() + "x" + p.getAbsoluteZ()); 
			}
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			Container fits = containers.get(0);
	
			assertEquals(2, LargestAreaFitFirstPackagerTest.countLevels(fits));
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 3));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			Container fits = containers.get(0);
	
			assertEquals(3, LargestAreaFitFirstPackagerTest.countLevels(fits));
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

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
			products.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertThat(containers).isEmpty();
		} finally {
			packager.close();
		}
	}

	@Test
	void issue453BoxesShouldNotFit() {
		Container container = Container
				.newBuilder()
				.withDescription("1")
				.withSize(70, 44, 56)
				.withEmptyWeight(0)
				.withMaxLoadWeight(100)
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				// capacity is 3*2*3 = 18
				.withContainer(container, 1)
				.build();

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager
				.newBuilder()
				.build();
		try {
			List<BoxItem> products = Arrays.asList(
					new BoxItem(Box.newBuilder().withId("1").withSize(32, 19, 24).withRotate3D().withWeight(0).build(), 1),
					new BoxItem(Box.newBuilder().withId("2").withSize(32, 21, 27).withRotate3D().withWeight(0).build(), 1),
					new BoxItem(Box.newBuilder().withId("3").withSize(34, 21, 24).withRotate3D().withWeight(0).build(), 1),
					new BoxItem(Box.newBuilder().withId("4").withSize(30, 19, 23).withRotate3D().withWeight(0).build(), 1),
					new BoxItem(Box.newBuilder().withId("5").withSize(30, 21, 25).withRotate3D().withWeight(0).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
	
			assertEquals(0, build.size());
		} finally {
			packager.close();
		}
	}

	@Test
	@Disabled
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(FastLargestAreaFitFirstPackager.newBuilder());
	}


	@Test
	public void testIssue698() {
		Container container1 = Container.newBuilder()
				.withId("1")
				.withDescription("Test container 1")
				.withSize(145, 220, 35)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container2 = Container.newBuilder()
				.withId("2")
				.withDescription("Test container 2")
				.withSize(160, 220, 77)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container3 = Container.newBuilder()
				.withId("3")
				.withDescription("Test container 3")
				.withSize(225, 310, 102)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container4 = Container.newBuilder()
				.withId("4")
				.withDescription("Test container 4")
				.withSize(200, 250, 150)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container5 = Container.newBuilder()
				.withId("5")
				.withDescription("Test container 5")
				.withSize(230, 230, 230)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container6 = Container.newBuilder()
				.withId("6")
				.withDescription("Test container 6")
				.withSize(215, 305, 250)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container7 = Container.newBuilder()
				.withId("7")
				.withDescription("Test container 7")
				.withSize(305, 305, 305)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		Container container8 = Container.newBuilder()
				.withId("8")
				.withDescription("Test container 8")
				.withSize(375, 375, 300)
				.withEmptyWeight(0)
				.withMaxLoadWeight(5000)
				.build();

		List<Container> containers = Arrays.asList(container1, container2, container3, container4, container5,
				container6, container7, container8).stream().sorted(Comparator.comparing(Container::getVolume))
				.collect(Collectors.toList());

		List<ContainerItem> containerItems = ContainerItem.newListBuilder()
				.withContainers(containers)
				.build();

		Box box = Box.newBuilder()
				.withId("test item")
				.withSize(15, 130, 130)
				.withRotate3D()
				.withWeight(3)
				.build();

		BoxItem stackableItems = new BoxItem(box, 20);
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager
				.newBuilder()
				.build();
		try {
			PackagerResult result = packager
					.newResultBuilder()
					.withMaxContainerCount(20)
					.withContainerItems(containerItems)
					.withBoxItems(stackableItems)
					.build();
	
			assertEquals(true, result.isSuccess());
		} finally {
			packager.close();
		}
	}
	
}
