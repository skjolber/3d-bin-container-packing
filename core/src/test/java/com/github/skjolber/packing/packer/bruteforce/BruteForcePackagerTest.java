package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
		List<Container> containers = build.getContainers();
		assertValid(containers);

		List<StackPlacement> placements = containers.get(0).getStack().getPlacements();

		assertThat(placements.get(0)).isAt(0, 0, 0).hasStackableName("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasStackableName("B");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasStackableName("C");

		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
	}

	@Test
	void testStackMultipleContainers() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 5)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(5).build();

		List<Container> packList = build.getContainers();

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
	void testStackingBinary1() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(8, 8, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();
		products.add(new StackableItem(Box.newBuilder().withDescription("J").withRotate3D().withSize(4, 4, 1).withWeight(1).build(), 1));

		for (int i = 0; i < 4; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
		}
		for (int i = 0; i < 16; i++) {
			products.add(new StackableItem(Box.newBuilder().withDescription("K").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		}

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withStackables(products).build();

		Container fits = build.getContainers().get(0);
		assertValid(fits);
		assertEquals(products.size(), fits.getStack().getPlacements().size());
	}

	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(10, 10, 4).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("J").withRotate3D().withSize(5, 10, 4).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("L").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("J").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("M").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("N").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();

		assertValid(build);
	}

	@Test
	public void testStackingBox() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("Container").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withStackables(products).build();
		assertValid(build);
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

	@Disabled // takes too long
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
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("Container").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(100)
						.withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = new ArrayList<>();

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
			products.add(new StackableItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
		}

		Collections.shuffle(products);

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withStackables(products).build();

		Container fits = build.getContainers().get(0);
		assertNotNull(bouwkampCode.getName(), fits);
		assertValid(fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().getSize(), squares.size());
	}

	@Test
	public void issueNew() {
		Container container = Container
				.newBuilder()
				.withDescription("1")
				.withSize(100, 150, 200)
				.withEmptyWeight(0)
				.withMaxLoadWeight(100)
				.build();

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(container)
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager
				.newBuilder()
				.build();

		List<StackableItem> products = Arrays.asList(
				new StackableItem(Box.newBuilder().withId("1").withSize(200, 2, 50).withRotate3D().withWeight(0).build(), 4),
				new StackableItem(Box.newBuilder().withId("2").withSize(1, 1, 1).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("3").withSize(53, 11, 21).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("4").withSize(38, 7, 19).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("5").withSize(15, 3, 7).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("6").withSize(95, 5, 3).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("7").withSize(48, 15, 42).withRotate3D().withWeight(0).build(), 1),
				new StackableItem(Box.newBuilder().withId("8").withSize(140, 10, 10).withRotate3D().withWeight(0).build(), 2),
				new StackableItem(Box.newBuilder().withId("9").withSize(150, 4, 65).withRotate3D().withWeight(0).build(), 2),
				new StackableItem(Box.newBuilder().withId("10").withSize(75, 17, 60).withRotate3D().withWeight(0).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withStackables(products).build();
		assertValid(build);
	}

	@Test
	@Disabled
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(BruteForcePackager.newBuilder());
	}
	
	@Test
	public void testImpossible1() throws Exception {
		DefaultContainer container = Container.newBuilder()
			.withDescription("1")
			.withSize(18, 12, 12)
			.withMaxLoadWeight(100000)
			.withEmptyWeight(0)
			.build();

		StackableItem b1 = new StackableItem(
			Box.newBuilder()
				.withId("b1")
				.withDescription("b1")
				.withSize(22, 5, 15)
				.withWeight(5)
				.withRotate3D()
				.build(),
			1
		);

		Packager packager = BruteForcePackager.newBuilder().build();

		PackagerResult build = packager.newResultBuilder()
			.withContainers(ContainerItem.newListBuilder()
				.withContainer(container)
				.build())
			.withStackables(b1)
			.withDeadline(60_000)
			.build();
		
		assertFalse(build.isSuccess());		
	}
	
	@Test
	public void testImpossible2() {
		// could not pack NonEmptyList(3x7x35 1) (total volume 735) in GroupedContainers(List(too small),2,7,35) (490)
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2,7,35)
						.withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		Packager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = Arrays.asList(
				box(3,7,35,1));

		PackagerResult build = packager
				.newResultBuilder()
				.withContainers(containerItems)
				.withStackables(products)
				.build();

		assertFalse(build.isSuccess());
	}


}
