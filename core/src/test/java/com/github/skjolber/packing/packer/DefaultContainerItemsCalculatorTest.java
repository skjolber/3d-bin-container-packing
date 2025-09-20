package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.ControlledContainerItem;

public class DefaultContainerItemsCalculatorTest {

	
	
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
		
		calculator.calculateMaxLoadVolume();
		calculator.calculateMaxLoadWeight();
		
		assertEquals(1000L, calculator.getMaxContainerLoadVolume());
		assertEquals(100L, calculator.getMaxContainerLoadWeight());
		
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
		
		calculator.calculateMaxLoadVolume();
		calculator.calculateMaxLoadWeight();
		
		assertEquals(8000L, calculator.getMaxContainerLoadVolume());
		assertEquals(100L, calculator.getMaxContainerLoadWeight());
		
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
		
		calculator.calculateMaxLoadVolume();
		calculator.calculateMaxLoadWeight();
		
		assertEquals(8000L, calculator.getMaxContainerLoadVolume());
		assertEquals(100L, calculator.getMaxContainerLoadWeight());
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		BoxItem boxItem = new BoxItem(box, 10);
		
		List<BoxItem> boxes = new ArrayList<>();
		boxes.add(boxItem);
		
		List<Integer> containers = calculator.getContainers(boxes, 1);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 1);
		
		containers = calculator.getContainers(boxes, 10);
		
		assertEquals(containers.size(), 1);
		assertEquals(containers.get(0), 1);
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
		
		calculator.calculateMaxLoadVolume();
		calculator.calculateMaxLoadWeight();
		
		assertEquals(8000L, calculator.getMaxContainerLoadVolume());
		assertEquals(100L, calculator.getMaxContainerLoadWeight());
		
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
	
	
	private ContainerItemsCalculator create(List<ContainerItem> items) {
		List<ControlledContainerItem> containerItems = new ArrayList<>(items.size());
		for(ContainerItem containerItem : items) {
			containerItems.add(new ControlledContainerItem(containerItem));
		}
		
		return new ContainerItemsCalculator(containerItems);
	}
}
