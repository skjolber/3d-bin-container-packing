package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;

public class DefaultPoint2DTest {

	/*
	 *  y
	 *  9 +-------------------+
	 *    |                   |      free point (0,0) - (19,9), no support
	 *    |   +---+           |
	 *    |   | S |           |      S: support placement
	 *    +---+---+-----------+
	 *    0                   19 x
	 *
	 * Moving the point keeps its own maximum x and y, whichever support is added.
	 */
	@Test
	public void moveWithSupportKeepsExtent() {
		DefaultPoint2D point = new DefaultPoint2D(0, 0, 0, 19, 9, 0);
		Placement support = new Placement(Box.newBuilder()
				.withSize(2, 2, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, 4, 0, 0, false);

		SimplePoint2D movedX = point.moveX(6, support);
		assertThat(movedX.getMinX()).isEqualTo(6);
		assertThat(movedX.getMaxX()).isEqualTo(19);
		assertThat(movedX.getMaxY()).isEqualTo(9);

		SimplePoint2D movedY = point.moveY(2, support);
		assertThat(movedY.getMinY()).isEqualTo(2);
		assertThat(movedY.getMaxX()).isEqualTo(19);
		assertThat(movedY.getMaxY()).isEqualTo(9);
	}
}
