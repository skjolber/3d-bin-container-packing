package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testStackingRectanglesOnSquare() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("E", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("F", 5, 10, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangle() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 10, 10, 3, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 6, 10, 2, 0), 1));
		products.add(new BoxItem(new Box("L", 4, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 4, 10, 2, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);

		assertEquals(1, fits.getLevels().get(fits.getLevels().size() - 1).getHeight());
	}
	
	@Test
	public void testStackingBinary1() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 2, 2, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		for(int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingBinary2() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 8, 8, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 4, 4, 1, 0), 1));
		
		for(int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 2, 2, 1, 0), 1));
		}
		for(int i = 0; i < 16; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	public void testStackingTooHigh() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 10, 10, 5, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 6, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {
		
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 10, 10, 5, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 5, 0), 1));

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}	
	
	
	@Test
	public void testStacking3xLP() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 350, 150, 400, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products1 = new ArrayList<BoxItem>();

		products1.add(new BoxItem(new Box("A", 400, 50, 350, 0), 1));
		products1.add(new BoxItem(new Box("B", 400, 50, 350, 0), 1));
		products1.add(new BoxItem(new Box("C", 400, 50, 350, 0), 1));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		
		List<BoxItem> products2 = new ArrayList<BoxItem>();
		products2.add(new BoxItem(new Box("A", 350, 50, 400, 0), 1));
		products2.add(new BoxItem(new Box("B", 350, 50, 400, 0), 1));
		products2.add(new BoxItem(new Box("C", 350, 50, 400, 0), 1));

		Container fits2 = packager.pack(products2);
		assertNotNull(fits2);

	}
	
	
	@Test
	public void testLargestAreaFitFirstDoesNotWork() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 15, 10, 10, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, true, true, true);

		List<BoxItem> products1 = new ArrayList<BoxItem>();
		
		products1.add(new BoxItem(new Box("01", 5, 10, 10, 0), 1));
		products1.add(new BoxItem(new Box("02", 5, 10, 10, 0).rotate3D(), 1));
		products1.add(new BoxItem(new Box("03", 5, 10, 10, 0).rotate3D().rotate3D(), 1));

		long time = System.currentTimeMillis();
		Container fits1 = bruteForcePackager.pack(products1);
		System.out.println(products1.size() + " boxes in " + (System.currentTimeMillis() - time));
		assertNotNull(fits1);
		assertEquals(products1.size(), fits1.getBoxCount());
		print(fits1);
 		assertNull(packager.pack(products1));
	}
	
	@Test
	@Ignore
	public void testRunsForLimitedTimeSeconds() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container("container1", 5000, 10, 10, 0));
		runsLimitedTimeSeconds(new BruteForcePackager(containers, true, true), 200);
	}

	@Test
	@Ignore("Run manually")
	public void testRunsPerformanceGraphLinearStacking() {
		long duration = 60 * 10;
		
		// n! permutations
		// 6 rotations per box
		// so something like n! * 6^n combinations, each needing to be stacked
		//
		// anyways my laptop cannot do more than perhaps 10 within 5 seconds 
		// on a single thread and this is quite a simple scenario
		
		System.out.println("Run for " + duration + " seconds");
		
		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while(deadline > System.currentTimeMillis()) {
			List<Container> containers = new ArrayList<Container>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new BruteForcePackager(containers, true, true);
			
			List<BoxItem> products1 = new ArrayList<BoxItem>();

			for(int i = 0; i < n; i++) {
				Box box = new Box(Integer.toString(i), 5, 10, 10, 0);
				for(int k = 0; k < i % 2; k++) {
					box.rotate3D();
				}
				products1.add(new BoxItem(box, 1));
			}

			long time = System.currentTimeMillis();
			Container container = bruteForcePackager.pack(products1, deadline);
			if(container != null) {
				System.out.println(n + " in " + (System.currentTimeMillis() - time));
			} else {
				System.out.println(n + " discarded in " + (System.currentTimeMillis() - time));
			}
			
			n++;
		}
		
	}

	@Test
	public void testIssue11ArrayOutOfBounds() {
		List<Container> containers = Arrays.asList(
			new Container("2", 330, 222, 121, 0),
			new Container("4", 330, 235, 225, 0)
		);

		List<BoxItem> items = Arrays.asList(
			new BoxItem(new Box(105, 105, 293, 0), 1),
			new BoxItem(new Box(92, 94, 255, 0), 1),
			new BoxItem(new Box(105, 70, 60, 0), 2)
		);

		BruteForcePackager packer = new BruteForcePackager(containers);
		packer.pack(items);
	}
	
	@Test
	public void testStackingInTwoContainers1() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("C", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("D", 10, 5, 1, 0), 1));
		
		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		for(Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
	}

	@Test
	public void testStackingInTwoContainers2() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 2));
		
		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		for(Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
	}

	@Test
	public void testStackingInSingleContainer() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(5, 10, 1, 0));
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 5, 5, 1, 0), 4));
		
		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 1);
		for(Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 4);
		}
		
		assertEquals(fits.get(0).getWidth(), 10);
		assertEquals(fits.get(0).getDepth(), 10);
	}
	
	@Test
	public void testStackingInTwoContainers3() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 3));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 3));
		
		List<Container> fits = packager.packList(products, 3, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 3);
		for(Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
		
		Container first = fits.get(0);
		Container second = fits.get(1);
		Container third = fits.get(2);

		assertEquals(first.get(0, 0).getBox().getName(), "A");
		assertEquals(first.get(0, 1).getBox().getName(), "A");

		assertEquals(second.get(0, 0).getBox().getName(), "A");
		assertEquals(second.get(0, 1).getBox().getName(), "B");

		assertEquals(third.get(0, 0).getBox().getName(), "B");
		assertEquals(third.get(0, 1).getBox().getName(), "B");
	}

	@Test
	public void testStackingInTwoContainersFitCorrectBox() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 20, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 20, 20, 1, 0), 1));
		
		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(2, fits.size());
		for(Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 1);
		}
		
		Container first = fits.get(0);
		Container second = fits.get(1);

		assertEquals("A", first.get(0, 0).getBox().getName());
		assertEquals("B", second.get(0, 0).getBox().getName());
	}

	@Test
	public void testStackingInMultipleContainersDoesNotConfuseInferiorContainer() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 2));
		
		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 1);
		assertEquals(20, fits.get(0).getWidth());
	}
	
	@Test
	public void testStackingInMultipleContainersOneBigAndOneSmall() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 3));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 3));
		
		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		assertEquals(20, fits.get(0).getWidth());
		assertEquals(10, fits.get(1).getWidth());
	}
	
	@Test
	public void testStackingInMultipleContainersWeight() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 2));
		containers.add(new Container(20, 10, 1, 1));
		BruteForcePackager packager = new BruteForcePackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 1), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 1), 2));
		
		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		assertEquals(10, fits.get(0).getWidth());
	}
}
