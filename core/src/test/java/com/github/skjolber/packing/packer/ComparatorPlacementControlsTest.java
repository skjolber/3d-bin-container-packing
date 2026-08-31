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
 * Unit tests for {@link ComparatorPlacementControls#getPlacement(int, int)}.
 *
 * <p>Scenarios:
 * <ul>
 *   <li>Fitting box in empty container → placement found</li>
 *   <li>Box larger than container → null</li>
 *   <li>{@link Order#NONE}: iterates all boxes, picks best via comparators</li>
 *   <li>{@link Order#CRONOLOGICAL}: only tries the first box, skips rest</li>
 *   <li>{@link Order#CRONOLOGICAL_ALLOW_SKIPPING}: skips non-fitting, stops after first hit</li>
 * </ul>
 */
class ComparatorPlacementControlsTest {

	// -----------------------------------------------------------------------
	// Fitting box → placement returned
	// -----------------------------------------------------------------------

	/**
	 * A single 5×5×5 box placed in an empty 10×10×10 container is found.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  5  +-----+
	 *  |  |  A  |              ← box placed at (0,0,0)
	 *  0  +-----+----------   ← floor / initial extreme point
	 *     0     5          10 x
	 * </pre>
	 */
	@Test
	void testGetPlacement_singleFittingBox_returnsNonNull() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("A", 5, 5, 5));

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isEqualTo(0);
	}

	// -----------------------------------------------------------------------
	// Box too big → null
	// -----------------------------------------------------------------------

	/**
	 * A 15×15×15 box cannot fit in a 10×10×10 container.
	 *
	 * <pre>
	 *  z
	 *  |
	 * 15  +-----------+
	 *  |  |     B     |        ← box B is bigger than the container
	 *  0  +-----------+
	 *     0           15 x
	 *
	 *     container only goes to x=10 → B does NOT fit → null returned
	 * </pre>
	 */
	@Test
	void testGetPlacement_boxExceedsContainer_returnsNull() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("B", 15, 15, 15));

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNull();
	}

	// -----------------------------------------------------------------------
	// Order.NONE: all boxes tried, best wins
	// -----------------------------------------------------------------------

	/**
	 * With {@link Order#NONE} the comparators are used to pick the globally
	 * best box+placement even when a smaller box appears first in the source.
	 *
	 * <pre>
	 *  Source order: [ S (5×5) ][ L (8×8) ]
	 *
	 *  Footprint comparison (LargestAreaBoxItemComparator):
	 *    S.area = 25 < L.area = 64  →  L is better
	 *
	 *  z
	 *  |
	 *  8  +--------+           ← L placed (preferred)
	 *  5  +-----+
	 *  |  |  S  |  |  |
	 *  0  +-----+--+--------
	 *     0     5  8       10 x
	 *
	 *  Expected: placement is for box "L"
	 * </pre>
	 */
	@Test
	void testGetPlacement_orderNone_picksBestAcrossAllBoxes() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		BoxItem small = boxItem("S", 5, 5, 5);
		BoxItem large = boxItem("L", 8, 8, 5);
		DefaultBoxItemSource src = source(small, large); // small is first

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		// LargestAreaPlacementComparator prefers the 8×8 footprint over 5×5
		assertThat(result.getStackValue().getArea()).isEqualTo(64L); // 8×8
	}

	// -----------------------------------------------------------------------
	// Order.CRONOLOGICAL: only first box tried
	// -----------------------------------------------------------------------

	/**
	 * {@link Order#CRONOLOGICAL} breaks after the <em>first</em> box regardless
	 * of whether it produced a result.  Here the first box is too big, so null
	 * is returned even though a fitting box exists at index 1.
	 *
	 * <pre>
	 *  Source:  [ BIG (15×15×15) ][ OK (5×5×5) ]
	 *
	 *  CRONOLOGICAL: tries BIG → does not fit → break immediately
	 *
	 *  z
	 *  |
	 * 15  ···············  ← BIG exceeds container on all axes
	 *  5  +-----+
	 *  |  |  OK |  ← never tried!
	 *  0  +-----+-------
	 *     0     5     10 x
	 *
	 *  Expected: null
	 * </pre>
	 */
	@Test
	void testGetPlacement_orderCronological_onlyFirstBoxTried_returnsNull() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("BIG", 15, 15, 15), boxItem("OK", 5, 5, 5));

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.CRONOLOGICAL);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNull();
	}

	/**
	 * {@link Order#CRONOLOGICAL} returns a placement for the first box when it
	 * fits, and never inspects the second box.
	 *
	 * <pre>
	 *  Source:  [ S (5×5×5) ][ L (8×8×5) ]
	 *
	 *  CRONOLOGICAL: tries S → fits → break
	 *
	 *  z
	 *  |
	 *  5  +-----+
	 *  |  |  S  |  ← placed (only box tried)
	 *  0  +-----+---------
	 *     0     5        10 x
	 *
	 *  Expected: placement for S (5×5 footprint, area=25), NOT L
	 * </pre>
	 */
	@Test
	void testGetPlacement_orderCronological_firstFits_returnsFirst() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		BoxItem small = boxItem("S", 5, 5, 5);
		BoxItem large = boxItem("L", 8, 8, 5);
		DefaultBoxItemSource src = source(small, large);

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.CRONOLOGICAL);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getStackValue().getArea()).isEqualTo(25L); // 5×5, not 8×8
	}

	// -----------------------------------------------------------------------
	// Order.CRONOLOGICAL_ALLOW_SKIPPING: skips non-fitting, stops at first hit
	// -----------------------------------------------------------------------

	/**
	 * {@link Order#CRONOLOGICAL_ALLOW_SKIPPING} skips boxes that produce no
	 * result and stops as soon as one succeeds.
	 *
	 * <pre>
	 *  Source:  [ BIG (15×15×15) ][ OK (5×5×5) ]
	 *
	 *  BIG → no fit (skipped), OK → fits → break
	 *
	 *  z
	 *  |
	 *  5  +-----+
	 *  |  |  OK |  ← placed after skipping BIG
	 *  0  +-----+-------
	 *     0     5     10 x
	 *
	 *  Expected: non-null (OK was placed)
	 * </pre>
	 */
	@Test
	void testGetPlacement_orderCronologicalAllowSkipping_skipsBig_findsOk() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		DefaultBoxItemSource src = source(boxItem("BIG", 15, 15, 15), boxItem("OK", 5, 5, 5));

		ComparatorPlacementControls ctrl = comparatorControls(
				src, calc, stack, 10, 10, 10, Order.CRONOLOGICAL_ALLOW_SKIPPING);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
	}

	/**
	 * Once a result is found {@link Order#CRONOLOGICAL_ALLOW_SKIPPING} does NOT
	 * try further boxes: a smaller box found second is still ignored.
	 *
	 * <pre>
	 *  Source:  [ S (5×5×5) ][ L (8×8×5) ]   (S first, L second)
	 *
	 *  S fits → result found → break (L is never tried)
	 *
	 *  z
	 *  |
	 *  8  +--------+
	 *  5  +-----+
	 *  |  |  S  |  |  |  ← L would be larger, but never reached
	 *  0  +-----+--+--
	 *     0     5  8  10 x
	 *
	 *  Expected: placement for S (area=25)
	 * </pre>
	 */
	@Test
	void testGetPlacement_orderCronologicalAllowSkipping_stopsAfterFirstSuccess() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();
		BoxItem small = boxItem("S", 5, 5, 5);
		BoxItem large = boxItem("L", 8, 8, 5);
		DefaultBoxItemSource src = source(small, large); // small first

		ComparatorPlacementControls ctrl = comparatorControls(
				src, calc, stack, 10, 10, 10, Order.CRONOLOGICAL_ALLOW_SKIPPING);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getStackValue().getArea()).isEqualTo(25L); // 5×5, stopped before L
	}

	// -----------------------------------------------------------------------
	// Stacked placement (box placed on top of another)
	// -----------------------------------------------------------------------

	/**
	 * After placing a base box, the controls correctly find a placement on
	 * top of it for a second box of the same size.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +-----+   ← TOP placed on extreme point at z=1
	 *  1  +-----+   ← extreme point created after BASE
	 *  0  +-----+   ← BASE placed first
	 *     0     5  10 x
	 * </pre>
	 */
	@Test
	void testGetPlacement_stackedOnBase_returnsPlacementAboveBase() {
		DefaultPointCalculator3D calc = calculator(10, 10, 10);
		Stack stack = new Stack();

		Placement base = placement("BASE", 5, 5, 1, 0, 0, 0);
		place(calc, stack, base);

		BoxItem top = boxItem("TOP", 5, 5, 1);
		DefaultBoxItemSource src = source(top);

		ComparatorPlacementControls ctrl = comparatorControls(src, calc, stack, 10, 10, 10, Order.NONE);
		Placement result = ctrl.getPlacement(0, src.size());

		assertThat(result).isNotNull();
		assertThat(result.getAbsoluteZ()).isGreaterThan(0);
	}
}
