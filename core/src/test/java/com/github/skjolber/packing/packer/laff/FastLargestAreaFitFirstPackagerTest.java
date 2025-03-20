package com.github.skjolber.packing.packer.laff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			Container fits = containers.get(0);
	
			LevelStack levelStack = (LevelStack)fits.getStack();
			assertEquals(2, levelStack.getLevels().size());
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			Container fits = containers.get(0);
	
			LevelStack levelStack = (LevelStack)fits.getStack();
			assertEquals(3, levelStack.getLevels().size());
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
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
	
			PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
	
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

}
