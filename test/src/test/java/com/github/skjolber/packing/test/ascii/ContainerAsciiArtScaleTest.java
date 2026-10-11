package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;

/**
 * The default scale of a figure is the same in all of its views. A view which is more than 80 columns wide or 40 lines high at the default scale is
 * drawn smaller on its own; the other views keep the default scale.
 */
public class ContainerAsciiArtScaleTest {

	private static ContainerAsciiArt.Builder newBuilder(int dx, int dy, int dz, Placement... placements) {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(placements), dx, dy, dz);
	}

	/**
	 * Container 6 x 1 x 1 with the boxes A (3 long), B (2 long) and C (1 long) in a row: all the views have 8 columns and 4 lines per unit, so the
	 * side view, where every box is 1 x 1, is as small as a unit box (and not stretched to fill the width of the front view).
	 */
	@Test
	public void testAllViewsOfASixByOneByOneContainerHaveTheSameScale() {
		ContainerAsciiArt art = newBuilder(6, 1, 1,
				place("A", 3, 1, 1, 0, 0, 0),
				place("B", 2, 1, 1, 3, 0, 0),
				place("C", 1, 1, 1, 5, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────────────────────┬───────────────┬───────┐
				  │                       │               │       │
				  │           A           │       B       │   C   │
				  │                       │               │       │
				0 └───────────────────────┴───────────────┴───────┘
				  0                       3               5       6   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				1 ┌───────────────────────┬───────────────┬───────┐
				  │                       │               │       │
				  │           A           │       B       │   C   │
				  │                       │               │       │
				0 └───────────────────────┴───────────────┴───────┘
				  0                       3               5       6   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				1 ┌───────┐
				  │       │
				  │   C   │
				  │       │
				0 └───────┘
				  0       1   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ ┌───────────────────────────────────────────────┐   y
				  │╱                       ╱               ╱       ╱│
				1 ┌───────────────────────┬───────────────┬───────┐ │ ╱
				  │                       │               │       │ │╱
				  │           A           │       B       │   C   │ │ 1
				  │                       │               │       │╱
				0 └───────────────────────┴───────────────┴───────┘── x
				  0                       3               5       6
				""");
		assertThat(art.side().getWidth()).isLessThan(16);
		assertThat(art.side().getHeight()).isLessThan(8);
		assertThat(art.front().getHeight()).isEqualTo(art.side().getHeight());
	}

	/**
	 * The same container as an overview: the four views are more than 160 characters wide in a row, so they are in two rows.
	 */
	@Test
	public void testOverviewOfASixByOneByOneContainerIsInTwoRows() {
		ContainerAsciiArt art = newBuilder(6, 1, 1,
				place("A", 3, 1, 1, 0, 0, 0),
				place("B", 2, 1, 1, 3, 0, 0),
				place("C", 1, 1, 1, 5, 0, 0))
				.withStyle(Style.ASCII)
				.build();

		assertThat(art.overview().toString()).isEqualTo("""
				  z                                                         z
				                                                            1 +-----------------------+---------------+-------+
				  | /-----------------------/---------------/-------|   y     |                       |               |       |
				  |/                       /               /       /|         |           A           |       B       |   C   |
				1 |-----------------------|---------------|-------| | /       |                       |               |       |
				  |                       |               |       | |/      0 +-----------------------+---------------+-------+
				  |           A           |       B       |   C   | | 1       0                       3               5       6   x
				  |                       |               |       |/
				0 |-----------------------|---------------|-------|-- x
				  0                       3               5       6

				y                                                         z
				1 +-----------------------+---------------+-------+       1 +-------+
				  |                       |               |       |         |       |
				  |           A           |       B       |   C   |         |   C   |
				  |                       |               |       |         |       |
				0 +-----------------------+---------------+-------+       0 +-------+
				  0                       3               5       6   x     0       1   y
				""");
	}

	/**
	 * By default the label has a blank line above and below it: a box which is one unit high is drawn with 4 lines per unit, a box which is higher
	 * with 2 lines per unit.
	 */
	@Test
	public void testDefaultScaleHasABlankLineAboveAndBelowTheLabel() {
		ContainerAsciiArt flat = newBuilder(3, 3, 1, place("A", 3, 3, 1, 0, 0, 0)).withAxes(false).build();
		ContainerAsciiArt tall = newBuilder(3, 3, 5, place("A", 3, 3, 5, 0, 0, 0)).withAxes(false).build();

		assertThat(flat.front().toString()).isEqualTo("""
				┌───────────────────────┐
				│                       │
				│           A           │
				│                       │
				└───────────────────────┘
				""");
		assertThat(flat.front().getHeight() - 1).isEqualTo(4);
		assertThat(tall.front().getHeight() - 1).isEqualTo(5 * 2);
	}

	/**
	 * The labels of 90 % of the faces which the views show must fit: the front face, the top face and the side face of every box. Of ten boxes, one
	 * is one unit wide with the label LONG, which needs 7 columns per unit: its front face and its top face are too narrow, 2 of 30 faces, so the
	 * scale stays at 4 columns per unit (the top view is checked, as the other views are higher than 40 lines and drawn smaller).
	 */
	@Test
	public void testScaleIsForNinetyPercentOfTheFacesOfAllViews() {
		List<Placement> placements = new ArrayList<>();
		placements.add(place("LONG", 1, 4, 4, 0, 0, 0));
		for (int i = 0; i < 9; i++) {
			placements.add(place("A", 4, 4, 4, 0, 0, 4 + i * 4));
		}
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withPlacements(placements, 4, 4, 40).withAxes(false).withContainerOutline(true).build();

		// the top view, which is not scaled down
		assertThat(art.top().getWidth() - 1).isEqualTo(4 * 4);
	}

	/**
	 * With a second box that is one unit wide, 4 of 30 faces are too narrow: the scale grows until the labels of 27 faces fit, so that the label
	 * LONG fits in a face which is one unit wide (7 columns per unit), in all the views.
	 */
	@Test
	public void testScaleGrowsWhenMoreThanTenPercentOfTheFacesAreTooNarrow() {
		List<Placement> placements = new ArrayList<>();
		placements.add(place("LONG", 1, 4, 4, 0, 0, 0));
		placements.add(place("LONG", 1, 4, 4, 1, 0, 0));
		for (int i = 0; i < 8; i++) {
			placements.add(place("A", 4, 4, 4, 0, 0, 4 + i * 4));
		}
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withPlacements(placements, 4, 4, 40).withAxes(false).withContainerOutline(true).build();

		assertThat(art.top().getWidth() - 1).isEqualTo(4 * 7);
	}

	/**
	 * A view which is higher than 40 lines at the default scale is drawn smaller, and the other views keep the default scale: in a container 4 x 4 x 100
	 * the front and side views are scaled down to 40 lines, and the top view, which shows 4 x 4 units, is drawn at the default scale of 4 columns and
	 * 2 lines per unit.
	 */
	@Test
	public void testAViewWhichIsHigherThanFortyLinesIsDrawnSmallerOnItsOwn() {
		ContainerAsciiArt art = newBuilder(4, 4, 100, place("A", 4, 4, 100, 0, 0, 0)).withAxes(false).build();

		assertThat(art.front().getHeight()).isLessThanOrEqualTo(40).isGreaterThan(35);
		assertThat(art.side().getHeight()).isLessThanOrEqualTo(40).isGreaterThan(35);
		assertThat(art.oblique().getHeight()).isLessThanOrEqualTo(40).isGreaterThan(35);

		assertThat(art.top().getWidth() - 1).isEqualTo(4 * 4);
		assertThat(art.top().getHeight() - 1).isEqualTo(4 * 2);
		// scaled down with 2 columns per line
		assertThat(art.front().getWidth() - 1).isLessThan(4 * 4);
		assertThat(art.front().getWidth() - 1).isBetween(4 * (art.front().getHeight() - 1) / 100 * 2 - 2, 4 * (art.front().getHeight() - 1) / 100 * 2 + 2);
	}

	/**
	 * The same for the width, in a container 4 x 100 x 4 where the top and side views show the 100 units of y: they are scaled down to 80 columns
	 * and 40 lines, and the front view is not.
	 */
	@Test
	public void testAViewWhichIsWiderThanEightyColumnsIsDrawnSmallerOnItsOwn() {
		ContainerAsciiArt art = newBuilder(4, 100, 4, place("A", 4, 100, 4, 0, 0, 0)).withAxes(false).build();

		assertThat(art.side().getWidth()).isLessThanOrEqualTo(80).isGreaterThan(75);
		assertThat(art.top().getHeight()).isLessThanOrEqualTo(40).isGreaterThan(35);
		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(80);

		// the default scale of 4 columns and 2 lines per unit
		assertThat(art.front().getWidth() - 1).isEqualTo(4 * 4);
		assertThat(art.front().getHeight() - 1).isEqualTo(4 * 2);
	}

	/**
	 * Container 14 x 195 x 74 with boxes of 7 x 37 x 39 and the label Foot: the depth shrinks the side and oblique views, but the front view keeps a
	 * scale at which the label fits (the boxes reach 39 units up, so the front view is about 80 lines high at the default scale and is scaled down to 40 lines,
	 * which is still enough for the label).
	 */
	@Test
	public void testTheFrontViewOfADeepContainerKeepsAReadableScale() {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			placements.add(place("Foot", 7, 37, 39, 0, i * 39, 0));
			placements.add(place("Foot", 7, 37, 39, 7, i * 39, 0));
		}
		ContainerAsciiArt art = newBuilder(14, 195, 74, placements.toArray(new Placement[0])).build();

		assertThat(art.front().getHeight()).isLessThanOrEqualTo(40);
		assertThat(art.front().toString()).contains("Foot");
		assertThat(art.side().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.side().getHeight()).isLessThan(art.front().getHeight());
	}

	/**
	 * A container which is low is not limited by the height, only by the width.
	 */
	@Test
	public void testDefaultScaleOfALowContainerKeepsEveryViewWithinEightyColumns() {
		ContainerAsciiArt art = newBuilder(200, 2, 2, place("A", 200, 2, 2, 0, 0, 0)).build();

		assertThat(art.front().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.top().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.side().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(80).isGreaterThan(75);
		assertThat(art.front().getHeight()).isLessThan(10);
	}

	/**
	 * Container 4 x 2 x 4 in 11 lines: the oblique view, which is the highest because of the depth, is 11 lines high with 4 columns and 2 lines
	 * per unit, and the front view has the same scale.
	 */
	@Test
	public void testHeight() {
		ContainerAsciiArt art = newBuilder(4, 2, 4, place("A", 4, 2, 4, 0, 0, 0))
				.withHeight(11)
				.withAxes(false)
				.build();

		assertThat(art.oblique().getHeight()).isEqualTo(11);
		assertThat(art.oblique().toString()).isEqualTo("""
				  ┌───────────────┐
				 ╱               ╱│
				┌───────────────┐ │
				│               │ │
				│               │ │
				│               │ │
				│       A       │ │
				│               │ │
				│               │ │
				│               │╱
				└───────────────┘
				""");
		assertThat(art.front().toString()).isEqualTo("""
				┌───────────────┐
				│               │
				│               │
				│               │
				│       A       │
				│               │
				│               │
				│               │
				└───────────────┘
				""");
		assertThat(art.top().toString()).isEqualTo("""
				┌───────────────┐
				│               │
				│       A       │
				│               │
				└───────────────┘
				""");
		assertThat(art.side().toString()).isEqualTo("""
				┌───────┐
				│       │
				│       │
				│       │
				│   A   │
				│       │
				│       │
				│       │
				└───────┘
				""");
	}

	/**
	 * The height counts everything in the view: the axes with their values, and the names of the axes.
	 */
	@Test
	public void testHeightCountsEverythingInTheView() {
		for (int lines : new int[] { 20, 31, 40, 60, 100 }) {
			// C reaches the far top right corner of the container
			ContainerAsciiArt art = newBuilder(1000, 600, 1200,
					place("A", 1000, 300, 600, 0, 0, 0),
					place("B", 500, 300, 600, 0, 300, 0),
					place("C", 500, 300, 1200, 500, 300, 0))
					.withHeight(lines)
					.build();

			assertThat(art.front().getHeight()).as("front at %d lines", lines).isLessThanOrEqualTo(lines);
			assertThat(art.top().getHeight()).as("top at %d lines", lines).isLessThanOrEqualTo(lines);
			assertThat(art.side().getHeight()).as("side at %d lines", lines).isLessThanOrEqualTo(lines);
			assertThat(art.oblique().getHeight()).as("oblique at %d lines", lines).isLessThanOrEqualTo(lines).isGreaterThan(lines - 4);
		}
	}

	/**
	 * Only the height is limited: the width is not, so the views are as wide as the scale makes them.
	 */
	@Test
	public void testHeightDoesNotLimitTheWidth() {
		ContainerAsciiArt art = newBuilder(200, 1, 2, place("A", 200, 1, 2, 0, 0, 0))
				.withHeight(10)
				.withAxes(false)
				.build();

		assertThat(art.oblique().getHeight()).isLessThanOrEqualTo(10);
		assertThat(art.front().getWidth()).isGreaterThan(100);
	}

	/**
	 * Both the width and the height: every view is at the largest scale at which it is neither wider nor higher than that, so the more restrictive
	 * limit wins. The oblique view is twice as wide as it is high for the same number of units of x and z, so the width of 45 columns is more
	 * restrictive than a height of 25 lines, and the height of 25 lines is more restrictive than a width of 100 columns.
	 */
	@Test
	public void testWidthAndHeight() {
		ContainerAsciiArt.Builder builder = newBuilder(40, 4, 40, place("A", 40, 4, 40, 0, 0, 0)).withAxes(false);

		ContainerAsciiArt widthOnly = builder.withWidth(45).build();
		ContainerAsciiArt widthAndHeight = builder.withHeight(100).build();
		ContainerAsciiArt heightOnly = newBuilder(40, 4, 40, place("A", 40, 4, 40, 0, 0, 0)).withAxes(false).withHeight(25).build();
		ContainerAsciiArt heightAndWidth = builder.withWidth(100).withHeight(25).build();
		ContainerAsciiArt both = builder.withWidth(45).withHeight(25).build();

		// the width is the limit
		assertThat(widthOnly.oblique().getWidth()).isLessThanOrEqualTo(45).isGreaterThan(40);
		assertThat(widthOnly.oblique().getHeight()).isLessThan(25);
		assertThat(widthAndHeight.front().toString()).isEqualTo(widthOnly.front().toString());
		// the height is the limit
		assertThat(heightOnly.oblique().getHeight()).isLessThanOrEqualTo(25).isGreaterThan(21);
		assertThat(heightOnly.oblique().getWidth()).isLessThan(100);
		assertThat(heightAndWidth.front().toString()).isEqualTo(heightOnly.front().toString());
		// both: no view is wider or higher
		for (Figure figure : List.of(both.front(), both.top(), both.side(), both.oblique())) {
			assertThat(figure.getWidth()).isLessThanOrEqualTo(45);
			assertThat(figure.getHeight()).isLessThanOrEqualTo(25);
		}
		assertThat(both.front().toString()).isEqualTo(widthOnly.front().toString());
	}

	/**
	 * The last of scale, width and height is used: a scale replaces the width and the height, and the width and the height replace the scale.
	 */
	@Test
	public void testTheLastOfScaleAndHeightIsUsed() {
		ContainerAsciiArt.Builder builder = newBuilder(4, 1, 4, place("A", 4, 1, 40, 0, 0, 0));

		String scaled = builder.withScale(3, 2).build().front().toString();
		String height = builder.withHeight(21).build().front().toString();
		assertThat(height).isNotEqualTo(scaled);

		assertThat(builder.withScale(3, 2).build().front().toString()).isEqualTo(scaled);
		assertThat(builder.withHeight(21).build().front().toString()).isEqualTo(height);
	}

	/**
	 * The width is a limit for every view on its own, not a scale for all of them: the front view is scaled down to 60 columns, but the side view,
	 * which shows only the 2 units of y, is not wider than 60 columns at the default scale and keeps it. A width does not make a view larger than the
	 * default scale.
	 */
	@Test
	public void testWidthIsALimitForEveryViewOnItsOwn() {
		ContainerAsciiArt art = newBuilder(40, 2, 2, place("A", 40, 2, 2, 0, 0, 0)).withWidth(60).withAxes(false).build();

		assertThat(art.front().getWidth()).isLessThanOrEqualTo(60).isGreaterThan(55);
		assertThat(art.side().getWidth() - 1).isEqualTo(2 * 4);
		assertThat(art.side().getHeight() - 1).isEqualTo(2 * 2);
	}

	/**
	 * A width which is larger than the views at the default scale leaves the default scale as it is.
	 */
	@Test
	public void testWidthDoesNotMakeAViewLargerThanTheDefaultScale() {
		ContainerAsciiArt.Builder builder = newBuilder(3, 3, 3, place("A", 3, 3, 3, 0, 0, 0)).withAxes(false);

		assertThat(builder.withWidth(100).build().front().toString()).isEqualTo(builder.build().front().toString());
		assertThat(builder.withHeight(100).build().front().toString()).isEqualTo(builder.build().front().toString());
	}
}
