package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.point.DefaultPointSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

/**
 * The fallback of {@link FullSupportPlacementControls}: when a box is not fully supported at the
 * origin of a point, inner positions on top of a single placement below are tried.
 */
public class FullSupportFallbackTest {

	// prefers lower x
	private static final PlacementComparator LOWER_X = (a, b) -> Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());

	/**
	 * <pre>
	 *  x: 0    6         15    19
	 *     point (0..19) at z=2
	 *          | F, top at z=1 |
	 *          | box at 6..9   |
	 * </pre>
	 */
	@Test
	void placesBoxOnInnerSupporter() {
		Placement result = fallback(placement(10, 10, 2, 6, 6), point(0, 0, 2, 19, 19), box(4, 4));

		StackPlacementAssert.assertThat(result).isAt(6, 6, 2).hasSupportedArea(16);
	}

	@Test
	void rejectsPartialSupport() {
		assertThat(fallback(placement(3, 3, 2, 6, 6), point(0, 0, 2, 19, 19), box(4, 4))).isNull();
	}

	/** Boundary: the box ends exactly at the point's end (x). The point is too shallow for the box rotated. */
	@Test
	void placesBoxEndingAtPointEndInX() {
		Placement result = fallback(placement(8, 10, 2, 2, 0), point(0, 0, 2, 9, 5), box(8, 4));

		StackPlacementAssert.assertThat(result).isAt(2, 0, 2);
	}

	/** Boundary: the box ends exactly at the point's end (y). The point is too narrow for the box rotated. */
	@Test
	void placesBoxEndingAtPointEndInY() {
		Placement result = fallback(placement(10, 8, 2, 0, 2), point(0, 0, 2, 5, 9), box(4, 8));

		StackPlacementAssert.assertThat(result).isAt(0, 2, 2);
	}

	/** Beyond the boundary: the box would extend past the point's end. */
	@Test
	void rejectsBoxBeyondPointEnd() {
		assertThat(fallback(placement(8, 10, 2, 3, 0), point(0, 0, 2, 9, 5), box(8, 4))).isNull();
	}

	/** Boundary: a box resting on a floor box of height 1 (the point is at z=1). */
	@Test
	void placesBoxOnSupporterOfHeightOne() {
		Placement result = fallback(placement(10, 10, 1, 6, 6), point(0, 0, 1, 19, 19), box(4, 4));

		StackPlacementAssert.assertThat(result).isAt(6, 6, 1);
	}

	private static Placement fallback(Placement below, DefaultPoint3D point, Box box) {
		Stack stack = new Stack();
		stack.add(below);
		List<Point> points = new ArrayList<>();
		points.add(point);
		DefaultBoxItemSource items = new DefaultBoxItemSource(AbstractPackagerSession.toRemainingBoxItems(List.of(new BoxItem(box))));
		TestControls controls = new TestControls(items, new DefaultPointControls(new DefaultPointSource(points)), stack);
		return controls.fallback();
	}

	private static class TestControls extends FullSupportPlacementControls {
		TestControls(DefaultBoxItemSource items, DefaultPointControls points, Stack stack) {
			super(items, points, null, null, stack, Order.NONE, LOWER_X, (a, b) -> 0);
		}

		Placement fallback() {
			return getFullySupportedPlacement(0, boxItems.size());
		}
	}

	private static DefaultPoint3D point(int minX, int minY, int minZ, int maxX, int maxY) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, 19);
	}

	private static Box box(int dx, int dy) {
		return Box.newBuilder().withSize(dx, dy, 1).withWeight(1).build();
	}

	private static Placement placement(int dx, int dy, int dz, int x, int y) {
		Box box = Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build();
		return new Placement(box.getStackValue(0), 0, x, y, 0);
	}
}
