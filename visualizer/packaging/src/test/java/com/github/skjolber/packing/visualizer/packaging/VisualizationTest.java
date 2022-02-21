package com.github.skjolber.packing.visualizer.packaging;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodeLine;
import com.github.skjolber.packing.test.BouwkampCodes;

public class VisualizationTest {

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	@Test
	public void testPackager() throws Exception {
		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).build();
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(container).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
		System.out.println(container);
		
		write(container);
	}
	
	@Test
	public void testBruteForcePackager() throws Exception {
		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).build();
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(container).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("E").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		write(fits);
	}
	
	@Test
	public void testFastBruteForcePackager() throws Exception {
		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).build();
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(container).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("E").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		write(fits);
	}

	@Test
	void testStackMultipleContainers() throws Exception {

		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).build();
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(container).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));

		List<Container> packList = packager.packList(products, 5, System.currentTimeMillis() + 5000);
		assertThat(packList).hasSize(2);
		
		Container fits = packList.get(0);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		System.out.println(fits.getStack().getPlacements());

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("B");
		
		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
		
		write(packList);
	}
	
	@Test
	void testStackMultipleContainers2() throws Exception {

		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).build();
		
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(container).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));

		List<Container> packList = packager.packList(products, 5, System.currentTimeMillis() + 5000);
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

		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(8, 8, 2).withMaxLoadWeight(100).build();
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(container).build();

		List<StackableItem> products = new ArrayList<>();
		products.add(new StackableItem(Box.newBuilder().withDescription("J").withSize(4, 4, 1).withRotate3D().withWeight(1).build(), 1)); // 16

		for(int i = 0; i < 8; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withSize(2, 2, 1).withRotate3D().withWeight(1).build(), 1)); // 4 * 8 = 32
		}
		for(int i = 0; i < 16; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1)); // 16
		}

		Container fits = packager.pack(products);
		
		write(fits);
	}	

	@Test
	public void testBowcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codes = directory.codesForCount(9);
		
		BouwkampCodes bouwkampCodes = codes.get(0);
		
		BouwkampCode bouwkampCode = bouwkampCodes.getCodes().get(0);
		
		List<Container> containers = new ArrayList<>();
		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build();
		containers.add(container);
		
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			List<Integer> squares = bouwkampCodeLine.getSquares();
			
			for(Integer square : squares) {
				products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), 1));
			}
		}

		Container fits = packager.pack(products);
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
		List<Container> containers = new ArrayList<>();
		DefaultContainer container = Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build();
		containers.add(container);
		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(256).withCheckpointsPerDeadlineCheck(1024).withContainers(containers).build();

		List<Integer> squares = new ArrayList<>(); 
		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			squares.addAll(bouwkampCodeLine.getSquares());
		}

		// map similar items to the same stack item - this actually helps a lot
		Map<Integer, Integer> frequencyMap = new HashMap<>();
		squares.forEach(word ->
        	frequencyMap.merge(word, 1, (v, newV) -> v + newV)
		);
		
		List<StackableItem> products = new ArrayList<>();
		for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
			int square = entry.getKey();
			int count = entry.getValue();
			products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
		}

		// shuffle
		Collections.shuffle(products);
		
		Container fits = packager.pack(products);
		assertNotNull(bouwkampCode.getName(), fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().getSize(), squares.size());
		
		for (StackPlacement stackPlacement : fits.getStack().getPlacements()) {
			StackValue stackValue = stackPlacement.getStackValue();
			System.out.println(stackPlacement.getAbsoluteX() + "x" + stackPlacement.getAbsoluteY() + "x" + stackPlacement.getAbsoluteZ() + " " + stackValue.getDx() + "x" + stackValue.getDy() + "x" + stackValue.getDz());
		}
		
		write(fits);
	}
	
	private void write(Container container) throws Exception {
		write(Arrays.asList(container));
	}

	private void write(List<Container> packList) throws Exception {
		DefaultPackagingResultVisualizerFactory p = new DefaultPackagingResultVisualizerFactory();
		
		File file = new File("../viewer/public/assets/containers.json");
		p.visualize(packList , file);
	}

	@Test
	void issue433() throws Exception {
		Container container = Container
				.newBuilder()
				.withDescription("1")
				.withSize(14, 185, 78)
				.withEmptyWeight(0)
				.withMaxLoadWeight(100)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
				.newBuilder()
				.withContainers(container)
				.build();

		Container pack = packager.pack(
				Arrays.asList(
						new StackableItem(Box.newBuilder().withId("Foot").withSize(7, 37, 39).withRotate3D().withWeight(0).build(), 20)
						)
				);

		write(pack);
	}

}
