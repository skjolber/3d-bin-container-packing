package com.github.skjolber.packing.packer.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.test.assertj.StackPlacementAssert;

/**
 * The max load weight of a placement limits the total weight resting on it, through all levels
 * and all paths of the support graph (as {@code WeightLoadValidator} computes it).
 */
class LoadWeightValidationTest {

	/**
	 * Weight reaching the base through two paths must be summed, not checked per path.
	 *
	 * <pre>
	 *  z |
	 *  4 +---------------------+
	 *    |        U w=4        |   ← U: base would carry 1+1+2+4 = 8 &gt; 6, REJECTED
	 *  3 +---------------------+     (each path alone carries 4+2 = 6)
	 *    |        T w=2        |
	 *  2 +----------+----------+
	 *    |  L w=1   |  R w=1   |
	 *  1 +----------+----------+
	 *    |  X w=10, maxLoadWeight=6 |  carries 1+1+2 = 4
	 *  0 +---------------------+
	 *      0       10         20  x
	 * </pre>
	 */
	@Test
	void sumsWeightOverConvergingPaths() {
		Stack stack = new Stack();
		Placement x = placement(box(20, 10, 10, 6), 0, 0, 0, 0);
		Placement l = placement(box(10, 10, 1, -1), 0, 0, 1, 1);
		Placement r = placement(box(10, 10, 1, -1), 10, 0, 1, 2);
		Placement t = placement(box(20, 10, 2, -1), 0, 0, 2, 3);

		WeightLoadAwarePlacementUtility utility = new WeightLoadAwarePlacementUtility(stack);
		utility.initialize(8);
		accept(utility, stack, x);
		accept(utility, stack, l);
		accept(utility, stack, r);
		accept(utility, stack, t);
		StackPlacementAssert.assertThat(x).hasLoadWeight(4.0);

		assertThat(supportedArea(utility, box(20, 10, 4, -1).getStackValue(0), 0, 0, 3)).isEqualTo(-1L);
		assertThat(supportedArea(utility, box(20, 10, 2, -1).getStackValue(0), 0, 0, 3)).isEqualTo(200L);
	}

	/**
	 * Weight resting on the boxes above counts too, not only their own weight.
	 *
	 * <pre>
	 *  z |
	 *  4 +----------+
	 *    |  D w=2   |   ← D: A would carry 2+2+2 = 6 &gt; 5, REJECTED
	 *  3 +----------+
	 *    |  C w=2   |
	 *  2 +----------+
	 *    |  B w=2   |
	 *  1 +----------+
	 *    |  A w=10  |   maxLoadWeight = 5, carries 2+2 = 4
	 *  0 +----------+
	 *      0       10  x
	 * </pre>
	 */
	@Test
	void countsLoadFromAllLevelsAbove() {
		Stack stack = new Stack();
		Placement a = placement(box(10, 10, 10, 5), 0, 0, 0, 0);
		Placement b = placement(box(10, 10, 2, -1), 0, 0, 1, 1);
		Placement c = placement(box(10, 10, 2, -1), 0, 0, 2, 2);

		WeightLoadAwarePlacementUtility utility = new WeightLoadAwarePlacementUtility(stack);
		utility.initialize(8);
		accept(utility, stack, a);
		accept(utility, stack, b);
		accept(utility, stack, c);
		StackPlacementAssert.assertThat(a).hasLoadWeight(4.0);

		assertThat(supportedArea(utility, box(10, 10, 2, -1).getStackValue(0), 0, 0, 3)).isEqualTo(-1L);
		assertThat(supportedArea(utility, box(10, 10, 1, -1).getStackValue(0), 0, 0, 3)).isEqualTo(100L);
	}

	private static long supportedArea(WeightLoadAwarePlacementUtility utility, BoxStackValue stackValue, int x, int y, int z) {
		Point point = new DefaultPoint3D(x, y, z, x + stackValue.getDx() - 1, y + stackValue.getDy() - 1, z + stackValue.getDz() - 1);
		utility.populatePointSupporters(point);
		utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
		return utility.getSupportedAreaAtPoint(point, stackValue, false);
	}

	private static void accept(WeightLoadAwarePlacementUtility utility, Stack stack, Placement placement) {
		stack.add(placement);
		utility.accepted(placement);
	}

	private static Box box(int dx, int dy, int weight, long maxLoadWeight) {
		Box.Builder builder = Box.newBuilder().withSize(dx, dy, 1).withWeight(weight);
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
