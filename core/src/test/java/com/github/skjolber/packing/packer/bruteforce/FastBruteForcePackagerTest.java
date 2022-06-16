package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	void testLargeProblem48BoxesShouldRespectDeadline() {

		List<Container> containers = new ArrayList<>();

		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1500, 1000, 3200)
			.withMaxLoadWeight(100).withStack(new ValidatingStack()).build());

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 550, 750).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 650, 750).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 850, 750).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(1).build(), 4));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(18, 2720, 160).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(2500, 650, 30).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(1).build(), 6));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(535, 110, 500).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(55, 500, 745).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(611, 31, 791).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(656, 18, 2033).withWeight(1).build(), 4));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(700, 400, 50).withWeight(1).build(), 4));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(700, 500, 50).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 78).withWeight(1).build(), 3));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(750, 17, 30).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(75, 650, 1600).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withRotate3D().withSize(80, 770, 850).withWeight(1).build(), 4));

		Container fits = packager.pack(products, System.currentTimeMillis() + 500);
		assertValid(fits);
	}

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

}
