package com.github.skjolber.packing.packer.bruteforce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
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

	protected abstract AbstractBruteForcePackager createPackager();
	
}
