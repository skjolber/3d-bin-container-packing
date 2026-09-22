package com.github.skjolber.packing.api.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

class DefaultBoxItemSourceTest {

	@Test
	void usesLocalIndexAfterAnEarlierItemIsRemoved() {
		BoxItem first = item("first");
		BoxItem second = item("second");
		DefaultBoxItemSource source = new DefaultBoxItemSource(List.of(first, second));

		source.decrement(0, 1);

		assertEquals(0, second.getLocalIndex());
		source.decrement(0, 1);
		assertEquals(0, source.size());
	}

	private static BoxItem item(String id) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build());
	}
}
