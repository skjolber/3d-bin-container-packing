package com.github.skjolber.packing.api.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Placement;

class RemainingBoxItemTest {

	private static BoxItem boxItem(String id, int count) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), count);
	}

	@Test
	void countsDownWithoutChangingTheBoxItem() {
		BoxItem boxItem = boxItem("a", 3);
		RemainingBoxItem remaining = new RemainingBoxItem(boxItem);

		remaining.decrement();
		remaining.mark();
		remaining.decrement(2);
		assertEquals(0, remaining.getCount());

		remaining.reset();
		assertEquals(2, remaining.getCount());

		assertEquals(3, boxItem.getCount());
		assertSame(boxItem, remaining.getBoxItem());
		assertSame(boxItem.getBox(), remaining.getBox());
	}

	@Test
	void placementRefersToTheBoxItem() {
		BoxItem boxItem = boxItem("a", 2);
		RemainingBoxItem remaining = new RemainingBoxItem(boxItem, 2, 0, 7);

		Placement placement = new Placement(remaining, boxItem.getBox().getStackValue(0), 0, 0, 0, 0);

		assertSame(boxItem, placement.getBoxItem());
		assertSame(remaining, placement.getRemainingBoxItem());
		assertEquals(7, placement.getRemainingBoxItem().getGlobalIndex());
	}

	@Test
	void groupCopiesShareTheGroupKey() {
		BoxItem a = boxItem("a", 1);
		BoxItem b = boxItem("b", 2);
		BoxItemGroup group = new BoxItemGroup("group", List.of(a, b));

		List<RemainingBoxItem> items = new ArrayList<>(List.of(new RemainingBoxItem(a), new RemainingBoxItem(b)));
		RemainingBoxItemGroup remaining = new RemainingBoxItemGroup(group, items, 0);
		RemainingBoxItemGroup copy = remaining.copy();

		copy.decrement(1, 2);

		assertEquals(1, copy.size());
		assertEquals(2, remaining.size());
		assertSame(group, remaining.get(0).getGroupKey());
		assertSame(group, copy.get(0).getGroupKey());
		assertEquals(3, group.getBoxCount());
		assertNull(new RemainingBoxItem(a).getGroupKey());
	}
}
