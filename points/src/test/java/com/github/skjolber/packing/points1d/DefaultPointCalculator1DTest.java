package com.github.skjolber.packing.points1d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points1d.DefaultPointCalculator1D;
import com.github.skjolber.packing.ep.points1d.Point1D;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;

public class DefaultPointCalculator1DTest {

	private Placement createStackPlacement(int x, int y, int endX, int endY) {
		return createStackPlacement(x, y, 0, endX, endY, 0);
	}
	
	private Placement createStackPlacement(int x, int y, int z, int endX, int endY, int endZ) {
		BoxStackValue stackValue = new BoxStackValue(endX + 1 - x, endY + 1 - y, 1, null, -1);
		
		Box box = Box.newBuilder().withSize(endX + 1 - x, endY + 1 - y, 1).withWeight(0).build();
		stackValue.setBox(box);
		
		BoxItem boxItem = new BoxItem(box, 1);
		
		return new Placement(stackValue, new DefaultPoint2D(x, y, z, 0, 0, 0));
	}
	
	@Test
	public void testSinglePoint() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 10, 10));
		assertThat(ep.getAll()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(11);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);
	}

	@Test
	public void testSinglePointCornerCase() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 0, 0));
		assertThat(ep.getAll()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(1);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);
	}

	@Test
	public void testSinglePointCoveringAllX() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 99, 10));
		assertThat(ep.getAll()).hasSize(0);
	}

	@Test
	public void testSinglePointCoveringWholeContainer() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 99, 99));
		assertThat(ep.getAll()).hasSize(0);
	}

	@Test
	public void testStackInXDirection() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 0);
		ep.add(0, createStackPlacement(0, 0, 9, 49));

		assertThat(ep.get(0).getMinX()).isEqualTo(10);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);

		ep.add(1, createStackPlacement(10, 0, 19, 24));

		assertThat(ep.getAll()).hasSize(1);

		assertThat(ep.get(0).getMinX()).isEqualTo(20);
		assertThat(ep.get(0).getMinY()).isEqualTo(0);
	}
	
	@Test
	public void testPointsX() {
		DefaultPointCalculator1D ep = new DefaultPointCalculator1D(16, Dimension.X);
		ep.clearToSize(100, 100, 100);
		
		//
		// y
		// |
		// |------------------------|
		// |                        |
		// |        free            |
		// |                        |
		// |------------------------|
		// |                        |
		// |          used          |
		// |                        |
		// |------------------------|---- x
		//
		
		ep.setPoints(Arrays.asList(new Point1D(0, 49, 0, 99, 99, 99)));
		
		//
		// y
		// |
		// |----|-------------------|
		// |    |                   |
		// | A  |       free        |
		// |    |                   |
		// |----|-------------------|
		// |                        |
		// |                        |
		// |                        |
		// |------------------------|---- x
		//
		
		ep.add(0, createStackPlacement(0, 50, 0, 9, 59, 10)); // A
		assertThat(ep.getAll()).hasSize(1);
		
		assertThat(ep.get(0).getMinX()).isEqualTo(10);
		assertThat(ep.get(0).getMinY()).isEqualTo(49);
	}

}
