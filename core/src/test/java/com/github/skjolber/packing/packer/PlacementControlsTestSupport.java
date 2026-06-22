package com.github.skjolber.packing.packer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaPlacementComparator;
import com.github.skjolber.packing.comparator.PlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

/**
 * Shared factory helpers for {@link ComparatorPlacementControls},
 * {@link SupportPlacementControls} and {@link FullSupportPlacementControls} tests.
 *
 * <p>All methods are package-private so every test class in this package can
 * access them without public API noise.
 */
class PlacementControlsTestSupport {

	// -----------------------------------------------------------------------
	// Box / BoxItem factories
	// -----------------------------------------------------------------------

	/** Creates a {@link BoxItem} whose box has the given dimensions (no rotation). */
	static BoxItem boxItem(String id, int dx, int dy, int dz) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(0).build();
		return new BoxItem(box); // wires box.setBoxItem(this) internally
	}

	/** Creates a {@link BoxItem} with explicit weight (no rotation). */
	static BoxItem boxItem(String id, int dx, int dy, int dz, int weight) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).build();
		return new BoxItem(box);
	}

	// -----------------------------------------------------------------------
	// BoxItemSource factory
	// -----------------------------------------------------------------------

	/** Wraps the given items in a {@link DefaultBoxItemSource} with correct indices. */
	static DefaultBoxItemSource source(BoxItem... items) {
		return new DefaultBoxItemSource(Arrays.asList(items));
	}

	// -----------------------------------------------------------------------
	// Point calculator factory
	// -----------------------------------------------------------------------

	/**
	 * Creates a {@link DefaultPointCalculator3D} cleared to the given container
	 * dimensions.  After calling this the calculator contains a single origin
	 * point at {@code (0,0,0)→(dx-1, dy-1, dz-1)} with full XY/XZ/YZ plane
	 * support (floor + two walls).
	 */
	static DefaultPointCalculator3D calculator(int dx, int dy, int dz) {
		DefaultPointCalculator3D calc = new DefaultPointCalculator3D(false, 10);
		calc.clearToSize(dx, dy, dz);
		return calc;
	}

	// -----------------------------------------------------------------------
	// Container factory
	// -----------------------------------------------------------------------

	static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(Integer.MAX_VALUE).build();
	}

	// -----------------------------------------------------------------------
	// Stack / placement helpers
	// -----------------------------------------------------------------------

	/**
	 * Places a pre-constructed {@link Placement} at the extreme point whose
	 * origin matches the placement's coordinates, then adds it to the stack.
	 */
	static void place(DefaultPointCalculator3D calc, Stack stack, Placement p) {
		int idx = calc.findPoint(p.getAbsoluteX(), p.getAbsoluteY(), p.getAbsoluteZ());
		calc.add(idx, p);
		stack.add(p);
	}

	/**
	 * Convenience: builds a minimal placement for a box of the given size at
	 * {@code (x, y, z)}.  A {@link BoxItem} is wired to the box so that
	 * {@code placement.getBoxItem()} never returns {@code null}.
	 */
	static Placement placement(String id, int dx, int dy, int dz, int x, int y, int z) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(0).build();
		new BoxItem(box); // wires bidirectional link
		return new Placement(box.getStackValues()[0], 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// Comparator factories (reused across all three control classes)
	// -----------------------------------------------------------------------

	static PlacementComparator placementComparator() {
		return new LargestAreaPlacementComparator();
	}

	static Comparator<BoxItem> boxItemComparator() {
		return LargestAreaBoxItemComparator.getInstance();
	}

	// -----------------------------------------------------------------------
	// Controls factories
	// -----------------------------------------------------------------------

	static ComparatorPlacementControls comparatorControls(
			DefaultBoxItemSource source, DefaultPointCalculator3D calc,
			Stack stack, int dx, int dy, int dz, Order order) {
		PointControls pts = new DefaultPointControls(calc);
		return new ComparatorPlacementControls(
				source, pts, calc, container(dx, dy, dz), stack, order,
				placementComparator(), boxItemComparator());
	}

	static SupportPlacementControls supportControls(
			DefaultBoxItemSource source, DefaultPointCalculator3D calc,
			Stack stack, int dx, int dy, int dz, Order order) {
		PointControls pts = new DefaultPointControls(calc);
		return new SupportPlacementControls(
				source, pts, calc, container(dx, dy, dz), stack, order,
				placementComparator(), boxItemComparator());
	}

	static FullSupportPlacementControls fullSupportControls(
			DefaultBoxItemSource source, DefaultPointCalculator3D calc,
			Stack stack, int dx, int dy, int dz, Order order) {
		PointControls pts = new DefaultPointControls(calc);
		return new FullSupportPlacementControls(
				source, pts, calc, container(dx, dy, dz), stack, order,
				placementComparator(), boxItemComparator());
	}

	// -----------------------------------------------------------------------
	// Testabl inner subclasses (exposed protected methods for unit testing)
	// -----------------------------------------------------------------------

	/**
	 * Exposes {@code createPlacement} from {@link SupportPlacementControls}
	 * so tests can call it directly with hand-crafted points.
	 */
	static final class TestableSupportControls extends SupportPlacementControls {

		TestableSupportControls(Stack stack, int dx, int dy, int dz) {
			super(null, null, null, container(dx, dy, dz), stack, Order.NONE, null, null);
		}

		/** Delegates to the protected {@code createPlacement(Point, BoxStackValue)}. */
		Placement testCreatePlacement(com.github.skjolber.packing.api.point.Point point,
				com.github.skjolber.packing.api.BoxStackValue stackValue) {
			return createPlacement(point, stackValue);
		}
	}

	/**
	 * Exposes {@code getFullySupportedPlacement} from
	 * {@link FullSupportPlacementControls} for direct unit testing of the
	 * fallback path.
	 */
	static final class TestableFullSupportControls extends FullSupportPlacementControls {

		TestableFullSupportControls(DefaultBoxItemSource source,
				DefaultPointCalculator3D calc, Stack stack,
				int dx, int dy, int dz, Order order) {
			super(source, new DefaultPointControls(calc), calc,
					container(dx, dy, dz), stack, order,
					placementComparator(), boxItemComparator());
		}

		List<Placement> testGetFullySupportedPlacementCandidates(int offset, int length) {
			// Call the protected method and collect results by calling it normally
			return java.util.Collections.singletonList(getFullySupportedPlacement(offset, length));
		}
	}
}
