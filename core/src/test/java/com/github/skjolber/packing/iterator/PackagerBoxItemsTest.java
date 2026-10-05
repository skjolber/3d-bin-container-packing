package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class PackagerBoxItemsTest {

	private static BoxItemGroup group(String id, int items) {
		List<BoxItem> boxItems = new ArrayList<>();
		for (int i = 0; i < items; i++) {
			boxItems.add(new BoxItem(Box.newBuilder().withId(id + i).withSize(1, 1, 1).withWeight(1).build(), 1));
		}
		return new BoxItemGroup(id, boxItems);
	}

	/**
	 * <pre>
	 *   box items:  a0 a1 | b0 b1 b2 | c0
	 *   index:       0  1 |  2  3  4 |  5
	 * </pre>
	 */
	@Test
	public void findsTheFirstBoxItemOfAGroup() {
		BoxItemGroup a = group("a", 2);
		BoxItemGroup b = group("b", 3);
		BoxItemGroup c = group("c", 1);
		PackagerBoxItems boxItems = new PackagerBoxItems(List.of(a, b, c));

		assertThat(boxItems.getFirstBoxItemIndexForGroup(0)).isEqualTo(0);
		assertThat(boxItems.getFirstBoxItemIndexForGroup(1)).isEqualTo(2);
		assertThat(boxItems.getFirstBoxItemIndexForGroup(2)).isEqualTo(5);

		assertThat(boxItems.getFirstBoxItemIndex(c)).isEqualTo(5);

		// after removing a group, the following groups start earlier
		boxItems.getFilteredBoxItemGroups().remove(0);
		assertThat(boxItems.getFirstBoxItemIndex(c)).isEqualTo(3);
		assertThat(boxItems.getFirstBoxItemIndex(a)).isEqualTo(-1);
	}
}
