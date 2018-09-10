package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LargestAreaFitFirstPackager3DTest extends AbstractPackagerTest {

	@Test
	public void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("B", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("C", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("D", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	public void testStackingSquaresOnSquareMultiLevel() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 2, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingRectanglesOnSquare() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		containers.add(new Container(10, 10, 3, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		containers.add(new Container(2, 2, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		for (int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	public void testStackingBinary2() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(8, 8, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingTooHigh() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 5, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 6, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 5, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		containers.add(new Container(350, 150, 400, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
	public void testIsse2() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(36, 55, 13, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products1 = new ArrayList<BoxItem>();

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
	public void about20Products() {
		final Container container = new Container(1500, 1000, 3200, 12000);
		List<Container> containers = BruteForcePropertyBasedTests.rotations(container).collect(Collectors.toList());
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = Arrays.asList(
				new BoxItem(new Box("", 75, 650, 1600, 350)),
				new BoxItem(new Box("", 600, 150, 18, 14)),
				new BoxItem(new Box("", 720, 620, 78, 130)),
				new BoxItem(new Box("", 611, 31, 791, 99)),
				new BoxItem(new Box("", 611, 31, 791, 99)),
				new BoxItem(new Box("", 656, 18, 2033, 143)),
				new BoxItem(new Box("", 656, 18, 2033, 143)),
				new BoxItem(new Box("", 100, 850, 750, 250)),
				new BoxItem(new Box("", 700, 400, 50, 60)),
				new BoxItem(new Box("", 80, 770, 850, 150)),
				new BoxItem(new Box("", 80, 770, 850, 150)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 16, 2500, 11, 4)),
				new BoxItem(new Box("", 16, 2500, 11, 4)),
				new BoxItem(new Box("", 18, 2720, 160, 48)),
				new BoxItem(new Box("", 2500, 650, 30, 210)),
				new BoxItem(new Box("", 700, 400, 50, 60)),
				new BoxItem(new Box("", 75, 650, 1600, 350)),
				new BoxItem(new Box("", 100, 650, 750, 220)),
				new BoxItem(new Box("", 720, 620, 78, 130)),
				new BoxItem(new Box("", 55, 500, 745, 90)),
				new BoxItem(new Box("", 750, 17, 30, 26)),
				new BoxItem(new Box("", 535, 110, 500, 35)),
				new BoxItem(new Box("", 100, 550, 750, 200)),
				new BoxItem(new Box("", 700, 500, 50, 75)),
				new BoxItem(new Box("", 1000, 1000, 1000, 100)));

		Container fits1 = packager.pack(products);
		assertNotNull(fits1);
	}

	@Test
	@Disabled
	public void testRunsForLimitedTimeSeconds() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(50000, 50000, 50000, 0));
		runsLimitedTimeSeconds(new LargestAreaFitFirstPackager(containers), 0);
	}

	@Test
	@Disabled
	public void testRunsPerformanceGraphLinearStacking() {
		long duration = 60 * 10;

		System.out.println("Run for " + duration + " seconds");

		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while (deadline > System.currentTimeMillis()) {
			List<Container> containers = new ArrayList<Container>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new LargestAreaFitFirstPackager(containers);

			List<BoxItem> products1 = new ArrayList<BoxItem>();

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
	public void testStackingMultipleLayersFor3Column() {

		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(30, 10, 6, 0)); // 1800
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		// 1
		products.add(new BoxItem(new Box("A", 10, 10, 6, 0), 1)); // 600
		// 2 
		products.add(new BoxItem(new Box("D", 10, 10, 2, 0), 3)); // 200 200 200
		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingInTwoContainers1() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingInTwoContainers2() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingInSingleContainer() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(5, 10, 1, 0));
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingInTwoContainers3() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingInTwoContainersFitCorrectBox() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 20, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

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
	public void testStackingInMultipleContainersDoesNotConfuseInferiorContainer() {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

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
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 1), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 1), 2));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.toString(), 2, fits.size());
		assertEquals(10, fits.get(0).getWidth());
	}
}
