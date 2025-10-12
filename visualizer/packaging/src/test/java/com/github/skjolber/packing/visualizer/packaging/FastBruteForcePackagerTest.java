package com.github.skjolber.packing.visualizer.packaging;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;

public class FastBruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testFastBruteForcePackager() throws Exception {

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(5, 5, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("C").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("D").withSize(3, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("E").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).build();
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

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));
		products.add(new BoxItem(Box.newBuilder().withDescription("C").withSize(1, 1, 1).withRotate3D().withWeight(1).build(), 2));

		PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).withMaxContainerCount(5).build();
		assertFalse(result.getContainers().isEmpty());

		Container fits = result.get(0);

		List<Placement> placements = fits.getStack().getPlacements();

		System.out.println(fits.getStack().getPlacements());

		assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemDescription("A");
		assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemDescription("A");
		assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemDescription("B");

		assertThat(placements.get(0)).isAlongsideX(placements.get(1));
		assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
		assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));

		write(result.getContainers());
	}

}
