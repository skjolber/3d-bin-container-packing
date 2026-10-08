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
				1 ┌───┬───┬───┐
				  │   │   │   │
				  │ A │ Y │ C │
				  │   │   │   │
				0 └───┴───┴───┘
				  0   1   2   3   x
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
				1 ┌────┬────┬────┐
				  │    │    │    │
				  │ x0 │ B  │ x2 │
				  │    │    │    │
				0 └────┴────┴────┘
				  0    1    2    3   x
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
	 * and 4 lines per unit (the label, a blank line above and below, and the edges).
	 */
	@Test
	public void testDefaultScaleFitsTheLabel() {
		ContainerAsciiArt art = newBuilder(1, 1, 1, place("AB", 1, 1, 1, 0, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌────┐
				  │    │
				  │ AB │
				  │    │
				0 └────┘
				  0    1   x
				""");
	}

	/**
	 * The scale is the smallest at which the labels of 90 % of the boxes fit; the box with the long label is more than 10 % but not shown in full.
	 */
	@Test
	public void testDefaultScaleFitsNinetyPercentOfTheLabels() {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			placements.add(place(String.valueOf((char) ('A' + i)), 1, 1, 1, i, 0, 0));
		}
		placements.add(place("LONGLABEL", 1, 1, 1, 9, 0, 0));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(placements, 10, 1, 1)
				.withAxes(false)
				.build();

		// 4 columns and 4 lines per unit: the long label is truncated
		assertThat(art.front().toString()).isEqualTo("""
				┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
				│   │   │   │   │   │   │   │   │   │   │
				│ A │ B │ C │ D │ E │ F │ G │ H │ I │ L │
				│   │   │   │   │   │   │   │   │   │   │
				└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
				""");
	}

	/**
	 * When fewer than 90 % of the labels fit, the scale grows until they do.
	 */
	@Test
	public void testDefaultScaleGrowsUntilNinetyPercentOfTheLabelsFit() {
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			placements.add(place(String.valueOf((char) ('A' + i)), 1, 1, 1, i, 0, 0));
		}
		placements.add(place("LONG", 1, 1, 1, 8, 0, 0));
		placements.add(place("LONG", 1, 1, 1, 9, 0, 0));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(placements, 10, 1, 1)
				.withAxes(false)
				.build();

		// 7 columns per unit for a label with four characters
		assertThat(art.front().toString()).isEqualTo("""
				┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
				│      │      │      │      │      │      │      │      │      │      │
				│  A   │  B   │  C   │  D   │  E   │  F   │  G   │  H   │ LONG │ LONG │
				│      │      │      │      │      │      │      │      │      │      │
				└──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘
				""");
	}

	@Test
	public void testDefaultScaleIsAtLeastFourColumnsByTwoLines() {
		ContainerAsciiArt art = newBuilder(8, 4, 4, place("A", 8, 4, 4, 0, 0, 0)).withAxes(false).build();

		assertThat(art.front().getWidth()).isEqualTo(8 * 4 + 1);
		assertThat(art.front().getHeight()).isEqualTo(4 * 2 + 1);
	}

	/**
	 * The scale follows the faces which are visible in the view: the box is only one unit high, but two units deep and wide.
	 */
	@Test
	public void testDefaultScaleIsPerView() {
		ContainerAsciiArt art = newBuilder(3, 4, 4, place("A", 3, 2, 1, 0, 0, 0))
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		// the front shows the face 3 x 1: 4 lines per unit of z
		assertThat(art.front().getHeight()).isEqualTo(4 * 4 + 1);
		// the top shows the face 3 x 2: 2 lines per unit of y
		assertThat(art.top().getHeight()).isEqualTo(4 * 2 + 1);
		// the side shows the face 2 x 1: 4 lines per unit of z, and 4 columns per unit of y
		assertThat(art.side().getHeight()).isEqualTo(4 * 4 + 1);
		assertThat(art.side().getWidth()).isEqualTo(4 * 4 + 1);
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
				  0   1   2   x
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
				3 │       B       │
				  │               │
				2 ├───────┐       │
				  │       │       │
				1 │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
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
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   x
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
				       ╱       ╱│
				      ╱   B   ╱ │
				     ╱       ╱  │
				    ────────┐   │
				   ╱       ╱│  ╱
				  ╱       ╱ │ ╱
				 ╱       ╱  │╱
				┌───────┐   │
				│       │  ╱
				│   A   │ ╱
				│       │╱
				└───────┘
				""");
	}
}
