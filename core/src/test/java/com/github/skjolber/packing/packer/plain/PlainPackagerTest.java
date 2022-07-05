package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

public class PlainPackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
	}
	
	@Test
	void testStackingRectangles() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
	}

	@Test
	void testStackingSquaresAndRectangle() {

		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 10, 10).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(5, 5, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
	}

	@Test
	void testStackingDecreasingRectangles() {

		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(3, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
	}
	
	@Test
	void testStackingRectanglesTwoLevels() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 2).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 2));

		Container fits = packager.pack(products);
		assertValid(fits);
	}
	
	@Test
	void testStackingRectanglesThreeLevels() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 3));

		Container fits = packager.pack(products);
		assertNotNull(fits);
	}

	@Test
	void testStackingNotPossible() {
		List<Container> containers = new ArrayList<>();

		// capacity is 3*2*3 = 18
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 3).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 18)); // 12
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1)); // 1

		Container fits = packager.pack(products);

		assertNull(fits);
	}
	
	@Test
	void testStackingMultipleContainers() {
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(1, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("3").withEmptyWeight(1).withSize(2, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		containers.add(Container.newBuilder().withDescription("4").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
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
				.withContainers(Arrays.asList(build))
				.build();

		for(int i = 1; i <= 10; i++) { 
			int boxCountPerStackableItem = i;

			List<StackableItem> stackableItems = Arrays.asList(
					createStackableItem("1",1200,750, 2280, 285, boxCountPerStackableItem),
					createStackableItem("2",1200,450, 2280, 155, boxCountPerStackableItem),
					createStackableItem("3",360,360, 570, 20, boxCountPerStackableItem),
					createStackableItem("4",2250,1200, 2250, 900, boxCountPerStackableItem),
					createStackableItem("5",1140,750, 1450, 395, boxCountPerStackableItem),
					createStackableItem("6",1130,1500, 3100, 800, boxCountPerStackableItem),
					createStackableItem("7",800,490, 1140, 156, boxCountPerStackableItem),
					createStackableItem("8",800,2100, 1200, 135, boxCountPerStackableItem),
					createStackableItem("9",1120,1700, 2120, 160, boxCountPerStackableItem),
					createStackableItem("10",1200,1050, 2280, 390, boxCountPerStackableItem)
					);

			List<Container> packList = packager.packList(stackableItems, i + 2);

			assertNotNull(packList);
			assertTrue(i >= packList.size());
		}
	}

	private StackableItem createStackableItem(String id, int width, int height,int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new StackableItem(box, boxCountPerStackableItem);
	}

	@Test
	void testAHugeProblemShouldRespectDeadline() {

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1900, 1500, 4000)
			.withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		PlainPackager packager = PlainPackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = Arrays.asList(
			box(1000, 12, 12,1),
			box(1000, 14, 14,1),
			box(100, 250, 410,2),
			box(104, 81, 46,1),
			box(116, 94, 48,1),
			box(120, 188, 368,1),
			box(1250, 4, 600,3),
			box(1270, 870, 45,1),
			box(135, 189, 75,2),
			box(13, 20, 2500,4),
			box(154, 195, 255,1),
			box(180, 170, 75,1),
			box(180, 60, 50,2),
			box(225, 120, 500,2),
			box(2500, 1200, 13,1),
			box(2500, 30, 600,1),
			box(250, 150, 230,1),
			box(28, 56, 1094,3),
			box(28, 75, 145,3),
			box(29, 71, 112,1),
			box(30, 30, 2000,2),
			box(30, 75, 150,2),
			box(310, 130, 460,10),
			box(313, 313, 16,18),
			box(32, 105, 163,2),
			box(32, 73, 150,2),
			box(355, 370, 161,1),
			box(36, 23, 23,2),
			box(380, 380, 130,1),
			box(385, 140, 55,1),
			box(38, 38, 30,6),
			box(397, 169, 133,2),
			box(39, 38, 28,2),
			box(39, 66, 206,2),
			box(40, 40, 2000,2),
			box(410, 410, 170,1),
			box(419, 646, 784,1),
			box(41, 29, 24,12),
			box(42, 34, 19,2),
			box(44, 35, 28,6),
			box(467, 174, 135,1),
			box(46, 41, 24,12),
			box(47, 44, 29,6),
			box(49, 36, 36,2),
			box(49, 48, 23,6),
			box(4, 4, 2500,4),
			box(50, 39, 25,2),
			box(50, 49, 21,6),
			box(52, 51, 21,6),
			box(55, 46, 45,2),
			box(570, 310, 85,29),
			box(58, 32, 32,1),
			box(614, 824, 96,1),
			box(61, 51, 26,14),
			box(625, 500, 50,6),
			box(640, 510, 1200,1),
			box(640, 960, 220,1),
			box(65, 48, 231,2),
			box(65, 64, 38,4),
			box(68, 66, 39,4),
			box(700, 325, 90,1),
			box(71, 42, 39,4),
			box(73, 43, 40,4),
			box(79, 78, 46,4),
			box(82, 80, 47,4),
			box(84, 67, 44,4),
			box(88, 52, 47,4),
			box(90, 800, 2040,1),
			box(970, 790, 2200,1));


		final long start = System.currentTimeMillis();
		Container fits = packager.pack(products, start + 200);
		final long duration = System.currentTimeMillis() - start;
		assertThat(duration).isLessThan(250);
		assertNull(fits);
	}
}
