package com.github.skjolber.packing.packer.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

/**
 * The full-support fallback of the load-aware controls: when a box is not fully supported at the
 * origin of a point, inner positions on top of a single supporting placement are tried.
 */
class LoadFullSupportFallbackTest {

	// prefers lower x
	private static final PlacementComparator LOWER_X = (a, b) -> Integer.compare(b.getAbsoluteX(), a.getAbsoluteX());

	/**
	 * <pre>
	 *  y (top view, z=2)
	 *  19 +--------------------------+  point (0,0,2)-(19,19)
	 *     |                          |
	 *  15 |       +----------+       |
	 *     |       |  F (top  |       |
	 *     |       |  at z=1) |       |
	 *   6 |  +--+ +----------+       |
	 *     |  |B |  ← B at the point origin is unsupported;
	 *   0 +--+--+------------------- x  the fallback places it at (6,6) on F
	 *     0     6           15      19
	 * </pre>
	 */
	@Test
	void placesBoxOnInnerSupporter() {
		Stack stack = new Stack();
		Placement floor = accept(stack, placement(box(10, 10, 2, 1, -1), 6, 6, 0, 0));
		TestUtility utility = utility(stack);
		DefaultPoint3D point = point(0, 0, 2, 19, 19, 19);
		BoxStackValue box = box(4, 4, 1, 1, -1).getStackValue(0);

		assertThat(utility.getSupportedAreaAtPoint(point, box, true)).isEqualTo(-1L);

		Placement placement = utility.findPlacementAtPointSupporters(point, box, LOWER_X);

		assertThat(placement).isNotNull();
		StackPlacementAssert.assertThat(placement).isAt(6, 6, 2).hasSupportedArea(16);
		assertThat(floor.getSupportees()).isEmpty();
	}

	/** A supporter which cannot carry the whole box gives no placement. */
	@Test
	void rejectsPartialSupport() {
		Stack stack = new Stack();
		accept(stack, placement(box(3, 3, 2, 1, -1), 6, 6, 0, 0));
		TestUtility utility = utility(stack);

		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 2, 19, 19, 19), box(4, 4, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNull();
	}

	/** A fully supported inner position which would overload the supporter gives no placement. */
	@Test
	void rejectsOverload() {
		Stack stack = new Stack();
		accept(stack, placement(box(10, 10, 2, 1, 2), 6, 6, 0, 0));
		TestUtility utility = utility(stack);
		DefaultPoint3D point = point(0, 0, 2, 19, 19, 19);

		assertThat(utility.findPlacementAtPointSupporters(point, box(4, 4, 1, 3, -1).getStackValue(0), LOWER_X)).isNull();
		assertThat(utility.findPlacementAtPointSupporters(point, box(4, 4, 1, 2, -1).getStackValue(0), LOWER_X)).isNotNull();
	}

	/** A supporter with its top at another height is not used. */
	@Test
	void ignoresSupporterAtOtherHeight() {
		Stack stack = new Stack();
		accept(stack, placement(box(10, 10, 3, 1, -1), 6, 6, 0, 0));
		TestUtility utility = utility(stack);

		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 2, 19, 19, 19), box(4, 4, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNull();
	}

	/**
	 * Of two supporters, the comparator decides.
	 *
	 * <pre>
	 *  x: 0    4 5    9 10   14 15  19
	 *        |  L   |      |  R   |      both with top at z=1
	 * </pre>
	 */
	@Test
	void comparatorSelectsBetweenSupporters() {
		Stack stack = new Stack();
		accept(stack, placement(box(5, 10, 2, 1, -1), 15, 5, 0, 0));
		accept(stack, placement(box(5, 10, 2, 1, -1), 5, 5, 0, 1));
		TestUtility utility = utility(stack);
		DefaultPoint3D point = point(0, 0, 2, 19, 19, 19);
		BoxStackValue box = box(4, 4, 1, 1, -1).getStackValue(0);

		StackPlacementAssert.assertThat(utility.findPlacementAtPointSupporters(point, box, LOWER_X)).isAt(5, 5, 2);
		StackPlacementAssert.assertThat(utility.findPlacementAtPointSupporters(point, box, (a, b) -> LOWER_X.compare(b, a))).isAt(15, 5, 2);
	}

	/** At the point origin, support is required to be full only when asked for. */
	@Test
	void placementAtPointWithPartialSupport() {
		Stack stack = new Stack();
		accept(stack, placement(box(2, 4, 2, 1, -1), 0, 0, 0, 0));
		TestUtility utility = utility(stack);
		DefaultPoint3D point = point(0, 0, 2, 19, 19, 19);
		BoxStackValue box = box(4, 4, 1, 1, -1).getStackValue(0);

		assertThat(utility.getPlacementAtPoint(point, box, true)).isNull();

		Placement placement = utility.getPlacementAtPoint(point, box, false);
		StackPlacementAssert.assertThat(placement).isAt(0, 0, 2).hasSupportedArea(8);
	}

	/** On the floor, a box is always fully supported. */
	@Test
	void placementAtFloorPoint() {
		TestUtility utility = utility(new Stack());
		BoxStackValue box = box(4, 4, 1, 1, -1).getStackValue(0);

		StackPlacementAssert.assertThat(utility.getPlacementAtPoint(point(0, 0, 0, 19, 19, 19), box, true)).isAt(0, 0, 0).hasSupportedArea(16);
	}

	/** After a placement is validated, its load is added to the supporters by contact area. */
	@Test
	void addsSupportersLoad() {
		Stack stack = new Stack();
		Placement left = accept(stack, placement(box(2, 4, 2, 1, -1), 0, 0, 0, 0));
		Placement right = accept(stack, placement(box(2, 4, 2, 1, -1), 2, 0, 0, 1));
		TestUtility utility = utility(stack);
		DefaultPoint3D point = point(0, 0, 2, 19, 19, 19);

		Placement placement = utility.getPlacementAtPoint(point, box(4, 4, 1, 6, -1).getStackValue(0), true);
		placement.setIndex(2);
		utility.addSupportersLoad(placement);

		StackPlacementAssert.assertThat(placement).isSupportedBy(left, right);
		StackPlacementAssert.assertThat(left).hasLoadWeight(3.0);
		StackPlacementAssert.assertThat(right).hasLoadWeight(3.0);
	}

	/**
	 * Boundary: the box fits exactly between the supporter's start and the point's end.
	 *
	 * <pre>
	 *  x: 0 1 2 . . . . . . 9
	 *     point (0..9)
	 *         | F (2..9), top at z=1 |
	 *         | box 8 wide at 2..9   |
	 * </pre>
	 */
	@Test
	void placesBoxEndingAtPointEnd() {
		Stack stack = new Stack();
		accept(stack, placement(box(8, 10, 2, 1, -1), 2, 0, 0, 0));
		TestUtility utility = utility(stack);

		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 2, 9, 19, 19), box(8, 4, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNotNull();
		StackPlacementAssert.assertThat(placement).isAt(2, 0, 2);
	}

	/** Boundary: the box fits exactly between the supporter's start and the point's end, in y. */
	@Test
	void placesBoxEndingAtPointEndInY() {
		Stack stack = new Stack();
		accept(stack, placement(box(10, 8, 2, 1, -1), 0, 2, 0, 0));
		TestUtility utility = utility(stack);

		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 2, 19, 9, 19), box(4, 8, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNotNull();
		StackPlacementAssert.assertThat(placement).isAt(0, 2, 2);
	}

	/** Beyond the boundary: the box would extend past the point's end. */
	@Test
	void rejectsBoxBeyondPointEnd() {
		Stack stack = new Stack();
		accept(stack, placement(box(8, 10, 2, 1, -1), 3, 0, 0, 0));
		TestUtility utility = utility(stack);

		// the supporter starts at x=3, so an 8-wide box would end at x=10, past the point's end (9)
		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 2, 9, 19, 19), box(8, 4, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNull();
	}

	/** Boundary: a box resting on a floor box of height 1 (the point is at z=1). */
	@Test
	void placesBoxOnSupporterOfHeightOne() {
		Stack stack = new Stack();
		accept(stack, placement(box(10, 10, 1, 1, -1), 6, 6, 0, 0));
		TestUtility utility = utility(stack);

		Placement placement = utility.findPlacementAtPointSupporters(point(0, 0, 1, 19, 19, 19), box(4, 4, 1, 1, -1).getStackValue(0), LOWER_X);

		assertThat(placement).isNotNull();
		StackPlacementAssert.assertThat(placement).isAt(6, 6, 1);
	}

	private static Placement accept(Stack stack, Placement placement) {
		stack.add(placement);
		return placement;
	}

	private static TestUtility utility(Stack stack) {
		TestUtility utility = new TestUtility(stack);
		utility.initialize(8);
		for(Placement placement : stack.getPlacements()) {
			utility.accepted(placement);
		}
		return utility;
	}

	/** Populates the point lists before the calculations, as the controls do. */
	private static class TestUtility extends WeightLoadAwarePlacementUtility {
		TestUtility(Stack stack) {
			super(stack);
		}

		@Override
		public long getSupportedAreaAtPoint(com.github.skjolber.packing.api.point.Point point, BoxStackValue sv, boolean fullSupport) {
			populate(point, sv);
			return super.getSupportedAreaAtPoint(point, sv, fullSupport);
		}

		@Override
		public Placement findPlacementAtPointSupporters(com.github.skjolber.packing.api.point.Point point3d, BoxStackValue stackValue, PlacementComparator comparator) {
			populate(point3d, stackValue);
			return super.findPlacementAtPointSupporters(point3d, stackValue, comparator);
		}

		private void populate(com.github.skjolber.packing.api.point.Point point, BoxStackValue sv) {
			populatePointSupporters(point);
			populatePointSupportees(point, sv.getDz(), sv.getDz());
		}
	}

	private static DefaultPoint3D point(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static Box box(int dx, int dy, int dz, int weight, long maxLoadWeight) {
		Box.Builder builder = Box.newBuilder().withSize(dx, dy, dz).withWeight(weight);
		if(maxLoadWeight != -1) {
			builder.withMaxLoadWeight(maxLoadWeight);
		}
		return builder.build();
	}

	private static Placement placement(Box box, int x, int y, int z, int index) {
		Placement placement = new Placement(box.getStackValue(0), 0, x, y, z);
		placement.setIndex(index);
		return placement;
	}
}
