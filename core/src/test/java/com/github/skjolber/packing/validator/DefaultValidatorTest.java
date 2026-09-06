package com.github.skjolber.packing.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;

public class DefaultValidatorTest {
	
	private DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws Exception {
		validator.close();
	}

	@Test
	void testInterruptAndPastDeadline() {
		Box box = Box.newBuilder().withId("A").withSize(1, 1, 1).withWeight(1).build();
		Container resultContainer = container("container");
		resultContainer.getStack().add(createPlacement(box.getStackValue(0), 0, 0, 0));
		PackagerResult packagerResult = new PackagerResult(List.of(resultContainer), 0, false);
		ContainerItem trustedContainer = new ContainerItem(container("container"), 1);

		ValidatorResult interrupted = validator.newResultBuilder()
				.withContainerItem(trustedContainer)
				.withPackagerResult(packagerResult)
				.withBoxItems(new BoxItem(box))
				.withInterrupt(() -> true)
				.build();
		assertTrue(interrupted.isTimeout());

		ValidatorResult expired = validator.newResultBuilder()
				.withContainerItem(trustedContainer)
				.withPackagerResult(packagerResult)
				.withBoxItems(new BoxItem(box))
				.withDeadline(System.currentTimeMillis() - 1)
				.build();
		assertTrue(expired.isTimeout());
	}

	@Test
	void testInterruptDuringLoadValidation() {
		List<BoxItem> boxItems = new ArrayList<>();
		Container resultContainer = container("container");
		for (int i = 0; i < 4; i++) {
			Box box = box("box-" + i);
			boxItems.add(new BoxItem(box));
			resultContainer.getStack().add(createPlacement(box.getStackValue(0), i, 0, 0));
		}

		AtomicInteger checks = new AtomicInteger();
		ValidatorResult result = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(resultContainer), 0, false))
				.withBoxItems(boxItems)
				.withInterrupt(() -> checks.incrementAndGet() >= 8)
				.build();

		assertTrue(result.isTimeout());
	}

	@Test
	void testDuplicateGroupIdsAreRejected() {
		BoxItemGroup first = new BoxItemGroup("duplicate", List.of(new BoxItem(box("first"))));
		BoxItemGroup second = new BoxItemGroup("duplicate", List.of(new BoxItem(box("second"))));

		assertThrows(IllegalStateException.class, () -> validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(container("container")), 0, false))
				.withBoxItemGroups(List.of(first, second))
				.build());
	}

	@Test
	void testChronologicalAllowSkippingRejectsReverseMovement() {
		Box first = box("first");
		Box second = box("second");
		Box third = box("third");
		Container resultContainer = container("container");
		resultContainer.getStack().add(createPlacement(third.getStackValue(0), 0, 0, 0));
		resultContainer.getStack().add(createPlacement(first.getStackValue(0), 1, 0, 0));
		resultContainer.getStack().add(createPlacement(second.getStackValue(0), 2, 0, 0));

		ValidatorResult result = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(resultContainer), 0, false))
				.withBoxItems(List.of(new BoxItem(first), new BoxItem(second), new BoxItem(third)))
				.withOrder(Order.CRONOLOGICAL_ALLOW_SKIPPING)
				.build();

		assertFalse(result.isValid());
	}

	@Test
	void testCountsAggregateDuplicateExpectedIdsAndRejectUnexpectedIds() {
		Box expected = box("expected");
		Container validContainer = container("container");
		validContainer.getStack().add(createPlacement(expected.getStackValue(0), 0, 0, 0));
		validContainer.getStack().add(createPlacement(expected.getStackValue(0), 1, 0, 0));

		ValidatorResult valid = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(validContainer), 0, false))
				.withBoxItems(List.of(new BoxItem(expected), new BoxItem(expected)))
				.build();
		assertTrue(valid.isValid());

		Box unexpected = box("unexpected");
		Container invalidContainer = container("container");
		invalidContainer.getStack().add(createPlacement(unexpected.getStackValue(0), 0, 0, 0));
		ValidatorResult invalid = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(invalidContainer), 0, false))
				.withBoxItems(new BoxItem(expected))
				.build();
		assertFalse(invalid.isValid());
	}

	@Test
	void testGroupsMustBePresentAndCannotSpanContainers() {
		Box first = box("first");
		Box second = box("second");
		BoxItemGroup group = new BoxItemGroup("group", List.of(new BoxItem(first), new BoxItem(second)));

		Container emptyContainer = container("container");
		ValidatorResult missing = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 1))
				.withPackagerResult(new PackagerResult(List.of(emptyContainer), 0, false))
				.withBoxItemGroups(List.of(group))
				.build();
		assertFalse(missing.isValid());

		Container firstContainer = container("container");
		firstContainer.getStack().add(createPlacement(first.getStackValue(0), 0, 0, 0));
		firstContainer.getStack().add(createPlacement(second.getStackValue(0), 1, 0, 0));
		Container secondContainer = container("container");
		secondContainer.getStack().add(createPlacement(first.getStackValue(0), 0, 0, 0));
		secondContainer.getStack().add(createPlacement(second.getStackValue(0), 1, 0, 0));
		ValidatorResult repeated = validator.newResultBuilder()
				.withContainerItem(new ContainerItem(container("container"), 2))
				.withMaxContainerCount(2)
				.withPackagerResult(new PackagerResult(List.of(firstContainer, secondContainer), 0, false))
				.withBoxItemGroups(List.of(group))
				.build();
		assertFalse(repeated.isValid());
	}

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
	
	@Test
	void testOrderIncorrect() {
		List<ContainerItem> trustedContainerItems = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withId("1").withEmptyWeight(1).withSize(3, 3, 3).withMaxLoadWeight(100).withStack(new Stack()).build())
				.build();

		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build();
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 1));
	
			Container untrustedContainer = Container.newBuilder().withId("1").withEmptyWeight(1).withSize(4, 4, 4).withMaxLoadWeight(100).withStack(new Stack()).build();
			
			Stack stack = untrustedContainer.getStack();
				
			// products intersects
			stack.add(createPlacement(products.get(1).getBox().getStackValue(0), 0, 0, 0));
			stack.add(createPlacement(products.get(0).getBox().getStackValue(0), 1, 1, 1));
			
			PackagerResult result = new PackagerResult(Arrays.asList(untrustedContainer), 0, false);
	
			assertNotValidUsingValidator(trustedContainerItems, 1, result, products, Order.CRONOLOGICAL);
		} finally {
			packager.close();
		}
	}

	protected void assertNotValidUsingValidator(List<ContainerItem> containerItems, int maxContainers, PackagerResult result, List<BoxItem> boxItems, Order order) {
		ValidatorResult validatorResult = validator.newResultBuilder()
				.withContainerItems(containerItems)
				.withMaxContainerCount(maxContainers)
				.withPackagerResult(result)
				.withOrder(order)
				.withBoxItems(boxItems)
				.build();
		
		assertFalse(validatorResult.isValid());
	}
	
	protected void assertNotValidUsingValidator(List<ContainerItem> containerItems, int maxContainers, PackagerResult result, List<BoxItem> boxItems) {
		assertNotValidUsingValidator(containerItems, maxContainers, result, boxItems, Order.NONE);
	}
	
	private Placement createPlacement(BoxStackValue stackValue, int x, int y, int z) {
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, stackValue.getDx() - 1, stackValue.getDy() - 1, stackValue.getDz() - 1));
	}

	private Box box(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private Container container(String id) {
		return Container.newBuilder().withId(id).withSize(10, 10, 10).withMaxLoadWeight(100).withStack(new Stack()).build();
	}

}
