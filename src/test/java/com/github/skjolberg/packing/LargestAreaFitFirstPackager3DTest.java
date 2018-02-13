package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class LargestAreaFitFirstPackager3DTest extends AbstractPackagerTest {

	@Test
	public void testStackingSquaresOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("A", 5, 5, 1));
		products.add(new Box("B", 5, 5, 1));
		products.add(new Box("C", 5, 5, 1));
		products.add(new Box("D", 5, 5, 1));
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("E", 5, 10, 1));
		products.add(new Box("F", 5, 10, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangle() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {
		
		List<Dimension> containers = new ArrayList<Dimension>();
		containers.add(new Dimension(10, 10, 3));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 6, 10, 2));
		products.add(new Box("L", 4, 10, 1));
		products.add(new Box("K", 4, 10, 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);

		assertEquals(1, fits.getLevels().get(fits.getLevels().size() - 1).getHeight());
	}
	
	@Test
	public void testStackingBinary1() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(2, 2, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		for(int i = 0; i < 4; i++) {
			products.add(new Box("K", 1, 1, 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingBinary2() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(8, 8, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 4, 4, 1));
		
		for(int i = 0; i < 4; i++) {
			products.add(new Box("K", 2, 2, 1));
		}
		for(int i = 0; i < 16; i++) {
			products.add(new Box("K", 1, 1, 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingTooHigh() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 6));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 5));

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}	
	
	
	@Test
	public void testStacking3xLP() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(350, 150, 400));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products1 = new ArrayList<Box>();

		products1.add(new Box("A", 400, 50, 350));
		products1.add(new Box("B", 400, 50, 350));
		products1.add(new Box("C", 400, 50, 350));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		
		List<Box> products2 = new ArrayList<Box>();
		products2.add(new Box("A", 350, 50, 400));
		products2.add(new Box("B", 350, 50, 400));
		products2.add(new Box("C", 350, 50, 400));

		Container fits2 = packager.pack(products2);
		assertNotNull(fits2);

	}
	
	@Test
	public void testIsse2() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(36, 55, 13));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<Box> products1 = new ArrayList<Box>();

		products1.add(new Box("01", 38, 10, 10));
		products1.add(new Box("02", 38, 10, 10));
		products1.add(new Box("03", 38, 10, 10));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		
		products1.add(new Box("04", 38, 10, 10));
		Container fits2 = packager.pack(products1);
		assertNull(fits2);
	}

}
