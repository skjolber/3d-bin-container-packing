package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;

public class ContainerAsciiArtLabelTest {

	private static ContainerAsciiArt.Builder newBuilder(int dx, int dy, int dz, Placement... placements) {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(placements), dx, dy, dz);
	}

	@Test
	public void testLabelIsIdThenDescriptionThenLetter() {
		ContainerAsciiArt art = newBuilder(3, 1, 1,
				place(Box.newBuilder()
						.withId("Id")
						.withDescription("Description"), 1, 1, 1, 0, 0, 0),
				place(Box.newBuilder().withDescription("Description"), 1, 1, 1, 1, 0, 0),
				place(Box.newBuilder(), 1, 1, 1, 2, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌─────────────┬─────────────┬─────────────┐
				  │             │             │             │
				  │     Id      │ Description │      C      │
				  │             │             │             │
				0 └─────────────┴─────────────┴─────────────┘
				  0             1             2             3   x
				""");
	}

	@Test
	public void testLettersFollowThePlacementOrder() {
		ContainerAsciiArt art = newBuilder(3, 1, 1,
				place(Box.newBuilder(), 1, 1, 1, 0, 0, 0),
				place(Box.newBuilder().withId("Y"), 1, 1, 1, 1, 0, 0),
				place(Box.newBuilder(), 1, 1, 1, 2, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────┬───────┬───────┐
				  │       │       │       │
				  │   A   │   Y   │   C   │
				  │       │       │       │
				0 └───────┴───────┴───────┘
				  0       1       2       3   x
				""");
	}

	@Test
	public void testLetters() {
		assertThat(ContainerAsciiArt.letters(0)).isEqualTo("A");
		assertThat(ContainerAsciiArt.letters(25)).isEqualTo("Z");
		assertThat(ContainerAsciiArt.letters(26)).isEqualTo("AA");
		assertThat(ContainerAsciiArt.letters(27)).isEqualTo("AB");
		assertThat(ContainerAsciiArt.letters(51)).isEqualTo("AZ");
		assertThat(ContainerAsciiArt.letters(52)).isEqualTo("BA");
		assertThat(ContainerAsciiArt.letters(701)).isEqualTo("ZZ");
		assertThat(ContainerAsciiArt.letters(702)).isEqualTo("AAA");
	}

	@Test
	public void testLabelsFunction() {
		ContainerAsciiArt art = newBuilder(3, 1, 1,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 1, 0, 0),
				place("C", 1, 1, 1, 2, 0, 0))
				.withLabels(p -> p.getAbsoluteX() == 1 ? null : "x" + p.getAbsoluteX())
				.build();

		// null: the default label
		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────┬───────┬───────┐
				  │       │       │       │
				  │  x0   │   B   │  x2   │
				  │       │       │       │
				0 └───────┴───────┴───────┘
				  0       1       2       3   x
				""");
	}

	@Test
	public void testEmptyLabelDrawsNoLabel() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("A", 1, 1, 1, 0, 0, 0)).withLabels(p -> "").build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───┐
				  │   │
				0 └───┘
				  0   1   x
				""");
	}

	/**
	 * A box 1 x 1 x 1 with a label of two characters needs 5 columns per unit (the label, a space on each side, and the edges),
	 * and 4 lines per unit (the label, a blank line above and below, and the edges). A line is twice as tall as a character is wide, so the
	 * drawing gets 8 columns per unit, so that the box looks like a cube.
	 */
	@Test
	public void testDefaultScaleFitsTheLabel() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("AB", 1, 1, 1, 0, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────┐
				  │       │
				  │  AB   │
				  │       │
				0 └───────┘
				  0       1   x
				""");
	}

	/**
	 * The scale is the smallest at which the labels of 90 % of the boxes fit; the box with the long label is more than 10 % but not shown in full.
	 * The ten boxes are in two rows of five, so that the front view is not wider than the largest default size.
	 */
	@Test
	public void testDefaultScaleFitsNinetyPercentOfTheLabels() {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			placements.add(place(String.valueOf((char) ('A' + i)), 1, 1, 1, i % 5, 0, i / 5));
		}
		placements.add(place("LONGLABEL", 1, 1, 1, 4, 0, 1));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(placements, 5, 1, 2)
				.withAxes(false)
				.build();

		// 8 columns and 4 lines per unit: the long label is truncated
		assertThat(art.front().toString()).isEqualTo("""
				┌───────┬───────┬───────┬───────┬───────┐
				│       │       │       │       │       │
				│   F   │   G   │   H   │   I   │ LONGL │
				│       │       │       │       │       │
				├───────┼───────┼───────┼───────┼───────┤
				│       │       │       │       │       │
				│   A   │   B   │   C   │   D   │   E   │
				│       │       │       │       │       │
				└───────┴───────┴───────┴───────┴───────┘
				""");
	}

	/**
	 * When fewer than 90 % of the labels fit, the scale grows until they do.
	 */
	@Test
	public void testDefaultScaleGrowsUntilNinetyPercentOfTheLabelsFit() {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			placements.add(place(String.valueOf((char) ('A' + i)), 1, 1, 1, i % 5, 0, i / 5));
		}
		placements.add(place("LONGER", 1, 1, 1, 3, 0, 1));
		placements.add(place("LONGER", 1, 1, 1, 4, 0, 1));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(placements, 5, 1, 2)
				.withAxes(false)
				.build();

		// 9 columns per unit for a label with six characters (more than the 8 columns of the default for the short labels)
		assertThat(art.front().toString()).isEqualTo("""
				┌────────┬────────┬────────┬────────┬────────┐
				│        │        │        │        │        │
				│   F    │   G    │   H    │ LONGER │ LONGER │
				│        │        │        │        │        │
				├────────┼────────┼────────┼────────┼────────┤
				│        │        │        │        │        │
				│   A    │   B    │   C    │   D    │   E    │
				│        │        │        │        │        │
				└────────┴────────┴────────┴────────┴────────┘
				""");
	}

	@Test
	public void testDefaultScaleIsAtLeastFourColumnsByTwoLines() {
		ContainerAsciiArt art = newBuilder(8, 4, 4, place("A", 8, 4, 4, 0, 0, 0)).withAxes(false).build();

		assertThat(art.front().getWidth()).isEqualTo(8 * 4 + 1);
		assertThat(art.front().getHeight()).isEqualTo(4 * 2 + 1);
	}

	/**
	 * A line is about twice as tall as a character is wide: with the default scale, the front face of a unit cube is twice as many characters wide
	 * as it is lines tall, so that the cube looks like a cube.
	 */
	@Test
	public void testDefaultScaleOfACubeHasTwiceAsManyColumnsAsLines() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("A", 1, 1, 1, 0, 0, 0)).withAxes(false).build();

		// the characters between the edges, not counting the edges
		int columns = art.front().getWidth() - 1;
		int lines = art.front().getHeight() - 1;
		assertThat(columns).isEqualTo(8);
		assertThat(lines).isEqualTo(4);
		assertThat(columns).isEqualTo(2 * lines);

		assertThat(art.front().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   A   │
				│       │
				└───────┘
				""");
	}

	/**
	 * An explicit scale is used exactly as given, also when it has fewer columns than twice the lines.
	 */
	@Test
	public void testExplicitScaleIsNotAdjusted() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("A", 1, 1, 1, 0, 0, 0)).withScale(4, 4).withAxes(false).build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───┐
				│   │
				│ A │
				│   │
				└───┘
				""");
	}

	/**
	 * The default scale is the same for all the views, so it follows the faces of all the views: the box is only one unit high, so its front face
	 * and its side face need 4 lines per unit for the label, and so does the top face, which would do with 2 lines per unit.
	 */
	@Test
	public void testDefaultScaleIsTheSameForAllViews() {
		ContainerAsciiArt art = newBuilder(3, 4, 4, place("A", 3, 2, 1, 0, 0, 0))
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		// 8 columns and 4 lines per unit
		assertThat(art.front().getHeight()).isEqualTo(4 * 4 + 1);
		assertThat(art.front().getWidth()).isEqualTo(3 * 8 + 1);
		assertThat(art.top().getHeight()).isEqualTo(4 * 4 + 1);
		assertThat(art.top().getWidth()).isEqualTo(3 * 8 + 1);
		assertThat(art.side().getHeight()).isEqualTo(4 * 4 + 1);
		assertThat(art.side().getWidth()).isEqualTo(4 * 8 + 1);
	}

	/**
	 * The same for the width: the side face of the box is only one unit wide, so the label with 8 characters needs 11 columns per unit in the side
	 * view, and also in the front and top views, where 4 columns per unit would do. The top face is one unit deep, so there are 4 lines per unit.
	 */
	@Test
	public void testDefaultScaleFollowsTheNarrowestFaceOfAnyView() {
		ContainerAsciiArt art = newBuilder(4, 2, 4, place("ABCDEFGH", 4, 1, 4, 0, 0, 0))
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		assertThat(art.front().getWidth()).isEqualTo(4 * 11 + 1);
		assertThat(art.front().getHeight()).isEqualTo(4 * 4 + 1);
		assertThat(art.top().getWidth()).isEqualTo(4 * 11 + 1);
		assertThat(art.top().getHeight()).isEqualTo(2 * 4 + 1);
		assertThat(art.side().getWidth()).isEqualTo(2 * 11 + 1);
		assertThat(art.side().getHeight()).isEqualTo(4 * 4 + 1);
	}

	@Test
	public void testLabelIsTruncated() {
		ContainerAsciiArt art = newBuilder(2, 1, 1, place("ABCDEFGH", 2, 1, 1, 0, 0, 0)).withScale(4, 2).build();

		// 7 columns between the edges, a space on each side
		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────┐
				  │ ABCDE │
				0 └───────┘
				  0       2   x
				""");
	}

	/**
	 * The label is within the edges of the face, also when it is truncated.
	 */
	@Test
	public void testObliqueLabelIsTruncatedWithinTheFace() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("ABCDE", 1, 1, 1, 0, 0, 0))
				.withScale(4, 2)
				.withAxes(false)
				.build();

		// three characters between the edges, a space on each side
		assertThat(art.oblique().toString()).isEqualTo("""
				  ┌───┐
				 ╱   ╱│
				┌───┐ │
				│ A │╱
				└───┘
				""");
	}

	/**
	 * The label is on the top face when the front face is hidden, within the edges of the top face.
	 */
	@Test
	public void testObliqueLabelIsTruncatedWithinTheTopFace() {
		ContainerAsciiArt art = newBuilder(1, 2, 1,
				place("ABCDE", 1, 1, 1, 0, 1, 0),
				place("F", 1, 1, 1, 0, 0, 0))
				.withScale(4, 2)
				.withAxes(false)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				    ┌───┐
				   ╱ A ╱│
				  ╱───┐ │
				 ╱   ╱│╱
				┌───┐ │
				│ F │╱
				└───┘
				""");
	}

	@Test
	public void testLabelOnNarrowFace() {
		ContainerAsciiArt art = newBuilder(2, 1, 1,
				place("AB", 1, 1, 1, 0, 0, 0),
				place("CD", 1, 1, 1, 1, 0, 0))
				.withScale(3, 2)
				.withAxes(false)
				.build();

		// two columns between the edges: no room for spaces
		assertThat(art.front().toString()).isEqualTo("""
				┌──┬──┐
				│AB│CD│
				└──┴──┘
				""");

		ContainerAsciiArt single = newBuilder(2, 1, 1,
				place("AB", 1, 1, 1, 0, 0, 0),
				place("CD", 1, 1, 1, 1, 0, 0))
				.withScale(2, 2)
				.withAxes(false)
				.build();

		// one column between the edges
		assertThat(single.front().toString()).isEqualTo("""
				┌─┬─┐
				│A│C│
				└─┴─┘
				""");
	}

	/**
	 * B is larger than A which is in front of it: the label of B is centered on what is visible of B, above A.
	 */
	@Test
	public void testLabelIsCenteredOnTheVisiblePart() {
		ContainerAsciiArt art = newBuilder(4, 4, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 4, 2, 4, 0, 2, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4 ┌───────────────┐
				  │               │
				  │       B       │
				  │               │
				2 ├───────┐       │
				  │       │       │
				  │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
	}

	@Test
	public void testHiddenBoxHasNoLabel() {
		ContainerAsciiArt art = newBuilder(2, 4, 2,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 0, 2, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.front().toString()).doesNotContain("B");
	}

	/**
	 * The front of B is hidden by A, so B is labelled on its top.
	 */
	@Test
	public void testObliqueLabelOnTheTopWhenTheFrontIsHidden() {
		ContainerAsciiArt art = newBuilder(2, 4, 2,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 0, 2, 0)).withAxes(false).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				    ┌───────┐
				   ╱   B   ╱│
				  ╱───────┐ │
				 ╱       ╱│ │
				┌───────┐ │ │
				│       │ │╱
				│   A   │ │
				│       │╱
				└───────┘
				""");
	}

	/**
	 * The face has no line between its edges (7 columns and 1 line per unit): the label is written into the top edge, centered between the corners,
	 * with two edge characters on each side.
	 */
	@Test
	public void testLabelIsWrittenIntoTheTopEdgeWhenTheFaceHasNoLine() {
		ContainerAsciiArt.Builder builder = newBuilder(1, 1, 1, place("b1", 1, 1, 1, 0, 0, 0))
				.withScale(7, 1)
				.withAxes(false);

		assertThat(builder.withStyle(Style.ASCII).build().front().toString()).isEqualTo("""
				+--b1--+
				+------+
				""");
		assertThat(builder.withStyle(Style.LIGHT).build().front().toString()).isEqualTo("""
				┌──b1──┐
				└──────┘
				""");
	}

	/**
	 * In the top and side views, the label is also written into the top edge of the face (the edge at the highest y in the top view).
	 */
	@Test
	public void testLabelIsWrittenIntoTheTopEdgeInTheTopAndSideViews() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("b1", 1, 1, 1, 0, 0, 0))
				.withScale(7, 1)
				.withAxes(false)
				.withStyle(Style.ASCII)
				.build();

		assertThat(art.top().toString()).isEqualTo("""
				+--b1--+
				+------+
				""");
		assertThat(art.side().toString()).isEqualTo("""
				+--b1--+
				+------+
				""");
	}

	/**
	 * In the oblique view, the label is written into the top edge of the front face if there is no room for it in any of the faces: A is below B
	 * so that its top face is hidden, and its right face is one line high. The front face of B is one line high too, but its top face has room.
	 */
	@Test
	public void testLabelIsWrittenIntoTheTopEdgeOfTheFrontFaceInTheObliqueView() {
		ContainerAsciiArt art = newBuilder(1, 1, 2,
				place("a1", 1, 1, 1, 0, 0, 0),
				place("b1", 1, 1, 1, 0, 0, 1))
				.withScale(7, 1)
				.withAxes(false)
				.withStyle(Style.ASCII)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  /------|
				 /  b1  /|
				|------|/|
				|--a1--|/
				|------|
				""");
	}

	/**
	 * The label is written between the corners with an edge character on each side: a face of 7 columns has 6 columns between the corners,
	 * of which 4 can be used. A longer label is truncated.
	 */
	@Test
	public void testLabelOnTheTopEdgeKeepsAnEdgeCharacterOnEachSide() {
		ContainerAsciiArt.Builder builder = newBuilder(1, 1, 1, place("abcd", 1, 1, 1, 0, 0, 0))
				.withScale(7, 1)
				.withAxes(false);

		assertThat(builder.build().front().toString()).isEqualTo("""
				┌─abcd─┐
				└──────┘
				""");

		ContainerAsciiArt longer = newBuilder(1, 1, 1, place("abcdef", 1, 1, 1, 0, 0, 0))
				.withScale(7, 1)
				.withAxes(false)
				.build();

		assertThat(longer.front().toString()).isEqualTo("""
				┌─abcd─┐
				└──────┘
				""");
	}

	/**
	 * A face with 3 columns between the corners has room for a label of one character, a face with 2 columns has no room for any label.
	 */
	@Test
	public void testLabelOnTheTopEdgeNeedsRoomForOneCharacterAndTwoEdgeCharacters() {
		ContainerAsciiArt three = newBuilder(1, 1, 1, place("ab", 1, 1, 1, 0, 0, 0)).withScale(4, 1).withAxes(false).build();

		assertThat(three.front().toString()).isEqualTo("""
				┌─a─┐
				└───┘
				""");

		ContainerAsciiArt two = newBuilder(1, 1, 1, place("ab", 1, 1, 1, 0, 0, 0)).withScale(3, 1).withAxes(false).build();

		assertThat(two.front().toString()).isEqualTo("""
				┌──┐
				└──┘
				""");
	}

	/**
	 * The label is written inside the face if the face has a line for it: only a face without a line has its label in the top edge. At one line per
	 * unit, the face of the box which is one unit high has no line, and the face of the box which is two units high has one.
	 */
	@Test
	public void testLabelIsInsideTheFaceWhenThereIsALine() {
		ContainerAsciiArt art = newBuilder(2, 1, 2,
				place("thin", 1, 1, 1, 0, 0, 0),
				place("tall", 1, 1, 2, 1, 0, 0))
				.withScale(7, 1)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				       ┌──────┐
				┌─thin─┤ tall │
				└──────┴──────┘
				""");
	}

	/**
	 * The label is not written over the corners or junctions of other boxes. A is 3 units wide and has B on top of its middle: the label of A would
	 * fit between the corners of A, but the corners of B are on the top edge of A, so it is written between them and truncated.
	 */
	@Test
	public void testLabelOnTheTopEdgeIsNotWrittenOverTheCornersOfOtherBoxes() {
		ContainerAsciiArt art = newBuilder(3, 1, 2,
				place("abcdef", 3, 1, 1, 0, 0, 0),
				place("b", 1, 1, 1, 1, 0, 1))
				.withScale(7, 1)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				       ┌──b───┐
				┌─abcd─┴──────┴──────┐
				└──────┴──────┴──────┘
				""");
	}

	/**
	 * Two boxes with the same top edge: the box that comes second gets what is left of the edge, and the labels are not written over each other. B is
	 * on top of A, and drawn with no height at this scale, so that its top edge is the top edge of A.
	 */
	@Test
	public void testLabelOnTheTopEdgeIsNotWrittenOverAnotherLabel() {
		ContainerAsciiArt art = newBuilder(8, 1, 2,
				place("A", 8, 1, 1, 0, 0, 0),
				place("B", 8, 1, 1, 0, 0, 1))
				.withScale(1, 0.5)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌─B─A───┐
				└───────┘
				""");
	}

	/**
	 * The edge of a box which is behind another box is only used where it is visible: C is behind A and B, and sticks out above B.
	 */
	@Test
	public void testLabelOnTheTopEdgeIsOnlyWrittenWhereTheEdgeIsVisible() {
		ContainerAsciiArt art = newBuilder(6, 2, 2,
				place("A", 3, 1, 2, 0, 0, 0),
				place("B", 3, 1, 1, 3, 0, 0),
				place("C", 6, 1, 2, 0, 1, 0))
				.withScale(7, 1)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌────────────────────┬─────────C──────────┐
				│         A          ├─────────B──────────┤
				└────────────────────┴────────────────────┘
				""");
	}
}
