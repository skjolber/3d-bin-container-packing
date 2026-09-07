package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.collections.api.iterator.IntIterator;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

class BruteForcePackagerBoundsTest {

	@Test
	void boundsCurrentPermutationByVolumeAndWeight() {
		BoxItem first = item("first", 2, 2, 1, 2);
		BoxItem second = item("second", 3, 1, 1, 2);
		BoxItem third = item("third", 2, 1, 1, 1);
		BoxItemPermutationRotationIterator iterator = iterator(List.of(first, second, third));

		assertThat(AbstractBruteForcePackager.getMaxPackableCount(iterator, 6L, 100L)).isEqualTo(1);
		assertThat(AbstractBruteForcePackager.getMaxPackableCount(iterator, 100L, 3L)).isEqualTo(1);
		assertThat(AbstractBruteForcePackager.getMaxPackableCount(iterator, 9L, 5L)).isEqualTo(3);

		assertThat(iterator.nextPermutation()).isNotEqualTo(-1);
		assertThat(AbstractBruteForcePackager.getMaxPackableCount(iterator, 6L, 100L)).isEqualTo(2);
	}

	@Test
	void stopsPointSearchAtCapacityBound() throws Exception {
		Box unit = Box.newBuilder().withId("unit").withSize(1, 1, 1).withWeight(1).build();
		BoxItemPermutationRotationIterator iterator = iterator(List.of(new BoxItem(unit, 3)));
		Container container = Container.newBuilder().withSize(2, 1, 1).withMaxLoadWeight(100).build();
		PointCalculator3DStack pointCalculator = new PointCalculator3DStack(4);
		List<Placement> placements = AbstractBruteForcePackager.getPlacements(3);
		CountingPointFilter pointFilter = new CountingPointFilter();

		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			List<Point> result = packager.packStackPlacement(pointCalculator, placements, iterator, new Stack(), container,
					() -> false, iterator.getMinStackableAreaIndex(0), null, null, pointFilter);

			assertThat(result).hasSize(2);
			assertThat(pointFilter.calls).isEqualTo(2);
		}
	}

	@Test
	void retainsTheLastPlacementAtAnExactWeightBound() throws Exception {
		Box unit = Box.newBuilder().withId("unit").withSize(1, 1, 1).withWeight(1).build();
		BoxItemPermutationRotationIterator iterator = iterator(List.of(new BoxItem(unit, 3)));
		Container container = Container.newBuilder().withSize(3, 1, 1).withMaxLoadWeight(2).build();

		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			List<Point> result = packager.packStackPlacement(new PointCalculator3DStack(4),
					AbstractBruteForcePackager.getPlacements(3), iterator, new Stack(), container,
					() -> false, iterator.getMinStackableAreaIndex(0), null, null, null);

			assertThat(result).hasSize(2);
		}
	}

	private static BoxItem item(String id, int dx, int dy, int dz, int weight) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).build(), 1);
	}

	private static BoxItemPermutationRotationIterator iterator(List<BoxItem> items) {
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(10, 10, 10)
				.withMaxLoadWeight(100)
				.withBoxItems(items)
				.build();
	}

	private static class CountingPointFilter extends BruteForcePackager.DefaultPointFilter {

		private int calls;

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, com.github.skjolber.packing.api.BoxStackValue stackValue) {
			calls++;
			return super.getPoints(pointCalculator, stackValue);
		}
	}
}
