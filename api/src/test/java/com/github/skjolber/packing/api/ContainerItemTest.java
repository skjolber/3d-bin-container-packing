package com.github.skjolber.packing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ContainerItemTest {

	@Test
	void countCanBeResetLikeBoxItem() {
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build();
		ContainerItem item = new ContainerItem(container, 3);

		item.decrement();
		item.reset();
		assertEquals(3, item.getCount());

		item.setCount(2);
		item.reset();
		assertEquals(3, item.getCount());

		item.setCount(2);
		item.mark();
		item.decrement();
		item.reset();
		assertEquals(2, item.getCount());

		item.setResetCount(4);
		item.decrementResetCount();
		ContainerItem copy = new ContainerItem(item);
		copy.reset();
		assertEquals(3, copy.getCount());
		assertEquals(2, item.getCount());
	}
}
