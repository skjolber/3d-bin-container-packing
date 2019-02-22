package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class LargestAreaFitFirstPackager3DTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("C", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("D", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingSquaresOnSquareMultiLevel() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 2, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("C", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("D", 5, 5, 1, 0), 1));

		products.add(new BoxItem(new Box("E", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("F", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("G", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("H", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);
	}


	@Test
	void testStackingRectanglesOnSquare() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("E", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("F", 5, 10, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingRectanglesOnSquareRectangle() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingRectanglesOnSquareRectangleVolumeFirst() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 3, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 6, 10, 2, 0), 1));
		products.add(new BoxItem(new Box("L", 4, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 4, 10, 2, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);

		assertEquals(1, fits.getLevels().get(fits.getLevels().size() - 1).getHeight());
	}

	@Test
	void testStackingBinary1() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(2, 2, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingBinary2() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(8, 8, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 4, 4, 1, 0), 1));

		for (int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 2, 2, 1, 0), 1));
		}
		for (int i = 0; i < 16; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingTooHigh() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 5, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 10, 10, 6, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	void testStackingTooHighLevel() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 5, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 10, 10, 5, 0), 1));

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}


	@Test
	void testStacking3xLP() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(350, 150, 400, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<>();

		products1.add(new BoxItem(new Box("A", 400, 50, 350, 0), 1));
		products1.add(new BoxItem(new Box("B", 400, 50, 350, 0), 1));
		products1.add(new BoxItem(new Box("C", 400, 50, 350, 0), 1));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);

		List<BoxItem> products2 = new ArrayList<>();
		products2.add(new BoxItem(new Box("A", 350, 50, 400, 0), 1));
		products2.add(new BoxItem(new Box("B", 350, 50, 400, 0), 1));
		products2.add(new BoxItem(new Box("C", 350, 50, 400, 0), 1));

		Container fits2 = packager.pack(products2);
		assertNotNull(fits2);

	}

	@Test
	void testIsse2() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(36, 55, 13, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<>();

		products1.add(new BoxItem(new Box("01", 38, 10, 10, 0), 1));
		products1.add(new BoxItem(new Box("02", 38, 10, 10, 0), 1));
		products1.add(new BoxItem(new Box("03", 38, 10, 10, 0), 1));

		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);

		products1.add(new BoxItem(new Box("04", 38, 10, 10, 0), 1));
		Container fits2 = packager.pack(products1);
		assertNull(fits2);
	}

	@Test
	void about20Products() {
		final Container container = new Container(1500, 1000, 3200, 12000);
		List<Container> containers = container.rotations();
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = listOf20Products();

		Container fits1 = packager.pack(products);
		assertNotNull(fits1);
	}

	@Test
	void containerWith28Products() {
		final Container container = new Container(1500, 1000, 3200, 0);
		List<Container> containers = container.rotations();
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = listOf28Products();

		Container fits1 = packager.pack(products);
		assertNotNull(fits1);
	}

	@Test
	@Disabled
	void testRunsForLimitedTimeSeconds() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(50000, 50000, 50000, 0));
		runsLimitedTimeSeconds(new LargestAreaFitFirstPackager(containers), 0);
	}

	@Test
	@Disabled
	void testRunsPerformanceGraphLinearStacking() {
		long duration = 60 * 10;

		System.out.println("Run for " + duration + " seconds");

		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while (deadline > System.currentTimeMillis()) {
			List<Container> containers = new ArrayList<>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new LargestAreaFitFirstPackager(containers);

			List<BoxItem> products1 = new ArrayList<>();

			for (int i = 0; i < n; i++) {
				Box box = new Box(Integer.toString(i), 5, 10, 10, 0);
				for (int k = 0; k < i % 2; k++) {
					box.rotate3D();
				}
				products1.add(new BoxItem(box, 1));
			}

			long time = System.currentTimeMillis();
			Container container = bruteForcePackager.pack(products1, deadline);
			if (container != null) {
				System.out.println(n + " discarded in " + (System.currentTimeMillis() - time));
			} else {
				System.out.println(n + " discarded");
			}

			n++;
		}
	}

	@Test
	void testStackingMultipleLayersFor3Column() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container(30, 10, 6, 0)); // 1800
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		// 1
		products.add(new BoxItem(new Box("A", 10, 10, 6, 0), 1)); // 600
		// 2
		products.add(new BoxItem(new Box("D", 10, 10, 2, 0), 3)); // 200 200 200
		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	void testStackingInTwoContainers1() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("C", 10, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("D", 10, 5, 1, 0), 1));

		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		for (Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
	}

	@Test
	void testStackingInTwoContainers2() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 2));

		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		for (Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
	}

	@Test
	void testStackingInSingleContainer() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(5, 10, 1, 0));
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 5, 5, 1, 0), 4));

		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 1);
		for (Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 4);
		}

		assertEquals(fits.get(0).getWidth(), 10);
		assertEquals(fits.get(0).getDepth(), 10);
	}

	@Test
	void testStackingInTwoContainers3() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 3));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 3));

		List<Container> fits = packager.packList(products, 3, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 3);
		for (Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 2);
		}
	}

	@Test
	void testStackingInTwoContainersFitCorrectBox() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 20, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 20, 20, 1, 0), 1));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(2, fits.size());
		for (Container container : fits) {
			assertEquals(container.getLevels().get(0).size(), 1);
		}

		Container first = fits.get(0);
		Container second = fits.get(1);

		assertEquals("A", first.get(0, 0).getBox().getName());
		assertEquals("B", second.get(0, 0).getBox().getName());
	}


	@Test
	void testStackingInMultipleContainersDoesNotConfuseInferiorContainer() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 2));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 1);
		assertEquals(20, fits.get(0).getWidth());
	}

	@Test
	void testStackingInMultipleContainersOneBigAndOneSmall() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 0), 3));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 0), 3));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		assertEquals(20, fits.get(0).getWidth());
		assertEquals(10, fits.get(1).getWidth());
	}

	@Test
	void testStackingInMultipleContainersWeight() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 2));
		containers.add(new Container(20, 10, 1, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 1), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 1), 2));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.toString(), 2, fits.size());
		assertEquals(10, fits.get(0).getWidth());
	}

	@Test
	public void testZPosition() {
		List<Container> containers = new ArrayList<Container>();

		containers.add(new Container(30, 30, 30, 500));

		Packager packager = new LargestAreaFitFirstPackager(containers,false,true,true);

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 10, 10, 25), 9));		

		Container pack = packager.pack(products, containers, Long.MAX_VALUE);

		assertEquals(1, pack.getLevels().size());
		for (Level level : pack.getLevels()) {
			for(Placement p : level) {
				assertEquals(0, p.getSpace().getZ());
			}
		}
	}
	
	//Issue #83
	@Test
	void testRemainingWeightNegative() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("Y",22, 22, 22, 35));
		containers.add(new Container("X",22, 22, 22, 45));
		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(new Box("A", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("B", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("C", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("D", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("E", 10, 10, 10, 10)));
		
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);

		validate(fits);
	}
	
	//Issue #83
	@Test
	void testWrongNumberOfContainers() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("X",22, 22, 22, 45));
		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(new Box("A", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("B", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("C", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("D", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("E", 10, 10, 10, 10)));
		
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		List<Container> fits = packager.packList(products, 2, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);

		validate(fits);
	}
		
	//Issue #83
	@Test
	void testRemainingWeightNegative2() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("X",30, 30, 30, 20));
		containers.add(new Container("Y",30, 30, 30, 60));
		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(new Box("A", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("B", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("C", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("D", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("E", 10, 10, 10, 10)));
		products.add(new BoxItem(new Box("F", 10, 10, 10, 10)));
		
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true, true);
		List<Container> fits = packager.packList(products, 50, Long.MAX_VALUE);
		assertNotNull(fits);

		validate(fits);
	}
				
}
