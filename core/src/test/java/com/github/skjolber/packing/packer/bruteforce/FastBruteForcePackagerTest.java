package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.*;
import java.util.Map.Entry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;



public class FastBruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertValid(fits);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("B");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");
		
		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
	}

	@Test
	void testStackMultipleContainers() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));

		List<Container> packList = packager.packList(products, 5, System.currentTimeMillis() + 5000);
		assertValid(packList);
		assertThat(packList).hasSize(2);
		
		Container fits = packList.get(0);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("B");
		
		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
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
				long timestamp = System.currentTimeMillis();
				System.out.println("Package " + bouwkampCode.getName() + " " + bouwkampCodes.getSource());
				pack(bouwkampCode);
				System.out.println("Packaged " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder() + " in " + (System.currentTimeMillis() - timestamp));
			}
		}
	}
	
	protected void pack(BouwkampCode bouwkampCode) {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withDescription("Container").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		List<Integer> squares = new ArrayList<>(); 
		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			squares.addAll(bouwkampCodeLine.getSquares());
		}

		// map similar items to the same stack item - this actually helps a lot
		Map<Integer, Integer> frequencyMap = new HashMap<>();
		squares.forEach(word ->
        	frequencyMap.merge(word, 1, (v, newV) -> v + newV)
		);
		
		for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
			int square = entry.getKey();
			int count = entry.getValue();
			products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
		}

		Collections.shuffle(products);

		Container fits = packager.pack(products);
		assertNotNull(bouwkampCode.getName(), fits);
		assertValid(fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().getSize(), squares.size());
	}

	@Test
	@Disabled
	void testAnotherLargeProblemShouldRespectDeadline() {

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1900, 1500, 4000)
			.withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = Arrays.asList(
			box(1000, 1000, 1000, 1),
			box(1000, 1000, 1000, 4),
			box(100, 1050, 750, 1),
			box(100, 650, 750, 1),
			box(16, 2500, 11, 1),
			box(250, 150, 80, 1),
			box(280, 800, 480, 1),
			box(30, 620, 10, 1),
			box(40, 1000, 1000, 1),
			box(40, 100, 165, 1),
			box(44, 575, 534, 1),
			box(475, 530, 150, 1),
			box(47, 3160, 660, 1),
			box(530, 120, 570, 1),
			box(55, 500, 745, 1),
			box(670, 25, 15, 1),
			box(700, 300, 30, 1),
			box(700, 400, 30, 1),
			box(75, 400, 720, 1),
			box(77, 360, 750, 1),
			box(80, 450, 760, 1),
			box(90, 210, 680, 1));

		// strangely when the timeout is set to now + 200ms it properly returns null
		Container fits = packager.pack(products, System.currentTimeMillis() + 1000);
		assertNull(fits);
	}

	@Test
	@Disabled
	void testAHugeProblemShouldRespectDeadline() {

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1900, 1500, 4000)
			.withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = Arrays.asList(
			box(1000, 12, 12,1),
			box(1000, 14, 14,1),
			box(100, 250, 410,2),
			box(104, 81, 46,1),
			box(116, 94, 48,1),
			box(120, 188, 368,1),
			box(1250, 4, 600,3),
			box(1270, 870, 45,1),
			box(135, 189, 75,2),
			box(13, 20, 2500,4),
			box(154, 195, 255,1),
			box(180, 170, 75,1),
			box(180, 60, 50,2),
			box(225, 120, 500,2),
			box(2500, 1200, 13,1),
			box(2500, 30, 600,1),
			box(250, 150, 230,1),
			box(28, 56, 1094,3),
			box(28, 75, 145,3),
			box(29, 71, 112,1),
			box(30, 30, 2000,2),
			box(30, 75, 150,2),
			box(310, 130, 460,10),
			box(313, 313, 16,18),
			box(32, 105, 163,2),
			box(32, 73, 150,2),
			box(355, 370, 161,1),
			box(36, 23, 23,2),
			box(380, 380, 130,1),
			box(385, 140, 55,1),
			box(38, 38, 30,6),
			box(397, 169, 133,2),
			box(39, 38, 28,2),
			box(39, 66, 206,2),
			box(40, 40, 2000,2),
			box(410, 410, 170,1),
			box(419, 646, 784,1),
			box(41, 29, 24,12),
			box(42, 34, 19,2),
			box(44, 35, 28,6),
			box(467, 174, 135,1),
			box(46, 41, 24,12),
			box(47, 44, 29,6),
			box(49, 36, 36,2),
			box(49, 48, 23,6),
			box(4, 4, 2500,4),
			box(50, 39, 25,2),
			box(50, 49, 21,6),
			box(52, 51, 21,6),
			box(55, 46, 45,2),
			box(570, 310, 85,29),
			box(58, 32, 32,1),
			box(614, 824, 96,1),
			box(61, 51, 26,14),
			box(625, 500, 50,6),
			box(640, 510, 1200,1),
			box(640, 960, 220,1),
			box(65, 48, 231,2),
			box(65, 64, 38,4),
			box(68, 66, 39,4),
			box(700, 325, 90,1),
			box(71, 42, 39,4),
			box(73, 43, 40,4),
			box(79, 78, 46,4),
			box(82, 80, 47,4),
			box(84, 67, 44,4),
			box(88, 52, 47,4),
			box(90, 800, 2040,1),
			box(970, 790, 2200,1));

		long start = System.currentTimeMillis();
		long desiredDuration = 50;
		long deadline = start + desiredDuration;
		
		Container fits = packager.pack(products, deadline);
		long timestamp = System.currentTimeMillis();
		long duration = timestamp - start;
		assertThat(duration).isLessThan(desiredDuration);
		assertNull(fits);
	}

}
