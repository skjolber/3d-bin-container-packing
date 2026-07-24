package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.packer.PlacementControlsTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.DefaultXYPlanePoint3D;

/**
 * Unit tests for {@link SupportPlacementControls}.
 *
 * <p>Focuses on the {@code supportedArea} value written by
 * {@code createPlacement(Point, BoxStackValue)}.  Three regimes are covered:
 * <ul>
 *   <li>Floor placement ({@code point.minZ == 0}) → full support</li>
 *   <li>XY-plane-supported point (box below fully covers footprint) → full support</li>
 *   <li>Partial support from below → {@code supportedArea < stackValue.getArea()}</li>
 * </ul>
 *
 * <p>The {@code getPlacement} ordering behaviours (NONE / CRONOLOGICAL / …) are
 * shared with {@link ComparatorPlacementControls} and are tested there; only
 * support-area semantics are verified here.
 */
class SupportPlacementControlsTest {

	// -----------------------------------------------------------------------
	// createPlacement – direct unit tests via TestableSupportControls
	// -----------------------------------------------------------------------

	/**
	 * A box placed on the floor ({@code point.minZ == 0}) always receives full
	 * supported area regardless of what is in the stack.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  5  +----------+
	 *  |  |    A     |   ← box placed here (dx=10, dy=10)
	 *  0  +----------+   ← point at z=0 (floor)
	 *     0          10 x
	 *
	 *  supportedArea = 10×10 = 100  (full)
	 * </pre>
	 */
	@Test
	void testCreatePlacement_onFloor_fullSupportedArea() {
		Stack stack = new Stack();
		PlacementControlsTestSupport.TestableSupportControls ctrl =
				new PlacementControlsTestSupport.TestableSupportControls(stack, 10, 10, 10);

		Box box = Box.newBuilder().withId("A").withSize(10, 10, 5).withWeight(0).build();
		new BoxItem(box);
		BoxStackValue sv = box.getStackValues()[0];

		// floor point: minZ == 0, no XY plane support needed
		DefaultPoint3D floorPoint = new DefaultPoint3D(0, 0, 0, 9, 9, 9);
		floorPoint.setIndex(0);

		Placement p = ctrl.testCreatePlacement(floorPoint, sv);

		assertThat(p.getSupportedArea()).isEqualTo(sv.getArea()); // 10×10 = 100
	}

	/**
	 * A box placed on top of another box that exactly matches its footprint
	 * (XY-plane-supported point) receives full supported area.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  6  +----------+   ← B placed on top of A
	 *  |  |    B     |   dx=10, dy=10
	 *  1  +----------+   ← XY-plane-supported point (from A below)
	 *  0  +----------+   ← A in stack (10×10×1)
	 *     0          10 x
	 *
	 *  A.endX=9 ≥ B.minX+dx-1=9  AND  A.endY=9 ≥ B.minY+dy-1=9
	 *  → isSupportedXYPlane(stackValue) == true
	 *  → supportedArea = 10×10 = 100 (full)
	 * </pre>
	 */
	@Test
	void testCreatePlacement_xyPlaneSupportedPoint_fullSupportedArea() {
		Stack stack = new Stack();
		Placement base = placement("A", 10, 10, 1, 0, 0, 0);
		stack.add(base);

		PlacementControlsTestSupport.TestableSupportControls ctrl =
				new PlacementControlsTestSupport.TestableSupportControls(stack, 10, 10, 10);

		Box box = Box.newBuilder().withId("B").withSize(10, 10, 5).withWeight(0).build();
		new BoxItem(box);
		BoxStackValue sv = box.getStackValues()[0];

		// XY-plane-supported point at z=1: xyPlane placement covers x=0..9, y=0..9
		DefaultXYPlanePoint3D xyPoint = new DefaultXYPlanePoint3D(0, 0, 1, 9, 9, 9, base);
		xyPoint.setIndex(0);

		Placement result = ctrl.testCreatePlacement(xyPoint, sv);

		assertThat(result.getSupportedArea()).isEqualTo(sv.getArea()); // 10×10 = 100
	}

	/**
	 * When the box extends beyond the supporting box below, {@code supportedArea}
	 * reflects only the overlapping region.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← B (10×10) extends into unsupported region
	 *  |  |  A  | ?? |   A covers x=0..4 (5 wide), B needs x=0..9 (10 wide)
	 *  1  +----·+----·   ← point at z=1, NO XY-plane support for B
	 *  0  +----+         ← A (5×10×1) in stack
	 *     0    5    10 x
	 *
	 *  calculateAreaSupport: overlap of A with B footprint at z=1
	 *    A.endZ=0 == z-1=0  ✓
	 *    overlap x: max(0,0)..min(4,9)=4  →  5 units
	 *    overlap y: max(0,0)..min(9,9)=9  →  10 units
	 *    supportedArea = 5×10 = 50  (< full 100)
	 * </pre>
	 */
	@Test
	void testCreatePlacement_partialSupportFromBelow_partialSupportedArea() {
		Stack stack = new Stack();
		// A covers only the left half of B's footprint
		Placement a = placement("A", 5, 10, 1, 0, 0, 0);
		stack.add(a);

		PlacementControlsTestSupport.TestableSupportControls ctrl =
				new PlacementControlsTestSupport.TestableSupportControls(stack, 10, 10, 10);

		Box box = Box.newBuilder().withId("B").withSize(10, 10, 1).withWeight(0).build();
		new BoxItem(box);
		BoxStackValue sv = box.getStackValues()[0];

		// Plain point at z=1 – no XY-plane support flag → falls through to calculateAreaSupport
		DefaultPoint3D midAirPoint = new DefaultPoint3D(0, 0, 1, 9, 9, 9);
		midAirPoint.setIndex(0);

		Placement result = ctrl.testCreatePlacement(midAirPoint, sv);

		long fullArea = sv.getArea(); // 10×10 = 100
		assertThat(result.getSupportedArea())
				.isLessThan(fullArea)
				.isEqualTo(50L); // only A's 5×10 overlap counted
	}

	// -----------------------------------------------------------------------
	// getPlacement integration – verify supportedArea through full call path
	// -----------------------------------------------------------------------

	/**
	 * End-to-end check: a box placed on the floor via {@code getPlacement}
	 * gets full supported area.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  5  +-----+
	 *  |  |  A  |   ← A (5×5×5) placed
	 *  0  +-----+---------   ← initial point (floor, XY plane support)
	 *     0     5        10 x
	 *
	 *  supportedArea = 5×5 = 25  (full)
	 * </pre>
	 */
	@Test
	void testGetPlacement_floorBox_reportFullSupportedArea() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		BoxItem item = boxItem("A", 5, 5, 5);
		DefaultBoxItemSource src = source(item);

		SupportPlacementControls ctrl = supportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		long expectedArea = result.getStackValue().getArea();
		assertThat(result.getSupportedArea()).isEqualTo(expectedArea);
	}

	/**
	 * A box placed on top of a same-footprint base via {@code getPlacement}
	 * gets full supported area.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +-----+   ← TOP (5×5×1) on extreme point at z=1
	 *  1  -------   ← extreme point with XY-plane support from BASE
	 *  0  +-----+   ← BASE (5×5×1) already in stack
	 *     0     5  10 x
	 *
	 *  supportedArea of TOP = 5×5 = 25  (full)
	 * </pre>
	 */
	@Test
	void testGetPlacement_stackedOnSameFootprint_fullSupportedArea() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();

		Placement base = placement("BASE", 5, 5, 1, 0, 0, 0);
		place(calc, stack, base);

		BoxItem top = boxItem("TOP", 5, 5, 1);
		DefaultBoxItemSource src = source(top);

		SupportPlacementControls ctrl = supportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isGreaterThan(0); // stacked, not on floor
		assertThat(result.getSupportedArea()).isEqualTo(result.getStackValue().getArea());
	}

	/**
	 * A wide box placed on a narrow base via {@code getPlacement} gets a
	 * supported area smaller than its full footprint.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← WIDE (10×5×1) – extends past NARROW
	 *  1  +----·-----·   ← extreme point at z=1 (XY support only covers x=0..4)
	 *  0  +----+         ← NARROW (5×5×1) in stack
	 *     0    5    10 x
	 *
	 *  WIDE.area       = 10×5 = 50
	 *  supportedArea   = 5×5  = 25   (only NARROW's 5-wide strip supports WIDE)
	 * </pre>
	 */
	@Test
	void testGetPlacement_wideBoxOnNarrowBase_partialSupportedArea() {
		DefaultPointCalculator3D calc = calculator(10, 5, 10);
		Stack stack = new Stack();

		// NARROW covers only x=0..4 in a 10-wide container
		Placement narrow = placement("NARROW", 5, 5, 1, 0, 0, 0);
		place(calc, stack, narrow);

		// WIDE spans the full 10 units in x; it must be placed at z=1
		BoxItem wide = boxItem("WIDE", 10, 5, 1);
		DefaultBoxItemSource src = source(wide);

		SupportPlacementControls ctrl = supportControls(src, calc, stack, 10, 5, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isGreaterThan(0);
		assertThat(result.getSupportedArea())
				.isLessThan(result.getStackValue().getArea()); // partial
	}
}
