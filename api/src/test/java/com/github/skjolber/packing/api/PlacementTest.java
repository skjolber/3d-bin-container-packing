package com.github.skjolber.packing.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Placement} load tracking: supporters / supportees relationships
 * and weight propagation across multiple levels.
 *
 * <p>Coordinate system: X = width (right), Y = depth (into page), Z = height (up).
 * Side-view diagrams show the X/Z plane unless noted otherwise.
 */
public class PlacementTest {

	/** Creates a simple Placement using the first (non-rotated) {@link BoxStackValue}. */
	private static Placement makePlacement(String id, int dx, int dy, int dz, int weight,
			int x, int y, int z) {
		Box box = Box.newBuilder()
				.withId(id)
				.withSize(dx, dy, dz)
				.withWeight(weight)
				.withRotate2D()
				.build();
		BoxStackValue sv = box.getStackValues()[0];
		return new Placement(sv, 0, x, y, z);
	}

	// -----------------------------------------------------------------------
	// 3-level straight stack
	// -----------------------------------------------------------------------

	/**
	 * Three boxes stacked vertically, all with the same 10×10 footprint.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  weight=5,  loadWeight expected=0
	 *  2  +----------+
	 *     |    B     |  weight=10, loadWeight expected=5
	 *  1  +----------+
	 *     |    A     |  weight=20, loadWeight expected=15
	 *  0  +-----------
	 *     0          10   x
	 * </pre>
	 *
	 * Supporter/supportee relationships:
	 * <ul>
	 *   <li>A.supportees = [B],  A.supporters = []</li>
	 *   <li>B.supportees = [C],  B.supporters = [A]</li>
	 *   <li>C.supportees = [],   C.supporters = [B]</li>
	 * </ul>
	 */
	@Test
	public void testThreeLevelStack_supportersAndSupportees() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		long area = 100L;

		a.addLoad(b, area, b.getWeight());
		b.addLoad(c, area, c.getWeight());

		// --- supporter / supportee relationships ---
		assertThat(a.getSupporters()).isEmpty();
		assertThat(a.getSupportees()).hasSize(1);
		assertThat(a.getSupportees().get(0).getPlacement()).isSameAs(b);

		assertThat(b.getSupporters()).hasSize(1);
		assertThat(b.getSupporters().get(0).getPlacement()).isSameAs(a);
		assertThat(b.getSupportees()).hasSize(1);
		assertThat(b.getSupportees().get(0).getPlacement()).isSameAs(c);

		assertThat(c.getSupporters()).hasSize(1);
		assertThat(c.getSupporters().get(0).getPlacement()).isSameAs(b);
		assertThat(c.getSupportees()).isEmpty();

		// --- load propagation ---
		assertThat(c.getLoadWeight()).isEqualTo(0);
		assertThat(b.getLoadWeight()).isEqualTo(5);   // C's weight
		assertThat(a.getLoadWeight()).isEqualTo(15);  // B.weight + C.weight
	}

	// -----------------------------------------------------------------------
	// 4-level straight stack – load propagates at least 3 levels down
	// -----------------------------------------------------------------------

	/**
	 * Four boxes in a single vertical column.  Adding D triggers load propagation
	 * all the way from D through C → B → A (three levels below D).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  4  +----------+
	 *     |    D     |  weight=5,  loadWeight expected=0
	 *  3  +----------+
	 *     |    C     |  weight=10, loadWeight expected=5
	 *  2  +----------+
	 *     |    B     |  weight=20, loadWeight expected=15
	 *  1  +----------+
	 *     |    A     |  weight=30, loadWeight expected=35
	 *  0  +-----------
	 *     0          10   x
	 * </pre>
	 */
	@Test
	public void testFourLevelStack_loadPropagatesThreeLevelsDown() {
		Placement a = makePlacement("A", 10, 10, 1, 30, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 20, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 2);
		Placement d = makePlacement("D", 10, 10, 1,  5, 0, 0, 3);

		long area = 100L;

		a.addLoad(b, area, b.getWeight());
		b.addLoad(c, area, c.getWeight());
		c.addLoad(d, area, d.getWeight());

		// Verify all four levels
		assertThat(d.getLoadWeight()).isEqualTo(0);
		assertThat(c.getLoadWeight()).isEqualTo(5);   // D.weight
		assertThat(b.getLoadWeight()).isEqualTo(15);  // C.weight + D.weight
		assertThat(a.getLoadWeight()).isEqualTo(35);  // B.weight + C.weight + D.weight
	}

	// -----------------------------------------------------------------------
	// Split load: one box resting equally on two side-by-side supporters
	// -----------------------------------------------------------------------

	/**
	 * Box C (10×10) spans two side-by-side boxes A and B, each 5×10.
	 * The overlap area with each supporter is 50 (equal split).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----+----+
	 *     |    C    |  weight=10, footprint 10×10
	 *  1  +----+----+
	 *     | A  | B  |  weight=20 each, footprint 5×10
	 *  0  +----+----+
	 *     0    5   10   x
	 * </pre>
	 *
	 * Each supporter overlaps C by area=50.
	 * Load share: A = 10 × 50/100 = 5,  B = 10 × 50/100 = 5
	 */
	@Test
	public void testSplitLoad_equalTwoSupporters() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 20, 5, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 1);

		long halfArea = 50L;

		a.addLoad(c, halfArea, 5L);  // A bears half of C's weight
		b.addLoad(c, halfArea, 5L);  // B bears half of C's weight

		// C has two supporters
		assertThat(c.getSupporters()).hasSize(2);
		assertThat(a.getSupportees()).hasSize(1);
		assertThat(b.getSupportees()).hasSize(1);
		assertThat(c.getSupportedArea()).isEqualTo(100L);

		assertThat(c.getLoadWeight()).isEqualTo(0);
		assertThat(a.getLoadWeight()).isEqualTo(5);
		assertThat(b.getLoadWeight()).isEqualTo(5);
	}

	// -----------------------------------------------------------------------
	// Split load + propagation (3 levels with a shared supporter)
	// -----------------------------------------------------------------------

	/**
	 * D rests equally on B and C (split load).  B itself rests on A.
	 * Adding D propagates weight three levels down through B → A.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----+----+
	 *     |    D    |  weight=10, footprint 10×10, overlap 50 with each of B/C
	 *  2  +----+----+
	 *     | B  | C  |  weight=20 each, footprint 5×10
	 *  1  +----+    |
	 *     | A  |    |  weight=30, footprint 5×10 (only beneath B)
	 *  0  +----+----+
	 *     0    5   10   x
	 * </pre>
	 *
	 * Expected loadWeights:
	 * <ul>
	 *   <li>D = 0</li>
	 *   <li>C = 5  (half of D)</li>
	 *   <li>B = 5  (half of D)</li>
	 *   <li>A = B.weight + B.loadWeight = 20 + 5 = 25  (A only supports B)</li>
	 * </ul>
	 */
	@Test
	public void testThreeLevels_splitLoadWithPropagation() {
		Placement a = makePlacement("A",  5, 10, 1, 30, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 20, 0, 0, 1);
		Placement c = makePlacement("C",  5, 10, 1, 20, 5, 0, 0);
		Placement d = makePlacement("D", 10, 10, 1, 10, 0, 0, 2);

		long fullFootprint = 50L; // 5×10

		// B rests on A (A carries all of B's own weight)
		a.addLoad(b, fullFootprint, b.getWeight());
		// D rests equally on B and C
		b.addLoad(d, 50L, 5L);
		c.addLoad(d, 50L, 5L);

		assertThat(d.getLoadWeight()).isEqualTo(0);
		assertThat(c.getLoadWeight()).isEqualTo(5);   // half of D
		assertThat(b.getLoadWeight()).isEqualTo(5);   // half of D
		// A bears B's own weight (20) + B's load share propagated from D (5)
		assertThat(a.getLoadWeight()).isEqualTo(25);
	}

	// -----------------------------------------------------------------------
	// Unequal split load
	// -----------------------------------------------------------------------

	/**
	 * Box C (10×10) rests 75% on A and 25% on B.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    C     |  weight=100, footprint 10×10
	 *  1  +-------+--+
	 *     |   A   |B |  A covers area=75, B covers area=25
	 *  0  +-------+--+
	 *     0       7.5 10   x   (integer units)
	 * </pre>
	 *
	 * Weight on A = 100 × 75/100 = 75
	 * Weight on B = 100 × 25/100 = 25
	 */
	@Test
	public void testSplitLoad_unequalTwoSupporters() {
		Placement a = makePlacement("A", 10, 10, 1, 50,  0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 50, 10, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 100, 0, 0, 1);

		a.addLoad(c, 75L, 75L);
		b.addLoad(c, 25L, 25L);

		assertThat(c.getLoadWeight()).isEqualTo(0);
		assertThat(a.getLoadWeight()).isEqualTo(75);
		assertThat(b.getLoadWeight()).isEqualTo(25);
		assertThat(c.getSupportedArea()).isEqualTo(100L);
	}

	// -----------------------------------------------------------------------
	// Remove load – top box of a 3-level stack
	// -----------------------------------------------------------------------

	/**
	 * Three-level stack; remove the top box C and verify that loadWeights
	 * are reduced correctly all the way down to A.
	 *
	 * <pre>
	 *  Before removal:            After b.removeLoad(c):
	 *
	 *  z                          z
	 *  |                          |
	 *  3  +----------+            3
	 *     |    C     |  weight=5
	 *  2  +----------+            2  +----------+
	 *     |    B     |               |    B     |  loadWeight: 5  → 0
	 *  1  +----------+            1  +----------+
	 *     |    A     |               |    A     |  loadWeight: 15 → 10
	 *  0  +----------+            0  +----------+
	 * </pre>
	 */
	@Test
	public void testRemoveLoad_topBox() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		long area = 100L;

		a.addLoad(b, area, b.getWeight());
		b.addLoad(c, area, c.getWeight());

		// Pre-condition
		assertThat(a.getLoadWeight()).isEqualTo(15);
		assertThat(b.getLoadWeight()).isEqualTo(5);

		b.removeLoad(c);

		// C is no longer linked
		assertThat(b.getSupportees()).isEmpty();
		assertThat(c.getSupporters()).isEmpty();

		// Load has propagated back down
		assertThat(b.getLoadWeight()).isEqualTo(0);
		assertThat(a.getLoadWeight()).isEqualTo(10);
	}

	// -----------------------------------------------------------------------
	// Remove load – top box of a 4-level stack (3 levels of propagation)
	// -----------------------------------------------------------------------

	/**
	 * Four-level stack; remove the top box D and verify that load is reduced
	 * at every level: C, B, and A — three levels below D.
	 *
	 * <pre>
	 *  Before c.removeLoad(d):    After:
	 *
	 *  z                          z
	 *  |                          |
	 *  4  +----------+            4
	 *     |    D     |  weight=5
	 *  3  +----------+            3  +----------+
	 *     |    C     |               |    C     |  loadWeight: 5  → 0
	 *  2  +----------+            2  +----------+
	 *     |    B     |               |    B     |  loadWeight: 15 → 10
	 *  1  +----------+            1  +----------+
	 *     |    A     |               |    A     |  loadWeight: 35 → 30
	 *  0  +----------+            0  +----------+
	 * </pre>
	 */
	@Test
	public void testRemoveLoad_topOfFourLevelStack() {
		Placement a = makePlacement("A", 10, 10, 1, 30, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 20, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 2);
		Placement d = makePlacement("D", 10, 10, 1,  5, 0, 0, 3);

		long area = 100L;

		a.addLoad(b, area, b.getWeight());
		b.addLoad(c, area, c.getWeight());
		c.addLoad(d, area, d.getWeight());

		// Pre-condition
		assertThat(a.getLoadWeight()).isEqualTo(35);
		assertThat(b.getLoadWeight()).isEqualTo(15);
		assertThat(c.getLoadWeight()).isEqualTo(5);

		c.removeLoad(d);

		// D is unlinked
		assertThat(c.getSupportees()).isEmpty();
		assertThat(d.getSupporters()).isEmpty();

		// Load reduced at all three levels below D
		assertThat(c.getLoadWeight()).isEqualTo(0);
		assertThat(b.getLoadWeight()).isEqualTo(10);
		assertThat(a.getLoadWeight()).isEqualTo(30);
	}

	// -----------------------------------------------------------------------
	// Remove load – split scenario (remove one of two supporters)
	// -----------------------------------------------------------------------

	/**
	 * Box C rests equally on A and B. Remove A's load contribution and verify
	 * that A's loadWeight drops to 0 and that B's load share is also unwound.
	 *
	 * <pre>
	 *  z                          After a.removeLoad(c):
	 *  |
	 *  2  +----+----+             2  + 
	 *     |    C    |  weight=10     |   
	 *  1  +----+----+             1  +----+----+
	 *     | A  | B  |                | A  | B  |  A.loadWeight: 5 → 0
	 *  0  +----+----+             0  +----+----+  B.loadWeight: 5 → 0
	 *     0    5   10   x
	 * </pre>
	 *
	 * Removal flow: {@code a.removeLoad(c)} →
	 * {@code c.removeSupporter(a)} → {@code c.propagateLoad(-5)} →
	 * B receives the -5 propagation and drops to 0; then
	 * {@code a.propagateLoad(-5)} → A drops to 0.
	 */
	@Test
	public void testRemoveLoad_oneOfTwoSupporters() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 20, 5, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 1);

		a.addLoad(c, 50L, 5L);
		b.addLoad(c, 50L, 5L);

		assertThat(a.getLoadWeight()).isEqualTo(5);
		assertThat(b.getLoadWeight()).isEqualTo(5);

		a.removeLoad(c);

		// A is no longer a supporter of C
		assertThat(a.getSupportees()).isEmpty();
		assertThat(a.getLoadWeight()).isEqualTo(0);

		// removeSupporter propagates the removal through all remaining supporters of C,
		// so B's load share is also unwound
		assertThat(b.getLoadWeight()).isEqualTo(0);
		assertThat(b.getSupportees()).hasSize(1);
	}

	// -----------------------------------------------------------------------
	// clearLoad
	// -----------------------------------------------------------------------

	/**
	 * clearLoad() resets all load-tracking state on the called placement.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    C     |  weight=5
	 *  2  +----------+
	 *     |    B     |  ← clearLoad() called here
	 *  1  +----------+
	 *     |    A     |  weight=20
	 *  0  +----------+
	 *     0          10   x
	 * </pre>
	 */
	@Test
	public void testClearLoad_resetsAllState() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1,  5, 0, 0, 2);

		long area = 100L;

		a.addLoad(b, area, b.getWeight());
		b.addLoad(c, area, c.getWeight());

		b.clearLoad();

		assertThat(b.getSupportees()).isEmpty();
		assertThat(b.getSupporters()).isEmpty();
		assertThat(b.getLoadWeight()).isEqualTo(0);
		assertThat(b.getSupportedArea()).isEqualTo(0);
	}

	// -----------------------------------------------------------------------
	// supportedArea tracking
	// -----------------------------------------------------------------------

	/**
	 * One box (A) supporting two boxes (B and C) side by side on top.
	 * Each box overlaps A by half its area.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----+----+
	 *     | B  | C  |  weight=10 each, footprint 5×10
	 *  1  +----+----+
	 *     |    A    |  weight=20, footprint 10×10
	 *  0  +----------+
	 *     0    5   10   x
	 * </pre>
	 *
	 * B.supportedArea = 50, C.supportedArea = 50.
	 * A.loadWeight = B.weight + C.weight = 20.
	 */
	@Test
	public void testSupportedArea_twoBoxesOnOneSupporter() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C",  5, 10, 1, 10, 5, 0, 1);

		a.addLoad(b, 50L, b.getWeight());
		a.addLoad(c, 50L, c.getWeight());

		assertThat(a.getSupportees()).hasSize(2);
		assertThat(b.getSupporters()).hasSize(1);
		assertThat(c.getSupporters()).hasSize(1);

		// Each box on top is fully supported by A
		assertThat(b.getSupportedArea()).isEqualTo(50L);
		assertThat(c.getSupportedArea()).isEqualTo(50L);

		// A bears both boxes
		assertThat(a.getLoadWeight()).isEqualTo(20L);
	}

	// -----------------------------------------------------------------------
	// Stability with stack (isStableWithStack)
	// -----------------------------------------------------------------------

	/**
	 * Single box on the floor with no supportees — trivially stable (CoM at centre
	 * of own footprint, which equals the support region).
	 */
	@Test
	public void testIsStableWithStack_floorBox_isStable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		assertThat(a.isStable()).isTrue();
	}

	/**
	 * Floating box with no supporters is unstable.
	 */
	@Test
	public void testIsStableWithStack_floatingNoSupporters_isUnstable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 5);
		assertThat(a.isStable()).isFalse();
	}

	/**
	 * A directly below B, both perfectly aligned.  B is centred above A, so
	 * the combined CoM is still centred — stable.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  weight=10, x=0..9, CoM x=4.5
	 *  1  +----------+
	 *     |    A     |  weight=20, x=0..9, CoM x=4.5
	 *  0  +----------+
	 * </pre>
	 *
	 * Combined CoM x = (20×4.5 + 10×4.5) / 30 = 4.5 → inside support [0..9]. Stable.
	 */
	@Test
	public void testIsStableWithStack_centredStack_isStable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());
		assertThat(a.isStable()).isTrue();
	}

	/**
	 * A (10×10, weight=20) sits on a pedestal P (2×10) so that A itself has a
	 * narrow support region (x=[4..5]).  B (10×10, weight=80) is placed far to
	 * the right on top of A.  The heavy B drags the combined CoM well outside
	 * A's narrow support region → unstable.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3          +----------+
	 *             |    B     |  weight=80, x=8..17, CoM x=13
	 *  2  +----------+
	 *     |    A     |  weight=20, x=0..9, CoM x=4.5
	 *  1      +--+
	 *         | P|  pedestal 2×10, x=4..5
	 *  0      +--+
	 * </pre>
	 *
	 * Overlap A∩P in X: [4..5].  Combined CoM x = (20×9 + 80×26) / 100 = (180+2080)/100 = 22
	 * (all ×2).  maxSupportX ×2 = 11.  22 > 11 → unstable.
	 */
	@Test
	public void testIsStableWithStack_heavySupporteeOffCenter_isUnstable() {
		Placement p = makePlacement("P",  2, 10, 1, 50, 4, 0, 0);
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 1);
		Placement b = makePlacement("B", 10, 10, 1, 80, 8, 0, 2);
		p.addLoad(a, 20L, a.getWeight());
		a.addLoad(b, 20L, b.getWeight());
		assertThat(a.isStable()).isFalse();
	}

	/**
	 * A light off-centre supportee does not move the combined CoM enough to
	 * topple the heavier base box.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3          +----------+
	 *             |    B     |  weight=5, x=8..17, CoM x=13
	 *  2  +----------+
	 *     |    A     |  weight=100, x=0..9, CoM x=4.5, supported by P over x=0..9
	 *  1  +----------+
	 *     |    P     |  pedestal 10×10
	 *  0  +----------+
	 * </pre>
	 *
	 * Combined CoM ×2 = (100000×9 + 5000×26) / 105000 ≈ 9.78 → inside support [0..9]. Stable.
	 */
	@Test
	public void testIsStableWithStack_lightSupporteeOffCenter_isStable() {
		Placement p = makePlacement("P", 10, 10, 1, 50, 0, 0, 0);
		Placement a = makePlacement("A", 10, 10, 1, 100, 0, 0, 1);
		Placement b = makePlacement("B", 10, 10, 1,   5, 8, 0, 2);
		p.addLoad(a, 100L, a.getWeight());
		a.addLoad(b, 20L, b.getWeight());
		assertThat(a.isStable()).isTrue();
	}

	/**
	 * Three-level stack: B is off-centre on pedestal P, and C is off-centre on B,
	 * compounding the CoM shift. B is the box checked for stability.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  4       +----------+
	 *          |    C     |  weight=50, x=6..15, CoM×2=22
	 *  3    +----------+
	 *       |    B     |    weight=5, x=3..12, CoM×2=15, supported by P over x=[4..9]
	 *  2      +------+
	 *         |  P   |      pedestal 6×10, x=4..9
	 *  1  ????
	 *  0      +------+
	 * </pre>
	 *
	 * Overlap P∩B: [4..9] (6 units wide). B.supportedArea=60.
	 * Overlap B∩C: [6..9] (4 units wide). C.supportedArea=40.
	 * Share of C from B = 1000 × 40/40 = 1000.
	 *
	 * accumulateStackCoM(B, 1000):
	 *   B: w=5000, com2x=2×3+10=16, com2y=10
	 *   C: overlapArea(B∩C)=(9-6+1)×10=40, share=1000×40/40=1000
	 *     sub(C,1000): w=50000, com2x=2×6+10=22
	 *   total=55000, weightedX=5000×16+50000×22=80000+1100000=1180000
	 *   com2x=1180000/55000≈21.45 → 21
	 * B support bounding box: overlap P∩B maxX=9 → 2×9=18.
	 * 21 > 18 → unstable.
	 */
	@Test
	public void testIsStableWithStack_threeLevels_compoundedShift_isUnstable() {
		Placement p = makePlacement("P",  6, 10, 1, 30, 4, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1,  5, 3, 0, 1);
		Placement c = makePlacement("C", 10, 10, 1, 50, 6, 0, 2);
		p.addLoad(b, 60L, b.getWeight());
		b.addLoad(c, 40L, c.getWeight());
		assertThat(b.isStable()).isFalse();
	}

	/**
	 * isStable() and isStableWithStack() agree for a centred single-box stack,
	 * and disagree when a heavy off-centre supportee tips the combined CoM.
	 * The box under test (A) is elevated on a narrow pedestal so its support region
	 * is restricted to x=[4..5].
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3          +----------+
	 *             |    B     |  weight=80, x=8..17
	 *  2  +----------+
	 *     |    A     |  weight=20, x=0..9, supported by P over x=[4..5]
	 *  1      +--+
	 *         | P|  x=4..5
	 *  0      +--+
	 * </pre>
	 */
	@Test
	public void testIsStableWithStack_vs_isStable_heavySupporteeOffCenter() {
		Placement p = makePlacement("P",  2, 10, 1, 50, 4, 0, 0);
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 1);
		Placement b = makePlacement("B", 10, 10, 1, 80, 8, 0, 2);
		p.addLoad(a, 20L, a.getWeight());
		a.addLoad(b, 20L, b.getWeight());

		// A's own CoM ×2 = 9, support region ×2 = [8..11] → inside → stable
		assertThat(a.isStableSupport()).isTrue();
		// Combined CoM ×2 ≈ 22 > 11 → outside → unstable
		assertThat(a.isStable()).isFalse();
	}

	/**
	 * Split load: C (10×10) rests equally on A and B (each 5×10 side by side).
	 * C itself has a centred box D on top.  Both A and B should be stable with stack.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *     |    D     |  weight=10, x=0..9, CoM x=4.5
	 *  2  +----------+
	 *     |    C     |  weight=10, x=0..9, CoM x=4.5
	 *  1  +----+----+
	 *     | A  | B  |  weight=20 each, A x=0..4, B x=5..9
	 *  0  +----+----+
	 * </pre>
	 *
	 * Both combined CoMs sit at x=4.5, centred over each supporter's overlap region.
	 */
	@Test
	public void testIsStableWithStack_splitLoad_centred_isStable() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 20, 5, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 1);
		Placement d = makePlacement("D", 10, 10, 1, 10, 0, 0, 2);

		a.addLoad(c, 50L, 5L);
		b.addLoad(c, 50L, 5L);
		c.addLoad(d, 100L, d.getWeight());

		assertThat(a.isStable()).isTrue();
		assertThat(b.isStable()).isTrue();
	}

	// -----------------------------------------------------------------------
	// Stability (isStable)
	// -----------------------------------------------------------------------

	/**
	 * Box resting on the container floor (z == 0) with no supporters is stable.
	 */
	@Test
	public void testIsStable_floorBox_noSupporters_isStable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		assertThat(a.isStableSupport()).isTrue();
	}

	/**
	 * Box floating in mid-air (z > 0) with no supporters is unstable.
	 */
	@Test
	public void testIsStable_floatingBox_noSupporters_isUnstable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 5);
		assertThat(a.isStableSupport()).isFalse();
	}

	/**
	 * Box perfectly centred on a single supporter — CoM directly over the support
	 * region.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  footprint 10×10, placed at (0,0)
	 *  1  +----------+
	 *     |    A     |  footprint 10×10, placed at (0,0)
	 *  0  +----------+
	 * </pre>
	 *
	 * B's CoM is at (5, 5). Support region is [0..10] × [0..10]. Stable.
	 */
	@Test
	public void testIsStable_fullySupported_isStable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(b, 100L, b.getWeight());
		assertThat(b.isStableSupport()).isTrue();
	}

	/**
	 * Box B (10×10) placed so its CoM lands exactly inside the support region.
	 * A covers x=0..9 (inclusive), B starts at x=4, CoM at x=4+5=9 — right at
	 * the edge of A's support. Stable.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2      +----------+
	 *         |    B     |  x=4..13, CoM at x=9
	 *  1  +----------+
	 *     |    A     |  x=0..9
	 *  0  +----------+
	 * </pre>
	 *
	 * Overlap X: [4..9]. CoM x=9 == maxSupportX → stable (boundary is inclusive).
	 */
	@Test
	public void testIsStable_halfOverhang_comAtEdge_isStable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 4, 0, 1);
		a.addLoad(b, 60L, b.getWeight());
		assertThat(b.isStableSupport()).isTrue();
	}

	/**
	 * Box B overhangs more than half: CoM falls outside the support region.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2           +----------+
	 *              |    B     |  x=6..16, CoM at x=11
	 *  1  +----------+
	 *     |    A     |  x=0..10
	 *  0  +----------+
	 * </pre>
	 *
	 * Overlap X: [6..10]. CoM x=11 &gt; 10 → unstable.
	 */
	@Test
	public void testIsStable_majorOverhang_comOutside_isUnstable() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 6, 0, 1);
		a.addLoad(b, 40L, b.getWeight());
		assertThat(b.isStableSupport()).isFalse();
	}

	/**
	 * Box C (10×10) is centred over two side-by-side supporters, each 5×10.
	 * Together they fully cover C's footprint — CoM is well inside. Stable.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----+----+
	 *     |    C    |  x=0..10, CoM at (5, 5)
	 *  1  +----+----+
	 *     | A  | B  |  A x=0..5, B x=5..10
	 *  0  +----+----+
	 * </pre>
	 */
	@Test
	public void testIsStable_twoSideBySideSupporters_isStable() {
		Placement a = makePlacement("A",  5, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 20, 5, 0, 0);
		Placement c = makePlacement("C", 10, 10, 1, 10, 0, 0, 1);
		a.addLoad(c, 50L, 5L);
		b.addLoad(c, 50L, 5L);
		assertThat(c.isStableSupport()).isTrue();
	}

	/**
	 * Box C rests on two widely-separated point supports; its CoM falls in the
	 * unsupported gap between them.  The union bounding box spans both, so the
	 * CoM appears inside — this demonstrates that {@code isStable()} uses the
	 * bounding-box approximation and returns {@code true} in this case.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----+----+----+
	 *     |         C    |  x=0..12, CoM at x=6
	 *  1  +--+      +--+
	 *     |A |      |B |   A x=0..2, B x=10..12
	 *  0  +--+      +--+
	 * </pre>
	 *
	 * Bounding box of overlaps: [0..12] × [0..10].  CoM x=6 is inside → {@code true}.
	 */
	@Test
	public void testIsStable_twoDistantSupporters_comInGap_boundingBoxTrue() {
		Placement a = makePlacement("A",  2, 10, 1, 10,  0, 0, 0);
		Placement b = makePlacement("B",  2, 10, 1, 10, 10, 0, 0);
		Placement c = makePlacement("C", 12, 10, 1, 20,  0, 0, 1);
		a.addLoad(c, 20L, 10L);
		b.addLoad(c, 20L, 10L);
		assertThat(c.isStableSupport()).isTrue();
	}

	// -----------------------------------------------------------------------
	// Load pressure
	// -----------------------------------------------------------------------

	/**
	 * Load pressure = loadWeight × 1000 / area.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *     |    B     |  weight=10, footprint 10×10 (area=100)
	 *  1  +----------+
	 *     |    A     |  footprint 10×10 (area=100)
	 *  0  +----------+
	 *     0          10   x
	 *
	 *  A.loadPressure = 10 × 1000 / 100 = 100
	 * </pre>
	 */
	@Test
	public void testLoadPressure_singleBox() {
		Placement a = makePlacement("A", 10, 10, 1, 20, 0, 0, 0);
		Placement b = makePlacement("B", 10, 10, 1, 10, 0, 0, 1);

		a.addLoad(b, 100L, b.getWeight());

		assertThat(a.getLoadWeight()).isEqualTo(10);
		assertThat(a.getLoadPressure()).isEqualTo(100L);  // 10*1000/100
	}

	/**
	 * Stacking two boxes (B weight=10, C weight=15) on A (10×10).
	 * A.loadPressure = (10+15) × 1000 / 100 = 250.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----+----+
	 *     | B  | C  |  weight=10, weight=15 – side by side, each 5×10
	 *  2  +----+----+
	 *     |    A    |  footprint 10×10
	 *  1  +----------+
	 * </pre>
	 */
	@Test
	public void testLoadPressure_twoBoxesOnTop() {
		Placement a = makePlacement("A", 10, 10, 1, 30, 0, 0, 0);
		Placement b = makePlacement("B",  5, 10, 1, 10, 0, 0, 1);
		Placement c = makePlacement("C",  5, 10, 1, 15, 5, 0, 1);

		a.addLoad(b, 50L, b.getWeight());
		a.addLoad(c, 50L, c.getWeight());

		assertThat(a.getLoadWeight()).isEqualTo(25);
		assertThat(a.getLoadPressure()).isEqualTo(250L);  // 25*1000/100
	}
}
