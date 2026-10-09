package com.github.skjolber.packing.api.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class BoxItemSourceTest {

	private static class ListBoxItemGroupSource implements BoxItemGroupSource {

		private final List<BoxItemGroup> groups;

		public ListBoxItemGroupSource(List<BoxItemGroup> groups) {
			this.groups = groups;
		}

		@Override
		public int size() {
			return groups.size();
		}

		@Override
		public boolean isEmpty() {
			return groups.isEmpty();
		}

		@Override
		public BoxItemGroup get(int index) {
			return groups.get(index);
		}

		@Override
		public BoxItemGroup remove(int index) {
			return groups.remove(index);
		}

		@Override
		public Iterator<BoxItemGroup> iterator() {
			return groups.iterator();
		}
	}

	private static BoxItem boxItem(String id, int dx, int dy, int dz) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(dx, dy, dz).withRotate3D().withWeight(1).build(), 1);
	}

	private static BoxItemSource source(BoxItem... boxItems) {
		return new DefaultBoxItemSource(List.of(boxItems));
	}

	private static BoxItemGroupSource groupSource(BoxItem... boxItems) {
		List<BoxItemGroup> groups = new ArrayList<>();
		for (BoxItem boxItem : boxItems) {
			groups.add(new BoxItemGroup("group-" + boxItem.getBox().getId(), List.of(boxItem)));
		}
		return new ListBoxItemGroupSource(groups);
	}

	/**
	 * Footprints, i.e. the areas of the faces a box can be placed on:
	 * <pre>
	 *   1x10x100: 10, 100, 1000 (minimum area 10, maximum area 1000)
	 *   2x3x4:    6, 8, 12      (minimum area 6, maximum area 12)
	 * </pre>
	 */

	@Test
	void testMaxAreaIsTheLargestMinimumArea() {
		BoxItem slim = boxItem("slim", 1, 10, 100);
		BoxItem small = boxItem("small", 2, 3, 4);

		assertEquals(10, slim.getBox().getMinimumArea());
		assertEquals(1000, slim.getBox().getMaximumArea());
		assertEquals(6, small.getBox().getMinimumArea());
		assertEquals(12, small.getBox().getMaximumArea());

		// the smallest footprint of the box which needs the most
		assertEquals(10, source(slim, small).getMaxArea());
		assertEquals(10, source(small, slim).getMaxArea());

		assertEquals(10, source(slim).getMaxArea());
		assertEquals(6, source(small).getMaxArea());
	}

	@Test
	void testMinVolumeAndMinAreaOfLargeBoxes() {
		// volume 3 * 10^11, i.e. above Integer.MAX_VALUE
		BoxItem large = boxItem("large", 2000, 3000, 50000);
		BoxItem larger = boxItem("larger", 4000, 3000, 50000);

		assertEquals(300_000_000_000L, large.getBox().getVolume());

		BoxItemSource source = source(large, larger);
		assertEquals(300_000_000_000L, source.getMinVolume());
		assertEquals(6_000_000L, source.getMinArea());
		assertEquals(300_000_000_000L, source(larger, large).getMinVolume());
	}

	@Test
	void testMinAreaAboveIntegerMaxValue() {
		// smallest footprint is 50000 * 60000 = 3 * 10^9, i.e. above Integer.MAX_VALUE
		BoxItem huge = boxItem("huge", 50000, 60000, 70000);

		assertEquals(3_000_000_000L, huge.getBox().getMinimumArea());

		BoxItemSource source = source(huge);
		assertEquals(3_000_000_000L, source.getMinArea());
		assertEquals(3_000_000_000L, source.getMaxArea());
		assertEquals(huge.getBox().getVolume(), source.getMinVolume());
	}

	@Test
	void testMinVolumeAndMinAreaOfLargeBoxesInGroups() {
		BoxItem large = boxItem("large", 2000, 3000, 50000);
		BoxItem larger = boxItem("larger", 4000, 3000, 50000);

		BoxItemGroupSource groups = groupSource(large, larger);
		assertEquals(300_000_000_000L, groups.getMinVolume());
		assertEquals(6_000_000L, groups.getMinArea());
	}

	@Test
	void testMinAreaAboveIntegerMaxValueInGroups() {
		BoxItem huge = boxItem("huge", 50000, 60000, 70000);

		BoxItemGroupSource groups = groupSource(huge);
		assertEquals(3_000_000_000L, groups.getMinArea());
		assertEquals(huge.getBox().getVolume(), groups.getMinVolume());
	}

	@Test
	void testMinimumsOfNothingAreLargerThanAnyBox() {
		assertEquals(Long.MAX_VALUE, source().getMinVolume());
		assertEquals(Long.MAX_VALUE, source().getMinArea());

		BoxItemGroupSource groups = new ListBoxItemGroupSource(new ArrayList<>());
		assertEquals(Long.MAX_VALUE, groups.getMinVolume());
		assertEquals(Long.MAX_VALUE, groups.getMinArea());
	}
}
