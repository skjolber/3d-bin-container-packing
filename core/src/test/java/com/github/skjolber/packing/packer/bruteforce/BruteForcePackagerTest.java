package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class BruteForcePackagerTest extends AbstractBruteForcePackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			List<Placement> placements = containers.get(0).getStack().getPlacements();
	
			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemId("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
			
			assertValidUsingValidator(containerItems, Integer.MAX_VALUE, build, products);
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackMultipleContainers() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 5)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItems(clone(products))
					.withMaxContainerCount(5)
					.build();
	
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
			
			assertValidUsingValidator(containers, Integer.MAX_VALUE, build, products);
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingBinary1() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(8, 8, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
			products.add(new BoxItem(Box.newBuilder().withId("J").withRotate3D().withSize(4, 4, 1).withWeight(1).build(), 1));
			
			for (int i = 0; i < 4; i++) {
				products.add(new BoxItem(Box.newBuilder().withId("K" + i).withRotate3D().withSize(2, 2, 1).withWeight(1).build(), 1));
			}
			for (int i = 0; i < 16; i++) {
				products.add(new BoxItem(Box.newBuilder().withId("N" + i).withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			}
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).build();
	
			Container fits = build.getContainers().get(0);
			assertValid(fits);
			assertEquals(products.size(), fits.getStack().getPlacements().size());
			
			assertValidUsingValidator(containers, Integer.MAX_VALUE, build, products);
		} finally {
			packager.close();
		}
	}

	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(10, 10, 4).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("J").withRotate3D().withSize(5, 10, 4).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("L").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("M").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("N").withRotate3D().withSize(5, 10, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(products).build();
	
			assertValid(build);
			
			assertValidUsingValidator(containerItems, Integer.MAX_VALUE, build, products);
		} finally {
			packager.close();
		}
	}

	@Test
	public void testStackingBox() {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("Container").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).build();
			assertValid(build);
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
				.withContainer(Container.newBuilder().withId("Container").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1).withMaxLoadWeight(100)
						.withStack(new ValidatingStack()).build())
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
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
				products.add(new BoxItem(Box.newBuilder().withId(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
			}
	
			//Collections.shuffle(products);
	
			PackagerResult build = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).build();
	
			Container fits = build.getContainers().get(0);
			assertNotNull(bouwkampCode.getName(), fits);
			assertValid(fits);
			assertEquals(bouwkampCode.getName(), fits.getStack().size(), squares.size());
		} finally {
			packager.close();
		}
	}

	@Test
	@Disabled
	public void testAHugeProblemShouldRespectDeadline() {
		assertDeadlineRespected(BruteForcePackager.newBuilder().build());
	}
	
	@Test
	public void testImpossible1() throws Exception {
		Container container = Container.newBuilder()
			.withId("1")
			.withSize(18, 12, 12)
			.withMaxLoadWeight(100000)
			.withEmptyWeight(0)
			.build();

		BoxItem b1 = new BoxItem(
			Box.newBuilder()
				.withId("b1")
				.withId("b1")
				.withSize(22, 5, 15)
				.withWeight(5)
				.withRotate3D()
				.build(),
			1
		);

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			PackagerResult build = packager.newResultBuilder()
				.withContainerItems(ContainerItem.newListBuilder()
					.withContainer(container)
					.build())
				.withBoxItems(b1)
				.build();
			
			assertFalse(build.isSuccess());		
		} finally {
			packager.close();
		}
	}
	
	@Test
	public void testImpossible2() {
		// could not pack NonEmptyList(3x7x35 1) (total volume 735) in GroupedContainers(List(too small),2,7,35) (490)
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(2,7,35)
						.withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = Arrays.asList(
					box(3,7,35,1));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.build();
	
			assertFalse(build.isSuccess());
		} finally {
			packager.close();
		}
	}

	@Test
	public void testImpossible3() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(2,7,35)
						.withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = Arrays.asList(
					box(1,1,1,1), box(3,7,35,1));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.withMaxContainerCount(3)
					.build();
	
			assertFalse(build.isSuccess());
		} finally {
			packager.close();
		}
	}

	@Override
	protected BruteForcePackager createPackager() {
		return BruteForcePackager.newBuilder().build();
	}

	@Test
	public void test752() throws Exception {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(32300, 10000, 6000)
					.withMaxLoadWeight(50000).build(), 1)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = Arrays.asList(
				new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(3350, 510, 3350).withWeight(250).build(), 1),
				new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2600, 20500, 3600).withWeight(1200).build(), 1),
				new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(2600, 25600, 4200).withWeight(1520).build(), 1),
				new BoxItem(Box.newBuilder().withId("D").withRotate3D().withSize(2600, 25600, 4200).withWeight(1900).build(), 1),
				new BoxItem(Box.newBuilder().withId("E").withRotate3D().withSize(2600, 25600, 4200).withWeight(1500).build(), 1),
				new BoxItem(Box.newBuilder().withId("F").withRotate3D().withSize(2600, 25600, 4200).withWeight(1420).build(), 1)
			);
	
			PackagerResult result = packager
					.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.withMaxContainerCount(1)
					.build();
	
			assertFalse(result.isSuccess());
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackingSquaresOnSquareWithPredefinedPoints() {
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(false, 16);
		calculator.clearToSize(2, 2, 1);
		Box box = Box.newBuilder().withId("0").withSize(1, 1, 1).withWeight(1).build();
		Placement pillar = new Placement(box.getStackValue(0), calculator.get(0));
		calculator.add(0, pillar);
		
		Container container = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(2, 2, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			PackagerResult build = packager.newResultBuilder()
					.withContainerItem( (c) -> {
						c.withContainerItem(container, 1);
						c.withPoints(calculator.getAll());
					})
					.withBoxItems(products)
					.build();
			
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			List<Placement> placements = containers.get(0).getStack().getPlacements();
			for(Placement placement : placements) {
				assertFalse(placement.getAbsoluteX() == 0 && placement.getAbsoluteY() == 0);
			}
			
			assertValidUsingValidator(Arrays.asList(new ContainerItem(container, 1)), 1, build, products);

		} finally {
			packager.close();
		}
	}

	
}
