package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static com.github.skjolber.packing.test.ascii.TestPlacements.point;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

/**
 * Points, inclusive coordinates and the overview. A point is drawn at its minimum corner, in the free corner which the point starts in: one cell to the right
 * and one line up from the corner cell. If that cell is not blank the cell to the right of the corner cell is tried, then the cell above it.
 */
public class ContainerAsciiArtPointsTest {

	private static ContainerAsciiArt.Builder newBuilder(int dx, int dy, int dz, Placement... placements) {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(placements), dx, dy, dz);
	}

	/**
	 * A is 2 x 2 x 2 in a 4 x 4 x 4 container, and the point starts at the back top right corner of A: one cell to the right and one line up from
	 * the corner, in every view.
	 */
	@Test
	public void testSinglePoint() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(2, 2, 2)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z
				             0
				  │ ┌───────┐   y
				  │╱       ╱│
				2 ┌───────┐ │ ╱
				  │       │ │╱
				  │   A   │ │ 2
				  │       │╱
				0 └───────┘── x
				  0       2
				""");
	}

	/**
	 * Points 0 and 1 are at the same cell in the front view (they only differ in y), where they share a label. In the top and side views, they have
	 * separate labels. Point 0 is next to the right face of A, so it has no free cell in the oblique view.
	 */
	@Test
	public void testPointsAtTheSameCellShareALabel() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(2, 0, 0), point(2, 3, 0), point(3, 1, 1)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │    2
				  │   A   │
				  │       │0,1
				0 └───────┘
				  0       2       x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y          1


				2 ┌───────┐
				  │       │    2
				  │   A   │
				  │       │0
				0 └───────┘
				  0       2       x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │    1
				0 └───────┘
				  0       2       y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z
				                     y
				  │ ┌───────┐
				  │╱       ╱│      ╱
				2 ┌───────┐ │ 1 2 ╱
				  │       │ │    ╱
				  │   A   │ │   ╱ 2
				  │       │╱   ╱
				0 └───────┘────── x
				  0       2
				""");
	}

	/**
	 * Point 0 is behind A, so A is in front of it in the oblique view. Point 1 is not hidden. In the front view, point 0 is at the corner of A, where
	 * there is no free cell.
	 */
	@Test
	public void testPointBehindBoxIsLeftOutOfTheObliqueView() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(0, 2, 0), point(2, 2, 2)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z          1
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y  0       1
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z          1
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │0
				0 └───────┘
				  0       2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z
				             1
				  │ ┌───────┐   y
				  │╱       ╱│
				2 ┌───────┐ │ ╱
				  │       │ │╱
				  │   A   │ │ 2
				  │       │╱
				0 └───────┘── x
				  0       2
				""");
	}

	/**
	 * The point is at the front bottom left corner of A: the cells next to the corner are the edges and the inside of A, in every view.
	 */
	@Test
	public void testPointWithoutFreeCellIsLeftOut() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(0, 0, 0)))
				.build();
		ContainerAsciiArt withoutPoints = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.top().toString()).isEqualTo(withoutPoints.top().toString());
		assertThat(art.side().toString()).isEqualTo(withoutPoints.side().toString());
		assertThat(art.oblique().toString()).isEqualTo(withoutPoints.oblique().toString());
	}

	/**
	 * B is on top of the right part of A. The cell above and to the right of the point is the bottom edge of B, so the label is put to the right of the point.
	 * One unit is one line high.
	 */
	@Test
	public void testLabelFallsBackToTheCellRightOfTheCorner() {
		ContainerAsciiArt art = newBuilder(3, 1, 2,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 2, 1, 1, 1, 0, 1))
				.withScale(4, 1)
				.withPoints(List.of(point(1, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2     ┌───B───┐
				1 ┌─A─┼───────┘
				0 └───┘0
				  0   1       3   x
				""");
	}

	/**
	 * C is next to the point: the cell above and to the right of the point, and the cell to the right of it, are edges of C. The label is put above the point.
	 * One unit is one column wide and one line high.
	 */
	@Test
	public void testLabelFallsBackToTheCellAboveTheCorner() {
		ContainerAsciiArt art = newBuilder(3, 1, 2, place("C", 2, 1, 2, 1, 0, 0))
				.withScale(1, 1)
				.withPoints(List.of(point(0, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2  ┌─┐
				  0│C│
				0  └─┘
				  0  3   x
				""");
	}

	/**
	 * As testLabelFallsBackToTheCellAboveTheCorner, but with two points at the same cell: the label "0,1" does not fit in any of the cells, so neither
	 * of the points is drawn.
	 */
	@Test
	public void testLabelNeedsRoomForAllCharacters() {
		ContainerAsciiArt art = newBuilder(3, 1, 2, place("C", 2, 1, 2, 1, 0, 0))
				.withScale(1, 1)
				.withPoints(List.of(point(0, 0, 0), point(0, 0, 0, 1, 1, 1)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2  ┌─┐
				   │C│
				0  └─┘
				  0  3   x
				""");
	}

	/**
	 * Points 0 and 1 share a label. Point 2 is one column to the right, where the cell above and to the right is used by the label "0,1", so the label
	 * of point 2 is put to the right of the point.
	 */
	@Test
	public void testLabelDoesNotOverwriteAnotherLabel() {
		ContainerAsciiArt art = newBuilder(8, 1, 3)
				.withScale(1, 1)
				.withPoints(List.of(point(0, 0, 0), point(0, 0, 0, 1, 1, 1), point(1, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				3

				   0,1
				0   2
				  0       8   x
				""");
	}

	/**
	 * The outline of the container is drawn with dots, and the obstacle X with '#'. These are not overwritten: point 1 is below the top edge, so its label is put to the
	 * right of the point, and point 2 is in the corner of the container where all three cells are on the outline, so it is left out.
	 * One unit is one column wide and one line high.
	 */
	@Test
	public void testPointsNextToObstacleAndOutline() {
		ContainerAsciiArt art = newBuilder(6, 2, 2)
				.withObstacles(List.of(place("X", 1, 2, 1, 0, 0, 0)))
				.withScale(1, 1)
				.withContainerOutline(true)
				.withPoints(List.of(point(1, 0, 0), point(2, 0, 1), point(5, 1, 1)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 .......
				1 ┌┐01  .
				0 └┘.....
				  0     6   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌┐.....
				  ││01  .
				0 └┘.....
				  0     6   x
				""");
	}

	/**
	 * Without axes there is no room beyond the boxes, but a point beyond the boxes extends the drawing to the point. In the front view, the label of
	 * point 0 (at the right edge of the drawing) is put above the point, which is the last choice. In the oblique view there is room to the right of
	 * the point.
	 */
	@Test
	public void testPointBeyondTheBoxesExtendsTheDrawing() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withAxes(false)
				.withPoints(List.of(point(4, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   A   │
				│       │       0
				└───────┘
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  ┌───────┐
				 ╱       ╱│
				┌───────┐ │
				│       │ │
				│   A   │ │
				│       │╱       0
				└───────┘
				""");
	}

	/**
	 * The container is larger than the boxes, but the drawing only reaches as far as the boxes and the points: a point far beyond the boxes extends
	 * the drawing, here to x = 6 in the front view.
	 */
	@Test
	public void testPointFarBeyondTheBoxesIsDrawn() {
		ContainerAsciiArt art = newBuilder(10, 10, 10, place("A", 2, 2, 2, 0, 0, 0))
				.withAxes(false)
				.withPoints(List.of(point(6, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   A   │
				│       │               0
				└───────┘
				""");
	}

	/**
	 * A point at the end of the boxes is within the drawing, also when the container is larger: the label is put in the free corner, in the margin of
	 * the axes.
	 */
	@Test
	public void testPointAtTheEndOfTheBoxesIsWithinTheDrawing() {
		ContainerAsciiArt art = newBuilder(10, 10, 10, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(2, 2, 2)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z          0
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z
				             0
				  │ ┌───────┐   y
				  │╱       ╱│
				2 ┌───────┐ │ ╱
				  │       │ │╱
				  │   A   │ │ 2
				  │       │╱
				0 └───────┘── x
				  0       2
				""");
	}

	/**
	 * B is behind A (higher y) and larger than A. The edges where a box starts are labelled with the start, the edges where boxes only end with the end minus 1,
	 * and the end of the drawing with the size minus 1.
	 *
	 * <pre>
	 * front: x to the right, z up       top: x to the right, y up the page
	 *
	 * B B B B                           B B B B
	 * B B B B                           B B B B
	 * B B B B                           A A
	 * A A B B                           A A
	 * A A B B
	 * </pre>
	 */
	@Test
	public void testInclusiveCoordinates() {
		ContainerAsciiArt art = newBuilder(4, 4, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 4, 2, 4, 0, 2, 0))
				.withInclusiveCoordinates(true)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				3 ┌───────────────┐
				  │               │
				  │       B       │
				  │               │
				1 ├───────┐       │
				  │       │       │
				  │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       1       3   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				3 ┌───────────────┐
				  │               │
				  │       B       │
				  │               │
				2 ├───────┬───────┘
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       1       3   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				3         ┌───────┐
				          │       │
				          │       │
				          │       │
				1 ┌───────┤   B   │
				  │       │       │
				  │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       2       3   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z   ┌───────────────┐
				     ╱               ╱│
				  │ ┌───────────────┐ │
				  │ │               │ │
				3 │ │       B       │ │   y
				  │ │               │ │
				  │ └───────┐       │ │ ╱
				  │╱       ╱│       │ │╱
				1 ┌───────┐ │       │ │ 3
				  │       │ │       │╱
				  │   A   │ └───────┘ 2
				  │       │╱       ╱
				0 └───────┘────────── x
				  0       1       3
				""");
	}

	/**
	 * The end of the drawing is labelled 9 instead of 10, so the margin is one column narrower than without inclusive coordinates.
	 */
	@Test
	public void testInclusiveCoordinatesWithFewerDigits() {
		ContainerAsciiArt.Builder builder = newBuilder(10, 1, 10, place("A", 10, 1, 10, 0, 0, 0)).withScale(1, 1);
		ContainerAsciiArt art = builder.withInclusiveCoordinates(true).build();
		ContainerAsciiArt exclusive = builder.withInclusiveCoordinates(false).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				9 ┌─────────┐
				  │         │
				  │         │
				  │         │
				  │         │
				  │    A    │
				  │         │
				  │         │
				  │         │
				  │         │
				0 └─────────┘
				  0         9   x
				""");
		assertThat(exclusive.front().toString()).isEqualTo("""
				z
				10 ┌─────────┐
				   │         │
				   │         │
				   │         │
				   │         │
				   │    A    │
				   │         │
				   │         │
				   │         │
				   │         │
				 0 └─────────┘
				   0         10   x
				""");
	}

	/**
	 * The oblique view is followed by the front, top and side views, in one row as that is less than 160 characters wide.
	 */
	@Test
	public void testOverviewInOneRow() {
		ContainerAsciiArt art = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0))
				.withPoints(List.of(point(2, 2, 2), point(2, 0, 0)))
				.build();

		assertThat(art.overview().toString()).isEqualTo("""
				  z                 z          0      y          0      z          0
				             0      2 ┌───────┐       2 ┌───────┐       2 ┌───────┐
				  │ ┌───────┐   y     │       │         │       │         │       │
				  │╱       ╱│         │   A   │         │   A   │         │   A   │
				2 ┌───────┐ │ ╱       │       │1        │       │1        │       │
				  │       │ │╱      0 └───────┘       0 └───────┘       0 └───────┘
				  │   A   │ │ 2       0       2   x     0       2   x     0       2   y
				  │       │╱
				0 └───────┘── x
				  0       2
				""");
	}

	/**
	 * The views are 161 characters wide or more in one row, so the oblique and front views are in the first row, and the top and side views in the second row.
	 */
	@Test
	public void testOverviewInTwoRows() {
		ContainerAsciiArt art = newBuilder(40, 3, 2,
				place("A", 15, 3, 2, 0, 0, 0),
				place("B", 15, 3, 2, 25, 0, 0))
				.withScale(1, 1)
				.withPoints(List.of(point(15, 0, 0), point(25, 0, 2)))
				.build();

		assertThat(art.overview().toString()).isEqualTo("""
				  z                                             y   z                           1
				                                                    2 ┌──────────────┐         ┌──────────────┐
				  │ ┌──────────────┐         ┌──────────────┐ ╱       │      A       │0        │      B       │
				  │╱              ╱│       1╱              ╱│╱      0 └──────────────┘         └──────────────┘
				2 ┌──────────────┐ │       ┌──────────────┐ │ 3       0              15        25             40   x
				  │      A       │╱        │      B       │╱
				0 └──────────────┘─────────└──────────────┘── x
				  0             15        25             40

				y                                                  z  1
				3 ┌──────────────┐         ┌──────────────┐        2 ┌──┐
				  │      A       │         │      B       │          │B │
				  │              │0        │              │        0 └──┘
				0 └──────────────┘         └──────────────┘          0  3   y
				  0              15        25             40   x
				""");
	}
	@Test
	public void testInclusiveCoordinatesAreOffByDefault() {
		ContainerAsciiArt.Builder builder = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0));

		ContainerAsciiArt art = builder.build();
		ContainerAsciiArt explicit = builder.withInclusiveCoordinates(true).withInclusiveCoordinates(false).build();

		assertThat(explicit.front().toString()).isEqualTo(art.front().toString());
		assertThat(explicit.oblique().toString()).isEqualTo(art.oblique().toString());
		assertThat(art.front().toString()).endsWith("  0       2   x\n");
	}

	@Test
	public void testNoPointsDrawsNothing() {
		ContainerAsciiArt.Builder builder = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0));

		ContainerAsciiArt art = builder.build();
		ContainerAsciiArt empty = builder.withPoints(List.of()).build();

		assertThat(empty.overview().toString()).isEqualTo(art.overview().toString());
	}

	@Test
	public void testPointsMustNotBeNull() {
		ContainerAsciiArt.Builder builder = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0));

		assertThatThrownBy(() -> builder.withPoints(null)).isInstanceOf(NullPointerException.class);

		List<Point> points = new ArrayList<>();
		points.add(point(2, 2, 2));
		points.add(null);
		assertThatThrownBy(() -> builder.withPoints(points)).isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testPointsAreCopiedByTheBuilder() {
		List<Point> points = new ArrayList<>();
		points.add(point(2, 2, 2));

		ContainerAsciiArt.Builder builder = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0)).withPoints(points);
		points.add(point(2, 0, 0));

		ContainerAsciiArt art = builder.build();
		ContainerAsciiArt expected = newBuilder(4, 4, 4, place("A", 2, 2, 2, 0, 0, 0)).withPoints(List.of(point(2, 2, 2))).build();

		assertThat(art.front().toString()).isEqualTo(expected.front().toString());
		assertThat(art.oblique().toString()).isEqualTo(expected.oblique().toString());
	}

	/**
	 * One row is used as long as it is at most 160 characters wide (here exactly 160), then the views are in two rows.
	 */
	@Test
	public void testOverviewIsInOneRowUpToTheMaximumWidth() {
		ContainerAsciiArt art = newBuilder(39, 2, 2,
				place("A", 19, 2, 2, 0, 0, 0),
				place("B", 20, 2, 2, 19, 0, 0))
				.withScale(1, 1)
				.build();

		Figure overview = art.overview();

		assertThat(overview.getWidth()).isEqualTo(160);
		assertThat(overview.getHeight()).isEqualTo(8);
		assertThat(overview.toString()).isEqualTo(Figures.horizontal(3, art.oblique(), art.front(), art.top(), art.side()).toString());
	}

	@Test
	public void testOverviewIsInTwoRowsAboveTheMaximumWidth() {
		ContainerAsciiArt art = newBuilder(39, 3, 2,
				place("A", 19, 3, 2, 0, 0, 0),
				place("B", 20, 3, 2, 19, 0, 0))
				.withScale(1, 1)
				.build();

		Figure overview = art.overview();

		// in one row, it would be 161 characters wide
		assertThat(Figures.horizontal(3, art.oblique(), art.front(), art.top(), art.side()).getWidth()).isEqualTo(161);
		assertThat(overview.getWidth()).isEqualTo(98);
		assertThat(overview.getHeight()).isEqualTo(15);
		assertThat(overview.toString()).isEqualTo(Figures.vertical(1,
				Figures.horizontal(3, art.oblique(), art.front()),
				Figures.horizontal(3, art.top(), art.side())).toString());
	}

	/**
	 * At the default scale, the four views of a container 12 x 6 x 6 do not fit in one row of 160 characters, but two rows of two views do: the
	 * overview is not drawn narrower.
	 */
	@Test
	public void testOverviewIsInTwoRowsAtTheDefaultScaleWhenEachRowIsAtMostTheMaximumWidth() {
		ContainerAsciiArt art = newBuilder(12, 6, 6,
				place("A", 6, 6, 6, 0, 0, 0),
				place("B", 6, 6, 6, 6, 0, 0))
				.build();

		Figure upper = Figures.horizontal(3, art.oblique(), art.front());
		Figure lower = Figures.horizontal(3, art.top(), art.side());
		assertThat(Figures.horizontal(3, art.oblique(), art.front(), art.top(), art.side()).getWidth()).isGreaterThan(160);
		assertThat(upper.getWidth()).isLessThanOrEqualTo(160);
		assertThat(lower.getWidth()).isLessThanOrEqualTo(160);

		Figure overview = art.overview();
		assertThat(overview.toString()).isEqualTo(Figures.vertical(1, upper, lower).toString());
		// at the default scale of 4 columns per unit: 48 columns for the 12 units, and the values and the name of the axes
		assertThat(art.front().getWidth()).isEqualTo(56);
	}

	/**
	 * If even two rows of two views are wider than 160 characters, the overview is as wide: the views are drawn narrower with a width. A width of
	 * 78 columns makes a row of two views at most 160 characters wide, at the same scale for all the views.
	 */
	@Test
	public void testOverviewIsDrawnNarrowerWithAWidthWhenTwoRowsAreTooWide() {
		ContainerAsciiArt.Builder builder = newBuilder(40, 20, 10,
				place("A", 20, 20, 10, 0, 0, 0),
				place("B", 20, 20, 10, 20, 0, 0));

		ContainerAsciiArt art = builder.build();
		assertThat(Figures.horizontal(3, art.oblique(), art.front()).getWidth()).isGreaterThan(160);
		assertThat(art.overview().getWidth()).isGreaterThan(160);

		ContainerAsciiArt narrow = builder.withWidth(78).build();
		Figure overview = narrow.overview();
		assertThat(overview.getWidth()).isLessThanOrEqualTo(160);
		assertThat(overview.toString()).isEqualTo(Figures.vertical(1,
				Figures.horizontal(3, narrow.oblique(), narrow.front()),
				Figures.horizontal(3, narrow.top(), narrow.side())).toString());
	}

	@Test
	public void testOverviewHasTheComment() {
		ContainerAsciiArt.Builder builder = newBuilder(40, 3, 2, place("A", 40, 3, 2, 0, 0, 0))
				.withScale(1, 1)
				.withComment("// ");

		ContainerAsciiArt art = builder.build();
		Figure overview = art.overview();

		assertThat(overview.getComment()).isEqualTo("// ");
		// the blank line between the rows has no trailing blank
		String[] lines = overview.toString().split("\n");
		int firstRow = Figures.horizontal(3, art.oblique(), art.front()).getHeight();
		assertThat(lines).hasSize(firstRow + 1 + Figures.horizontal(3, art.top(), art.side()).getHeight());
		assertThat(lines[firstRow]).isEqualTo("//");
		for (String line : lines) {
			assertThat(line).startsWith("//");
			assertThat(line).doesNotEndWith(" ");
		}
	}
}
