package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;

import static com.github.skjolber.packing.test.assertj.StackablePlacementAssert.assertThat;



public class FastBruteForcePackagerTest {

	@Test
	void testStackingSquaresOnSquare() {

		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withRotate(3, 1, 1, 3, 1, 1, 100, null).withStack(new ValidatingStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		List<StackPlacement> placements = fits.getStack().getPlacements();

		System.out.println(fits.getStack().getPlacements());

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
		
		containers.add(Container.newBuilder().withDescription("1").withEmptyWeight(1).withRotate(3, 1, 1, 3, 1, 1, 100, null).withStack(new ValidatingStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
		products.add(new StackableItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));

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
	}
	

}
