package com.github.skjolber.packing.packer.laff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;

public class LargestAreaFitFirstPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 1, 1, 3, 1, 1, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withOrientation(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withOrientation(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withOrientation(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("B");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");
		
		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
	}
	
	@Test
	void testStackingRectangles() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 2, 1, 3, 2, 1, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(0, 1, 0).hasStackableName("B");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");
		
	}

	@Test
	void testStackingSquaresAndRectangle() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(10, 10, 1, 6, 10, 10, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
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
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(6, 1, 1, 6, 1, 1, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(3, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(3, 0, 0).hasStackableName("B"); // point with lowest x is selected first
		assertThat(placements.get(2)).isAt(5, 0, 0).hasStackableName("C");

	}
	
	@Test
	void testStackingRectanglesTwoLevels() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 2, 3, 2, 2, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 1, 1).withWeight(1).build(), 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		LevelStack levelStack = (LevelStack)fits.getStack();
		assertEquals(2, levelStack.getLevels().size());
		
		List<StackPlacement> placements = fits.getStack().getPlacements();
		
		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(0, 1, 0).hasStackableName("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("B");
		
		assertThat(placements.get(3)).isAt(0, 0, 1).hasStackableName("B");
		assertThat(placements.get(4)).isAt(0, 1, 1).hasStackableName("C");
		assertThat(placements.get(5)).isAt(2, 0, 1).hasStackableName("C");
	}
	
	@Test
	void testStackingRectanglesThreeLevels() {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotateXYZ(3, 2, 3, 3, 2, 3, 100, null).withStack(new ValidatingStack()).build());
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 2, 1).withWeight(1).build(), 1));

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
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(1, 2, 1).withWeight(1).build(), 18)); // 12
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(1, 1, 1).withWeight(1).build(), 1)); // 1

		Container fits = packager.pack(products);

		assertNull(fits);
	}



}
