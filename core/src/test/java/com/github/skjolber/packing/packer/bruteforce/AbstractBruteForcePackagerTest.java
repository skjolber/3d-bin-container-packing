package com.github.skjolber.packing.packer.bruteforce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerTest;

public abstract class AbstractBruteForcePackagerTest extends AbstractPackagerTest {

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

		AbstractPackager packager = createPackager();
		try {
			PackagerResult build = packager.newResultBuilder()
				.withContainers(ContainerItem.newListBuilder()
					.withContainer(container)
					.build())
				.withStackables(b1)
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

		AbstractPackager packager = createPackager();
		try {
			List<StackableItem> products = Arrays.asList(
					box(3,7,35,1));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainers(containerItems)
					.withStackables(products)
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

		AbstractPackager packager = createPackager();
		try {
			List<StackableItem> products = Arrays.asList(
					box(1,1,1,1), box(3,7,35,1));
	
			PackagerResult build = packager
					.newResultBuilder()
					.withContainers(containerItems)
					.withStackables(products)
					.withMaxContainerCount(3)
					.build();
	
			assertFalse(build.isSuccess());
		} finally {
			packager.close();
		}
	}


	@Test
	public void testMutuallyExclusiveBoxesAndContainersForMultiContainerResult() throws Exception {
		DefaultContainer thin = Container.newBuilder()
				.withDescription("1")
				.withSize(10, 10, 2)
				.withMaxLoadWeight(100000)
				.withEmptyWeight(0)
				.build();

			DefaultContainer thick = Container.newBuilder()
					.withDescription("1")
					.withSize(3, 3, 3)
					.withMaxLoadWeight(100000)
					.withEmptyWeight(0)
					.build();

			StackableItem thinBox = new StackableItem(
				Box.newBuilder()
					.withId("b1")
					.withDescription("b1")
					.withSize(10, 10, 1)
					.withWeight(0)
					.withRotate3D()
					.build(),
				2
			);

		StackableItem thickBox = new StackableItem(
				Box.newBuilder()
					.withId("b1")
					.withDescription("b1")
					.withSize(3, 3, 3)
					.withWeight(5)
					.withRotate3D()
					.build(),
				1
			);

		AbstractPackager packager = createPackager();
		try {
			PackagerResult build = packager.newResultBuilder()
				.withContainers(ContainerItem.newListBuilder()
					.withContainers(thin, thick)
					.build())
				.withStackables(thinBox, thickBox)
				.withMaxContainerCount(2)
				.build();
			
			assertTrue(build.isSuccess());		
		} finally {
			packager.close();
		}
	}

	protected abstract AbstractPackager createPackager();
	
}
