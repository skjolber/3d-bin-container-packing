package com.github.skjolber.packing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Placement} load tracking: supporters / supportees relationships
 * and weight propagation across multiple levels; and the insertion order rules (see {@link ContainerAccess}).
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
		assertThat(c.getLoadWeight()).isEqualTo(0.0);
		assertThat(b.getLoadWeight()).isEqualTo(5.0);   // C's weight
		assertThat(a.getLoadWeight()).isEqualTo(15.0);  // B.weight + C.weight
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
		assertThat(d.getLoadWeight()).isEqualTo(0.0);
		assertThat(c.getLoadWeight()).isEqualTo(5.0);   // D.weight
		assertThat(b.getLoadWeight()).isEqualTo(15.0);  // C.weight + D.weight
		assertThat(a.getLoadWeight()).isEqualTo(35.0);  // B.weight + C.weight + D.weight
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

		assertThat(c.getLoadWeight()).isEqualTo(0.0);
		assertThat(a.getLoadWeight()).isEqualTo(5.0);
		assertThat(b.getLoadWeight()).isEqualTo(5.0);
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

		assertThat(d.getLoadWeight()).isEqualTo(0.0);
		assertThat(c.getLoadWeight()).isEqualTo(5.0);   // half of D
		assertThat(b.getLoadWeight()).isEqualTo(5.0);   // half of D
		// A bears B's own weight (20) + B's load share propagated from D (5)
		assertThat(a.getLoadWeight()).isEqualTo(25.0);
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

		assertThat(c.getLoadWeight()).isEqualTo(0.0);
		assertThat(a.getLoadWeight()).isEqualTo(75.0);
		assertThat(b.getLoadWeight()).isEqualTo(25.0);
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
		assertThat(a.getLoadWeight()).isEqualTo(15.0);
		assertThat(b.getLoadWeight()).isEqualTo(5.0);

		b.removeLoad(c);

		// C is no longer linked
		assertThat(b.getSupportees()).isEmpty();
		assertThat(c.getSupporters()).isEmpty();

		// Load has propagated back down
		assertThat(b.getLoadWeight()).isEqualTo(0.0);
		assertThat(a.getLoadWeight()).isEqualTo(10.0);
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
		assertThat(a.getLoadWeight()).isEqualTo(35.0);
		assertThat(b.getLoadWeight()).isEqualTo(15.0);
		assertThat(c.getLoadWeight()).isEqualTo(5.0);

		c.removeLoad(d);

		// D is unlinked
		assertThat(c.getSupportees()).isEmpty();
		assertThat(d.getSupporters()).isEmpty();

		// Load reduced at all three levels below D
		assertThat(c.getLoadWeight()).isEqualTo(0.0);
		assertThat(b.getLoadWeight()).isEqualTo(10.0);
		assertThat(a.getLoadWeight()).isEqualTo(30.0);
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

		assertThat(a.getLoadWeight()).isEqualTo(5.0);
		assertThat(b.getLoadWeight()).isEqualTo(5.0);

		a.removeLoad(c);

		// A is no longer a supporter of C
		assertThat(a.getSupportees()).isEmpty();
		assertThat(a.getLoadWeight()).isEqualTo(0.0);

		// removeSupporter propagates the removal through all remaining supporters of C,
		// so B's load share is also unwound
		assertThat(b.getLoadWeight()).isEqualTo(0.0);
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
		assertThat(b.getLoadWeight()).isEqualTo(0.0);
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
		assertThat(a.getLoadWeight()).isEqualTo(20.0);
	}

	@Test
	public void testFractionalLoadPropagation() {
		Placement left = makePlacement("left", 1, 1, 1, 1, 0, 0, 0);
		Placement right = makePlacement("right", 2, 1, 1, 1, 1, 0, 0);
		Placement middle = makePlacement("middle", 3, 1, 1, 1, 0, 0, 1);
		Placement top = makePlacement("top", 3, 1, 1, 1, 0, 0, 2);

		left.addLoad(middle, 1L, 1.0 / 3.0);
		right.addLoad(middle, 2L, 2.0 / 3.0);
		middle.addLoad(top, 3L, top.getWeight());

		assertThat(middle.getLoadWeight()).isEqualTo(1.0);
		assertThat(left.getLoadWeight()).isEqualTo(2.0 / 3.0);
		assertThat(right.getLoadWeight()).isEqualTo(4.0 / 3.0);
	}

	/**
	 * <pre>
	 *  y
	 *  |
	 *  15 |       +------+  other (5..14, 5..14)
	 *     |  +----+--+   |
	 *     |  |    |xx|   |  xx: overlap 5x5
	 *  10 |  |    +--+---+
	 *     |  |       |       this (0..9, 0..9)
	 *   0 +--+-------+------ x
	 * </pre>
	 */
	@Test
	public void testOverlapArea2D() {
		Placement placement = makePlacement("A", 10, 10, 1, 1, 0, 0, 0);

		assertThat(placement.overlapArea2D(5, 14, 5, 14)).isEqualTo(25L);
		assertThat(placement.overlapArea2D(0, 9, 0, 9)).isEqualTo(100L);
		assertThat(placement.overlapArea2D(10, 19, 0, 9)).isZero();
		assertThat(placement.overlapArea2D(0, 9, 10, 19)).isZero();
		assertThat(placement.overlapArea2D(makePlacement("B", 10, 10, 1, 1, 5, 5, 1))).isEqualTo(25L);
	}

	/** Areas beyond the int range, as with fine-grained units (here 1/10000 inch). */
	@Test
	public void testOverlapArea2DBeyondIntRange() {
		Placement placement = makePlacement("A", 200000, 80000, 1, 1, 0, 0, 0);

		assertThat(placement.overlapArea2D(0, 199999, 0, 79999)).isEqualTo(16_000_000_000L);
	}

	// -----------------------------------------------------------------------
	// insertion order
	// -----------------------------------------------------------------------

	/**
	 * <pre>
	 *  z
	 *  2 +-------+
	 *    |   B   |   B rests on A: A is inserted first, whatever the access;
	 *  1 +---+---+   C only touches A at the side
	 *    | A | C |
	 *  0 +---+---+
	 *    0   1   2  x
	 * </pre>
	 */
	@Test
	public void boxIsInsertedAfterTheBoxesItRestsOn() {
		Placement a = makePlacement("A", 1, 1, 1, 1, 0, 0, 0);
		Placement b = makePlacement("B", 2, 1, 1, 1, 0, 0, 1);
		Placement c = makePlacement("C", 1, 1, 1, 1, 1, 0, 0);

		assertThat(b.restsOn(a)).isTrue();
		assertThat(a.restsOn(b)).isFalse();
		assertThat(c.restsOn(a)).isFalse();
		for (ContainerAccess access : ContainerAccess.values()) {
			assertThat(a.mustPrecede(b, access)).isTrue();
			assertThat(b.mustPrecede(a, access)).isFalse();
		}
		assertThat(a.mustPrecede(c, ContainerAccess.ANY)).isFalse();
		assertThat(c.mustPrecede(a, ContainerAccess.ANY)).isFalse();
	}

	/**
	 * <pre>
	 *  z
	 *  3 +---+
	 *    | B |       from the top, B is above A (with a gap): A is inserted first
	 *  2 +---+
	 *
	 *  1 +---+
	 *    | A |
	 *  0 +---+
	 *    0   1  x
	 * </pre>
	 */
	@Test
	public void fromTheTopBoxIsInsertedBeforeTheBoxesAboveIt() {
		Placement a = makePlacement("A", 1, 1, 1, 1, 0, 0, 0);
		Placement b = makePlacement("B", 1, 1, 1, 1, 0, 0, 2);

		assertThat(a.isBlockedBy(b, ContainerAccess.TOP)).isTrue();
		assertThat(a.mustPrecede(b, ContainerAccess.TOP)).isTrue();
		assertThat(b.mustPrecede(a, ContainerAccess.TOP)).isFalse();
		// without access restrictions, or through a door, the order is free
		assertThat(a.mustPrecede(b, ContainerAccess.ANY)).isFalse();
		assertThat(a.mustPrecede(b, ContainerAccess.FRONT)).isFalse();
		assertThat(b.mustPrecede(a, ContainerAccess.FRONT)).isFalse();
	}

	/**
	 * <pre>
	 *  z
	 *  1 +---+   +---+
	 *    | A |   | B |   door ->   A is inserted first, B is between it and the door
	 *  0 +---+   +---+
	 *    0   1   2   3  x
	 * </pre>
	 */
	@Test
	public void throughADoorBoxIsInsertedBeforeTheBoxesBetweenItAndTheDoor() {
		Placement a = makePlacement("A", 1, 1, 1, 1, 0, 0, 0);
		Placement b = makePlacement("B", 1, 1, 1, 1, 2, 0, 0);

		assertThat(a.isBlockedBy(b, ContainerAccess.FRONT)).isTrue();
		assertThat(a.mustPrecede(b, ContainerAccess.FRONT)).isTrue();
		assertThat(b.mustPrecede(a, ContainerAccess.FRONT)).isFalse();
		assertThat(a.mustPrecede(b, ContainerAccess.TOP)).isFalse();
		assertThat(b.mustPrecede(a, ContainerAccess.TOP)).isFalse();
	}

	/**
	 * The variants for a box at given coordinates, which is not placed yet, agree with the variants for placements.
	 */
	@Test
	public void coordinateVariantsAgreeWithPlacements() {
		Random random = new Random(1);
		int precede = 0;
		for (int i = 0; i < 20_000; i++) {
			Placement a = makePlacement("A", 1 + random.nextInt(4), 1 + random.nextInt(4), 1 + random.nextInt(4), 1, random.nextInt(8), random.nextInt(8), random.nextInt(8));
			Placement b = makePlacement("B", 1 + random.nextInt(4), 1 + random.nextInt(4), 1 + random.nextInt(4), 1, random.nextInt(8), random.nextInt(8), random.nextInt(8));
			int x = b.getAbsoluteX();
			int y = b.getAbsoluteY();
			int z = b.getAbsoluteZ();
			int endX = b.getAbsoluteEndX();
			int endY = b.getAbsoluteEndY();
			int endZ = b.getAbsoluteEndZ();
			for (ContainerAccess access : ContainerAccess.values()) {
				assertThat(a.mustPrecede(x, y, z, endX, endY, endZ, access)).isEqualTo(a.mustPrecede(b, access));
				assertThat(a.mustFollow(x, y, z, endX, endY, endZ, access)).isEqualTo(b.mustPrecede(a, access));
				if(a.mustPrecede(b, access)) {
					precede++;
				}
			}
		}
		assertThat(precede).isGreaterThan(1000);
	}

	// -----------------------------------------------------------------------
	// intersection
	// -----------------------------------------------------------------------

	private static Placement placementAt(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(new BoxStackValue(dx, dy, dz, null, 0), 0, x, y, z);
	}

	/**
	 * A spans B on the x axis while B spans A on the y axis; they share the z range.
	 *
	 * <pre>
	 *  y
	 *  5 |    +---+
	 *  4 |    | B |
	 *  3 +----+---+----+
	 *    |    A xx     |   A: x 0..9, y 3..4;  B: x 4..5, y 0..7
	 *  2 +----+---+----+
	 *  1      |   |
	 *  0      +---+
	 *    0    4   6    10  x
	 * </pre>
	 */
	@Test
	public void testIntersects_crossSpan() {
		Placement a = placementAt(10, 2, 1, 0, 3, 0);
		Placement b = placementAt(2, 8, 1, 4, 0, 0);

		assertThat(a.intersectsX(b)).isTrue();
		assertThat(b.intersectsX(a)).isTrue();
		assertThat(a.intersectsY(b)).isTrue();
		assertThat(b.intersectsY(a)).isTrue();
		assertThat(a.intersectsZ(b)).isTrue();
		assertThat(b.intersectsZ(a)).isTrue();

		assertThat(a.intersects(b)).isTrue();
		assertThat(b.intersects(a)).isTrue();
		assertThat(a.intersects3D(b)).isTrue();
	}

	/**
	 * B is entirely inside A.
	 *
	 * <pre>
	 *  y
	 *  9 +-----------+
	 *    |     B     |
	 *  5 |   +---+   |   A: x 0..9, y 0..9, z 0..9;  B: x 3..5, y 3..5, z 3..5
	 *  3 |   +---+   |
	 *    |           |
	 *  0 +-----------+
	 *    0   3   5   9  x
	 * </pre>
	 */
	@Test
	public void testIntersects_contained() {
		Placement a = placementAt(10, 10, 10, 0, 0, 0);
		Placement b = placementAt(3, 3, 3, 3, 3, 3);

		assertThat(a.intersects(b)).isTrue();
		assertThat(b.intersects(a)).isTrue();
		assertThat(a.intersectsX(b)).isTrue();
		assertThat(b.intersectsX(a)).isTrue();
		assertThat(a.intersectsY(b)).isTrue();
		assertThat(b.intersectsY(a)).isTrue();
		assertThat(a.intersectsZ(b)).isTrue();
		assertThat(b.intersectsZ(a)).isTrue();
	}

	/**
	 * Boxes which touch at an edge do not intersect: coordinates are inclusive.
	 *
	 * <pre>
	 *  z
	 *  1 +---+---+
	 *    | A | B |   A: x 0..4;  B: x 5..9
	 *  0 +---+---+
	 *    0   5   10  x
	 * </pre>
	 */
	@Test
	public void testIntersects_adjacentAndSeparate() {
		Placement a = placementAt(5, 5, 1, 0, 0, 0);
		Placement adjacent = placementAt(5, 5, 1, 5, 0, 0);
		Placement above = placementAt(5, 5, 1, 0, 0, 1);
		Placement far = placementAt(5, 5, 1, 20, 20, 20);

		assertThat(a.intersectsX(adjacent)).isFalse();
		assertThat(adjacent.intersectsX(a)).isFalse();
		assertThat(a.intersects(adjacent)).isFalse();
		assertThat(adjacent.intersects(a)).isFalse();

		assertThat(a.intersectsZ(above)).isFalse();
		assertThat(above.intersectsZ(a)).isFalse();
		assertThat(a.intersects(above)).isFalse();
		assertThat(above.intersects(a)).isFalse();

		assertThat(a.intersectsX(far)).isFalse();
		assertThat(a.intersectsY(far)).isFalse();
		assertThat(a.intersectsZ(far)).isFalse();
		assertThat(a.intersects(far)).isFalse();
		assertThat(far.intersects(a)).isFalse();
	}

	/** Overlapping on two of the three axes is not an intersection. */
	@Test
	public void testIntersects_requiresAllAxes() {
		Placement a = placementAt(10, 10, 10, 0, 0, 0);
		Placement b = placementAt(10, 10, 10, 5, 5, 10);

		assertThat(a.intersectsX(b)).isTrue();
		assertThat(a.intersectsY(b)).isTrue();
		assertThat(a.intersectsZ(b)).isFalse();
		assertThat(a.intersects(b)).isFalse();
		assertThat(b.intersects(a)).isFalse();
	}

	@Test
	public void testIntersects_agreesWithIntersects3D() {
		Random random = new Random(1);
		int intersecting = 0;
		for (int i = 0; i < 20_000; i++) {
			Placement a = placementAt(1 + random.nextInt(8), 1 + random.nextInt(8), 1 + random.nextInt(8), random.nextInt(10), random.nextInt(10), random.nextInt(10));
			Placement b = placementAt(1 + random.nextInt(8), 1 + random.nextInt(8), 1 + random.nextInt(8), random.nextInt(10), random.nextInt(10), random.nextInt(10));

			assertThat(a.intersects(b)).isEqualTo(a.intersects3D(b));
			assertThat(a.intersects(b)).isEqualTo(b.intersects(a));
			if (a.intersects(b)) {
				intersecting++;
			}
		}
		assertThat(intersecting).isGreaterThan(1000);
	}
}
