package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

public class ContainerItemLoadsCalculatorTest {

	@Test
	public void testSingleContainer() {
		Container container = Container.newBuilder()
				.withMaxLoadWeight(100)
				.withSize(10, 10, 10)
				.build();
		List<ContainerItem> items = ContainerItem.newListBuilder()
				.withContainer(container, 1)
				.build();
		ContainerItemLoadsCalculator calculator = create(items);
		
		assertEquals(1000L, calculator.calculateMaxVolume(1).getValue().intValue());
		assertEquals(100L, calculator.calculateMaxWeight(1).getValue().intValue());
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0).getIndex(), 0);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0).getIndex(), 0);
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		assertEquals(9000L, calculator.calculateMaxVolume(2).getValue().intValue());
		assertEquals(200L, calculator.calculateMaxWeight(2).getValue().intValue());
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0).getIndex(), 0);
		assertEquals(containers.get(1).getIndex(), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0).getIndex(), 0);
		assertEquals(containers.get(1).getIndex(), 1);
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0).getIndex(), 1);
		
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0).getIndex(), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0).getIndex(), 0);
		assertEquals(containers.get(1).getIndex(), 1);
	}

	@Test
	public void testAccept() {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(6, 2, 1).withMaxLoadWeight(100).build(), 3)
				.withContainer(Container.newBuilder().withDescription("2").withEmptyWeight(1).withSize(8, 2, 1).withMaxLoadWeight(100).build(), 1)
				.build();

		ContainerItemLoadsCalculator calculator = create(containers);
		
		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(Box.newBuilder().withDescription("A").withSize(4, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("B").withSize(4, 2, 1).withRotate3D().withWeight(1).build(), 1));
		products.add(new BoxItem(Box.newBuilder().withDescription("C").withSize(6, 2, 1).withRotate3D().withWeight(1).build(), 1));

		ContainerItemLoadCalculations indexes = calculator.getContainers(products, 2);
		assertEquals(2, indexes.size());
		
		ControlledContainerItem first = calculator.getContainerItem(indexes.get(0).getIndex());
		ControlledContainerItem second = calculator.getContainerItem(indexes.get(1).getIndex());
		
		assertEquals(first.getContainer().getDx(), 6);
		assertEquals(second.getContainer().getDx(), 8);
		
		calculator.toContainer(second, new Stack()); // so now out of 8x2x1 container 
		
		List<BoxItem> products2 = products.subList(0, 2);
		
		ContainerItemLoadCalculations indexes2 = calculator.getContainers(products2, 2);
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
		ContainerItemLoadsCalculator calculator = create(items);
		
		Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 12);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		ContainerItemLoadCalculations containers = calculator.getContainers(boxes, 2);
		
		assertEquals(containers.size(), 2);
		assertEquals(containers.get(0).getIndex(), 1);
		assertEquals(containers.get(1).getIndex(), 2);
	}

	private ContainerItemLoadsCalculator create(List<ContainerItem> items) {
		List<ControlledContainerItem> containerItems = new ArrayList<>(items.size());
		for(ContainerItem containerItem : items) {
			ControlledContainerItem c = new ControlledContainerItem(containerItem);
			c.setIndex(containerItems.size());
			containerItems.add(c);
		}
		
		return new ContainerItemLoadsCalculator(containerItems, false);
	}
}
