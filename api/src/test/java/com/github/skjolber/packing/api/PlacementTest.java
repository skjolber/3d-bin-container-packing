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
