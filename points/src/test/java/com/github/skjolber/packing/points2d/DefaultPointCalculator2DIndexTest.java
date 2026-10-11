package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

public class DefaultPointCalculator2DIndexTest {

	/*
	 *  y
	 *  9 +---+-----------+
	 *    | s |     L     |      s: small point (area 2), L: large point
	 *    +---+           |
	 *    0   2           11 x
	 *
	 * Raising the minimum area removes s; L moves to index 0 and must say so,
	 * because packagers pass point indexes back to add(..).
	 */
	@Test
	public void filteringUpdatesPointIndexes() {
		DefaultPointCalculator2D calculator = new DefaultPointCalculator2D(false, 4);
		calculator.clearToSize(12, 10, 1);
		calculator.setPoints(List.of(new DefaultPoint3D(0, 0, 0, 1, 0, 0), new DefaultPoint3D(2, 0, 0, 11, 9, 0)));
		calculator.clear();
		assertThat(calculator.size()).isEqualTo(2);

		calculator.setMinimumAreaLimit(4);

		assertThat(calculator.size()).isEqualTo(1);
		Point large = calculator.get(0);
		assertThat(large.getIndex()).isEqualTo(0);

		Placement placement = new Placement(Box.newBuilder()
				.withSize(2, 2, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, large.getMinX(), large.getMinY(), 0, false);
		calculator.add(large.getIndex(), placement);
		assertThat(calculator.getPlacements()).containsExactly(placement);
	}
}
