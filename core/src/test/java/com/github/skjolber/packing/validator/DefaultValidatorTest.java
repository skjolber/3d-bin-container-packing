package com.github.skjolber.packing.validator;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;

public class DefaultValidatorTest {
	
	private DefaultValidator validator = new DefaultValidator();

	@Test
	void testPlacementsIntersects() {
		List<ContainerItem> trustedContainerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));
	
			Container untrustedContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(4, 4, 4).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = untrustedContainer.getStack();
				
			// products intersects
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 0, 0, 0));
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 1, 1));
			
			PackagerResult result = new PackagerResult(Arrays.asList(untrustedContainer), 0, false);
	
			assertNotValidUsingValidator(trustedContainerItems, 1, result, products);
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testPlacementsOutsideContainer() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));
	
			Container resultContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = resultContainer.getStack();
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 0, 0, 0));
			
			// out of bounds
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 2, 2, 2));
			
			PackagerResult result = new PackagerResult(Arrays.asList(resultContainer), 0, false);
	
			assertNotValidUsingValidator(containerItems, 1, result, products);
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testMissingProduct() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			Container resultContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = resultContainer.getStack();
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 0, 0, 0));
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 0, 0));
			
			// missing: C
			
			PackagerResult result = new PackagerResult(Arrays.asList(resultContainer), 0, false);
	
			assertNotValidUsingValidator(containerItems, 1, result, products);
		} finally {
			packager.close();
		}
	}
	
	@Test
	void testTooLowQuantity() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
	
			Container resultContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = resultContainer.getStack();
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 0, 0, 0));
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 0, 0));
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 1, 0));
			
			// one too few
			
			PackagerResult result = new PackagerResult(Arrays.asList(resultContainer), 0, false);
	
			assertNotValidUsingValidator(containerItems, 1, result, products);
		} finally {
			packager.close();
		}
	}

	@Test
	void testTooHighQuantity() {
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			Container resultContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = resultContainer.getStack();
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 0, 0, 0));
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 0, 0));
			
			// one too many
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 1, 1, 0));
			
			PackagerResult result = new PackagerResult(Arrays.asList(resultContainer), 0, false);
	
			assertNotValidUsingValidator(containerItems, 1, result, products);
		} finally {
			packager.close();
		}
	}

	protected void assertNotValidUsingValidator(List<ContainerItem> containerItems, int maxContainers, PackagerResult result, List<BoxItem> boxItems) {
		ValidatorResult validatorResult = validator.newResultBuilder()
				.withContainerItems(containerItems)
				.withMaxContainerCount(maxContainers)
				.withPackagerResult(result)
				.withBoxItems(boxItems)
				.build();
		
		assertFalse(validatorResult.isValid());
	}
	
	private Placement createPlacement(BoxStackValue stackValue, int x, int y, int z) {
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, stackValue.getDx() - 1, stackValue.getDy() - 1, stackValue.getDz() - 1));
	}

}
