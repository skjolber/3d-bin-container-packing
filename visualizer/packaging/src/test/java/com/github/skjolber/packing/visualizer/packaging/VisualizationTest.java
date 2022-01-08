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
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodeLine;
import com.github.skjolber.packing.test.BouwkampCodes;

public class VisualizationTest {

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	@Test
	public void testPackager() throws Exception {
		List<Container> containers = new ArrayList<>();
		
		DefaultContainer container = Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 2, 1, 3, 2, 1, 100, null).withStack(new DefaultStack()).build();
		containers.add(container);
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
		System.out.println(container);
		
		write(container);
	}
	
	@Test
	public void testBruteForcePackager() throws Exception {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotateXYZ(5, 5, 1, 5, 5, 1, 100, null).withStack(new DefaultStack()).build());
		
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("D").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("E").with3DOrientations(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		write(fits);
	}
	
	@Test
	public void testFastBruteForcePackager() throws Exception {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotateXYZ(5, 5, 1, 5, 5, 1, 100, null).withStack(new DefaultStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("D").with3DOrientations(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("E").with3DOrientations(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		write(fits);
	}

	@Test
	void testStackMultipleContainers() throws Exception {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 1, 1, 3, 1, 1, 100, null).withStack(new DefaultStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withOrientation(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("B").withOrientation(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("C").withOrientation(1, 1, 1).withWeight(1).build(), 2));

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

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 1, 1, 3, 1, 1, 100, null).withStack(new DefaultStack()).build());
		
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withOrientation(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("B").withOrientation(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withName("C").withOrientation(1, 1, 1).withWeight(1).build(), 2));

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

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(8, 8, 1, 8, 8, 1, 100, null).withStack(new DefaultStack()).build());
		
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();
		products.add(new StackableItem(Box.newBuilder().withName("J").with3DOrientations(4, 4, 1).withWeight(1).build(), 1)); // 16

		for(int i = 0; i < 8; i++) {
			products.add(new StackableItem(Box.newBuilder().withName("K").with3DOrientations(2, 2, 1).withWeight(1).build(), 1)); // 4 * 8 = 32
		}
		for(int i = 0; i < 16; i++) {
			products.add(new StackableItem(Box.newBuilder().withName("K").with3DOrientations(1, 1, 1).withWeight(1).build(), 1)); // 16
		}

		Container fits = packager.pack(products);
		
		write(fits);
	}	

	private void write(Container container) throws Exception {
		write(Arrays.asList(container));
	}

	private void write(List<Container> packList) throws Exception {
		ContainerProjection p = new ContainerProjection();
		
		File file = new File("../viewer/public/assets/containers.json");
		p.project(packList , file);
	}
	
	@Test
	public void testBowcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codes = directory.codesForCount(9);
		
		BouwkampCodes bouwkampCodes = codes.get(0);
		
		BouwkampCode bouwkampCode = bouwkampCodes.getCodes().get(0);
		
		List<Container> containers = new ArrayList<>();
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotate(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth() * bouwkampCode.getDepth(), null).withStack(new DefaultStack()).build());

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			List<Integer> squares = bouwkampCodeLine.getSquares();
			
			for(Integer square : squares) {
				products.add(new StackableItem(Box.newBuilder().withName(Integer.toString(square)).with3DOrientations(square, square, 1).withWeight(1).build(), 1));
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
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotate(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth() * bouwkampCode.getDepth(), null).withStack(new DefaultStack()).build());

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
			products.add(new StackableItem(Box.newBuilder().withName(Integer.toString(square)).with3DOrientations(square, square, 1).withWeight(1).build(), count));
		}

		Collections.reverse(products);

		Container fits = packager.pack(products);
		assertNotNull(bouwkampCode.getName(), fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().getSize(), squares.size());
		
		for (StackPlacement stackPlacement : fits.getStack().getPlacements()) {
			StackValue stackValue = stackPlacement.getStackValue();
			System.out.println(stackPlacement.getAbsoluteX() + "x" + stackPlacement.getAbsoluteY() + "x" + stackPlacement.getAbsoluteZ() + " " + stackValue.getDx() + "x" + stackValue.getDy() + "x" + stackValue.getDz());
		}
		
		write(fits);
	}
}
