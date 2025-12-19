package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class ParallelBoxItemBruteForcePackagerTest extends AbstractBruteForcePackagerTest {

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
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
			assertValid(build);
	
			Container fits = build.get(0);
			assertValid(fits);
			assertEquals(fits.getStack().size(), products.size());
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			assertValidUsingValidator(containerItems, 1, build, products);
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

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(2)
				.build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(clone(products)).withMaxContainerCount(5).build();
			assertValid(build);
	
			List<Container> packList = build.getContainers();
			assertValid(packList);
			assertThat(packList).hasSize(2);
	
			Container fits = packList.get(0);
	
			List<Placement> placements = fits.getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("B");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			assertValidUsingValidator(containerItems, 5, build, products);
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

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				.withParallelizationCount(2)
				.withExecutorService(Executors.newSingleThreadExecutor())
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("J").withSize(4, 4, 1).withRotate3D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("K").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 4));
			products.add(new BoxItem(Box.newBuilder().withId("N").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 16));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			assertValid(build);
	
			Container fits = build.get(0);
			assertValid(fits);
			assertEquals(21, fits.getStack().getPlacements().size());
			
			assertValidUsingValidator(containerItems, 1, build, products);
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

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
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
			assertValid(build);
	
			Container fits = build.get(0);
			assertEquals(fits.getStack().size(), products.size());
			
			assertValidUsingValidator(containerItems, 1, build, products);
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

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
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
			assertValid(build);
			Container fits = build.get(0);
	
			assertEquals(fits.getStack().size(), products.size());
		} finally {
			packager.close();
		}
	}

	@Test
	public void testSimpleImperfectSquaredRectangles() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredRectangles(9));
	}

	@Test
	public void testSimpleImperfectSquaredSquares() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(9));
	}

	@Test
	public void testSimplePerfectSquaredRectangles() {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(9));
	}

	protected void pack(List<BouwkampCodes> codes) {
		for (BouwkampCodes bouwkampCodes : codes) {
			for (BouwkampCode bouwkampCode : bouwkampCodes.getCodes()) {
				System.out.println("Package " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder());
				long timestamp = System.currentTimeMillis();
				pack(bouwkampCode);
				System.out.println("Packaged " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder() + " in " + (System.currentTimeMillis() - timestamp));
			}
		}
	}

	protected void pack(BouwkampCode bouwkampCode) {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(100)
						.withStack(new ValidatingStack()).build(), 1)
				.build();

		ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder()
				//.withExecutorService(Executors.newSingleThreadExecutor())
				.withParallelizationCount(4)
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
			assertValid(build);
			Container fits = build.get(0);
	
			assertNotNull(bouwkampCode.getName(), fits);
			assertValid(fits);
			assertEquals(bouwkampCode.getName(), fits.getStack().size(), squares.size());
		} finally {
			packager.close();
		}
	}

	@Disabled // TODO
	@Test
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(ParallelBoxItemBruteForcePackager.newBuilder().build());
	}

	@Override
	protected ParallelBoxItemBruteForcePackager createPackager() {
		return ParallelBoxItemBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(256).build();
	}
}
