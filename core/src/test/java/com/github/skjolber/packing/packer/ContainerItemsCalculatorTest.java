package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Motion;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.cost.LinearBucketWeightContainerCostCalculator;

public class ContainerItemsCalculatorTest {

	@Test
	public void clonedCalculatorHasIndependentContainerInventory() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 10, 10)
				.build();
		ContainerItemsCalculator original = create(ContainerItem.newListBuilder()
				.withContainer(container, 2)
				.build());
		ContainerItemsCalculator clone = original.clone();

		assertNotSame(original.getContainerItem(0), clone.getContainerItem(0));
		assertEquals(0, clone.getContainerItem(0).getIndex());
		clone.getContainerItem(0).decrement();
		assertEquals(2, original.getContainerItem(0).getCount());
		assertEquals(1, clone.getContainerItem(0).getCount());
		assertEquals(1000L, clone.calculateMaxVolume(1).getValue().longValue());
	}

	@Test
	public void resetRestoresInitialCountsWithoutReplacingItems() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 10, 10)
				.build();
		ContainerItemsCalculator calculator = create(ContainerItem.newListBuilder()
				.withContainer(container, 2)
				.withContainer(container, 1)
				.build());
		ControlledContainerItem first = calculator.getContainerItem(0);
		ControlledContainerItem second = calculator.getContainerItem(1);
		first.decrement();
		second.decrement();

		ContainerItemsCalculator clone = calculator.clone();
		clone.reset();
		assertEquals(2, clone.getContainerItem(0).getCount());
		assertEquals(1, clone.getContainerItem(1).getCount());
		assertEquals(1, first.getCount());
		assertEquals(0, second.getCount());

		calculator.reset();
		assertSame(first, calculator.getContainerItem(0));
		assertSame(second, calculator.getContainerItem(1));
		assertEquals(2, first.getCount());
		assertEquals(1, second.getCount());

		first.setCount(4);
		first.mark();
		first.decrement();
		calculator.reset();
		assertEquals(4, first.getCount());
	}

	@Test
	public void testSingleContainer() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 10, 10)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		assertEquals(1000L, calculator.calculateMaxVolume(1).getValue().intValue());
		assertEquals(100L, calculator.calculateMaxWeight(1).getValue().intValue());
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 0);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 0);
	}

	@Test
	public void testMultipleContainers() {
		Container container1 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 10, 10)
				.build();
		Container container2 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(20, 20, 20)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container1, 1)
				.withContainer(container2, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		assertEquals(9000L, calculator.calculateMaxVolume(2).getValue().intValue());
		assertEquals(200L, calculator.calculateMaxWeight(2).getValue().intValue());
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0), 0);
		assertEquals(containers.get(1), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0), 0);
		assertEquals(containers.get(1), 1);
	}
	
	@Test
	public void testSingleContainerWeightTooLow() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(1)
				.withSize(10, 10, 10)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 0);

		containers = calculator.getContainers(boxes, 10);
		assertEquals(containers.size(), 0);
	}
	
	@Test
	public void testSingleContainerVolumeTooLow() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(2, 2, 2)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 0);

		containers = calculator.getContainers(boxes, 10);
		assertEquals(containers.size(), 0);
	}
	
	@Test
	public void testMultipleContainersSomeTooSmall() {
		Container container1 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(2, 2, 3)
				.build();
		Container container2 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(20, 20, 20)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container1, 1)
				.withContainer(container2, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 2);
	}
	
	
	@Test
	public void testMultipleContainersSomeSmallButNumerious() {
		Container container1 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(2, 2, 3)
				.build();
		Container container2 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(20, 20, 20)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container1, 10)
				.withContainer(container2, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0), 0);
		assertEquals(containers.get(1), 1);
	}

	@Test
	public void testAccept() {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 2, 1).withMaxLoadWeight(100).build(), 3)
				.withContainer(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(8, 2, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		ContainerItemsCalculator calculator = create(containers);
		
		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(4, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(4, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("C").withSize(6, 2, 1).withRotate3D().withWeight(1).build(), 1));

		List<Integer> indexes = calculator.getContainers(products, 2);
		assertEquals(2, indexes.size());
		
		ControlledContainerItem first = calculator.getContainerItem(indexes.get(0));
		ControlledContainerItem second = calculator.getContainerItem(indexes.get(1));
		
		assertEquals(first.getContainer().getDx(), 6);
		assertEquals(second.getContainer().getDx(), 8);
		
		calculator.toContainer(second, new Stack()); // so now out of 8x2x1 container 
		
		List<BoxItem> products2 = products.subList(0, 2);
		
		List<Integer> indexes2 = calculator.getContainers(products2, 2);
		assertEquals(indexes2.size(), 1);
	}
	
	@Test
	public void testDontUseASmallBoxIfMaxVolumeReductionIsTooBig() {
		Container container1 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(1, 1, 1)
				.build();
		Container container2 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(5, 1, 1)
				.build();
		Container container3 = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 1, 1)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container1, 1)
				.withContainer(container2, 1)
				.withContainer(container3, 1)
				.build();
		ContainerItemsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 12);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 2);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0), 1);
		assertEquals(containers.get(1), 2);
	}

	@Test
	public void testToContainerPreservesMotion() {
		Motion motion = new Motion();
		Container container = new Container("id", "description", 10, 10, 10, 1, 10, 10, 10, 100, new Stack(), motion);
		ContainerItem containerItem = new ContainerItem(container, 1);

		ContainerItemsCalculator calculator = create(List.of(containerItem));

		Container result = calculator.toContainer(calculator.getContainerItem(0), new Stack());

		assertSame(motion, result.getMotion());
	}

	@Test
	public void estimateUsesTheNextCostEffectiveContainerAfterInventoryIsExhausted() {
		Container cheap = Container.newBuilder().withSize(2, 1, 1).withMaxLoadWeight(2).build();
		Container next = Container.newBuilder().withSize(2, 1, 1).withMaxLoadWeight(2).build();
		ContainerItemsCalculator calculator = create(ContainerItem.newListBuilder()
				.withContainer(cheap, 2, new FixedContainerCostCalculator(1, 2, null, 0))
				.withContainer(next, 2, new FixedContainerCostCalculator(3, 2, null, 0))
				.build());
		BoxItem boxes = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).build(), 6);

		ContainerItemsCostCalculator estimate = new EstimatingContainerItemsCostCalculator();
		ContainerItemsCostCalculator exact = new ExactContainerItemsCostCalculator();
		assertEquals(5, estimate.getMinimumCost(calculator, List.of(boxes), 3));
		assertEquals(5, exact.getMinimumCost(calculator, List.of(boxes), 3));
		assertEquals(Long.MAX_VALUE, estimate.getMinimumCost(calculator, List.of(boxes), 2));
		assertEquals(Long.MAX_VALUE, exact.getMinimumCost(calculator, List.of(boxes), 2));
		calculator.getContainerItem(0).decrement();
		assertEquals(7, estimate.getMinimumCost(calculator, List.of(boxes), 3));
		assertEquals(7, exact.getMinimumCost(calculator, List.of(boxes), 3));
	}

	@Test
	public void exactCostAssignsWholeBoxesAndGroups() {
		Container small = Container.newBuilder().withSize(3, 1, 1).withMaxLoadWeight(3).build();
		Container large = Container.newBuilder().withSize(4, 1, 1).withMaxLoadWeight(4).build();
		ContainerItemsCalculator calculator = create(ContainerItem.newListBuilder()
				.withContainer(small, 2, new FixedContainerCostCalculator(2, 3, null, 0))
				.withContainer(large, 1, new FixedContainerCostCalculator(5, 4, null, 0))
				.build());
		BoxItem boxes = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(2).build(), 2);
		BoxItemGroup group = new BoxItemGroup("pair", List.of(boxes));

		ContainerItemsCostCalculator estimate = new EstimatingContainerItemsCostCalculator();
		ContainerItemsCostCalculator exact = new ExactContainerItemsCostCalculator();
		assertEquals(2, estimate.getMinimumCost(calculator, List.of(boxes), 2));
		assertEquals(4, exact.getMinimumCost(calculator, List.of(boxes), 2));
		assertEquals(2, estimate.getGroupMinimumCost(calculator, List.of(group), 2));
		assertEquals(5, exact.getGroupMinimumCost(calculator, List.of(group), 2));
	}

	@Test
	public void exactCostRejectsContainersThatCannotFitTheBoxDimensions() {
		Container cheap = Container.newBuilder().withSize(1, 2, 2).withMaxLoadWeight(4).build();
		Container fitting = Container.newBuilder().withSize(2, 2, 1).withMaxLoadWeight(4).build();
		ContainerItemsCalculator calculator = create(ContainerItem.newListBuilder()
				.withContainer(cheap, 1, new FixedContainerCostCalculator(1, 4, null, 0))
				.withContainer(fitting, 1, new FixedContainerCostCalculator(5, 4, null, 0))
				.build());
		BoxItem box = new BoxItem(Box.newBuilder().withSize(2, 2, 1).withWeight(1).build(), 1);

		assertEquals(1, new EstimatingContainerItemsCostCalculator().getMinimumCost(calculator, List.of(box), 1));
		assertEquals(5, new ExactContainerItemsCostCalculator().getMinimumCost(calculator, List.of(box), 1));
	}

	@Test
	public void exactCostPricesTheAssignedLoadWeight() {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItemsCalculator calculator = create(ContainerItem.newListBuilder()
				.withContainer(container, 1, new LinearBucketWeightContainerCostCalculator(0, 0, 1, 10, 1, 1, null, 0))
				.withContainer(container, 1, new FixedContainerCostCalculator(5, 1, null, 0))
				.build());
		BoxItem box = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).build(), 1);

		assertEquals(0, new EstimatingContainerItemsCostCalculator().getMinimumCost(calculator, List.of(box), 1));
		assertEquals(5, new ExactContainerItemsCostCalculator().getMinimumCost(calculator, List.of(box), 1));
	}


	private ContainerItemsCalculator create(List<ContainerItem> items) {
		List<ControlledContainerItem> containerItems = new ArrayList<>(items.size());
		for(ContainerItem containerItem : items) {
			ControlledContainerItem c = new ControlledContainerItem(containerItem);
			c.setIndex(containerItems.size());
			containerItems.add(c);
		}
		
		return new ContainerItemsCalculator(containerItems);
	}
}
