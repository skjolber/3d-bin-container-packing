package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class LargestAreaFitFirstPackager3DTest extends AbstractPackagerTest {

	@Test
	public void testStackingSquaresOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("B", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("C", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("D", 5, 5, 1), 1));
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingSquaresOnSquareMultiLevel() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 2));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("B", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("C", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("D", 5, 5, 1), 1));

		products.add(new BoxItem(new Box("E", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("F", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("G", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("H", 5, 5, 1), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);
	}

	
	@Test
	public void testStackingRectanglesOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("E", 5, 10, 1), 1));
		products.add(new BoxItem(new Box("F", 5, 10, 1), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangle() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 5, 10, 1), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {
		
		List<Dimension> containers = new ArrayList<Dimension>();
		containers.add(new Dimension(10, 10, 3));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 6, 10, 2), 1));
		products.add(new BoxItem(new Box("L", 4, 10, 1), 1));
		products.add(new BoxItem(new Box("K", 4, 10, 2), 1));

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
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		for(int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1), 1));
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
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 4, 4, 1), 1));
		
		for(int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 2, 2, 1), 1));
		}
		for(int i = 0; i < 16; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1), 1));
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
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 6), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 5), 1));

		products.add(new BoxItem(new Box("J", 5, 10, 1), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}	
	
	
	@Test
	public void testStacking3xLP() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(350, 150, 400));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products1 = new ArrayList<BoxItem>();

		products1.add(new BoxItem(new Box("A", 400, 50, 350), 1));
		products1.add(new BoxItem(new Box("B", 400, 50, 350), 1));
		products1.add(new BoxItem(new Box("C", 400, 50, 350), 1));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		
		List<BoxItem> products2 = new ArrayList<BoxItem>();
		products2.add(new BoxItem(new Box("A", 350, 50, 400), 1));
		products2.add(new BoxItem(new Box("B", 350, 50, 400), 1));
		products2.add(new BoxItem(new Box("C", 350, 50, 400), 1));

		Container fits2 = packager.pack(products2);
		assertNotNull(fits2);

	}

	@Test
	public void testWith5Packets3D() {
		List<Box> containers = new ArrayList<>();
		containers.add(new Box(96,96,118));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<>();

		products1.add(new BoxItem(new Box("A", 45, 46, 55), 1));
		products1.add(new BoxItem(new Box("B", 46, 44, 57), 1));
		products1.add(new BoxItem(new Box("C", 46, 46, 55), 1));
		products1.add(new BoxItem(new Box("D", 44, 46, 55), 1));
		products1.add(new BoxItem(new Box("E", 46, 46, 55), 1));

		Container fits1 = packager.pack(products1);
		// All boxes are smaller than 46x46x57 so we should fit 8 of them in a volume of 92x92x114
		// Here we fail to fit only 5
		assertNotNull(fits1);
	}
	@Test
	public void testWith5Packets2D() {
		List<Box> containers = new ArrayList<>();
		containers.add(new Box(38,66,19));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<>();

		products1.add(new BoxItem(new Box("A", 17, 19, 15), 1));
		products1.add(new BoxItem(new Box("B", 18, 17, 15), 1));
		products1.add(new BoxItem(new Box("C", 14, 20, 18), 1));
		products1.add(new BoxItem(new Box("D", 17, 18, 15), 1));
		products1.add(new BoxItem(new Box("E", 16, 17, 14), 1));

		// All boxes are smaller than 18x20x18 so we should fit 6 of them in a volume of 36x60x18
		// Here we fail to fit only 5
		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
	}
	
	@Test
	public void testIsse2() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(36, 55, 13));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<BoxItem>();

		products1.add(new BoxItem(new Box("01", 38, 10, 10), 1));
		products1.add(new BoxItem(new Box("02", 38, 10, 10), 1));
		products1.add(new BoxItem(new Box("03", 38, 10, 10), 1));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		
		products1.add(new BoxItem(new Box("04", 38, 10, 10), 1));
		Container fits2 = packager.pack(products1);
		assertNull(fits2);
	}
	
	@Test
	@Ignore
	public void testRunsForLimitedTimeSeconds() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(50000, 50000, 50000));
		runsLimitedTimeSeconds(new LargestAreaFitFirstPackager(containers), 0);
	}

	@Test
	@Ignore("Run manually")
	public void testRunsPerformanceGraphLinearStacking() {
		long duration = 60 * 10;
		
		System.out.println("Run for " + duration + " seconds");
		
		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while(deadline > System.currentTimeMillis()) {
			List<Box> containers = new ArrayList<Box>();
			containers.add(new Box(5 * n, 10, 10));
			Packager bruteForcePackager = new LargestAreaFitFirstPackager(containers);
			
			List<BoxItem> products1 = new ArrayList<BoxItem>();

			for(int i = 0; i < n; i++) {
				Box box = new Box(Integer.toString(i), 5, 10, 10);
				for(int k = 0; k < i % 2; k++) {
					box.rotate3D();
				}
				products1.add(new BoxItem(box, 1));
			}

			long time = System.currentTimeMillis();
			Container container = bruteForcePackager.pack(products1, deadline);
			if(container != null) {
				System.out.println(n + " discarded in " + (System.currentTimeMillis() - time));
			} else {
				System.out.println(n + " discarded");
			}
			
			n++;
		}
	}

}
