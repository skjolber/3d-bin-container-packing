package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

/**
 * The expected drawings are checked in the order: front (x right, z up), top (x right, y up the page), side (y right, z up) and oblique (the
 * front, top and right sides of the boxes).
 */
public class ContainerAsciiArtTest {

	private static ContainerAsciiArt.Builder newBuilder(int dx, int dy, int dz, Placement... placements) {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(placements), dx, dy, dz);
	}

	/**
	 * Container 4 x 4 x 4 (x, y, z) with four boxes: A and B are on the floor next to each other, C is behind them (higher y) and taller,
	 * and D is on top of B.
	 *
	 * <pre>
	 * front: x to the right, z up       top: x to the right, y up the page
	 *
	 *     D D                           C C C C
	 * C C D D                           C C C C
	 * A A B B                           A A D D
	 * A A B B                           A A D D
	 * </pre>
	 */
	private static ContainerAsciiArt.Builder scene() {
		return newBuilder(4, 4, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0),
				place("C", 4, 2, 3, 0, 2, 0),
				place("D", 2, 2, 2, 2, 0, 2));
	}

	@Test
	public void testSingleBoxFront() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────────┐
				  │           │
				1 │     A     │
				  │           │
				0 └───────────┘
				  0   1   2   3   x
				""");
	}

	@Test
	public void testSingleBoxTop() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────────┐
				  │           │
				1 │     A     │
				  │           │
				0 └───────────┘
				  0   1   2   3   x
				""");
	}

	@Test
	public void testSingleBoxSide() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   y
				""");
	}

	@Test
	public void testSingleBoxOblique() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				z           y
				│      ┌───────────┐
				│     ╱           ╱│
				│    ╱           ╱ │
				│   ╱           ╱  │
				│  ┌───────────┐   │
				│  │           │  ╱
				│  │     A     │ ╱
				│  │           │╱
				│  └───────────┘
				│ ╱
				│╱
				└─────────────────── x
				""");
	}

	/**
	 * A (lower x) is to the left of B.
	 *
	 * <pre>
	 * A A B B
	 * A A B B
	 * </pre>
	 */
	@Test
	public void testTwoBoxesSideBySide() {
		ContainerAsciiArt art = newBuilder(4, 2, 2,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┬───────┐
				  │       │       │
				1 │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z           y
				│      ┌───────────────┐
				│     ╱       ╱       ╱│
				│    ╱       ╱       ╱ │
				│   ╱       ╱       ╱  │
				│  ┌───────┬───────┐   │
				│  │       │       │  ╱
				│  │   A   │   B   │ ╱
				│  │       │       │╱
				│  └───────┴───────┘
				│ ╱
				│╱
				└─────────────────────── x
				""");
	}

	/**
	 * B is behind A (higher y) and larger than A, so A hides a part of B in the front view, and the rest of B is drawn around A.
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
	public void testBoxPartlyHiddenBehindAnotherBox() {
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
		assertThat(art.top().toString()).isEqualTo("""
				y
				4 ┌───────────────┐
				  │               │
				3 │       B       │
				  │               │
				2 ├───────┬───────┘
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				3         │       │
				          │       │
				2 ┌───────┤   B   │
				  │       │       │
				1 │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z                   y
				│          ┌───────────────┐
				│         ╱               ╱│
				│        ╱               ╱ │
				│       ╱               ╱  │
				│      ┌───────────────┐   │
				│      │               │   │
				│      │       B       │   │
				│      │               │   │
				│      └───────┐       │   │
				│     ╱       ╱│       │  ╱
				│    ╱       ╱ │       │ ╱
				│   ╱       ╱  │       │╱
				│  ┌───────┐   └───────┘
				│  │       │  ╱
				│  │   A   │ ╱
				│  │       │╱
				│  └───────┘
				│ ╱
				│╱
				└─────────────────────────── x
				""");
	}

	@Test
	public void testStackedBoxes() {
		ContainerAsciiArt art = newBuilder(2, 2, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 0, 0, 2)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4 ┌───────┐
				  │       │
				3 │   B   │
				  │       │
				2 ├───────┤
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z               y
				│      ┌───────┐
				│     ╱       ╱│
				│    ╱       ╱ │
				│   ╱       ╱  │
				│  ┌───────┐   │
				│  │       │  ╱│
				│  │   B   │ ╱ │
				│  │       │╱  │
				│  ├───────┤   │
				│  │       │  ╱
				│  │   A   │ ╱
				│  │       │╱
				│  └───────┘
				│ ╱
				│╱
				└─────────────── x
				""");
	}

	/**
	 * B is on top of A, and overhangs it to the right.
	 *
	 * <pre>
	 *     B B B B
	 *     B B B B
	 * A A A A
	 * A A A A
	 * </pre>
	 */
	@Test
	public void testOverhang() {
		ContainerAsciiArt art = newBuilder(6, 2, 4,
				place("A", 4, 2, 2, 0, 0, 0),
				place("B", 4, 2, 2, 2, 0, 2)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4         ┌───────────────┐
				          │               │
				3         │       B       │
				          │               │
				2 ┌───────┴───────┬───────┘
				  │               │
				1 │       A       │
				  │               │
				0 └───────────────┘
				  0   1   2   3   4   5   6   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────┬───────────────┐
				  │       │               │
				1 │   A   │       B       │
				  │       │               │
				0 └───────┴───────────────┘
				  0   1   2   3   4   5   6   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z               y
				│              ┌───────────────┐
				│             ╱               ╱│
				│            ╱               ╱ │
				│           ╱               ╱  │
				│      ┌───┬───────────────┐   │
				│     ╱    │               │  ╱
				│    ╱     │       B       │ ╱
				│   ╱      │               │╱
				│  ┌───────┴───────┬───────┘
				│  │               │  ╱
				│  │       A       │ ╱
				│  │               │╱
				│  └───────────────┘
				│ ╱
				│╱
				└─────────────────────────────── x
				""");
	}

	@Test
	public void testObstacle() {
		ContainerAsciiArt art = newBuilder(4, 2, 2, place("A", 2, 2, 2, 2, 0, 0))
				.withObstacles(List.of(place("X", 2, 2, 2, 0, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┬───────┐
				  │#######│       │
				1 │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────┬───────┐
				  │#######│       │
				1 │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z           y
				│      ┌───────────────┐
				│     ╱#######╱       ╱│
				│    ╱#######╱       ╱ │
				│   ╱#######╱       ╱  │
				│  ┌───────┬───────┐   │
				│  │#######│       │  ╱
				│  │#######│   A   │ ╱
				│  │#######│       │╱
				│  └───────┴───────┘
				│ ╱
				│╱
				└─────────────────────── x
				""");
	}

	@Test
	public void testEmptyContainer() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2

				1

				0
				  0   1   2   3   4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2

				1

				0
				  0   1   2   3   4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2

				1

				0
				  0   1   2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z           y
				│          ╱
				│         ╱
				│        ╱
				│       ╱
				│      ╱
				│     ╱
				│    ╱
				│   ╱
				│  ╱
				│ ╱
				│╱
				└─────────────────────── x
				""");
	}

	@Test
	public void testEmptyContainerWithoutAxesDrawsNothing() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).withAxes(false).build();

		assertThat(art.front().toString()).isEmpty();
		assertThat(art.top().toString()).isEmpty();
		assertThat(art.side().toString()).isEmpty();
		assertThat(art.oblique().toString()).isEmpty();
	}

	@Test
	public void testWithoutAxesAndOutlineBlankLinesAboveAndBelowAreLeftOut() {
		// the box is in the middle of the container, in the air
		ContainerAsciiArt art = newBuilder(2, 1, 3, place("A", 2, 1, 1, 0, 0, 1))
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   A   │
				│       │
				└───────┘
				""");
	}

	@Test
	public void testWithOutlineTheContainerIsDrawnAboveAndBelowTheBox() {
		ContainerAsciiArt art = newBuilder(2, 1, 3, place("A", 2, 1, 1, 0, 0, 1))
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				.........
				.       .
				.       .
				.       .
				┌───────┐
				│       │
				│   A   │
				│       │
				└───────┘
				.       .
				.       .
				.       .
				.........
				""");
	}

	@Test
	public void testEmptyContainerWithOutline() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).withContainerOutline(true).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 .................
				  .               .
				1 .               .
				  .               .
				0 .................
				  0   1   2   3   4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z           y
				│      ....╱............
				│     ..  ╱           ..
				│    . . ╱           . .
				│   .  .╱           .  .
				│  ....╱................
				│  .  ╱            .  .
				│  . ╱             . .
				│  .╱              ..
				│  ╱................
				│ ╱
				│╱
				└─────────────────────── x
				""");
	}

	@Test
	public void testSceneFront() {
		assertThat(scene().build().front().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				3 ┌───────┤   D   │
				  │   C   │       │
				2 ├───────┼───────┤
				  │       │       │
				1 │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
	}

	@Test
	public void testSceneTop() {
		assertThat(scene().build().top().toString()).isEqualTo("""
				y
				4 ┌───────────────┐
				  │               │
				3 │       C       │
				  │               │
				2 ├───────┬───────┤
				  │       │       │
				1 │   A   │   D   │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
	}

	@Test
	public void testSceneSide() {
		assertThat(scene().build().side().toString()).isEqualTo("""
				z
				4 ┌───────┐
				  │       │
				3 │   D   ├───────┐
				  │       │       │
				2 ├───────┤       │
				  │       │   C   │
				1 │   B   │       │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   y
				""");
	}

	@Test
	public void testSceneOblique() {
		assertThat(scene().build().oblique().toString()).isEqualTo("""
				z                   y
				│                  ╱
				│                 ╱
				│          ┌───────────────┐
				│         ╱       C       ╱│
				│        ╱     ┌───────┐ ╱ │
				│       ╱     ╱       ╱│╱  │
				│      ┌─────╱       ╱ │   │
				│      │    ╱       ╱  │   │
				│      └───┬───────┐   │   │
				│     ╱    │       │  ╱│  ╱
				│    ╱     │   D   │ ╱ │ ╱
				│   ╱      │       │╱  │╱
				│  ┌───────┼───────┤   │
				│  │       │       │  ╱
				│  │   A   │   B   │ ╱
				│  │       │       │╱
				│  └───────┴───────┘
				│ ╱
				│╱
				└─────────────────────────── x
				""");
	}

	@Test
	public void testSceneWithoutAxes() {
		ContainerAsciiArt art = scene().withAxes(false).build();

		assertThat(art.front().toString()).isEqualTo("""
				        ┌───────┐
				        │       │
				┌───────┤   D   │
				│   C   │       │
				├───────┼───────┤
				│       │       │
				│   A   │   B   │
				│       │       │
				└───────┴───────┘
				""");
		assertThat(art.top().toString()).isEqualTo("""
				┌───────────────┐
				│               │
				│       C       │
				│               │
				├───────┬───────┤
				│       │       │
				│   A   │   D   │
				│       │       │
				└───────┴───────┘
				""");
		assertThat(art.side().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   D   ├───────┐
				│       │       │
				├───────┤       │
				│       │   C   │
				│   B   │       │
				│       │       │
				└───────┴───────┘
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				        ┌───────────────┐
				       ╱       C       ╱│
				      ╱     ┌───────┐ ╱ │
				     ╱     ╱       ╱│╱  │
				    ┌─────╱       ╱ │   │
				    │    ╱       ╱  │   │
				    └───┬───────┐   │   │
				   ╱    │       │  ╱│  ╱
				  ╱     │   D   │ ╱ │ ╱
				 ╱      │       │╱  │╱
				┌───────┼───────┤   │
				│       │       │  ╱
				│   A   │   B   │ ╱
				│       │       │╱
				└───────┴───────┘
				""");
	}

	@Test
	public void testSceneWithContainerOutline() {
		ContainerAsciiArt art = scene().withContainerOutline(true).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4 ........┌───────┐
				  .       │       │
				3 ┌───────┤   D   │
				  │   C   │       │
				2 ├───────┼───────┤
				  │       │       │
				1 │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z                   y
				│          ........╱........
				│         ..      ╱       ..
				│        . ┌───────────────┐
				│       . ╱       C       ╱│
				│      . ╱     ┌───────┐ ╱ │
				│     . ╱     ╱       ╱│╱  │
				│    . ┌─────╱       ╱ │   │
				│   .  │    ╱       ╱  │   │
				│  ....└───┬───────┐   │   │
				│  .  ╱    │       │  ╱│  ╱
				│  . ╱     │   D   │ ╱ │ ╱
				│  .╱      │       │╱  │╱
				│  ┌───────┼───────┤   │
				│  │       │       │  ╱
				│  │   A   │   B   │ ╱
				│  │       │       │╱
				│  └───────┴───────┘
				│ ╱
				│╱
				└─────────────────────────── x
				""");
	}

	@Test
	public void testSceneWithoutAxesWithContainerOutline() {
		ContainerAsciiArt art = scene()
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				        .................
				       ..              ..
				      . ┌───────────────┐
				     . ╱       C       ╱│
				    . ╱     ┌───────┐ ╱ │
				   . ╱     ╱       ╱│╱  │
				  . ┌─────╱       ╱ │   │
				 .  │    ╱       ╱  │   │
				....└───┬───────┐   │   │
				.  ╱    │       │  ╱│  ╱
				. ╱     │   D   │ ╱ │ ╱
				.╱      │       │╱  │╱
				┌───────┼───────┤   │
				│       │       │  ╱
				│   A   │   B   │ ╱
				│       │       │╱
				└───────┴───────┘
				""");
	}

	@Test
	public void testComment() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).withComment("// ").build();

		assertThat(art.oblique().toString()).isEqualTo("""
				// z           y
				// │      ┌───────────┐
				// │     ╱           ╱│
				// │    ╱           ╱ │
				// │   ╱           ╱  │
				// │  ┌───────────┐   │
				// │  │           │  ╱
				// │  │     A     │ ╱
				// │  │           │╱
				// │  └───────────┘
				// │ ╱
				// │╱
				// └─────────────────── x
				""");
	}

	@Test
	public void testCommentOfBlankLinesHasNoTrailingBlank() {
		// two boxes with space between them: three blank lines
		ContainerAsciiArt art = newBuilder(2, 1, 3,
				place("A", 2, 1, 1, 0, 0, 0),
				place("B", 2, 1, 1, 0, 0, 2))
				.withAxes(false)
				.withComment("// ")
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				// ┌───────┐
				// │       │
				// │   B   │
				// │       │
				// └───────┘
				//
				//
				//
				// ┌───────┐
				// │       │
				// │   A   │
				// │       │
				// └───────┘
				""");
	}

	/**
	 * Of the boxes in the same place in the view, the nearest is drawn: from the front the one with the lowest y.
	 */
	@Test
	public void testFrontViewNearestIsLowestY() {
		ContainerAsciiArt art = newBuilder(2, 4, 2,
				place("B", 2, 2, 2, 0, 2, 0),
				place("A", 2, 2, 2, 0, 0, 0)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   x
				""");
	}

	/**
	 * Of the boxes in the same place in the view, the nearest is drawn: from the top the one with the highest z.
	 */
	@Test
	public void testTopViewNearestIsHighestZ() {
		ContainerAsciiArt art = newBuilder(2, 2, 4,
				place("B", 2, 2, 2, 0, 0, 2),
				place("A", 2, 2, 2, 0, 0, 0)).build();

		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────┐
				  │       │
				1 │   B   │
				  │       │
				0 └───────┘
				  0   1   2   x
				""");
	}

	/**
	 * Of the boxes in the same place in the view, the nearest is drawn: from the right (the side view) the one with the highest x.
	 */
	@Test
	public void testSideViewNearestIsHighestX() {
		ContainerAsciiArt art = newBuilder(4, 2, 2,
				place("B", 2, 2, 2, 2, 0, 0),
				place("A", 2, 2, 2, 0, 0, 0)).build();

		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				1 │   B   │
				  │       │
				0 └───────┘
				  0   1   2   y
				""");
	}

	/**
	 * B is to the right of A (higher x), further up the page in the top view (higher y), further to the right in the side view (higher y), and higher than A (higher z).
	 */
	@Test
	public void testOrientation() {
		ContainerAsciiArt art = newBuilder(4, 4, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 2, 2)).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				3         │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4         ┌───────┐
				          │       │
				3         │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   3   4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				3         │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   3   4   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z                   y
				│                  ┌───────┐
				│                 ╱       ╱│
				│                ╱       ╱ │
				│               ╱       ╱  │
				│              ┌───────┐   │
				│             ╱│       │  ╱
				│            ╱ │   B   │ ╱
				│           ╱  │       │╱
				│      ┌───────┼───────┘
				│     ╱       ╱│
				│    ╱       ╱ │
				│   ╱       ╱  │
				│  ┌───────┐   │
				│  │       │  ╱
				│  │   A   │ ╱
				│  │       │╱
				│  └───────┘
				│ ╱
				│╱
				└─────────────────────────── x
				""");
	}

	/**
	 * The values of the units are at the axes: of z on the left and of x below.
	 *
	 * <pre>
	 * C
	 * A B
	 * </pre>
	 */
	@Test
	public void testCoordinates() {
		ContainerAsciiArt art = newBuilder(2, 1, 2,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 1, 0, 0),
				place("C", 1, 1, 1, 0, 0, 1))
				.withScale(8, 2)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │   C   │
				1 ├───────┼───────┐
				  │   A   │   B   │
				0 └───────┴───────┘
				  0       1       2   x
				""");
		// y is up the page
		assertThat(art.top().toString()).isEqualTo("""
				y
				1 ┌───────┬───────┐
				  │   C   │   B   │
				0 └───────┴───────┘
				  0       1       2   x
				""");
		// y is to the right
		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │   C   │
				1 ├───────┤
				  │   B   │
				0 └───────┘
				  0       1   y
				""");
	}

	@Test
	public void testCoordinatesWithSeveralDigits() {
		ContainerAsciiArt art = newBuilder(10, 1, 10, place("A", 10, 1, 10, 0, 0, 0))
				.withScale(3, 1)
				.build();

		// the values on the left are aligned to the right
		assertThat(art.front().toString()).isEqualTo("""
				z
				10 ┌─────────────────────────────┐
				 9 │                             │
				 8 │                             │
				 7 │                             │
				 6 │                             │
				 5 │              A              │
				 4 │                             │
				 3 │                             │
				 2 │                             │
				 1 │                             │
				 0 └─────────────────────────────┘
				   0  1  2  3  4  5  6  7  8  9  10   x
				""");
	}

	/**
	 * The values are written for every second unit when the values for every unit would be too close: there are two columns per unit, a value
	 * with two digits needs three, but there is a line for every unit.
	 */
	@Test
	public void testCoordinatesForEverySecondUnit() {
		ContainerAsciiArt art = newBuilder(20, 2, 4, place("A", 20, 2, 4, 0, 0, 0))
				.withWidth(41)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4 ┌───────────────────────────────────────┐
				3 │                                       │
				2 │                   A                   │
				1 │                                       │
				0 └───────────────────────────────────────┘
				  0   2   4   6   8   10  12  14  16  18  20   x
				""");
	}

	/**
	 * The container is 100 x 100 units, drawn in 41 columns: the values are written for every 10th unit along x and every 5th unit along z.
	 */
	@Test
	public void testCoordinatesForEveryFifthAndTenthUnit() {
		ContainerAsciiArt art = newBuilder(100, 1, 100, place("A", 50, 1, 50, 0, 0, 0))
				.withWidth(41)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				100
				 95
				 90
				 85
				 80
				 75
				 70
				 65
				 60
				 55
				 50 ┌───────────────────┐
				 45 │                   │
				 40 │                   │
				 35 │                   │
				 30 │                   │
				 25 │         A         │
				 20 │                   │
				 15 │                   │
				 10 │                   │
				  5 │                   │
				  0 └───────────────────┘
				    0   10  20  30  40  50  60  70  80  90  100   x
				""");
	}

	@Test
	public void testWithoutAxesThereAreNoCoordinates() {
		ContainerAsciiArt art = newBuilder(2, 1, 2, place("A", 2, 1, 2, 0, 0, 0))
				.withScale(4, 2)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───────┐
				│       │
				│   A   │
				│       │
				└───────┘
				""");
		assertThat(art.top().toString()).doesNotContain("0").doesNotContain("y").doesNotContain("x");
		assertThat(art.side().toString()).doesNotContain("0").doesNotContain("y").doesNotContain("z");
	}

	@Test
	public void testWithContainer() {
		Stack stack = new Stack();
		stack.add(place("A", 2, 2, 2, 2, 0, 0));

		Container container = Container.newBuilder()
				.withSize(4, 2, 2)
				.withMaxLoadWeight(10)
				.withStack(stack)
				.build()
				.withObstacles(List.of(place("X", 2, 2, 2, 0, 0, 0)));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withContainer(container).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┬───────┐
				  │#######│       │
				1 │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0   1   2   3   4   x
				""");
	}

	@Test
	public void testWithContainerUsesTheLoadSize() {
		Stack stack = new Stack();
		stack.add(place("A", 2, 2, 2, 0, 0, 0));

		Container container = Container.newBuilder()
				.withSize(6, 6, 6)
				.withLoadSize(2, 2, 2)
				.withMaxLoadWeight(10)
				.withStack(stack)
				.build();

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withContainer(container).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   x
				""");
	}

	@Test
	public void testWithStack() {
		Stack stack = new Stack();
		stack.add(place("A", 2, 2, 2, 0, 0, 0));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withStack(stack, 4, 2, 2).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				1 │   A   │
				  │       │
				0 └───────┘
				  0   1   2   3   4   x
				""");
	}

	@Test
	public void testExplicitScale() {
		ContainerAsciiArt art = newBuilder(4, 2, 2,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 1, 2, 0, 0)).withScale(3, 1).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌─────┐
				1 │  A  ├─────┐
				0 └─────┴─────┘
				  0  1  2  3  4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌─────┬─────┐
				1 │  A  │  B  │
				0 └─────┴─────┘
				  0  1  2  3  4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌─────┐
				1 ├─────┤
				0 └─────┘
				  0  1  2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				z       y
				│    ┌─────┐
				│   ╱     ╱└─────┐
				│  ┌─────┐╱  B  ╱│
				│  │  A  ├─────┐╱
				│  └─────┴─────┘
				│ ╱
				│╱
				└───────────────── x
				""");
	}

	/**
	 * Container 10 x 6 x 6 in 21 columns: two columns and one line per unit in the front and top views. In the side view, the 6 units of y are 21 columns.
	 */
	@Test
	public void testWidth() {
		ContainerAsciiArt art = newBuilder(10, 6, 6,
				place("A", 5, 3, 4, 0, 0, 0),
				place("B", 5, 3, 2, 5, 0, 0),
				place("C", 10, 3, 3, 0, 3, 0))
				.withWidth(21)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌─────────┐
				│         ├─────────┐
				│    A    ├─────────┤
				│         │    B    │
				└─────────┴─────────┘
				""");
		assertThat(art.top().toString()).isEqualTo("""
				┌───────────────────┐
				│         C         │
				│                   │
				├─────────┬─────────┤
				│    A    │    B    │
				│         │         │
				└─────────┴─────────┘
				""");
		assertThat(art.side().toString()).isEqualTo("""
				┌─────────┐
				│         │
				│    A    ├─────────┐
				│         │         │
				├─────────┤    C    │
				│    B    │         │
				│         │         │
				└─────────┴─────────┘
				""");
	}

	@Test
	public void testWidthMeasuresTheContainerExcludingAxes() {
		ContainerAsciiArt art = newBuilder(1000, 500, 400, place("A", 1000, 500, 400, 0, 0, 0))
				.withWidth(31)
				.withAxes(false)
				.build();

		assertThat(art.front().getWidth()).isEqualTo(31);
		assertThat(art.top().getWidth()).isEqualTo(31);
		assertThat(art.side().getWidth()).isEqualTo(31);
		assertThat(art.oblique().getWidth()).isEqualTo(31);

		assertThat(art.front().toString()).isEqualTo("""
				┌─────────────────────────────┐
				│                             │
				│                             │
				│              A              │
				│                             │
				│                             │
				└─────────────────────────────┘
				""");
	}

	@Test
	public void testFractionalScaleKeepsBoxesJoined() {
		// 1.5 columns per unit: the edge between the boxes is at the same column for both
		ContainerAsciiArt art = newBuilder(6, 1, 4,
				place("A", 3, 1, 4, 0, 0, 0),
				place("B", 3, 1, 4, 3, 0, 0))
				.withScale(1.5, 1)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌────┬───┐
				│    │   │
				│ A  │ B │
				│    │   │
				└────┴───┘
				""");
	}
}
