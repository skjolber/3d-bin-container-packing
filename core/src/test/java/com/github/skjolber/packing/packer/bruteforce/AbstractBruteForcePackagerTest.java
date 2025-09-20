package com.github.skjolber.packing.packer.bruteforce;

import static com.github.skjolber.packing.test.assertj.StackPlacementAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

public abstract class AbstractBruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testImpossible1() throws Exception {
		Container container = Container.newBuilder()
			.withDescription("1")
			.withSize(18, 12, 12)
			.withMaxLoadWeight(100000)
			.withEmptyWeight(0)
			.build();

		BoxItem b1 = new BoxItem(
			Box.newBuilder()
				.withId("b1")
				.withDescription("b1")
				.withSize(22, 5, 15)
				.withWeight(5)
				.withRotate3D()
				.build(),
			1
		);

		AbstractBruteForcePackager packager = createPackager();
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
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2,7,35)
						.withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		AbstractBruteForcePackager packager = createPackager();
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
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(2,7,35)
						.withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 1)
				.build();

		AbstractBruteForcePackager packager = createPackager();
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


	@Test
	public void testMutuallyExclusiveBoxesAndContainersForMultiContainerResult() throws Exception {
		Container thin = Container.newBuilder()
				.withDescription("1")
				.withSize(10, 10, 2)
				.withMaxLoadWeight(100000)
				.withEmptyWeight(0)
				.build();

			Container thick = Container.newBuilder()
					.withDescription("1")
					.withSize(3, 3, 3)
					.withMaxLoadWeight(100000)
					.withEmptyWeight(0)
					.build();

			BoxItem thinBox = new BoxItem(
				Box.newBuilder()
					.withId("b1")
					.withDescription("b1")
					.withSize(10, 10, 1)
					.withWeight(0)
					.withRotate3D()
					.build(),
				2
			);

		BoxItem thickBox = new BoxItem(
				Box.newBuilder()
					.withId("b1")
					.withDescription("b1")
					.withSize(3, 3, 3)
					.withWeight(5)
					.withRotate3D()
					.build(),
				1
			);

		AbstractBruteForcePackager packager = createPackager();
		try {
			PackagerResult build = packager.newResultBuilder()
				.withContainerItems(ContainerItem.newListBuilder()
					.withContainers(thin, thick)
					.build())
				.withBoxItems(thinBox, thickBox)
				.withMaxContainerCount(2)
				.build();
			
			assertTrue(build.isSuccess());		
		} finally {
			packager.close();
		}
	}


	@Test
	public void testMutuallyExclusiveBoxesAndContainersForMultiContainerResult2() throws Exception {
		Container thin = Container.newBuilder()
			.withDescription("1")
			.withSize(10, 10, 2)
			.withMaxLoadWeight(100000)
			.withEmptyWeight(0)
			.build();

		Container thick = Container.newBuilder()
				.withDescription("1")
				.withSize(3, 3, 3)
				.withMaxLoadWeight(100000)
				.withEmptyWeight(0)
				.build();

		BoxItem thinBox1 = new BoxItem(
			Box.newBuilder()
				.withId("b1")
				.withDescription("b1")
				.withSize(10, 10, 1)
				.withWeight(0)
				.withRotate3D()
				.build(),
			1
		);
		
		BoxItem thinBox2 = new BoxItem(
				Box.newBuilder()
					.withId("b1")
					.withDescription("b1")
					.withSize(10, 10, 1)
					.withWeight(0)
					.withRotate3D()
					.build(),
				1
			);

		BoxItem thickBox = new BoxItem(
				Box.newBuilder()
					.withId("b2")
					.withDescription("b2")
					.withSize(3, 3, 3)
					.withWeight(0)
					.withRotate3D()
					.build(),
				1
			);

		AbstractBruteForcePackager packager = createPackager();
		try {
			PackagerResult build = packager.newResultBuilder()
				.withContainerItems(ContainerItem.newListBuilder()
					.withContainers(thin, thick)
					.build())
				.withBoxItems(thinBox1, thickBox, thinBox2)
				.withMaxContainerCount(2)
				.build();
			
			assertTrue(build.isSuccess());		
		} finally {
			packager.close();
		}
	}

	protected abstract AbstractBruteForcePackager createPackager();
	
	@Test
	void testStackingSquaresOnSquareGroups() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build())
				.build();

		AbstractBruteForcePackager packager = createPackager();
		try {
			List<BoxItemGroup> groups = new ArrayList<>();
			List<BoxItem> products1 = new ArrayList<>();
			products1.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products1.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

			groups.add(new BoxItemGroup("1", products1));
			
			List<BoxItem> products2 = new ArrayList<>();
			products2.add(new BoxItem(Box.newBuilder().withDescription("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));

			groups.add(new BoxItemGroup("2", products2));

			PackagerResult build = packager.newResultBuilder().withContainerItems(containerItems)
					.withBoxItemGroups(groups).build();
			List<Container> containers = build.getContainers();
			assertValid(containers);
	
			List<Placement> placements = containers.get(0).getStack().getPlacements();
			assertThat(placements).size().isEqualTo(3);

			assertThat(placements.get(0)).isAt(0, 0, 0).hasBoxItemDescription("A");
			assertThat(placements.get(1)).isAt(1, 0, 0).hasBoxItemDescription("B");
			assertThat(placements.get(2)).isAt(2, 0, 0).hasBoxItemDescription("C");
	
			assertThat(placements.get(0)).isAlongsideX(placements.get(1));
			assertThat(placements.get(2)).followsAlongsideX(placements.get(1));
			assertThat(placements.get(1)).preceedsAlongsideX(placements.get(2));
		} finally {
			packager.close();
		}
	}

	@Test
	void testStackMultipleContainersGroups() {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(3, 1, 1).withMaxLoadWeight(100).withStack(new ValidatingStack()).build(), 5)
				.build();

		AbstractBruteForcePackager packager = createPackager();
		try {
			List<BoxItemGroup> groups = new ArrayList<>();
			List<BoxItem> products1 = new ArrayList<>();

			products1.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			groups.add(new BoxItemGroup("1", products1));
			
			List<BoxItem> products2 = new ArrayList<>();
			products2.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			groups.add(new BoxItemGroup("2", products2));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItemGroups(groups)
					.withMaxContainerCount(5)
					.build();
	
			List<Container> packList = build.getContainers();
	
			assertValid(packList);
			assertThat(packList).hasSize(2);
	
			Container container1 = packList.get(0);
			List<Placement> placements1 = container1.getStack().getPlacements();
			assertThat(placements1).size().isEqualTo(2);
	
			assertThat(placements1.get(0)).isAt(0, 0, 0).hasBoxItemId("A");
			assertThat(placements1.get(1)).isAt(1, 0, 0).hasBoxItemId("A");
			
			Container container2 = packList.get(1);
			List<Placement> placements2 = container2.getStack().getPlacements();
			assertThat(placements2).size().isEqualTo(2);
	
			assertThat(placements2.get(0)).isAt(0, 0, 0).hasBoxItemId("B");
			assertThat(placements2.get(1)).isAt(1, 0, 0).hasBoxItemId("B");
		} finally {
			packager.close();
		}
	}
}
