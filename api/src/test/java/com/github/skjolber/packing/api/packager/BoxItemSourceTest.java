package com.github.skjolber.packing.api.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * Tests of the default methods of {@link BoxItemSource} and {@link BoxItemGroupSource}.
 */
class BoxItemSourceTest {

	@Test
	void emptySourceMinimumsAreLargerThanAnyBox() {
		DefaultBoxItemSource source = new DefaultBoxItemSource();
		assertEquals(Long.MAX_VALUE, source.getMinVolume());
		assertEquals(Long.MAX_VALUE, source.getMinArea());

		ListBoxItemSource empty = new ListBoxItemSource(new ArrayList<>());
		assertEquals(Long.MAX_VALUE, empty.getMinVolume());
		assertEquals(Long.MAX_VALUE, empty.getMinArea());
	}

	private static class ListBoxItemSource implements BoxItemSource {

		private final List<BoxItem> items;

		ListBoxItemSource(List<BoxItem> items) {
			this.items = items;
		}

		@Override
		public int size() {
			return items.size();
		}

		@Override
		public boolean isEmpty() {
			return items.isEmpty();
		}

		@Override
		public BoxItem get(int localIndex) {
			return items.get(localIndex);
		}

		@Override
		public boolean decrement(int localIndex, int count) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BoxItem remove(int localIndex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BoxItemGroupSource getGroups() {
			return null;
		}

		@Override
		public Iterator<BoxItem> iterator() {
			return items.iterator();
		}
	}

	private static class ListBoxItemGroupSource implements BoxItemGroupSource {

		private final List<BoxItemGroup> groups;

		ListBoxItemGroupSource(List<BoxItemGroup> groups) {
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
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<BoxItemGroup> iterator() {
			return groups.iterator();
		}
	}

	private static BoxItem item(int dx, int dy, int dz) {
		return new BoxItem(Box.newBuilder().withSize(dx, dy, dz).withRotate3D().withWeight(1).build());
	}

	private static List<BoxItem> items(BoxItem... items) {
		List<BoxItem> list = new ArrayList<>();
		for (BoxItem item : items) {
			list.add(item);
		}
		return list;
	}

	/**
	 * The 1x10x100 box has a minimum footprint of 10 and a maximum footprint of 1000; the 2x3x4 box has a minimum footprint of 6 and a maximum of 12.
	 */
	@Test
	void maxAreaIsTheLargestMinimumAreaOfTheBoxes() {
		BoxItem slender = item(1, 10, 100);
		BoxItem small = item(2, 3, 4);

		assertEquals(10L, slender.getBox().getMinimumArea());
		assertEquals(1000L, slender.getBox().getMaximumArea());

		assertEquals(10L, new ListBoxItemSource(items(slender)).getMaxArea());
		assertEquals(10L, new ListBoxItemSource(items(slender, small)).getMaxArea());
		assertEquals(10L, new ListBoxItemSource(items(small, slender)).getMaxArea());
	}

	@Test
	void minVolumeBeyondIntRange() {
		BoxItemSource source = new ListBoxItemSource(items(item(3000, 3000, 3000), item(2000, 2000, 2000)));

		assertEquals(8_000_000_000L, source.getMinVolume());
	}

	@Test
	void minAreaBeyondIntRange() {
		BoxItemSource source = new ListBoxItemSource(items(item(60000, 60000, 60000), item(50000, 50000, 50000)));

		assertEquals(2_500_000_000L, source.getMinArea());
	}

	@Test
	void groupMinVolumeBeyondIntRange() {
		BoxItemGroupSource source = new ListBoxItemGroupSource(List.of(
				new BoxItemGroup("a", items(item(3000, 3000, 3000), item(60000, 60000, 60000))),
				new BoxItemGroup("b", items(item(2000, 2000, 2000), item(50000, 50000, 50000)))));

		assertEquals(8_000_000_000L, source.getMinVolume());
	}

	@Test
	void groupMinAreaBeyondIntRange() {
		BoxItemGroupSource source = new ListBoxItemGroupSource(List.of(
				new BoxItemGroup("a", items(item(60000, 60000, 60000))),
				new BoxItemGroup("b", items(item(50000, 50000, 50000)))));

		assertEquals(2_500_000_000L, source.getMinArea());
	}

	@Test
	void defaultSourceMinimumsBeyondIntRange() {
		DefaultBoxItemSource source = new DefaultBoxItemSource(items(item(60000, 60000, 60000), item(50000, 50000, 50000)));

		assertEquals(2_500_000_000L, source.getMinArea());
		assertEquals(125_000_000_000_000L, source.getMinVolume());
	}
}
