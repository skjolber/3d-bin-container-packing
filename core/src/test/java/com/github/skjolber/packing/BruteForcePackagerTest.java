package com.github.skjolber.packing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Dimension;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Packager;
import com.github.skjolber.packing.Placement;

class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingRectanglesOnSquare() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Container("container1", 10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		// this test uses the space between the level floor and box top
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 4, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 5, 10, 4, 0), 1));
		products.add(new BoxItem(new Box("L", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("K", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("M", 5, 10, 1, 0), 1));
		products.add(new BoxItem(new Box("N", 5, 10, 1, 0), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);

		assertEquals(fits.getLevels().get(0).get(0).getSpace().getZ(), 0);
		assertEquals(fits.getLevels().get(0).get(1).getSpace().getZ(), 0);
		
		// note stacking in height:
		assertEquals(fits.getLevels().get(0).get(2).getSpace().getZ(), 1);
		assertEquals(fits.getLevels().get(0).get(3).getSpace().getZ(), 2);
		assertEquals(fits.getLevels().get(0).get(4).getSpace().getZ(), 3);
	}

	@Test
	void testStackingBinary1() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 2, 2, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

		for(int i = 0; i < 4; i++) {
			products.add(new BoxItem(new Box("K", 1, 1, 1, 0), 1));
		}

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
	}

	@Test
	void testStackingBinary2() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 8, 8, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
		
		assertEquals(1, fits.getLevels().size());
		for (Level level : fits.getLevels()) {
			for(Placement p : level) {
				assertEquals(0, p.getSpace().getZ());
			}
		}		
	}

	@Test
	void testStackingTooHigh() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 5, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("J", 10, 10, 6, 0), 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	void testStackingTooHighLevel() {

		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 10, 10, 5, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		containers.add(new Container("container1", 350, 150, 400, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
	void testLargestAreaFitFirstDoesNotWork() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 15, 10, 10, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, true, true, true);

		List<BoxItem> products1 = new ArrayList<>();

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
	void testPackagingExposesVolumeUsed() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 60, 25, 20, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 7, 24, 58, 0), 1),
				new BoxItem(new Box("2", 7, 25, 56, 0), 1),
				new BoxItem(new Box("3", 5, 25, 58, 0), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);

		assertEquals(new Dimension(58, 25, 19), fits1.getUsedSpace());
	}

	@Test
	@Disabled
	void testRunsForLimitedTimeSeconds() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 5000, 10, 10, 0));
		runsLimitedTimeSeconds(new BruteForcePackager(containers, true, true), 200);
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

		long deadline = System.currentTimeMillis() + duration * 1000;
		int n = 1;
		while(deadline > System.currentTimeMillis()) {
			List<Container> containers = new ArrayList<>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

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

	@Test
	void testIssue11ArrayOutOfBounds() {
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
	void testStackingInTwoContainers1() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
	void testStackingInTwoContainers2() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
	void testStackingInSingleContainer() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(5, 10, 1, 0));
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
	void testStackingInTwoContainers3() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
	void testStackingInTwoContainersFitCorrectBox() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 20, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

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
	void testStackingInMultipleContainersDoesNotConfuseInferiorContainer() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container(10, 10, 1, 0));
		containers.add(new Container(20, 10, 1, 0));
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		BruteForcePackager packager = new BruteForcePackager(containers);

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
		BruteForcePackager packager = new BruteForcePackager(containers);

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(new Box("A", 10, 5, 1, 1), 2));
		products.add(new BoxItem(new Box("B", 10, 5, 1, 1), 2));

		List<Container> fits = packager.packList(products, Integer.MAX_VALUE, Long.MAX_VALUE);
		assertNotNull(fits);
		assertEquals(fits.size(), 2);
		assertEquals(10, fits.get(0).getWidth());
	}

	@Test
	void test2DBruteForceFor6PacketsIssue28() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 152, 252, 1, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, false, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 73, 82, 1, 0), 1),
				new BoxItem(new Box("2", 72, 80, 1, 0), 1),
				new BoxItem(new Box("3", 73, 83, 1, 0), 1),
				new BoxItem(new Box("4", 71, 83, 1, 0), 1),
				new BoxItem(new Box("5", 74, 83, 1, 0), 1),
				new BoxItem(new Box("6", 74, 82, 1, 0), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);
		assertNotNull(fits1);
	}

	@Test
	void test3DBruteForceFor6PacketsIssue28() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 152, 252, 1, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, true, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 73, 82, 1, 0), 1),
				new BoxItem(new Box("2", 72, 80, 1, 0), 1),
				new BoxItem(new Box("3", 73, 83, 1, 0), 1),
				new BoxItem(new Box("4", 71, 83, 1, 0), 1),
				new BoxItem(new Box("5", 74, 83, 1, 0), 1),
				new BoxItem(new Box("6", 74, 82, 1, 0), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);
		assertNotNull(fits1);
	}

	@Test
	void testBruteForceFor6PacketsIssue28() {
		List<Container> containers = new ArrayList<>();
		containers.add(new Container("container1", 152, 252, 58, 0));
		Packager bruteForcePackager = new BruteForcePackager(containers, false, true);

		List<BoxItem> products1 = Arrays.asList(
				new BoxItem(new Box("1", 73, 82, 54, 0), 1),
				new BoxItem(new Box("2", 72, 80, 57, 0), 1),
				new BoxItem(new Box("3", 73, 83, 53, 0), 1),
				new BoxItem(new Box("4", 71, 83, 53, 0), 1),
				new BoxItem(new Box("5", 74, 83, 53, 0), 1),
				new BoxItem(new Box("6", 74, 82, 54, 0), 1)
		);

		Container fits1 = bruteForcePackager.pack(products1);
		assertNotNull(fits1);
	}

	@Test
	void testPackagerInConcurrentScenario() throws Exception {
		Container container = new Container("2", 2390, 1500, 1000, 0);

		List<BoxItem> items = Collections.singletonList(new BoxItem(new Box(990, 1490, 2390, 0), 1));

		final ExecutorService service = Executors.newFixedThreadPool(4);
		final List<Callable<Container>> threads = IntStream
				.range(0, 1000)
				.boxed()
				.map(i -> packInThread(container, items))
				.collect(Collectors.toList());
		final List<Future<Container>> futures = service.invokeAll(threads);
		for (final Future<Container> future : futures) {
			assertNotNull(future.get());
		}
	}

	@Test
	void testPackagerWith3ContainersAnd1BoxItemUsingSingleConstraint() {
		final List<Container> containers = Arrays.asList(
				new Container(1,1,4417,1),
				new Container(4417,1,1,1),
				new Container(1,4417,1,1));
		final List<BoxItem> packets = Collections.singletonList(new BoxItem(new Box(1, 1, 4417,1), 1));
		final long deadline = System.currentTimeMillis() + 300;
		final Container pack = new BruteForcePackager(containers).pack(packets, deadline);
		assertThat(pack).isNotNull();
	}



	private Callable<Container> packInThread(final Container container, List<BoxItem> items) {
		return () -> {
			BruteForcePackager packer = new BruteForcePackager(Collections.singletonList(container));
			return packer.pack(items);
		};
	}

	@Test
    public void packList_shouldChooseBestContainers_issue99() {

        List<Container> boxes = new ArrayList<Container>() {{
            add(new Container("Box 1", 100, 100, 100, 20000));
            add(new Container("Box 2", 200, 200, 200, 20000));
            add(new Container("Box 3", 300, 300, 300, 20000));
        }};

        Packager packager = new BruteForcePackager(boxes);
        int maxContainers = 1000;

        List<BoxItem> products = new ArrayList<BoxItem>();
        products.add(new BoxItem(new Box("Product 1", 299, 299, 99, 25), 1));
        products.add(new BoxItem(new Box("Product 2", 299, 299, 99, 25), 1));
        products.add(new BoxItem(new Box("Product 3", 299, 299, 99, 25), 1));
        products.add(new BoxItem(new Box("Product 4", 99, 99, 99, 25), 1));
        List<Container> containers = packager.packList(products, maxContainers, System.currentTimeMillis() + 15000);
        assertEquals(2, containers.size());
        assertEquals(containers.get(0).getName(), "Box 3");
        assertEquals(containers.get(0).getLevels().size(), 3);
        
        assertEquals(containers.get(1).getName(), "Box 1");
        assertEquals(containers.get(1).getLevels().size(), 1);
    }
	
}
