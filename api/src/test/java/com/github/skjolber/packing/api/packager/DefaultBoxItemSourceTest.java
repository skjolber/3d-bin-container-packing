package com.github.skjolber.packing.api.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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

	@Test
	void emptySourceCanBeFilled() {
		DefaultBoxItemSource source = new DefaultBoxItemSource();
		assertTrue(source.isEmpty());
		assertEquals(0, source.size());

		BoxItem first = item("first");
		BoxItem second = item("second");
		source.add(first);
		source.add(second);

		assertEquals(2, source.size());
		assertEquals(0, first.getLocalIndex());
		assertEquals(1, second.getLocalIndex());

		source.decrement(first.getLocalIndex(), 1);

		assertEquals(1, source.size());
		assertEquals(0, second.getLocalIndex());
	}

	@Test
	void addSetsLocalIndexAfterTheExistingItems() {
		BoxItem first = item("first");
		BoxItem second = item("second");
		BoxItem added = item("added");
		DefaultBoxItemSource source = new DefaultBoxItemSource(List.of(first, second));

		source.add(added);

		assertEquals(2, added.getLocalIndex());
		assertEquals(added, source.get(added.getLocalIndex()));
	}

	@Test
	void setValuesRenumbersLocalIndexes() {
		BoxItem first = item("first");
		BoxItem second = item("second");
		BoxItem third = item("third");
		DefaultBoxItemSource source = new DefaultBoxItemSource(List.of(first, second));

		List<BoxItem> values = new ArrayList<>();
		values.add(third);
		values.add(second);
		values.add(first);
		source.setValues(values);

		assertEquals(0, third.getLocalIndex());
		assertEquals(1, second.getLocalIndex());
		assertEquals(2, first.getLocalIndex());

		source.decrement(third.getLocalIndex(), 1);

		assertEquals(2, source.size());
		assertEquals(0, second.getLocalIndex());
		assertEquals(1, first.getLocalIndex());
	}

	private static BoxItem item(String id) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build());
	}
}
