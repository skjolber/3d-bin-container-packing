package com.github.skjolber.packing.packer.laff;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.old.BoxItem;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;



public class FastLargestAreaFitFirstPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(10, 10, 10, 10, 10, 10, 100, null).withStack(new DefaultStack()).build());
		
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotate(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotate(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotate(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
	}
	
	@Test
	void testStackingRectangles() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 1, 3, 2, 1, 100, null).withStack(new DefaultStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
	}

	@Test
	void testStackingSquaresAndRectangle() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(10, 10, 1, 6, 10, 10, 100, null).withStack(new DefaultStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(5, 5, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(5, 5, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
	}

	@Test
	void testStackingDecreasingRectangles() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(6, 1, 1, 6, 1, 1, 100, null).withStack(new DefaultStack()).build());
		
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(3, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
	}
	
	@Test
	void testStackingRectanglesTwoLevels1() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 2, 3, 2, 2, 100, null).withStack(new DefaultStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(2, 1, 1).withWeight(1).build(), 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		LevelStack levelStack = (LevelStack)fits.getStack();
		assertEquals(2, levelStack.getLevels().size());
		System.out.println(fits.getStack().getPlacements());
	}
	
	@Test
	void testStackingRectanglesThreeLevels() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 3, 3, 2, 3, 100, null).withStack(new DefaultStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotate(2, 2, 1).withWeight(1).build(), 3));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		LevelStack levelStack = (LevelStack)fits.getStack();
		assertEquals(3, levelStack.getLevels().size());
		System.out.println(fits.getStack().getPlacements());
		
		List<StackPlacement> placements = fits.getStack().getPlacements();
	}


}
