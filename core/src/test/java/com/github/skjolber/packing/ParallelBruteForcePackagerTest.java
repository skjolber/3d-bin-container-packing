package com.github.skjolber.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.ParallelBruteForcePackager;

public class ParallelBruteForcePackagerTest {

	@Test
	void testStackingRectanglesOnSquare() {

		final ExecutorService pool = Executors.newFixedThreadPool(1);
		
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new ParallelBruteForcePackager(containers, pool, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("E", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("F", 5, 10, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingRectanglesOnSquareRectangle() {
		final ExecutorService pool = Executors.newFixedThreadPool(1);

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new ParallelBruteForcePackager(containers, pool, 1);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}
	
	@Test
	void testStackingRectanglesOnSquareRectangleInParallel() {
		final ExecutorService pool = Executors.newFixedThreadPool(4);

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new ParallelBruteForcePackager(containers, pool, 4);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 5, 1, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 5, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}	
	
	@Test
	@Disabled
	void testRunsPerformanceGraphLinearStacking() {
		long duration = 60 * 10;

		// n! permutations
		// 6 rotations per box
		// so something like n! * 6^n combinations, each needing to be stacked
		//
		// anyways my laptop cannot do more than perhaps 10 within 5 seconds
		// on a single thread and this is quite a simple scenario

		// this test only really test rotations, not permutations
		
		System.out.println("Run for " + duration + " seconds");

		int count = 4;
		final ExecutorService pool = Executors.newFixedThreadPool(count);
		System.out.println("Use " + count + " threads");
		
		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while(deadline > System.currentTimeMillis()) {
			List<Container> containers = new ArrayList<>();
			containers.add(new Container(5 * n, 10, 10, 0));
			BruteForcePackager bruteForcePackager = new ParallelBruteForcePackager(containers, pool, count);

			List<BoxItem> products1 = new ArrayList<>();

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
	
}
