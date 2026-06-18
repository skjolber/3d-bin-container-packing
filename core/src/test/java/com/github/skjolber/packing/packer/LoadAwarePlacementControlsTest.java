package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;

/**
 * Unit tests for the utility methods that previously lived in
 * {@code LoadAwarePlacementControls} (now deleted).
 *
 * <p>Static helpers ({@code canStackLevels}, {@code canStackOneMore},
 * {@code isWithinMaxLoadBoxCount}) were promoted to
 * {@link AbstractPlacementControls} and are tested here.
 *
 * <p>Tests that relied on instance methods ({@code findSupporters},
 * {@code findSupportees}, {@code applyLoad}) which were removed in the
 * refactoring are annotated {@link Disabled} and kept for reference.
 */
class LoadAwarePlacementControlsTest {

	// -----------------------------------------------------------------------
	// helper factories
	// -----------------------------------------------------------------------

	/**
	 * Creates a Placement whose BoxStackValue has the given size and weight.
	 * The boxStackValue carries no load constraints unless added via
	 * {@code withMaxLoadBoxCount} / {@code withMaxLoadWeight}.
	 */
	private static Placement placement(String id, int dx, int dy, int dz, int weight,
			int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight).withRotate2D()
				.build();
		BoxStackValue sv = box.getStackValues()[0];
		return new Placement(sv, 0, x, y, z);
	}

	private static Placement placementWithCount(String id, int dx, int dy, int dz,
			int weight, int maxLoadBoxCount, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadBoxCount(maxLoadBoxCount)
				.withRotate2D()
				.build();
		BoxStackValue sv = box.getStackValues()[0];
		return new Placement(sv, 0, x, y, z);
	}

	private static Placement placementWithWeight(String id, int dx, int dy, int dz,
			int weight, long maxLoadWeight, int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id).withSize(dx, dy, dz).withWeight(weight)
				.withMaxLoadWeight(maxLoadWeight)
				.withRotate2D()
				.build();
		BoxStackValue sv = box.getStackValues()[0];
		return new Placement(sv, 0, x, y, z);
	}

	/** Minimal subclass that exposes {@code overlapArea} for testing. */
	private static class TestableControls extends WeightLoadAwarePlacementControls {
		TestableControls(Stack stack) {
			super(new DefaultBoxItemSource(java.util.List.of()), null, null, null, stack,
					Order.NONE, null, null, false);
		}

		long testOverlapArea(int minX, int minY, int maxX, int maxY, Placement p) {
			return overlapArea(minX, minY, maxX, maxY, p);
		}
	}

	// -----------------------------------------------------------------------
	// canStackLevels / canStackOneMore
	// -----------------------------------------------------------------------

	/**
	 * A placement with no load constraint always allows any depth of stacking.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  [new]
	 *  2  [new]
	 *  1  [new]
	 *  0  [ A ]  no maxLoadBoxCount
	 * </pre>
	 */
	@Test
	void testCanStackLevels_noConstraint_alwaysAllowed() {
		Placement a = placement("A", 10, 10, 1, 5, 0, 0, 0);

		assertThat(AbstractPlacementControls.canStackLevels(a, 1)).isTrue();
		assertThat(AbstractPlacementControls.canStackLevels(a, 10)).isTrue();
		assertThat(AbstractPlacementControls.canStackOneMore(a)).isTrue();
	}

	/**
	 * {@code maxLoadBoxCount = 2} allows 2 boxes on top:
	 * levels ≤ 2 → allowed; levels = 3 → rejected.
	 *
	 * <pre>
	 *  levels passed to canStackLevels(A, levels):
	 *  levels=1 → 2 &lt; 1? no  → true  (1st box above ok)
	 *  levels=2 → 2 &lt; 2? no  → true  (2nd box above ok)
	 *  levels=3 → 2 &lt; 3? yes → false (3rd box above rejected)
	 * </pre>
	 */
	@Test
	void testCanStackLevels_count2_boundary() {
		Placement a = placementWithCount("A", 10, 10, 1, 5, 2, 0, 0, 0);

		assertThat(AbstractPlacementControls.canStackLevels(a, 1)).isTrue();
		assertThat(AbstractPlacementControls.canStackLevels(a, 2)).isTrue();
		assertThat(AbstractPlacementControls.canStackLevels(a, 3)).isFalse();
		assertThat(AbstractPlacementControls.canStackOneMore(a)).isTrue(); // levels=1 ≤ 2
	}

	/**
	 * {@code maxLoadBoxCount = 1} allows exactly 1 box on top.
	 *
	 * <pre>
	 *  levels=1 → 1 &lt; 1? no  → true  (exactly 1 box on top ok)
	 *  levels=2 → 1 &lt; 2? yes → false (2nd box above rejected)
	 * </pre>
	 */
	@Test
	void testCanStackLevels_count1_exactlyOneAbove() {
		Placement a = placementWithCount("A", 10, 10, 1, 5, 1, 0, 0, 0);

		assertThat(AbstractPlacementControls.canStackOneMore(a)).isTrue();
		assertThat(AbstractPlacementControls.canStackLevels(a, 2)).isFalse();
	}

	/**
	 * Constraint propagates through the supporter chain.
	 * A (count=1) supports B (no constraint). Asking whether B can have
	 * one more box on top recurses into A and detects the violation.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  [new?]  ← canStackOneMore(B) → recurse into A → A count=1 &lt; 2 → false
	 *  2  [ B  ]  no constraint
	 *  1  [ A  ]  maxLoadBoxCount=1
	 *  0  floor
	 *
	 *  canStackOneMore(B): levels at B=1 → ok for B;
	 *  levels at A (B's supporter) = 2 → 1 &lt; 2 = true → rejected
	 * </pre>
	 */
	@Test
	void testCanStackLevels_chainPropagation() {
		Placement a = placementWithCount("A", 10, 10, 1, 5, 1, 0, 0, 0);
		Placement b = placement("B", 10, 10, 1, 5, 0, 0, 1);
		// B is supported by A
		a.addLoad(b, 100, b.getWeight());

		// B itself has no count constraint, but A (its supporter) has count=1
		// B is 1 level above A; adding one more above B would be 2 levels above A → rejected
		assertThat(AbstractPlacementControls.canStackOneMore(b)).isFalse();
	}

	/**
	 * Constraint propagates through the supporter chain.
	 * A (count=2) supports B, B supports C.
	 * Placing above B = 2 levels above A → allowed (2 ≤ count=2).
	 * Placing above C = 3 levels above A → rejected (3 > count=2).
	 *
	 * <pre>
	 *  z
	 *  |          canStackOneMore(B)            canStackOneMore(C)
	 *  3  [new?]  levels at A=2, 2&lt;2=false→ok  levels at A=3, 2&lt;3=true→false
	 *  2  [ C ]
	 *  1  [ B ]
	 *  0  [ A ]   maxLoadBoxCount=2
	 * </pre>
	 */
	@Test
	void testCanStackLevels_deepChain_count2() {
		Placement a = placementWithCount("A", 10, 10, 1, 1, 2, 0, 0, 0);
		Placement b = placement("B", 10, 10, 1, 1, 0, 0, 1);
		Placement c = placement("C", 10, 10, 1, 1, 0, 0, 2);
		a.addLoad(b, 100, b.getWeight());
		b.addLoad(c, 100, c.getWeight());

		// Placing above B = 2 levels above A: 2 < 2 = false → allowed
		assertThat(AbstractPlacementControls.canStackOneMore(b)).isTrue();
		// Placing above C = 3 levels above A: 2 < 3 = true → rejected
		assertThat(AbstractPlacementControls.canStackOneMore(c)).isFalse();
	}

	/**
	 * With count=3, A allows 3 boxes on top. B (1 above) and C (2 above) are both ok;
	 * canStackOneMore(B) and canStackOneMore(C) are both true (A still permits 3rd level).
	 * Only placing a 4th level above C would be rejected.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  [ C ]   2 levels above A, count=3 allows up to 3 → ok
	 *  2  [ B ]   1 level above A → ok
	 *  1  [ A ]   maxLoadBoxCount=3
	 *
	 *  canStackOneMore(B) → levels at A = 3, 3 &lt; 3 = false → allowed
	 *  canStackOneMore(C) → levels at A = 3, 3 &lt; 3 = false → allowed
	 *  canStackLevels(C, 2) → levels at A = 4, 3 &lt; 4 = true → rejected
	 * </pre>
	 */
	@Test
	void testCanStackLevels_deepChain_count3() {
		Placement a = placementWithCount("A", 10, 10, 1, 1, 3, 0, 0, 0);
		Placement b = placement("B", 10, 10, 1, 1, 0, 0, 1);
		Placement c = placement("C", 10, 10, 1, 1, 0, 0, 2);
		a.addLoad(b, 100, b.getWeight());
		b.addLoad(c, 100, c.getWeight());

		assertThat(AbstractPlacementControls.canStackOneMore(b)).isTrue();  // 3 ≤ 3 at A → ok
		assertThat(AbstractPlacementControls.canStackOneMore(c)).isTrue();  // 3 ≤ 3 at A → ok
		assertThat(AbstractPlacementControls.canStackLevels(c, 2)).isFalse(); // 4th level above A → rejected
	}

	// -----------------------------------------------------------------------
	// isWithinMaxLoadBoxCount
	// -----------------------------------------------------------------------

	/**
	 * Empty supporter list → always within box-count limit.
	 */
	@Test
	void testIsWithinMaxLoadBoxCount_emptyList() {
		assertThat(AbstractPlacementControls.isWithinMaxLoadBoxCount(List.of())).isTrue();
	}

	/**
	 * Single supporter that can still accept one more box → true.
	 *
	 * <pre>
	 *  [new]
	 *  [ A ]  maxLoadBoxCount=2, nothing on top yet
	 * </pre>
	 */
	@Test
	void testIsWithinMaxLoadBoxCount_oneSupporter_canAccept() {
		Placement a = placementWithCount("A", 10, 10, 1, 1, 2, 0, 0, 0);
		assertThat(AbstractPlacementControls.isWithinMaxLoadBoxCount(List.of(a))).isTrue();
	}

	/**
	 * Single supporter already at its chain limit → false.
	 *
	 * <pre>
	 *  [new?]  ← rejected: A.count=1 but chain already at depth 1
	 *  [ B  ]  (B is on top of A, count=1 means only 1 box on top)
	 *  [ A  ]  maxLoadBoxCount=1
	 * </pre>
	 */
	@Test
	void testIsWithinMaxLoadBoxCount_oneSupporter_chainFull() {
		Placement a = placementWithCount("A", 10, 10, 1, 1, 1, 0, 0, 0);
		Placement b = placement("B", 10, 10, 1, 1, 0, 0, 1);
		a.addLoad(b, 100, b.getWeight());
		// Asking: can a new box go on top of B? B's supporter A has count=1, and we'd be at depth 2.
		assertThat(AbstractPlacementControls.isWithinMaxLoadBoxCount(List.of(b))).isFalse();
	}

	/**
	 * Two supporters: first ok, second at its limit → false.
	 */
	@Test
	void testIsWithinMaxLoadBoxCount_twoSupporters_oneFull() {
		Placement left = placementWithCount("L", 10, 10, 1, 1, 2, 0, 0, 0);
		Placement rightBottom = placementWithCount("RB", 10, 10, 1, 1, 1, 10, 0, 0);
		Placement rightTop = placement("RT", 10, 10, 1, 1, 10, 0, 1);
		rightBottom.addLoad(rightTop, 100, rightTop.getWeight());

		// left can still accept; rightTop cannot (its supporter rightBottom is at count=1 depth=2)
		assertThat(AbstractPlacementControls.isWithinMaxLoadBoxCount(List.of(left, rightTop))).isFalse();
	}

	// -----------------------------------------------------------------------
	// overlapArea
	// -----------------------------------------------------------------------

	/**
	 * Full overlap: new box and supporter have identical 10×10 footprints.
	 *
	 * <pre>
	 *  New:       [0..9] × [0..9]
	 *  Supporter: [0..9] × [0..9]
	 *  Overlap:   10 × 10 = 100
	 * </pre>
	 */
	@Test
	void testOverlapArea_fullOverlap() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p = placement("P", 10, 10, 1, 1, 0, 0, 0);
		assertThat(ctrl.testOverlapArea(0, 0, 9, 9, p)).isEqualTo(100L);
	}

	/**
	 * Partial overlap: new 20×10 box shares the right half with a 10×10 supporter.
	 *
	 * <pre>
	 *  New:  x=0..19, y=0..9
	 *  P:    x=10..19, y=0..9
	 *  Overlap: x=10..19, y=0..9 → 10×10 = 100
	 * </pre>
	 */
	@Test
	void testOverlapArea_partialOverlap() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p = placement("P", 10, 10, 1, 1, 10, 0, 0);
		assertThat(ctrl.testOverlapArea(0, 0, 19, 9, p)).isEqualTo(100L);
	}

	/**
	 * L-shaped partial overlap: 6×4 region.
	 *
	 * <pre>
	 *  New:  x=0..9,  y=0..9
	 *  P:    x=4..9,  y=3..9
	 *  Overlap: x=4..9, y=3..9 → 6 × 7 = 42
	 * </pre>
	 */
	@Test
	void testOverlapArea_cornerOverlap() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p = placement("P", 6, 7, 1, 1, 4, 3, 0);
		assertThat(ctrl.testOverlapArea(0, 0, 9, 9, p)).isEqualTo(42L);
	}

	// -----------------------------------------------------------------------
	// findSupporters
	// -----------------------------------------------------------------------

	/**
	 * Single supporting placement directly below (full overlap).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+  ← P (to be placed)
	 *  1  +----------+  ← S (supporter, endZ=1)
	 *  0  +----------+
	 *
	 *  P.z=1, so supporters must have endZ=0 → S qualifies.
	 * </pre>
	 *
	 * <p>{@code findSupporters} was removed with {@code LoadAwarePlacementControls}.
	 */
	@Disabled("findSupporters was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testFindSupporters_singleFullOverlap() {
	}

	/** {@code findSupporters} was removed. */
	@Disabled("findSupporters was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testFindSupporters_twoEqualSupporters() {
	}

	/** {@code findSupporters} was removed. */
	@Disabled("findSupporters was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testFindSupporters_wrongZLevel() {
	}

	// -----------------------------------------------------------------------
	// findSupportees
	// -----------------------------------------------------------------------

	/** {@code findSupportees} was removed. */
	@Disabled("findSupportees was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testFindSupportees_singleAbove() {
	}

	/** {@code findSupportees} was removed. */
	@Disabled("findSupportees was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testFindSupportees_skipsNonDirectAbove() {
	}

	// -----------------------------------------------------------------------
	// applyLoad
	// -----------------------------------------------------------------------

	/** {@code applyLoad} was removed. */
	@Disabled("applyLoad was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testApplyLoad_singleSupporter() {
	}

	/** {@code applyLoad} was removed. */
	@Disabled("applyLoad was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testApplyLoad_twoEqualSupporters_splitWeight() {
	}

	/** {@code applyLoad} was removed. */
	@Disabled("applyLoad was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testApplyLoad_threeLevel_propagation() {
	}

	/** {@code applyLoad} was removed. */
	@Disabled("applyLoad was removed in the LoadAwarePlacementControls refactoring")
	@Test
	void testApplyLoad_gapFill_wiresSupporteeAndSupporter() {
	}
}
