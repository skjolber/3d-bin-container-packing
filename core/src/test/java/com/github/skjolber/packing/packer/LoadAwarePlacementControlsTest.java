package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

/**
 * Unit tests for the utility methods of {@link LoadAwarePlacementControls}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@code canStackLevels} / {@code canStackOneMore} – stacking-depth constraint</li>
 *   <li>{@code isWithinMaxLoadBoxCount} – list-level box-count gate</li>
 *   <li>{@code populatePlacementSupporters} – supporter discovery by z-level and 2D overlap</li>
 *   <li>{@code overlapArea} – area of the XY overlap between two rectangles</li>
 *   <li>{@code findSupporters} / {@code findSupportees} – support-graph edges</li>
 *   <li>{@code applyLoad} – weight propagation into the support graph</li>
 * </ul>
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

	/** Minimal subclass that exposes protected instance methods for testing. */
	private static class TestableControls extends LoadAwarePlacementControls {
		TestableControls(Stack stack) {
			super(null, null, null, null, stack, Order.NONE, null, null, false, false);
		}

		long testOverlapArea(int minX, int minY, int maxX, int maxY, Placement p) {
			return overlapArea(minX, minY, maxX, maxY, p);
		}

		List<PlacementLoad> testFindSupporters(Placement p, List<Placement> existing) {
			return findSupporters(p, existing);
		}

		List<PlacementLoad> testFindSupportees(Placement p, List<Placement> existing) {
			return findSupportees(p, existing);
		}

		void testApplyLoad(Placement p) {
			applyLoad(p);
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

		assertThat(LoadAwarePlacementControls.canStackLevels(a, 1)).isTrue();
		assertThat(LoadAwarePlacementControls.canStackLevels(a, 10)).isTrue();
		assertThat(LoadAwarePlacementControls.canStackOneMore(a)).isTrue();
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

		assertThat(LoadAwarePlacementControls.canStackLevels(a, 1)).isTrue();
		assertThat(LoadAwarePlacementControls.canStackLevels(a, 2)).isTrue();
		assertThat(LoadAwarePlacementControls.canStackLevels(a, 3)).isFalse();
		assertThat(LoadAwarePlacementControls.canStackOneMore(a)).isTrue(); // levels=1 ≤ 2
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

		assertThat(LoadAwarePlacementControls.canStackOneMore(a)).isTrue();
		assertThat(LoadAwarePlacementControls.canStackLevels(a, 2)).isFalse();
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
		assertThat(LoadAwarePlacementControls.canStackOneMore(b)).isFalse();
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
		assertThat(LoadAwarePlacementControls.canStackOneMore(b)).isTrue();
		// Placing above C = 3 levels above A: 2 < 3 = true → rejected
		assertThat(LoadAwarePlacementControls.canStackOneMore(c)).isFalse();
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

		assertThat(LoadAwarePlacementControls.canStackOneMore(b)).isTrue();  // 3 ≤ 3 at A → ok
		assertThat(LoadAwarePlacementControls.canStackOneMore(c)).isTrue();  // 3 ≤ 3 at A → ok
		assertThat(LoadAwarePlacementControls.canStackLevels(c, 2)).isFalse(); // 4th level above A → rejected
	}

	// -----------------------------------------------------------------------
	// isWithinMaxLoadBoxCount
	// -----------------------------------------------------------------------

	/**
	 * Empty supporter list → always within box-count limit.
	 */
	@Test
	void testIsWithinMaxLoadBoxCount_emptyList() {
		assertThat(LoadAwarePlacementControls.isWithinMaxLoadBoxCount(List.of())).isTrue();
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
		assertThat(LoadAwarePlacementControls.isWithinMaxLoadBoxCount(List.of(a))).isTrue();
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
		assertThat(LoadAwarePlacementControls.isWithinMaxLoadBoxCount(List.of(b))).isFalse();
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
		assertThat(LoadAwarePlacementControls.isWithinMaxLoadBoxCount(List.of(left, rightTop))).isFalse();
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
	 */
	@Test
	void testFindSupporters_singleFullOverlap() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p = placement("P", 10, 10, 1, 5, 0, 0, 1);
		Placement s = placement("S", 10, 10, 1, 3, 0, 0, 0);

		List<PlacementLoad> result = ctrl.testFindSupporters(p, List.of(s));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getPlacement()).isSameAs(s);
		assertThat(result.get(0).getArea()).isEqualTo(100L);
	}

	/**
	 * Two supporters sharing the load of a wide box.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  1  +----------+----------+  ← P (20×10, z=1)
	 *  0  +----------+----------+  ← S1 (x=0..9), S2 (x=10..19), both endZ=0
	 * </pre>
	 */
	@Test
	void testFindSupporters_twoEqualSupporters() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p  = placement("P",  20, 10, 1, 8, 0, 0, 1);
		Placement s1 = placement("S1", 10, 10, 1, 2, 0, 0, 0);
		Placement s2 = placement("S2", 10, 10, 1, 2, 10, 0, 0);

		List<PlacementLoad> result = ctrl.testFindSupporters(p, List.of(s1, s2));

		assertThat(result).hasSize(2);
		result.forEach(sl -> assertThat(sl.getArea()).isEqualTo(100L));
	}

	/**
	 * Placement at wrong z level is not returned as a supporter.
	 */
	@Test
	void testFindSupporters_wrongZLevel() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement p    = placement("P",  10, 10, 1, 1, 0, 0, 2); // z=2, needs supports at endZ=1
		Placement low  = placement("Lo", 10, 10, 1, 1, 0, 0, 0); // endZ=0 → wrong
		Placement good = placement("Ok", 10, 10, 1, 1, 0, 0, 1); // endZ=1 → correct

		List<PlacementLoad> result = ctrl.testFindSupporters(p, List.of(low, good));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getPlacement()).isSameAs(good);
	}

	// -----------------------------------------------------------------------
	// findSupportees
	// -----------------------------------------------------------------------

	/**
	 * Finds a single placement resting directly on top.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+  ← top (z=1, absoluteZ=1 matches S.endZ+1=1)
	 *  1  +----------+  ← S (endZ=0; supportee is at z=1)
	 *  0
	 * </pre>
	 */
	@Test
	void testFindSupportees_singleAbove() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement s   = placement("S",   10, 10, 1, 2, 0, 0, 0); // endZ=0
		Placement top = placement("top", 10, 10, 1, 3, 0, 0, 1); // z=1 = endZ+1

		List<PlacementLoad> result = ctrl.testFindSupportees(s, List.of(top));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getPlacement()).isSameAs(top);
		assertThat(result.get(0).getArea()).isEqualTo(100L);
	}

	/**
	 * Placement two levels above is NOT a direct supportee.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+  ← far (z=2, two above S)
	 *  2  +----------+  ← mid (z=1, direct supportee)
	 *  1  +----------+  ← S
	 * </pre>
	 */
	@Test
	void testFindSupportees_skipsNonDirectAbove() {
		TestableControls ctrl = new TestableControls(new Stack());
		Placement s   = placement("S",   10, 10, 1, 1, 0, 0, 0);
		Placement mid = placement("mid", 10, 10, 1, 1, 0, 0, 1);
		Placement far = placement("far", 10, 10, 1, 1, 0, 0, 2);

		List<PlacementLoad> result = ctrl.testFindSupportees(s, List.of(mid, far));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getPlacement()).isSameAs(mid);
	}

	// -----------------------------------------------------------------------
	// applyLoad
	// -----------------------------------------------------------------------

	/**
	 * Single supporter: the placed box's full weight is assigned to the supporter.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  1  +----------+  ← P (weight=7)
	 *  0  +----------+  ← S (supporter in stack)
	 *
	 *  After applyLoad(P):
	 *    P.supporters = [S]
	 *    S.loadWeight = 7
	 * </pre>
	 */
	@Test
	void testApplyLoad_singleSupporter() {
		Stack stack = new Stack();
		Placement s = placementWithWeight("S", 10, 10, 1, 3, 50, 0, 0, 0);
		stack.add(s);

		TestableControls ctrl = new TestableControls(stack);
		Placement p = placement("P", 10, 10, 1, 7, 0, 0, 1);

		ctrl.testApplyLoad(p);

		assertThat(p.getSupporters()).hasSize(1);
		assertThat(p.getSupporters().get(0).getPlacement()).isSameAs(s);
		assertThat(s.getLoadWeight()).isEqualTo(7L);
	}

	/**
	 * Two equal-area supporters split the load 50/50.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  1  +----+----+  ← P (20×10, weight=10)
	 *  0  +----+----+  ← S1 (x=0..9), S2 (x=10..19), both in stack
	 *
	 *  After applyLoad(P):
	 *    S1.loadWeight = 5
	 *    S2.loadWeight = 5
	 * </pre>
	 */
	@Test
	void testApplyLoad_twoEqualSupporters_splitWeight() {
		Stack stack = new Stack();
		Placement s1 = placementWithWeight("S1", 10, 10, 1, 2, 50, 0, 0, 0);
		Placement s2 = placementWithWeight("S2", 10, 10, 1, 2, 50, 10, 0, 0);
		stack.add(s1);
		stack.add(s2);

		TestableControls ctrl = new TestableControls(stack);
		Placement p = placement("P", 20, 10, 1, 10, 0, 0, 1);

		ctrl.testApplyLoad(p);

		assertThat(s1.getLoadWeight()).isEqualTo(5L);
		assertThat(s2.getLoadWeight()).isEqualTo(5L);
	}

	/**
	 * Load propagates 3 levels: applyLoad(C) → S2 bears C.weight,
	 * S1 in turn bears S2.weight (propagated from S2's own weight call through S1).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+  ← C (weight=4, placed via applyLoad)
	 *  1  +----------+  ← S2 (weight=3, already in stack with S1 as supporter)
	 *  0  +----------+  ← S1 (weight=2, in stack)
	 *
	 *  After applyLoad(C):
	 *    S2.loadWeight = 4         (C's weight)
	 *    S1.loadWeight = 3 + 4 = 7 (S2's own weight propagated to S1, plus C through S2)
	 *
	 *  Wait – S1's loadWeight was already set when S2 was placed (via its own applyLoad).
	 *  Here we test only that applyLoad(C) adds C's weight to both S2 and S1.
	 * </pre>
	 */
	@Test
	void testApplyLoad_threeLevel_propagation() {
		Stack stack = new Stack();
		Placement s1 = placementWithWeight("S1", 10, 10, 1, 2, 100, 0, 0, 0);
		Placement s2 = placementWithWeight("S2", 10, 10, 1, 3, 100, 0, 0, 1);
		stack.add(s1);
		stack.add(s2);

		TestableControls ctrl1 = new TestableControls(stack);
		ctrl1.testApplyLoad(s2); // s1 now bears s2's weight (3)

		Placement c = placement("C", 10, 10, 1, 4, 0, 0, 2);
		ctrl1.testApplyLoad(c); // s2 now bears c's weight; that propagates to s1 too

		assertThat(s2.getLoadWeight()).isEqualTo(4L);
		assertThat(s1.getLoadWeight()).isEqualTo(3L + 4L); // s2 weight + c weight
	}

	/**
	 * Gap-fill: a placement is inserted below an existing one (P below top).
	 * applyLoad(P) should find 'top' as its supportee and 's' as its supporter,
	 * wiring up the three-level chain.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+  ← top (already in stack, weight=5)
	 *  1  +----------+  ← P (new, inserted below top)
	 *  0  +----------+  ← s (already in stack)
	 * </pre>
	 */
	@Test
	void testApplyLoad_gapFill_wiresSupporteeAndSupporter() {
		Stack stack = new Stack();
		Placement s   = placement("s",   10, 10, 1, 2, 0, 0, 0);
		Placement top = placement("top", 10, 10, 1, 5, 0, 0, 2);
		stack.add(s);
		stack.add(top);

		TestableControls ctrl = new TestableControls(stack);
		Placement p = placement("P", 10, 10, 1, 3, 0, 0, 1);

		ctrl.testApplyLoad(p);

		// P should be supported by s and should support top
		assertThat(p.getSupporters()).hasSize(1)
				.allMatch(sl -> sl.getPlacement() == s);
		assertThat(p.getSupportees()).hasSize(1)
				.allMatch(sl -> sl.getPlacement() == top);
		// s now bears P's weight
		assertThat(s.getLoadWeight()).isEqualTo(3L);
	}
}
