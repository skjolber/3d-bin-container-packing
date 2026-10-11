package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class PackagerBoxItemsTest {

	/*
	 * Groups of 2, 5 and 1 box items:
	 *
	 *   index  0 1 | 2 3 4 5 6 | 7
	 *   group   A  |     B     | C
	 *
	 * The first box item of group C is at index 7.
	 */
	@Test
	public void firstBoxItemIndexSumsPrecedingGroupSizes() {
		PackagerBoxItems items = new PackagerBoxItems(groups(2, 5, 1));
		assertThat(items.getFirstBoxItemIndexForGroup(0)).isEqualTo(0);
		assertThat(items.getFirstBoxItemIndexForGroup(1)).isEqualTo(2);
		assertThat(items.getFirstBoxItemIndexForGroup(2)).isEqualTo(7);
	}

	/* Removing the last group leaves exactly the box items of the other groups. */
	@Test
	public void firstBoxItemIndexOfAGroupMovesWhenAPrecedingGroupIsRemoved() {
		List<BoxItemGroup> groups = groups(2, 3, 1);
		PackagerBoxItems items = new PackagerBoxItems(groups);
		assertThat(items.getFirstBoxItemIndex(groups.get(2))).isEqualTo(5);

		// after removing a group, the following groups start earlier
		items.getFilteredBoxItemGroups().remove(0);
		assertThat(items.getFirstBoxItemIndex(groups.get(2))).isEqualTo(3);
		assertThat(items.getFirstBoxItemIndex(groups.get(0))).isEqualTo(-1);
	}
	@Test
	public void removingGroupRemovesItsBoxItems() {
		List<BoxItemGroup> groups = groups(2, 5, 1);
		PackagerBoxItems items = new PackagerBoxItems(groups);
		BoxItemGroup removed = items.getFilteredBoxItemGroups().remove(2);
		assertThat(removed.getId()).isEqualTo("group-2");
		assertThat(items.getFilteredBoxItems().size()).isEqualTo(7);
		for(int i = 0; i < 7; i++) {
			assertThat(items.getFilteredBoxItems().get(i).getLocalIndex()).isEqualTo(i);
		}
	}

	/*
	 * An emptied box item in group A is removed; box items after it must keep their group:
	 *
	 *   before  A A B B B C      after  A B B B C
	 */
	@Test
	public void removingEmptyBoxItemKeepsGroupMapping() {
		PackagerBoxItems items = new PackagerBoxItems(groups(2, 3, 1));
		items.getFilteredBoxItems().get(1).decrement(1);
		items.removeEmpty(false);
		assertThat(items.getFilteredBoxItems().size()).isEqualTo(5);
		assertThat(items.boxToGroupIndexes).startsWith(0, 1, 1, 1, 2);
	}

	private static List<BoxItemGroup> groups(int... sizes) {
		List<BoxItemGroup> groups = new ArrayList<>();
		int box = 0;
		for(int g = 0; g < sizes.length; g++) {
			List<BoxItem> items = new ArrayList<>();
			for(int i = 0; i < sizes[g]; i++) {
				items.add(new BoxItem(Box.newBuilder()
						.withId("box-" + box++)
						.withSize(1, 1, 1)
						.withWeight(1)
						.build(), 1));
			}
			groups.add(new BoxItemGroup("group-" + g, items));
		}
		return groups;
	}
}
