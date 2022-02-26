package com.github.skjolber.packing.visualizer.packaging;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.StackAssert;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

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
					.withSize(14, 195, 74)
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

			assertNotNull(pack);
			

		write(pack);
	}
	
	@Test
	void issue440() throws Exception {
		DefaultContainer build = Container.newBuilder()
				.withDescription("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		PlainPackager packager = PlainPackager.newBuilder()
				.withContainers(Arrays.asList(build))
				.build();

		for(int i = 1; i <= 10; i++) { 
			int boxCountPerStackableItem = i;

			List<StackableItem> stackableItems = Arrays.asList(
					createStackableItem("1",1200,750, 2280, 285, boxCountPerStackableItem),
					createStackableItem("2",1200,450, 2280, 155, boxCountPerStackableItem),
					createStackableItem("3",360,360, 570, 20, boxCountPerStackableItem),
					createStackableItem("4",2250,1200, 2250, 900, boxCountPerStackableItem),
					createStackableItem("5",1140,750, 1450, 395, boxCountPerStackableItem),
					createStackableItem("6",1130,1500, 3100, 800, boxCountPerStackableItem),
					createStackableItem("7",800,490, 1140, 156, boxCountPerStackableItem),
					createStackableItem("8",800,2100, 1200, 135, boxCountPerStackableItem),
					createStackableItem("9",1120,1700, 2120, 160, boxCountPerStackableItem),
					createStackableItem("10",1200,1050, 2280, 390, boxCountPerStackableItem)
					);

			List<Container> packList = packager.packList(stackableItems, i + 2);

			assertNotNull(packList);
			assertTrue(i >= packList.size());
			
			write(packList);
			
			Thread.sleep(5000);
		}
	}
	
	private StackableItem createStackableItem(String id, int width, int height,int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new StackableItem(box, boxCountPerStackableItem);
	}
	

	@Test
	void issue450() throws Exception {
		DefaultContainer build = Container.newBuilder()
				.withDescription("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.withContainers(Arrays.asList(build))
				.build();

		for(int i = 1; i <= 100; i++) { 
			int boxCountPerStackableItem = 1;

			List<StackableItem> stackableItems = Arrays.asList(
		            createStackableItem("1", 1200, 750, 2280, 285, boxCountPerStackableItem),
		            createStackableItem("2", 1200, 450, 2280, 155, boxCountPerStackableItem),
		            createStackableItem("3", 360, 360, 570, 20, boxCountPerStackableItem),
		            createStackableItem("4", 2250, 1200, 2250, 900, boxCountPerStackableItem),
		            createStackableItem("5", 1140, 750, 1450, 395, boxCountPerStackableItem),
		            createStackableItem("6", 1130, 1500, 3100, 800, boxCountPerStackableItem),
		            createStackableItem("7", 800, 490, 1140, 156, boxCountPerStackableItem),
		            createStackableItem("8", 800, 2100, 1200, 135, boxCountPerStackableItem),
		            createStackableItem("9", 1120, 1700, 2120, 160, boxCountPerStackableItem),
		            createStackableItem("10", 1200, 1050, 2280, 390, boxCountPerStackableItem),
		            createStackableItem("11", 900, 700, 1200, 114, boxCountPerStackableItem),
		            createStackableItem("12", 800, 1500, 1200, 506, boxCountPerStackableItem),
		            createStackableItem("13", 800, 1500, 1200, 513, boxCountPerStackableItem),
		            createStackableItem("14", 800, 1600, 1200, 592, boxCountPerStackableItem),
		            createStackableItem("15", 800, 1500, 1200, 318, boxCountPerStackableItem),
		            createStackableItem("16", 2300, 1700, 3950, 2260, boxCountPerStackableItem),
		            createStackableItem("17", 2300, 2250, 4700, 1080, boxCountPerStackableItem),
		            createStackableItem("22", 1100, 2200, 1600, 450, boxCountPerStackableItem),
		            createStackableItem("24", 2320, 1000, 1650, 140, boxCountPerStackableItem),
		            createStackableItem("25", 730, 750, 1140, 120, boxCountPerStackableItem),
		            createStackableItem("26", 400, 200, 400, 3, boxCountPerStackableItem),
		            createStackableItem("27", 1200, 750, 2280, 460, boxCountPerStackableItem),
		            createStackableItem("28", 1200, 750, 2280, 325, boxCountPerStackableItem),
		            createStackableItem("29", 1450, 750, 1140, 550, boxCountPerStackableItem),
		            createStackableItem("30", 1100, 2200, 3200, 550, boxCountPerStackableItem),
		            createStackableItem("31", 800, 800, 1500, 150, boxCountPerStackableItem),
		            createStackableItem("32", 1000, 1000, 1900, 350, boxCountPerStackableItem),
		            createStackableItem("33", 500, 2000, 2300, 350, boxCountPerStackableItem),
		            createStackableItem("34", 2300, 2300, 4000, 1710, boxCountPerStackableItem),
		            createStackableItem("35", 1400, 500, 1400, 650, boxCountPerStackableItem),
		            createStackableItem("36", 1600, 1200, 4200, 1500, boxCountPerStackableItem),
		            createStackableItem("37", 2300, 1800, 4400, 3235, boxCountPerStackableItem),
		            createStackableItem("38", 1200, 750, 2280, 370, boxCountPerStackableItem),
		            createStackableItem("40", 2300, 1200, 2930, 500, boxCountPerStackableItem),
		            createStackableItem("41", 2320, 1500, 3300, 1030, boxCountPerStackableItem),
		            createStackableItem("42", 2300, 1800, 3300, 1250, boxCountPerStackableItem),
		            createStackableItem("43", 2320, 1800, 3300, 625, boxCountPerStackableItem),
		            createStackableItem("44", 2300, 1200, 2930, 520, boxCountPerStackableItem),
		            createStackableItem("45", 2300, 2300, 3300, 1340, boxCountPerStackableItem),
		            createStackableItem("46", 1700, 1000, 2300, 400, boxCountPerStackableItem),
		            createStackableItem("47", 1700, 1000, 2300, 400, boxCountPerStackableItem),
		            createStackableItem("48", 1450, 750, 1140, 450, boxCountPerStackableItem),
		            createStackableItem("49", 2300, 500, 3300, 700, boxCountPerStackableItem),
		            createStackableItem("50", 1200, 750, 2280, 320, boxCountPerStackableItem),
		            createStackableItem("51", 1140, 750, 1450, 260, boxCountPerStackableItem),
		            createStackableItem("52", 730, 750, 1140, 105, boxCountPerStackableItem),
		            createStackableItem("53", 500, 800, 2260, 140, boxCountPerStackableItem),
		            createStackableItem("54", 1140, 450, 1450, 245, boxCountPerStackableItem),
		            createStackableItem("55", 1140, 450, 1450, 285, boxCountPerStackableItem),
		            createStackableItem("56", 1140, 450, 1450, 260, boxCountPerStackableItem),
		            createStackableItem("57", 1140, 750, 1450, 280, boxCountPerStackableItem),
		            createStackableItem("58", 1140, 750, 1450, 355, boxCountPerStackableItem),
		            createStackableItem("59", 1140, 450, 1450, 210, boxCountPerStackableItem),
		            createStackableItem("60", 1140, 750, 1450, 235, boxCountPerStackableItem),
		            createStackableItem("61", 1140, 450, 1450, 260, boxCountPerStackableItem),
		            createStackableItem("62", 650, 1400, 850, 20, boxCountPerStackableItem),
		            createStackableItem("63", 1150, 1700, 2150, 200, boxCountPerStackableItem),
		            createStackableItem("64", 2320, 2350, 6900, 1835, boxCountPerStackableItem),
		            createStackableItem("65", 370, 350, 6200, 110, boxCountPerStackableItem),
		            createStackableItem("66", 1050, 1800, 6050, 805, boxCountPerStackableItem),
		            createStackableItem("67", 850, 750, 3400, 290, boxCountPerStackableItem),
		            createStackableItem("68", 800, 1330, 1220, 70, boxCountPerStackableItem),
		            createStackableItem("69", 150, 250, 6050, 91, boxCountPerStackableItem),
		            createStackableItem("70", 850, 750, 6200, 605, boxCountPerStackableItem),
		            createStackableItem("71", 850, 900, 4200, 450, boxCountPerStackableItem),
		            createStackableItem("72", 1200, 620, 1200, 75, boxCountPerStackableItem),
		            createStackableItem("73", 750, 500, 2600, 170, boxCountPerStackableItem),
		            createStackableItem("74", 800, 500, 1200, 50, boxCountPerStackableItem),
		            createStackableItem("75", 370, 350, 3400, 180, boxCountPerStackableItem),
		            createStackableItem("76", 800, 770, 1200, 86, boxCountPerStackableItem),
		            createStackableItem("77", 1200, 750, 2280, 610, boxCountPerStackableItem),
		            createStackableItem("78", 800, 940, 1200, 100, boxCountPerStackableItem),
		            createStackableItem("79", 1600, 550, 3200, 700, boxCountPerStackableItem),
		            createStackableItem("80", 2320, 2200, 2950, 1000, boxCountPerStackableItem),
		            createStackableItem("81", 350, 370, 2280, 50, boxCountPerStackableItem),
		            createStackableItem("82", 1200, 250, 2280, 120, boxCountPerStackableItem),
		            createStackableItem("83", 1200, 450, 2280, 280, boxCountPerStackableItem),
		            createStackableItem("84", 2300, 1500, 3300, 800, boxCountPerStackableItem),
		            createStackableItem("85", 2300, 2000, 3300, 1000, boxCountPerStackableItem),
		            createStackableItem("87", 730, 750, 1140, 120, boxCountPerStackableItem),
		            createStackableItem("88", 2300, 2250, 3300, 1000, boxCountPerStackableItem),
		            createStackableItem("89", 1200, 900, 2280, 600, boxCountPerStackableItem),
		            createStackableItem("90", 2320, 2350, 3950, 1120, boxCountPerStackableItem),
		            createStackableItem("92", 2300, 2300, 3300, 820, boxCountPerStackableItem),
		            createStackableItem("93", 2300, 2250, 2100, 860, boxCountPerStackableItem),
		            createStackableItem("94", 2280, 2280, 4800, 2390, boxCountPerStackableItem),
		            createStackableItem("96", 1200, 950, 2100, 340, boxCountPerStackableItem),
		            createStackableItem("97", 2300, 2000, 4200, 1100, boxCountPerStackableItem),
		            createStackableItem("99", 2300, 2250, 3300, 1320, boxCountPerStackableItem),
		            createStackableItem("101", 600, 1250, 800, 60, boxCountPerStackableItem)			
					);


			List<Container> packList = packager.packList(stackableItems, i);

			if(packList != null) {
				System.out.println(packList.size());
				write(packList);
				for (Container container : packList) {
					StackAssert.assertThat(container.getStack()).placementsDoNotIntersect();
				}
			}
		}

	}

	private StackableItem createStackableItem(String id, int width, int height,int depth, int weight) {
		return createStackableItem(id, width, height, depth, weight, 1);
	}
	
	@Test
	void issue4502() throws Exception {
		DefaultContainer build = Container.newBuilder()
				.withDescription("1")
				.withSize(2352, 2394, 12031)
				.withEmptyWeight(4000)
				.withMaxLoadWeight(26480)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder()
				.withContainers(Arrays.asList(build))
				.build();

		for(int i = 1; i <= 100; i++) { 
			int boxCountPerStackableItem = 1;

			List<StackableItem> stackableItems = Arrays.asList(
		            createStackableItem("1", 1200, 750, 2280, 285, boxCountPerStackableItem),
		            createStackableItem("2", 1200, 450, 2280, 155, boxCountPerStackableItem),
		            createStackableItem("3", 360, 360, 570, 20, boxCountPerStackableItem),
		            createStackableItem("4", 2250, 1200, 2250, 900, boxCountPerStackableItem),
		            createStackableItem("5", 1140, 750, 1450, 395, boxCountPerStackableItem),
		            createStackableItem("6", 1130, 1500, 3100, 800, boxCountPerStackableItem),
		            createStackableItem("7", 800, 490, 1140, 156, boxCountPerStackableItem)
		            
					);


			List<Container> packList = packager.packList(stackableItems, i);

			if(packList != null) {
				write(packList);
				System.out.println(packList.size());
				for (Container container : packList) {
					StackAssert.assertThat(container.getStack()).placementsDoNotIntersect();
				}
			}
		}

	}


}
