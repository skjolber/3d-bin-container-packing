package com.github.skjolber.packing.visualizer.packaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

public class VisualizationTest extends AbstractPackagerTest {

	private List<ContainerItem> containers = ContainerItem
			.newListBuilder()
			.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1500, 1900, 4000).withMaxLoadWeight(100).build())
			.build();

	@Test
	public void testPackager() throws Exception {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 2, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build();

		List<BoxItem> products = new ArrayList<>();

		products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("C").withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 1));

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}
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

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(container)
				.build();

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
				.newBuilder()
				.build();

		List<BoxItem> products = Arrays.asList(
				new BoxItem(Box.newBuilder().withId("Foot").withSize(7, 37, 39).withRotate3D().withWeight(0).build(), 20));

		PackagerResult result = packager.newResultBuilder().withContainers(containers).withStackables(products).withMaxContainerCount(1).build();
		Container pack = result.get(0);

		assertNotNull(pack);

		write(pack);
	}

	private BoxItem createStackableItem(String id, int width, int height, int depth, int weight, int boxCountPerStackableItem) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(width, height, depth)
				.withWeight(weight)
				.withRotate3D()
				.build();

		return new BoxItem(box, boxCountPerStackableItem);
	}

	private static List<BoxItem> products33 = Arrays.asList(
			new BoxItem(Box.newBuilder().withRotate3D().withSize(56, 1001, 1505).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(360, 1100, 120).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(70, 70, 120).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(50, 80, 80).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(20, 20, 500).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(50, 230, 50).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(40, 40, 50).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(50, 50, 60).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(1000, 32, 32).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(2000, 40, 40).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(40, 40, 60).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(60, 90, 40).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(56, 40, 20).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(100, 280, 380).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(2500, 600, 80).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(125, 125, 85).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(80, 180, 360).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(25, 140, 140).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(115, 150, 170).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(76, 76, 222).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(326, 326, 249).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(70, 130, 240).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(330, 120, 490).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(9, 23, 2500).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(2000, 20, 20).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(50, 50, 235).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(90, 610, 210).withWeight(0).build(), 1),
			new BoxItem(Box.newBuilder().withRotate3D().withSize(144, 630, 1530).withWeight(0).build(), 1));

	@Test
	public void testPlainPackager() throws Exception {
		PlainPackager packager = PlainPackager.newBuilder().build();

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products33).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}

	}

	@Test
	@Disabled
	public void testLAFFPackager() throws Exception {
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build();

		PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products33).build();
		if(build.isSuccess()) {
			write(build.getContainers());
		} else {
			fail();
		}
	}

}
