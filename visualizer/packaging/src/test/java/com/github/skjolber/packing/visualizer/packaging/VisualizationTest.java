package com.github.skjolber.packing.visualizer.packaging;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class VisualizationTest {

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	private List<ContainerItem> containers = ContainerItem
			.newListBuilder()
			.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1500, 1900, 4000).withMaxLoadWeight(100).build())
			.build();

	@Test
	public void testPackager() throws Exception {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}
	}

	@Test
	public void testBruteForcePackager() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("E").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).build();
		assertFalse(result.getContainers().isEmpty());

		Container fits = result.get(0);

		write(fits);
	}

	@Test
	public void testFastBruteForcePackager() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("E").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).build();
		assertFalse(result.getContainers().isEmpty());

		Container fits = result.get(0);

		write(fits);
	}

	@Test
	void testStackMultipleContainers() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).build(), 5)
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(5).build();
		assertFalse(result.getContainers().isEmpty());

		Container fits = result.get(0);

		List<StackPlacement> placements = fits.getStack().getPlacements();

		System.out.println(fits.getStack().getPlacements());

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("B");

		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));

		write(result.getContainers());
	}

	@Test
	void testStackMultipleContainers2() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).build(), 5)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(5).build();

		List<Container> packList = result.getContainers();
		assertThat(packList).hasSize(2);

		Container fits = packList.get(0);

		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("B");

		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));

		write(packList);
	}

	@Test
	void testStackingBinary1() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(8, 8, 2).withMaxLoadWeight(100).build(), 1)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();
		products.add(new StackableItem(Box.newBuilder().withDescription("J").withSize(4, 4, 1).withRotate3D().withWeight(1).build(), 1)); // 16

		for (int i = 0; i < 8; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withSize(2, 2, 1).withRotate3D().withWeight(1).build(), 1)); // 4 * 8 = 32
		}
		for (int i = 0; i < 16; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1)); // 16
		}

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(5).build();

		List<Container> packList = result.getContainers();
		assertThat(packList).hasSize(2);

		Container fits = packList.get(0);

		write(fits);
	}

	@Test
	public void testBowcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codes = directory.codesForCount(9);

		BouwkampCodes bouwkampCodes = codes.get(0);

		BouwkampCode bouwkampCode = bouwkampCodes.getCodes().get(0);

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1)
						.withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			List<Integer> squares = bouwkampCodeLine.getSquares();

			for (Integer square : squares) {
				products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), 1));
			}
		}

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(1).build();

		Container fits = result.get(0);
		assertNotNull(fits);
		assertEquals(fits.getStack().getSize(), products.size());

		write(fits);
	}

	@Test
	public void testSimpleImperfectSquaredRectangles() throws Exception {
		// if you do not have a lot of CPU cores, this will take quite some time

		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		int level = 10;

		pack(directory.getSimpleImperfectSquaredRectangles(level));

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(level));

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(level));
	}

	protected void pack(List<BouwkampCodes> codes) throws Exception {
		for (BouwkampCodes bouwkampCodes : codes) {
			for (BouwkampCode bouwkampCode : bouwkampCodes.getCodes()) {
				long timestamp = System.currentTimeMillis();
				pack(bouwkampCode);
				System.out.println("Packaged " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder() + " in " + (System.currentTimeMillis() - timestamp));

				Thread.sleep(5000);
			}
		}
	}

	protected void pack(BouwkampCode bouwkampCode) throws Exception {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1)
						.withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(256).withCheckpointsPerDeadlineCheck(1024).build();

		List<Integer> squares = new ArrayList<>();
		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			squares.addAll(bouwkampCodeLine.getSquares());
		}

		// map similar items to the same stack item - this actually helps a lot
		Map<Integer, Integer> frequencyMap = new HashMap<>();
		squares.forEach(word -> frequencyMap.merge(word, 1, (v, newV) -> v + newV));

		List<StackableItem> products = new ArrayList<>();
		for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
			int square = entry.getKey();
			int count = entry.getValue();
			products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
		}

		// shuffle
		Collections.shuffle(products);

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(1).build();

		Container fits = result.get(0);
		assertNotNull(bouwkampCode.getName(), fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().getSize(), squares.size());

		for (StackPlacement stackPlacement : fits.getStack().getPlacements()) {
			StackValue stackValue = stackPlacement.getStackValue();
			System.out.println(stackPlacement.getAbsoluteX() + "x" + stackPlacement.getAbsoluteY() + "x" + stackPlacement.getAbsoluteZ() + " " + stackValue.getDx() + "x" + stackValue.getDy() + "x"
					+ stackValue.getDz());
		}

		write(fits);
	}

	private void write(PackagerResult result) throws Exception {
		write(result.getContainers());
	}

	private void write(Container container) throws Exception {
		write(Arrays.asList(container));
	}

	private void write(List<Container> packList) throws Exception {
		DefaultPackagingResultVisualizerFactory p = new DefaultPackagingResultVisualizerFactory();

		File file = new File("../viewer/public/assets/containers.json");
		p.visualize(packList, file);
	}

	@Test
	void issue433() throws Exception {
		Container container = Container
				.newBuilder()
				.withDescription("1")
				.withSize(14, 195, 74)
				.withEmptyWeight(0)
				.withMaxLoadWeight(100)
				.build();

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(container)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
				.newBuilder()
				.build();

		List<StackableItem> products = Arrays.asList(
				new StackableItem(Box.newBuilder().withId("Foot").withSize(7, 37, 39).withRotate3D().withWeight(0).build(), 20));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(1).build();
		Container pack = result.get(0);

		assertNotNull(pack);

		write(pack);
	}

	private StackableItem createStackableItem(String id, int width, int height, int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new StackableItem(box, boxCountPerStackableItem);
	}

	private static List<StackableItem> products33 = Arrays.asList(
			new StackableItem(Box.newBuilder().withRotate3D().withSize(56, 1001, 1505).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(360, 1100, 120).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(70, 70, 120).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 80, 80).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(20, 20, 500).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 230, 50).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 40, 50).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 60).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 32, 32).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2000, 40, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 40, 60).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(60, 90, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(56, 40, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 280, 380).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2500, 600, 80).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(125, 125, 85).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(80, 180, 360).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(25, 140, 140).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(115, 150, 170).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(76, 76, 222).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(326, 326, 249).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(70, 130, 240).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(330, 120, 490).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(9, 23, 2500).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2000, 20, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 235).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(90, 610, 210).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(144, 630, 1530).withWeight(0).build(), 1));

	@Test
	public void testPlainPackager() throws Exception {
		PlainPackager packager = PlainPackager.newBuilder().build();

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products33).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}

	}

	@Test
	@Disabled
	public void testLAFFPackager() throws Exception {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products33).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}
	}

}
