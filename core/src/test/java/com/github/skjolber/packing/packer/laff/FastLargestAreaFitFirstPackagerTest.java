package com.github.skjolber.packing.packer.laff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;

public class FastLargestAreaFitFirstPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(2, 2, 1, 2, 2, 1, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withOrientation(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withOrientation(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withOrientation(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
	}
	
	@Test
	void testStackingRectangles() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 1, 3, 2, 1, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
	}

	@Test
	void testStackingSquaresAndRectangle() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(10, 10, 1, 6, 10, 10, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(5, 5, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(5, 5, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
	}

	@Test
	void testStackingDecreasingRectangles() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(6, 1, 1, 6, 1, 1, 100, null).withStack(new ValidatingStack()).build());
		
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(3, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
	}
	
	@Test
	void testStackingRectanglesTwoLevels() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 2, 3, 2, 2, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		LevelStack levelStack = (LevelStack)fits.getStack();
		assertEquals(2, levelStack.getLevels().size());
	}
	
	@Test
	void testStackingRectanglesThreeLevels() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 3, 3, 2, 3, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withOrientation(2, 2, 1).withWeight(1).build(), 3));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		LevelStack levelStack = (LevelStack)fits.getStack();
		assertEquals(3, levelStack.getLevels().size());
	}

	@Test
	void testStackingNotPossible() {
		List<Container> containers = new ArrayList<>();

		// capacity is 3*2*3 = 18
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 3, 3, 2, 3, 100, null).withStack(new ValidatingStack()).build());
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(1, 2, 1).withWeight(1).build(), 18)); // 12
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(1, 1, 1).withWeight(1).build(), 1)); // 1

		Container fits = packager.pack(products);

		assertNull(fits);
	}
}
