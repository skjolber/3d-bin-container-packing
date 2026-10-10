package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.test.assertj.ContainerAssert;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class ParallelBruteForcePackagerTest extends AbstractBruteForcePackagerTest {

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	@Test
	void propagatesExternalWorkerInterrupt() {
		Thread callingThread = Thread.currentThread();
		AtomicBoolean workerInterrupted = new AtomicBoolean();
		PackagerInterruptSupplier interrupt = () -> {
			if(Thread.currentThread() != callingThread) {
				workerInterrupted.set(true);
				return true;
			}
			return workerInterrupted.get();
		};

		ExecutorService executor = Executors.newFixedThreadPool(2);
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(2)
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				products.add(new BoxItem(Box.newBuilder().withId("box-" + i).withSize(1, 1, 1).withWeight(1).build()));
			}
			ContainerItem container = new ContainerItem(Container.newBuilder().withId("container").withSize(5, 1, 1).withMaxLoadWeight(100).build(), 1);

			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(container)
					.withBoxItems(products)
					.withInterrupt(interrupt)
					.build();

			assertTrue(workerInterrupted.get());
			assertTrue(result.isTimeout());
		} finally {
			try {
				packager.close();
			} finally {
				executor.shutdownNow();
			}
		}
	}

	@Test
	void testSkipReversePermutationsInParallel() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(5, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(executor)
				.withParallelizationCount(4)
				.withSkipReversePermutations(true)
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				products.add(new BoxItem(Box.newBuilder().withId(String.valueOf((char)('A' + i))).withSize(1, 1, 1).withWeight(1).build(), 1));
			}

			PackagerResult result = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();

			PackagerResultAssert.assertThat(result).isSuccess();
			assertEquals(products.size(), result.get(0).getStack().size());
			PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
		} finally {
			try {
				packager.close();
			} finally {
				executor.shutdownNow();
			}
		}
	}

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withParallelizationCount(2)
				.withExecutorService(Executors.newSingleThreadExecutor())
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			assertTrue(build.isSuccess());
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
	
			Container fits = build.get(0);
			ContainerAssert.assertThat(fits).isStackedWithinConstraints();
			assertEquals(fits.getStack().size(), products.size());
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			PackagerResultAssert.assertThat(build).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackMultipleContainers() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 5)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(2)
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(copy(products)).withMaxContainerCount(5).build();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
	
			List<Container> packList = build.getContainers();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
			assertThat(packList).hasSize(2);
	
			Container fits = packList.get(0);
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("B");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			PackagerResultAssert.assertThat(build).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(5)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingBinary1() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(8, 8, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withParallelizationCount(2)
				.withExecutorService(Executors.newSingleThreadExecutor())
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("J").withSize(4, 4, 1).withRotate3D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("K").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 4));
			products.add(new BoxItem(Box.newBuilder().withId("N").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 16));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
	
			Container fits = build.get(0);
			ContainerAssert.assertThat(fits).isStackedWithinConstraints();
			assertEquals(21, fits.getStack().getPlacements().size());
			
			PackagerResultAssert.assertThat(build).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(10, 10, 4).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(2)
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("J").withRotate3D().withSize(5, 10, 4).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("L").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("K").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("M").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("N").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
	
			Container fits = build.get(0);
			assertEquals(fits.getStack().size(), products.size());
			
			PackagerResultAssert.assertThat(build).isAcceptedBy(validator.newResultBuilder()
					.withContainerItems(containerItems)
					.withMaxContainerCount(1)
					.withBoxItems(products));
		} finally {
			packager.close();
		}
	}

	@Test
	public void testStackingBox() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				.withParallelizationCount(2)
				.withExecutorService(Executors.newSingleThreadExecutor())
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(3, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(3, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(3, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("D").withRotate3D().withSize(3, 2, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
			Container fits = build.get(0);
	
			assertEquals(fits.getStack().size(), products.size());
		} finally {
			packager.close();
		}
	}

	@Test
	public void testSimpleImperfectSquaredRectangles() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredRectangles(9), false);
	}

	@Test
	public void testSimpleImperfectSquaredSquares() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(9), false);
	}

	@Test
	public void testSimplePerfectSquaredRectangles() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(9), false);
	}
	
	@Test
	public void testSimpleImperfectSquaredRectanglesSkipReverse() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredRectangles(9), true);
	}

	@Test
	public void testSimpleImperfectSquaredSquaresSkipReverse() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(9), true);
	}

	@Test
	public void testSimplePerfectSquaredRectanglesSkipReverse() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(9), true);
	}

	protected void pack(List<BouwkampCodes> codes, boolean skipReverse) {
		for (BouwkampCodes bouwkampCodes : codes) {
			for (BouwkampCode bouwkampCode : bouwkampCodes.getCodes()) {
				System.out.println("Package " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder());
				long timestamp = System.currentTimeMillis();
				pack(bouwkampCode, skipReverse);
				System.out.println("Packaged " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder() + " in " + (System.currentTimeMillis() - timestamp));
			}
		}
	}

	protected void pack(BouwkampCode bouwkampCode, boolean skipReverse) {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(100)
						.withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				//.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(4)
				.withSkipReversePermutations(skipReverse)
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();
	
			List<Integer> squares = new ArrayList<>();
			for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
				squares.addAll(bouwkampCodeLine.getSquares());
			}
	
			// map similar items to the same stack item - this actually helps a lot
			Map<Integer, Integer> frequencyMap = new TreeMap<>();
			squares.forEach(word -> frequencyMap.merge(word, 1, (v, newV) -> v + newV));
	
			for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
				int square = entry.getKey();
				int count = entry.getValue();
				products.add(new BoxItem(Box.newBuilder().withId(Integer.toString(square)).withRotate3D().withSize(square, square, 1).withWeight(1).build(), count));
			}
	
			//Collections.shuffle(products);
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
			Container fits = build.get(0);
	
			assertNotNull(bouwkampCode.getName(), fits);
			ContainerAssert.assertThat(fits).isStackedWithinConstraints();
			assertEquals(bouwkampCode.getName(), fits.getStack().size(), squares.size());
		} finally {
			packager.close();
		}
	}

	@Disabled // TODO
	@Test
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(ParallelBruteForcePackager.newBuilder().build());
	}

	@Override
	protected ParallelBruteForcePackager createPackager() {
		return ParallelBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(256).build();
	}
	
	@Test
	void testStackingRectanglesWithObstacles() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder()
				//.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(4)
				.build();
		try {
			Container container = Container.newBuilder()
					.withDescription("1")
					.withEmptyWeight(1)
					.withSize(3, 3, 3)
					.withMaxLoadWeight(100)
					.withStack(new ValidatingStack())
					.build();
	
			List<BoxItem> products9 = new ArrayList<>();
			for(int i = 0; i < 9; i++) {
				products9.add(new BoxItem(Box.newBuilder().withId("" + (char)(i + 'A')).withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			}
			
			PackagerResult build9 = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
			}).withBoxItems(products9).build();
			
			List<Placement> placements = build9.getContainers().get(0).getStack().getPlacements();
			
			for(int obstacleIndex = 0; obstacleIndex < 9; obstacleIndex++) {
				
				Placement obstacle = placements.get(obstacleIndex);
				
				List<BoxItem> products = new ArrayList<>();
				
				for(int i = 0; i < 8; i++) {
					products.add(new BoxItem(Box.newBuilder().withId("" + (char)(i + 'A')).withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
				}
	
				PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
					b.withContainerItem(new ContainerItem(container, 1));
					b.withObstacles( o -> {
						o.withObstacle(
								obstacle.getAbsoluteX(), obstacle.getAbsoluteY(), obstacle.getAbsoluteZ(),
								obstacle.getAbsoluteEndX() - obstacle.getAbsoluteX() + 1, obstacle.getAbsoluteEndY() - obstacle.getAbsoluteY() + 1, obstacle.getAbsoluteEndZ() - obstacle.getAbsoluteZ() + 1 
							);
					});
				}).withBoxItems(products).build();
				
				PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
				
				List<Placement> buildPlacements = build.getContainers().get(0).getStack().getPlacements();
				for (Placement placement : buildPlacements) {
					assertFalse(placement.intersects3D(obstacle));
				}
			}
		} finally {
			packager.close();
		}
	}

	//
	//  three container types, more than the two work units; only the last holds the three different unit cubes,
	//  which have enough permutations to be split between the work units:
	//
	//   [a]   [a][b]   [a][b][c]
	//
	@Test
	void packsWithMoreContainerTypesThanWorkUnits() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build();
		try {
			List<ContainerItem> containers = ContainerItem.newListBuilder()
					.withContainer(Container.newBuilder().withId("1").withSize(1, 1, 1).withMaxLoadWeight(100).build(), 1)
					.withContainer(Container.newBuilder().withId("2").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1)
					.withContainer(Container.newBuilder().withId("3").withSize(3, 1, 1).withMaxLoadWeight(100).build(), 1)
					.build();

			List<BoxItem> products = new ArrayList<>();
			for(String id : List.of("a", "b", "c")) {
				products.add(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1));
			}

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(products)
					.withMaxContainerCount(1)
					.withInterruptDuration(10_000)
					.build();

			assertTrue(result.isSuccess());
			assertThat(result.getContainers()).extracting(Container::getId).containsExactly("3");
		} finally {
			packager.close();
		}
	}

	@Test
	void closeShutsDownTheExecutorServiceCreatedByTheBuilder() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).build();
		packager.close();
		assertTrue(packager.getExecutorService().isShutdown());

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		try {
			ParallelBruteForcePackager withExecutorService = ParallelBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(4).build();
			withExecutorService.close();
			// the caller's executor service
			assertFalse(executorService.isShutdown());
		} finally {
			executorService.shutdownNow();
		}
	}

	@Test
	void inputWithoutOrderIsSupported() {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).build();
		try {
			List<BoxItem> products = List.of(new BoxItem(Box.newBuilder().withId("a").withSize(1, 1, 1).withWeight(1).build(), 1));
			List<ContainerItem> containers = List.of(new ContainerItem(Container.newBuilder().withId("c").withSize(1, 1, 1).withMaxLoadWeight(10).build(), 1));
			// no order is the same as Order.NONE
			assertThat(packager.getUnsupportedReason(new com.github.skjolber.packing.packer.PackagerInput(products, null, containers, 1, null))).isNull();
		} finally {
			packager.close();
		}
	}

	/**
	 * The packager is thread-safe: packings at the same time must not take each other's worker results.
	 */
	@Test
	void packsFromSeveralThreadsAtTheSameTime() throws Exception {
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(4).withParallelizationCount(2).build();
		ExecutorService callers = Executors.newFixedThreadPool(4);
		try {
			List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
			for(int i = 0; i < 40; i++) {
				results.add(callers.submit(() -> {
					List<BoxItem> products = new ArrayList<>();
					for(String id : new String[] {"a", "b", "c", "d", "e"}) {
						products.add(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1));
					}
					Container container = Container.newBuilder().withId("1").withSize(5, 1, 1).withMaxLoadWeight(100).build();
					PackagerResult result = packager.newResultBuilder()
							.withContainerItems(ContainerItem.newListBuilder().withContainer(container, 1).build())
							.withBoxItems(products)
							.withMaxContainerCount(1)
							.withInterruptDuration(10_000)
							.build();
					return result.isSuccess();
				}));
			}
			for (java.util.concurrent.Future<Boolean> result : results) {
				assertTrue(result.get());
			}
		} finally {
			callers.shutdownNow();
			packager.close();
		}
	}

	@Test
	void attemptReturnsNullWhenNoBoxFitsTheContainer() throws Exception {
		Container container = Container.newBuilder().withId("container").withSize(2, 2, 2).withMaxLoadWeight(100).build();
		Box big = Box.newBuilder().withId("big").withSize(3, 3, 3).withRotate3D().withWeight(1).build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).build();
		try {
			com.github.skjolber.packing.api.packager.strategy.PackagerSession session = packager.createSession(
					new com.github.skjolber.packing.packer.PackagerInput(List.of(new BoxItem(big, 2)), null, List.of(new ContainerItem(container, 1)), 1, com.github.skjolber.packing.api.Order.NONE),
					() -> false);

			assertThat(session.attempt(0, null, false)).isNull();
			assertThat(session.attempt(0, null, true)).isNull();
		} finally {
			packager.close();
		}
	}

	@Test
	void attemptReturnsNullWhenNoBoxOfAGroupFitsTheContainer() throws Exception {
		Container container = Container.newBuilder().withId("container").withSize(2, 2, 2).withMaxLoadWeight(100).build();
		Box big = Box.newBuilder().withId("big").withSize(3, 3, 3).withRotate3D().withWeight(1).build();
		com.github.skjolber.packing.api.BoxItemGroup group = new com.github.skjolber.packing.api.BoxItemGroup("group", new ArrayList<>(List.of(new BoxItem(big, 2))));

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withThreads(2).build();
		try {
			com.github.skjolber.packing.api.packager.strategy.PackagerSession session = packager.createSession(
					new com.github.skjolber.packing.packer.PackagerInput(null, List.of(group), List.of(new ContainerItem(container, 1)), 1, com.github.skjolber.packing.api.Order.NONE),
					() -> false);

			assertThat(session.attempt(0, null, false)).isNull();
			assertThat(session.attempt(0, null, true)).isNull();
		} finally {
			packager.close();
		}
	}
}
