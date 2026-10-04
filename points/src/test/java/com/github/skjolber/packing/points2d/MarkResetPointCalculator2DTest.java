package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.MarkResetPointCalculator2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

public class MarkResetPointCalculator2DTest {

	@Test
	public void testMarkReset() {
		MarkResetPointCalculator2D ep = new MarkResetPointCalculator2D(false, 16);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		assertThat(ep.getAll()).hasSize(2);

		SimplePoint2D point1 = ep.get(0);
		ep.add(0, createStackPlacement(point1.getMinX(), point1.getMinY(), point1.getMinZ(), point1.getMinX() + 9, point1.getMinY() + 9, point1.getMinZ() + 9));

		ep.mark();
		
		SimplePoint2D point2 = ep.get(0);
		ep.add(0, createStackPlacement(point2.getMinX(), point2.getMinY(), point2.getMinZ(), point2.getMinX() + 9, point2.getMinY() + 9, point2.getMinZ() + 9));
		
		assertThat(ep.getPlacements()).hasSize(3);
		
		ep.reset();
		
		assertThat(ep.getPlacements()).hasSize(2);
	}
	
	/**
	 * In mutable mode, points are constrained in place: the mark must not share them.
	 *
	 * <pre>
	 *  y
	 *  |
	 *  20 |    +----+
	 *     |    | B  |    ← B constrains the point at (0, 10) to maxX 9
	 *  10 +----+    |
	 *     | A  |    |
	 *   0 +----+----+---- x
	 *     0    10   20
	 * </pre>
	 */
	@Test
	public void testResetRestoresPointsConstrainedInPlace() {
		MarkResetPointCalculator2D ep = new MarkResetPointCalculator2D(false, 16);
		ep.clearToSize(100, 100, 100);
		ep.add(0, createStackPlacement(0, 0, 0, 9, 9, 9));

		ep.mark();
		List<String> marked = geometry(ep);

		int index = ep.findPoint(10, 0);
		ep.add(index, createStackPlacement(10, 0, 0, 19, 19, 9));
		assertThat(geometry(ep)).isNotEqualTo(marked);

		ep.reset();

		assertThat(geometry(ep)).isEqualTo(marked);
		assertThat(ep.getPlacements()).hasSize(1);
	}

	@Test
	public void testResetWithoutMarkGivesNoPlacements() {
		MarkResetPointCalculator2D ep = new MarkResetPointCalculator2D(false, 16);
		ep.clearToSize(100, 100, 100);

		ep.reset();

		assertThat(ep.getPlacements()).isEmpty();
	}

	private static List<String> geometry(MarkResetPointCalculator2D ep) {
		List<String> geometry = new ArrayList<>();
		for(int i = 0; i < ep.size(); i++) {
			SimplePoint2D point = ep.get(i);
			geometry.add(point.getMinX() + "," + point.getMinY() + "-" + point.getMaxX() + "," + point.getMaxY());
		}
		return geometry;
	}

	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, endZ + 1 - z, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, endZ + 1 - z).withWeight(1).build();
		stackValue.setBox(box);
		
		new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint3D(x, y, z, 0, 0, 0));
	}
	
}
