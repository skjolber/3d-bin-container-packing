package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.point.DefaultPointSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

/**
 * The quick rejection in placement searches: a box rotation is skipped only when it is larger
 * than every point (in area or in a dimension), and cached bounds are dropped between searches.
 */
public class PlacementControlsPointBoundsTest {

	private final ComparatorPlacementControls controls = new ComparatorPlacementControls(null, null, null, null, null, Order.NONE, null, null);

	@Test
	public void rejectsOnlyWhenNoPointIsLargeEnough() {
		//  z
		//  |  ┌────────┐ 10x2x2
		//  |  └────────┘
		//  |  ┌──┐ 2x10x2
		//  |  │  │
		//  |  └──┘
		//  └──────────── x
		DefaultPointSource points = new DefaultPointSource(points(point(10, 2, 2), point(2, 10, 2)));
		controls.resetPointBounds();

		// fits the first point
		assertThat(controls.canFitAny(points, stackValue(10, 2, 2))).isTrue();
		// within every maximum dimension, but larger than every point area: rejected
		assertThat(controls.canFitAny(points, stackValue(5, 5, 1))).isFalse();
		// too high
		assertThat(controls.canFitAny(points, stackValue(1, 1, 3))).isFalse();
		// within the bounds, yet fits no point: not rejected, the points are checked one by one
		assertThat(controls.canFitAny(points, stackValue(4, 4, 1))).isTrue();
	}

	@Test
	public void resetDropsBoundsOfChangedPoints() {
		List<Point> values = points(point(10, 10, 10));
		DefaultPointSource points = new DefaultPointSource(values);
		controls.resetPointBounds();
		assertThat(controls.canFitAny(points, stackValue(10, 10, 10))).isTrue();

		// same source, other contents (as after a placement)
		values.set(0, point(2, 2, 2));
		controls.resetPointBounds();

		assertThat(controls.canFitAny(points, stackValue(10, 10, 10))).isFalse();
	}

	private static BoxStackValue stackValue(int dx, int dy, int dz) {
		return Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build().getStackValues()[0];
	}

	private static DefaultPoint3D point(int dx, int dy, int dz) {
		return new DefaultPoint3D(0, 0, 0, dx - 1, dy - 1, dz - 1);
	}

	private static List<Point> points(Point... points) {
		List<Point> list = new ArrayList<>();
		for(Point point : points) {
			list.add(point);
		}
		return list;
	}
}
