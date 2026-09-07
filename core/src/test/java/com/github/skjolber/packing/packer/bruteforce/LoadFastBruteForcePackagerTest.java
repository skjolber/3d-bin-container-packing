package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

class LoadFastBruteForcePackagerTest extends AbstractLoadBruteForcePackagerTest {

	@Test
	void validatesOnlyPointsWhichCanImproveTheCurrentBest() {
		LoadFastBruteForcePackager packager = createPackager();
		try {
			Container container = Container.newBuilder().withSize(10, 10, 10).withMaxLoadWeight(100).build();
			Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
			BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
					.withLoadSize(10, 10, 10)
					.withMaxLoadWeight(100)
					.withBoxItems(List.of(new BoxItem(box)))
					.build();
			FastPointCalculator3DStack pointCalculator = new FastPointCalculator3DStack(2);
			pointCalculator.clearToSize(10, 10, 10);
			pointCalculator.setPoints(List.of(
					new DefaultPoint3D(0, 0, 0, 9, 9, 9),
					new DefaultPoint3D(1, 0, 0, 9, 9, 9)));
			pointCalculator.clear();
			CountingLoadPlacementUtility utility = new CountingLoadPlacementUtility();

			int count = packager.packStackPlacement(pointCalculator, new ArrayList<>(List.of(new Placement())),
					iterator, new Stack(), container, 0, () -> false, 0, 100, utility,
					(stackValue, bestPoint, candidatePoint) -> 0);

			assertThat(count).isEqualTo(1);
			// The selected point is validated once while ranking and once when the
			// utility cache is primed for adding its load. The inferior point is skipped.
			assertThat(utility.validationCount).isEqualTo(2);
		} finally {
			packager.close();
		}
	}

	@Override
	protected LoadFastBruteForcePackager createPackager() {
		return LoadFastBruteForcePackager.newBuilder().build();
	}

	private static class CountingLoadPlacementUtility implements LoadPlacementUtility {

		private int validationCount;

		@Override
		public void initialize(int count) {
		}

		@Override
		public void populatePointSupporters(Point point) {
		}

		@Override
		public void populatePointSupportees(Point point, int minDz, int maxDz) {
		}

		@Override
		public Placement getPlacementAtPoint(Point point, BoxStackValue stackValue, boolean fullSupport) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getSupportedAreaAtPoint(Point point, BoxStackValue stackValue, boolean fullSupport) {
			validationCount++;
			return stackValue.getArea();
		}

		@Override
		public void addSupportersLoad(Placement placement) {
		}

		@Override
		public Placement findPlacementAtPointSupporters(Point point, BoxStackValue stackValue, PlacementComparator comparator) {
			throw new UnsupportedOperationException();
		}
	}
}
