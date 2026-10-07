package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.packer.AbstractPackagerSession;

class AnyOrderBoxItemGroupIteratorTest {

	@Test
	void acceptsNegativeComparatorValuesOtherThanMinusOne() {
		RemainingBoxItemGroup first = group("first");
		RemainingBoxItemGroup second = group("second");
		PackagerBoxItems boxItems = new PackagerBoxItems(List.of(first, second));
		AnyOrderBoxItemGroupIterator iterator = new AnyOrderBoxItemGroupIterator(
				boxItems.getFilteredBoxItemGroups(), null, null, (reference, candidate) -> -2);

		assertThat(iterator.next()).isEqualTo(1);
	}

	private static RemainingBoxItemGroup group(String id) {
		Box box = Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
		return AbstractPackagerSession.toRemainingBoxItemGroups(List.of(new BoxItemGroup(id, List.of(new BoxItem(box))))).get(0);
	}
}
