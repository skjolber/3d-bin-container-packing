package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.test.assertj.PackagerAssert;

public class PlainPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {
		
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();
		
		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);

	}

	@Test
	void testStackingRectangles() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);

	}

	@Test
	void testStackingSquaresAndRectangle() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 10, 10).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();
		
		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);

	}

	@Test
	void testStackingDecreasingRectangles() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);

	}

	@Test
	void testStackingRectanglesTwoLevels() {
		
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);

	}

	@Test
	void testStackingRectanglesThreeLevels() {
		
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 3));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);
	}

	@Test
	void testStackingNotPossible() {
		
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				// capacity is 3*2*3 = 18
				.withLimited(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertThat(build.getContainers()).isEmpty();
	}

	@Test
	void testStackingMultipleContainers() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(1, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("3").withEmptyWeight(1).withSize(2, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("4").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withLimited(containers, 1)
				.build();
		
		PlainPackager packager = PlainPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		assertValid(build);
		
		Container fits = build.get(0);
		assertEquals(fits.getVolume(), containers.get(3).getVolume());
	}

	@Test
	void issue440() {
		DefaultContainer build = Container.newBuilder()
				.withDescription("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		
		PlainPackager packager = PlainPackager.newBuilder()
				.build();

		for (int i = 1; i <= 10; i++) {
			int boxCountPerStackableItem = i;

			List<ContainerItem> containerItems = ContainerItem
					.newListBuilder()
					.withLimited(build, i + 2)
					.build();

			List<StackableItem> stackableItems = Arrays.asList(
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

			PackagerResult result = packager.newResultBuilder().withContainers(containerItems).withStackables(stackableItems).build();
			List<Container> packList = result.getContainers();

			assertNotNull(packList);
			assertTrue(i >= packList.size());
		}
	}

	private StackableItem createStackableItem(String id, int width, int height, int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new StackableItem(box, boxCountPerStackableItem);
	}

	@Test
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(PlainPackager.newBuilder());
	}

}
