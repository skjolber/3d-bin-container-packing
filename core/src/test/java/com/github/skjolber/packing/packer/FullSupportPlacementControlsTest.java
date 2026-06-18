package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.packer.PlacementControlsTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

/**
 * Unit tests for {@link FullSupportPlacementControls}.
 *
 * <p>{@code FullSupportPlacementControls} requires every new box to be fully
 * supported from below before it is accepted.  It runs in two passes:
 * <ol>
 *   <li><b>First pass</b> – accepts a point when
 *       {@code minZ == 0} (floor), the point carries XY-plane support that
 *       covers the entire footprint, or {@code isFullSupport(…)} is true
 *       (the placements collectively cover 100 % of the footprint).</li>
 *   <li><b>Fallback pass</b> ({@code getFullySupportedPlacement}) – searches
 *       for shifted positions within existing extreme points where the footprint
 *       can land completely on top of a single underlying placement.</li>
 * </ol>
 *
 * <p>Contrast with {@link SupportPlacementControls}, which always places and
 * merely <em>reports</em> the supported area; and with
 * {@link ComparatorPlacementControls}, which ignores support entirely.
 */
class FullSupportPlacementControlsTest {

	// -----------------------------------------------------------------------
	// Floor placement: always accepted (minZ == 0)
	// -----------------------------------------------------------------------

	/**
	 * Any box can be placed on the container floor (initial extreme point has
	 * {@code minZ == 0}).  Full support is assumed for ground-level placements.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  5  +-----+     ← A (5×5×5) placed at origin
	 *  |  |  A  |
	 *  0  +-----+-------   ← initial extreme point, minZ=0 → always ok
	 *     0     5      10 x
	 *
	 *  Expected: non-null placement on the floor
	 * </pre>
	 */
	@Test
	void testGetPlacement_floorBox_alwaysAccepted() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("A", 5, 5, 5));

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isEqualTo(0);
		assertThat(result.getSupportedArea()).isEqualTo(result.getStackValue().getArea()); // full
	}

	// -----------------------------------------------------------------------
	// Stacked on same-size base: isFullSupport == true → accepted in first pass
	// -----------------------------------------------------------------------

	/**
	 * A box placed exactly on top of a same-footprint base is fully supported
	 * by the XY-plane support at the extreme point above the base.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +-----+   ← TOP (5×5×1) placed at z=1
	 *  |  |TOP  |
	 *  1  +-----+   ← extreme point with XY-plane support from BASE
	 *  0  +-----+   ← BASE (5×5×1) in stack
	 *     0     5  10 x
	 *
	 *  XY-plane support covers x=0..4, y=0..4 == TOP's footprint
	 *  → isSupportedXYPlane(TOP) == true → first pass accepts
	 *  → supportedArea = 5×5 = 25 (full)
	 * </pre>
	 */
	@Test
	void testGetPlacement_stackedOnSameFootprint_acceptedWithFullSupport() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();

		Placement base = placement("BASE", 5, 5, 1, 0, 0, 0);
		place(calc, stack, base);

		BoxItem top = boxItem("TOP", 5, 5, 1);
		DefaultBoxItemSource src = source(top);

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isGreaterThan(0);
		assertThat(result.getSupportedArea()).isEqualTo(result.getStackValue().getArea());
	}

	// -----------------------------------------------------------------------
	// isFullSupport via multiple supporting boxes
	// -----------------------------------------------------------------------

	/**
	 * Two boxes that together cover the entire footprint of a new box satisfy
	 * {@code isFullSupport} in the first pass.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← WIDE (10×5×1) at z=1
	 *  |  | L  |  R  |
	 *  1  +----+----+·   ← extreme point at z=1 (above both bases)
	 *  0  +----++----+   ← LEFT (5×5×1) and RIGHT (5×5×1), side by side
	 *     0    5    10 x
	 *
	 *  LEFT.area + RIGHT.area overlapping WIDE = 5×5 + 5×5 = 50 == WIDE.area
	 *  → isFullSupport == true → first pass accepts
	 * </pre>
	 */
	@Test
	void testGetPlacement_twoBaseBoxesFullyCoveringWide_acceptedInFirstPass() {
		DefaultPointCalculator3D calc = calculator(10, 5, 10);
		Stack stack = new Stack();

		Placement left  = placement("LEFT",  5, 5, 1, 0, 0, 0);
		Placement right = placement("RIGHT", 5, 5, 1, 5, 0, 0);
		place(calc, stack, left);
		place(calc, stack, right);

		BoxItem wide = boxItem("WIDE", 10, 5, 1);
		DefaultBoxItemSource src = source(wide);

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 5, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isGreaterThan(0);
		assertThat(result.getSupportedArea()).isEqualTo(result.getStackValue().getArea());
	}

	// -----------------------------------------------------------------------
	// Unsupported box: both passes fail → null
	// -----------------------------------------------------------------------

	/**
	 * When a box cannot be fully supported anywhere in the container both the
	 * first pass and the fallback return {@code null}.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  ·· [WIDE?] ··   ← WIDE (10×10) needs z=1, but only half the floor
	 *  |                      is covered by NARROW (5×10)
	 *  1  +----+·····+    ← extreme point at z=1; XY support covers x=0..4 only
	 *  0  +----+          ← NARROW (5×10×1) in stack
	 *     0    5    10 x
	 *
	 *  First pass at point (0,0,1):
	 *    minZ=1 ≠ 0
	 *    isSupportedXYPlane(WIDE): xyPlane.endX=4 ≥ 9? NO → false
	 *    isFullSupport: overlap = 5×10=50, need 10×10=100 → false
	 *  Fallback: NARROW.endX=4 < minMaxX=10 → no candidate qualifies → null
	 *
	 *  Expected: null
	 * </pre>
	 */
	@Test
	void testGetPlacement_halfCoveredBase_wideBoxRejected() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();

		// NARROW covers only the left half of the 10-wide container
		Placement narrow = placement("NARROW", 5, 10, 1, 0, 0, 0);
		place(calc, stack, narrow);

		BoxItem wide = boxItem("WIDE", 10, 10, 1);
		DefaultBoxItemSource src = source(wide);

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNull();
	}

	// -----------------------------------------------------------------------
	// Contrast with SupportPlacementControls for same scenario
	// -----------------------------------------------------------------------

	/**
	 * The same half-covered scenario that rejects the box in
	 * {@link FullSupportPlacementControls} is accepted by
	 * {@link SupportPlacementControls} (which places with partial support).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← SupportPlacementControls: WIDE placed (partial ok)
	 *  |  | supp| ?? |
	 *  1  +----·+----·   ← point at z=1
	 *  0  +----+         ← NARROW (5×10×1)
	 *     0    5    10 x
	 *
	 *  SupportPlacementControls  → non-null, supportedArea = 50 (partial)
	 *  FullSupportPlacementControls → null  (rejects partial support)
	 * </pre>
	 */
	@Test
	void testGetPlacement_halfCovered_supportControlsAccepts_fullSupportRejects() {
		// --- SupportPlacementControls setup ---
		DefaultPointCalculator3D calcS = calculator(10, 10, 10);
		Stack stackS = new Stack();
		Placement narrowS = placement("NARROW", 5, 10, 1, 0, 0, 0);
		place(calcS, stackS, narrowS);
		BoxItem wideS = boxItem("WIDE", 10, 10, 1);
		DefaultBoxItemSource srcS = source(wideS);

		SupportPlacementControls support = supportControls(srcS, calcS, stackS, 10, 10, 10, Order.NONE);
		Placement supportResult = support.getPlacement(0, srcS.size());

		// --- FullSupportPlacementControls setup (identical geometry) ---
		DefaultPointCalculator3D calcF = calculator(10, 10, 10);
		Stack stackF = new Stack();
		Placement narrowF = placement("NARROW", 5, 10, 1, 0, 0, 0);
		place(calcF, stackF, narrowF);
		BoxItem wideF = boxItem("WIDE", 10, 10, 1);
		DefaultBoxItemSource srcF = source(wideF);

		FullSupportPlacementControls full = fullSupportControls(srcF, calcF, stackF, 10, 10, 10, Order.NONE);
		Placement fullResult = full.getPlacement(0, srcF.size());

		assertThat(supportResult).isNotNull();
		assertThat(supportResult.getSupportedArea()).isLessThan(supportResult.getStackValue().getArea());

		assertThat(fullResult).isNull();
	}

	// -----------------------------------------------------------------------
	// Fallback: getFullySupportedPlacement finds a shifted position
	// -----------------------------------------------------------------------

	/**
	 * When the initial extreme-point position has no full support but a shifted
	 * position within the same point's bounds does, the fallback places the box.
	 *
	 * <pre>
	 *  Container: 10×10×10
	 *  BASE (10×10×5) covers the full floor → placed at (0,0,0)
	 *  After placing BASE the extreme point at (0,0,5) has XY-plane support
	 *  from BASE for any footprint ≤ 10×10.
	 *
	 *  z
	 *  |
	 * 10  ··············
	 *  6  +----------+   ← TOP (10×10×1) placed via first pass
	 *  5  +----------+   ← extreme point with XY-plane support from BASE
	 *  0  +----------+   ← BASE (10×10×5) in stack
	 *     0          10 x
	 *
	 *  First pass: isSupportedXYPlane(TOP) true (BASE covers 10×10 == TOP)
	 *  → TOP is accepted with full supportedArea
	 * </pre>
	 */
	@Test
	void testGetPlacement_fallback_shiftedPositionOnBase_accepted() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();

		// BASE covers the entire floor of the container
		Placement base = placement("BASE", 10, 10, 5, 0, 0, 0);
		place(calc, stack, base);

		BoxItem top = boxItem("TOP", 10, 10, 1);
		DefaultBoxItemSource src = source(top);

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isEqualTo(5);
		assertThat(result.getSupportedArea()).isEqualTo(result.getStackValue().getArea());
	}

	// -----------------------------------------------------------------------
	// Box exceeding container → null (no point fits)
	// -----------------------------------------------------------------------

	/**
	 * A box larger than the container cannot be placed by either pass.
	 *
	 * <pre>
	 *  z
	 *  |
	 * 15  ···············  ← BIG exceeds all container dimensions
	 *  |
	 *  0  +·············   ← container only goes to (9,9,9)
	 *     0             10 x
	 *
	 *  Expected: null
	 * </pre>
	 */
	@Test
	void testGetPlacement_boxLargerThanContainer_returnsNull() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("BIG", 15, 15, 15));

		FullSupportPlacementControls ctrl = fullSupportControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNull();
	}
}
