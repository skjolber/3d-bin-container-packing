package com.github.skjolber.packing.visualizer.packaging;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.ep.points3d.DefaultPlacement3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;

public class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testSimpleImperfectSquaredRectangles() throws Exception {
		// this will take quite some time

		BruteForcePackager packager = BruteForcePackager.newBuilder().withCheckpointsPerDeadlineCheck(1024).build();

		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		int level = 10;

		pack(directory.getSimpleImperfectSquaredRectangles(level), packager);

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(level), packager);

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(level), packager);
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
	void testStackMultipleContainers() throws Exception {

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
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(8, 8, 1).withMaxLoadWeight(100).build(), 1)
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
		assertThat(packList).hasSize(1);

		Container fits = packList.get(0);

		write(fits);
	}

	@Test
	public void testImpossible4() throws Exception {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(32300, 10000, 6000)
					.withMaxLoadWeight(50000).build(), 1)
				.build();

		Packager packager = BruteForcePackager.newBuilder().build();

		List<StackableItem> products = Arrays.asList(
			new StackableItem(Box.newBuilder().withRotate3D().withSize(3350, 510, 3350).withWeight(250).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2600, 20500, 3600).withWeight(1200).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2600, 25600, 4200).withWeight(1520).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2600, 25600, 4200).withWeight(1900).build(), 1),
			//new StackableItem(Box.newBuilder().withRotate3D().withSize(2600, 25600, 4200).withWeight(1500).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2600, 25600, 4200).withWeight(1420).build(), 1)
		);

		PackagerResult result = packager
				.newResultBuilder()
				.withContainers(containerItems)
				.withStackables(products)
				.withMaxContainerCount(1)
				.build();

		List<Container> packList = result.getContainers();
		assertThat(packList).hasSize(1);

		Container fits = packList.get(0);

		System.out.println(fits);
		write(fits);
	}
	
	@Test
	public void testImpossible5() throws Exception {

		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(32300, 10000, 6000);

		ep.add(0, new DefaultPlacement3D(0, 0, 0, 3349, 509, 3349));

		System.out.println();
		List<SimplePoint3D<DefaultPlacement3D>> values = ep.getValues();
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = values.get(i);
			System.out.println(i + " " + simplePoint3D);
		}

		ep.add(1, new DefaultPlacement3D(0, 510, 0, 20499, 4109, 2599));
		
		System.out.println();
		values = ep.getValues();
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = values.get(i);
			System.out.println(i + " " + simplePoint3D);
		}

		ep.add(4, new DefaultPlacement3D(3350, 0, 2600, 28949, 4199, 5199));

		System.out.println();
		values = ep.getValues();
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = values.get(i);
			System.out.println(i + " " + simplePoint3D);
		}
		
		/*
		System.out.println();
		System.out.println("Add");
		
		System.out.println();

		ep.add(5, new DefaultPlacement3D(3350, 4200, 2600, 23849, 7799, 5199));

		
		System.out.println("After");
		values = ep.getValues();
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = values.get(i);
			System.out.println(i + " " + simplePoint3D);
		}
		
		//ep.add(2, new DefaultPlacement3D(0, 0, 0, 25600, 2600, 4200));
		//ep.add(4, new DefaultPlacement3D(0, 0, 0, 25600, 2600, 4200));

	*/
		
	}
	
	@Test
	public void testClone() {
		ExtremePoints3D<DefaultPlacement3D> ep = new ExtremePoints3D<>(32300, 10000, 6000, false);
		add(ep);
		
		List<SimplePoint3D<DefaultPlacement3D>> values = ep.getValues();
		for (int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = values.get(i);
			System.out.println(i + " " + simplePoint3D);
		}
		
		System.out.println();
		ExtremePoints3D<DefaultPlacement3D> epClone = new ExtremePoints3D<>(32300, 10000, 6000, true);
		add(epClone);
		
		List<SimplePoint3D<DefaultPlacement3D>> valuesClone = epClone.getValues();
		for (int i = 0; i < valuesClone.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> simplePoint3D = valuesClone.get(i);
			System.out.println(i + " " + simplePoint3D);
		}
		
		System.out.println();
		for(int i = 0; i < values.size(); i++) {
			SimplePoint3D<DefaultPlacement3D> p = values.get(i);
			SimplePoint3D<DefaultPlacement3D> pClone = valuesClone.get(i);
			
			if(!p.toString().equals(pClone.toString())) {
				System.out.println(i);
				System.out.println(p);
				System.out.println(pClone);
				break;
			}
		}
		assertEquals(values.size(), valuesClone.size());
	}
	
	public void add(ExtremePoints3D<DefaultPlacement3D> ep) {
		ep.add(0, new DefaultPlacement3D(0, 0, 0, 509, 3349, 3349));
		ep.add(1, new DefaultPlacement3D(0, 3350, 0, 25599, 5949, 4199));
		ep.add(2, new DefaultPlacement3D(0, 5950, 0, 25599, 8549, 4199));
		ep.add(3, new DefaultPlacement3D(510, 0, 0, 26109, 2599, 4199));
		ep.add(5, new DefaultPlacement3D(510, 2600, 0, 21009, 5199, 3599));
	}
	
}
