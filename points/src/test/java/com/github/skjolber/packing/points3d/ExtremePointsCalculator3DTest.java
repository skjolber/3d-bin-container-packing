package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.ExtremePointsCalculator3D;
import com.github.skjolber.packing.ep.points3d.ExtremePointsCalculator3D.Obstacle;

public class ExtremePointsCalculator3DTest {

	private final ExtremePointsCalculator3D calculator = new ExtremePointsCalculator3D();

	// -----------------------------------------------------------------------
	// Empty container
	// -----------------------------------------------------------------------

	@Test
	public void testNoObstacles() {
		List<Point> points = calculator.calculate(10, 10, 10, Collections.emptyList());

		assertThat(points).hasSize(1);
		Point p = points.get(0);
		assertThat(p.getMinX()).isEqualTo(0);
		assertThat(p.getMinY()).isEqualTo(0);
		assertThat(p.getMinZ()).isEqualTo(0);
		assertThat(p.getMaxX()).isEqualTo(9);
		assertThat(p.getMaxY()).isEqualTo(9);
		assertThat(p.getMaxZ()).isEqualTo(9);
	}

	// -----------------------------------------------------------------------
	// Single obstacle at origin
	// -----------------------------------------------------------------------

	/**
	 * Container 100x100x100, one 10x10x10 obstacle at (0,0,0).
	 * Expected: 3 extreme points at the three faces of the obstacle.
	 *
	 * <pre>
	 * z
	 * |
	 * |-----|
	 * |     |
	 * |     |
	 * |-----|---- x
	 * </pre>
	 */
	@Test
	public void testSingleObstacleAtOrigin() {
		List<Point> points = calculator.calculate(100, 100, 100,
				List.of(new Obstacle(0, 0, 0, 10, 10, 10)));

		assertThat(points).hasSize(3);

		// sort by (minX, minY, minZ) for deterministic assertions
		points.sort(Point.COMPARATOR_X_THEN_Y_THEN_Z);

		// (0, 0, 10): beyond z-face of obstacle
		Point p0 = points.get(0);
		assertThat(p0.getMinX()).isEqualTo(0);
		assertThat(p0.getMinY()).isEqualTo(0);
		assertThat(p0.getMinZ()).isEqualTo(10);
		assertThat(p0.getMaxX()).isEqualTo(99);
		assertThat(p0.getMaxY()).isEqualTo(99);
		assertThat(p0.getMaxZ()).isEqualTo(99);

		// (0, 10, 0): beyond y-face of obstacle
		Point p1 = points.get(1);
		assertThat(p1.getMinX()).isEqualTo(0);
		assertThat(p1.getMinY()).isEqualTo(10);
		assertThat(p1.getMinZ()).isEqualTo(0);
		assertThat(p1.getMaxX()).isEqualTo(99);
		assertThat(p1.getMaxY()).isEqualTo(99);
		assertThat(p1.getMaxZ()).isEqualTo(99);

		// (10, 0, 0): beyond x-face of obstacle
		Point p2 = points.get(2);
		assertThat(p2.getMinX()).isEqualTo(10);
		assertThat(p2.getMinY()).isEqualTo(0);
		assertThat(p2.getMinZ()).isEqualTo(0);
		assertThat(p2.getMaxX()).isEqualTo(99);
		assertThat(p2.getMaxY()).isEqualTo(99);
		assertThat(p2.getMaxZ()).isEqualTo(99);
	}

	// -----------------------------------------------------------------------
	// Obstacle filling the entire container
	// -----------------------------------------------------------------------

	@Test
	public void testObstacleFillingContainer() {
		List<Point> points = calculator.calculate(10, 10, 10,
				List.of(new Obstacle(0, 0, 0, 10, 10, 10)));

		assertThat(points).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Two obstacles side by side — container fully packed
	// -----------------------------------------------------------------------

	@Test
	public void testContainerFullyPacked() {
		// Two 10x10x10 obstacles fill a 20x10x10 container.
		List<Point> points = calculator.calculate(20, 10, 10, Arrays.asList(
				new Obstacle(0, 0, 0, 10, 10, 10),
				new Obstacle(10, 0, 0, 10, 10, 10)));

		assertThat(points).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Obstacle NOT touching origin — free space on both sides
	// -----------------------------------------------------------------------

	@Test
	public void testObstacleInMiddle() {
		// Container 20x10x10, obstacle at x=[5,9], full y and z.
		// Free space: x=[0,4] and x=[10,19].
		List<Point> points = calculator.calculate(20, 10, 10,
				List.of(new Obstacle(5, 0, 0, 5, 10, 10)));

		assertThat(points).hasSize(2);
		points.sort(Point.COMPARATOR_X_THEN_Y_THEN_Z);

		Point left = points.get(0);
		assertThat(left.getMinX()).isEqualTo(0);
		assertThat(left.getMinY()).isEqualTo(0);
		assertThat(left.getMinZ()).isEqualTo(0);
		assertThat(left.getMaxX()).isEqualTo(4);   // blocked by obstacle at x=5
		assertThat(left.getMaxY()).isEqualTo(9);
		assertThat(left.getMaxZ()).isEqualTo(9);

		Point right = points.get(1);
		assertThat(right.getMinX()).isEqualTo(10);
		assertThat(right.getMinY()).isEqualTo(0);
		assertThat(right.getMinZ()).isEqualTo(0);
		assertThat(right.getMaxX()).isEqualTo(19);
		assertThat(right.getMaxY()).isEqualTo(9);
		assertThat(right.getMaxZ()).isEqualTo(9);
	}

	// -----------------------------------------------------------------------
	// Obstacle not spanning full Y — free space in front and behind
	// -----------------------------------------------------------------------

	@Test
	public void testObstaclePartialY() {
		// Container 10x20x10, obstacle at y=[0,9], full x and z.
		// Free space only at y=10..19.
		List<Point> points = calculator.calculate(10, 20, 10,
				List.of(new Obstacle(0, 0, 0, 10, 10, 10)));

		assertThat(points).hasSize(1);
		Point p = points.get(0);
		assertThat(p.getMinX()).isEqualTo(0);
		assertThat(p.getMinY()).isEqualTo(10);
		assertThat(p.getMinZ()).isEqualTo(0);
		assertThat(p.getMaxX()).isEqualTo(9);
		assertThat(p.getMaxY()).isEqualTo(19);
		assertThat(p.getMaxZ()).isEqualTo(9);
	}

	// -----------------------------------------------------------------------
	// Two obstacles stacked — the second sits on top of the first
	// -----------------------------------------------------------------------

	@Test
	public void testTwoObstaclesStacked() {
		// Container 10x10x20.
		// Obstacle A: z=[0,9], full x and y.
		// Obstacle B: z=[10,19], full x and y.
		// Container fully packed → no free space.
		List<Point> points = calculator.calculate(10, 10, 20, Arrays.asList(
				new Obstacle(0, 0, 0, 10, 10, 10),
				new Obstacle(0, 0, 10, 10, 10, 10)));

		assertThat(points).isEmpty();
	}

	// -----------------------------------------------------------------------
	// Obstacle order does not matter
	// -----------------------------------------------------------------------

	@Test
	public void testObstacleOrderDoesNotMatter() {
		Obstacle a = new Obstacle(0, 0, 0, 10, 10, 10);
		Obstacle b = new Obstacle(0, 10, 0, 10, 10, 10);

		List<Point> result1 = calculator.calculate(100, 100, 100, Arrays.asList(a, b));
		List<Point> result2 = calculator.calculate(100, 100, 100, Arrays.asList(b, a));

		result1.sort(Point.COMPARATOR_X_THEN_Y_THEN_Z);
		result2.sort(Point.COMPARATOR_X_THEN_Y_THEN_Z);

		assertThat(result1).isEqualTo(result2);
	}

	// -----------------------------------------------------------------------
	// Max bounds are constrained by adjacent obstacles
	// -----------------------------------------------------------------------

	@Test
	public void testMaxBoundsConstrainedByObstacle() {
		// Container 30x10x10.
		// Obstacle at x=[0,9]. Point at (10,0,0) should have maxX=29.
		// Second obstacle at x=[20,29] further constrains nothing from (10,0,0)
		// because it starts at x=20 and the point is at x=10 — maxX = 19.
		List<Point> points = calculator.calculate(30, 10, 10, Arrays.asList(
				new Obstacle(0, 0, 0, 10, 10, 10),
				new Obstacle(20, 0, 0, 10, 10, 10)));

		assertThat(points).hasSize(1);
		Point p = points.get(0);
		assertThat(p.getMinX()).isEqualTo(10);
		assertThat(p.getMaxX()).isEqualTo(19);  // blocked by second obstacle at x=20
		assertThat(p.getMaxY()).isEqualTo(9);
		assertThat(p.getMaxZ()).isEqualTo(9);
	}
}
