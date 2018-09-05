package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;

public class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testStackingRectanglesOnSquare() {

		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 10, 10, 1));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Box("container1", 10, 10, 1));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Dimension("container1", 10, 10, 3));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Box("container1", 2, 2, 1));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Box("container1", 8, 8, 1));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Box("container1", 10, 10, 5));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<BoxItem>();

		products.add(new BoxItem(new Box("J", 10, 10, 6), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {

		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 10, 10, 5));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Box("container1", 350, 150, 400));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
	public void testLargestAreaFitFirstDoesNotWork() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 15, 10, 10));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, true, true, true);

		List<BoxItem> products1 = new ArrayList<BoxItem>();

		products1.add(new BoxItem(new Box("01", 5, 10, 10), 1));
		products1.add(new BoxItem(new Box("02", 5, 10, 10).rotate3D(), 1));
		products1.add(new BoxItem(new Box("03", 5, 10, 10).rotate3D().rotate3D(), 1));

		long time = System.currentTimeMillis();
		Container fits1 = bruteForcePackager.pack(products1);
		System.out.println(products1.size() + " boxes in " + (System.currentTimeMillis() - time));
		assertNotNull(fits1);
		assertEquals(products1.size(), fits1.getBoxCount());
		print(fits1);
		assertNull(packager.pack(products1));
	}

	@Test
	public void testBruteForceDoesNotWorkFor6Packets() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 152, 252, 58));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 73, 82, 54), 1),
				new BoxItem(new Box("2", 72, 80, 57), 1),
				new BoxItem(new Box("3", 73, 83, 53), 1),
				new BoxItem(new Box("4", 71, 83, 53), 1),
				new BoxItem(new Box("5", 74, 83, 53), 1),
				new BoxItem(new Box("6", 74, 82, 54), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);

		assertEquals(products1.size(), fits1.getBoxCount());
	}

	@Test
	public void testPackagingExposesVolumeUsed() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 60, 25, 20));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 7, 24, 58), 1),
				new BoxItem(new Box("2", 7, 25, 56), 1),
				new BoxItem(new Box("3", 5, 25, 58), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);

		assertEquals(new Dimension(58, 25, 19), fits1.getUsedSpace());
	}



	@Test
	@Ignore
	public void testRunsForLimitedTimeSeconds() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box("container1", 5000, 10, 10));
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
			List<Box> containers = new ArrayList<Box>();
			containers.add(new Box(5 * n, 10, 10));
			Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

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
				System.out.println(n + " in " + (System.currentTimeMillis() - time));
			} else {
				System.out.println(n + " discarded in " + (System.currentTimeMillis() - time));
			}

			n++;
		}

	}

	@Test
	public void testIssue11ArrayOutOfBounds() {
		List<Dimension> containers = Arrays.asList(
			new Dimension("2", 330, 222, 121),
			new Dimension("4", 330, 235, 225)
		);

		List<BoxItem> items = Arrays.asList(
			new BoxItem(new Box(105, 105, 293), 1),
			new BoxItem(new Box(92, 94, 255), 1),
			new BoxItem(new Box(105, 70, 60), 2)
		);

		BruteForcePackager packer = new BruteForcePackager(containers);
		packer.pack(items);
	}

	@Test
	public void testPackagerInConcurrentScenario() {
		Dimension[] containers = new Dimension[]{
				new Dimension("0", 2390, 1500, 1000),
				new Dimension("1", 2400, 1500, 1000),
				new Dimension("2", 2390, 1500, 1000)};

		List<BoxItem> items = Collections.singletonList(new BoxItem(new Box(990, 1490, 2390), 1));

		IntStream
				.range(0, 1000)
				.boxed()
				.map(i -> packInThread(containers[i % 3], items))
				.forEach(t -> {
					t.start();
					try {
						t.join();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				});
	}

	private Thread packInThread(final Dimension container, final List<BoxItem> items) {
		return new Thread(() -> {
			try {
				Thread.sleep(new Random().nextInt(50));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			BruteForcePackager packer = new BruteForcePackager(Collections.singletonList(container));
			assertNotNull(packer.pack(items));
		});
	}
}
