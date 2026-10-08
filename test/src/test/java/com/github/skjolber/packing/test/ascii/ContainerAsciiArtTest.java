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
				  │     A     │
				  │           │
				0 └───────────┘
				  0           3   x
				""");
	}

	@Test
	public void testSingleBoxTop() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────────┐
				  │           │
				  │     A     │
				  │           │
				0 └───────────┘
				  0           3   x
				""");
	}

	@Test
	public void testSingleBoxSide() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────┐
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   y
				""");
	}

	@Test
	public void testSingleBoxOblique() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ ┌───────────┐   y
				  │╱           ╱│
				2 ┌───────────┐ │ ╱
				  │           │ │╱
				  │     A     │ │ 2
				  │           │╱
				0 └───────────┘── x
				  0           3
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
				  │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ ┌───────────────┐   y
				  │╱       ╱       ╱│
				2 ┌───────┬───────┐ │ ╱
				  │       │       │ │╱
				  │   A   │   B   │ │ 2
				  │       │       │╱
				0 └───────┴───────┘── x
				  0       2       4
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
				  │       B       │
				  │               │
				2 ├───────┐       │
				  │       │       │
				  │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4 ┌───────────────┐
				  │               │
				  │       B       │
				  │               │
				2 ├───────┬───────┘
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2       4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				          │       │
				          │       │
				2 ┌───────┤   B   │
				  │       │       │
				  │   A   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z   ┌───────────────┐
				     ╱               ╱│
				  │ ┌───────────────┐ │
				  │ │               │ │
				4 │ │       B       │ │   y
				  │ │               │ │
				  │ └───────┐       │ │ ╱
				  │╱       ╱│       │ │╱
				2 ┌───────┐ │       │ │ 4
				  │       │ │       │╱
				  │   A   │ └───────┘ 2
				  │       │╱       ╱
				0 └───────┘────────── x
				  0       2       4
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
				  │   B   │
				  │       │
				2 ├───────┤
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ ┌───────┐
				  │╱       ╱│
				4 ┌───────┐ │
				  │       │ │
				  │   B   │ │   y
				  │       │╱│
				2 ├───────┤ │ ╱
				  │       │ │╱
				  │   A   │ │ 2
				  │       │╱
				0 └───────┘── x
				  0       2
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
				          │       B       │
				          │               │
				2 ┌───────┴───────┬───────┘
				  │               │
				  │       A       │
				  │               │
				0 └───────────────┘
				  0       2       4       6   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────┬───────────────┐
				  │       │               │
				  │   A   │       B       │
				  │       │               │
				0 └───────┴───────────────┘
				  0       2       4       6   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │         ┌───────────────┐
				  │        ╱               ╱│
				4 │       ┌───────────────┐ │
				  │       │               │ │
				  │ ┌─────│       B       │ │   y
				  │╱      │               │╱
				2 ┌───────┴───────┬───────┘   ╱
				  │               │ │        ╱
				  │       A       │ │       ╱ 2
				  │               │╱       ╱
				0 └───────────────┘────────── x
				  0       2       4       6
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
				  │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────┬───────┐
				  │#######│       │
				  │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ ┌───────────────┐   y
				  │╱#######╱       ╱│
				2 ┌───────┬───────┐ │ ╱
				  │#######│       │ │╱
				  │#######│   A   │ │ 2
				  │#######│       │╱
				0 └───────┴───────┘── x
				  0       2       4
				""");
	}

	@Test
	public void testEmptyContainer() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2



				0
				  0               4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2



				0
				  0               4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2



				0
				  0       2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │                     y
				  │
				2 │                   ╱
				  │                  ╱
				  │                 ╱ 2
				  │                ╱
				0 └────────────────── x
				  0               4
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
				┌───────────────┐
				│               │
				│       A       │
				│               │
				└───────────────┘
				""");
	}

	@Test
	public void testWithOutlineTheContainerIsDrawnAboveAndBelowTheBox() {
		ContainerAsciiArt art = newBuilder(2, 1, 3, place("A", 2, 1, 1, 0, 0, 1))
				.withAxes(false)
				.withContainerOutline(true)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				.................
				.               .
				.               .
				.               .
				┌───────────────┐
				│               │
				│       A       │
				│               │
				└───────────────┘
				.               .
				.               .
				.               .
				.................
				""");
	}

	@Test
	public void testEmptyContainerWithOutline() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).withContainerOutline(true).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 .................
				  .               .
				  .               .
				  .               .
				0 .................
				  0               4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │ .................   y
				  │..              ..
				2 │................ . ╱
				  │ .             . .╱
				  │ ................╱ 2
				  │.              .╱
				0 └────────────────── x
				  0               4
				""");
	}

	/**
	 * The container is 10 x 10 x 10, but the boxes only reach 4 x 2 x 2: every view is drawn as far as the boxes reach, as for a container of that
	 * size. The axes end there, and the size of the container is not written.
	 */
	@Test
	public void testDrawingReachesAsFarAsTheBoxesInEveryView() {
		ContainerAsciiArt art = newBuilder(10, 10, 10,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 1, 1, 2, 0, 0))
				.build();
		ContainerAsciiArt exact = newBuilder(4, 2, 2,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 1, 1, 2, 0, 0))
				.build();

		assertThat(art.front().toString()).isEqualTo(exact.front().toString());
		assertThat(art.top().toString()).isEqualTo(exact.top().toString());
		assertThat(art.side().toString()).isEqualTo(exact.side().toString());
		assertThat(art.oblique().toString()).isEqualTo(exact.oblique().toString());

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───────────────┐
				  │               │
				  │               │
				  │               │
				1 │       A       ├───────────────┐
				  │               │               │
				  │               │       B       │
				  │               │               │
				0 └───────────────┴───────────────┘
				  0               2               4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌───────────────┐
				  │               │
				  │               │
				  │               │
				1 │       A       ├───────────────┐
				  │               │               │
				  │               │       B       │
				  │               │               │
				0 └───────────────┴───────────────┘
				  0               2               4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌───────────────┐
				  │               │
				  │       A       │
				  │               │
				1 ├───────┐       │
				  │       │       │
				  │   B   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       1       2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z   ┌───────────────┐
				     ╱               ╱│
				  │ ╱               ╱ │
				  │╱               ╱  │
				2 ┌───────────────┐   │                   y
				  │               │   │
				  │               │ ┌───────────────┐   ╱
				  │               │╱               ╱│  ╱
				1 │       A       ├───────────────┐ │ ╱ 2
				  │               │               │ │╱
				  │               │       B       │ │ 1
				  │               │               │╱
				0 └───────────────┴───────────────┘── x
				  0               2               4
				""");
	}

	/**
	 * Obstacles count as boxes for how far the drawing reaches: here the obstacle is the farthest in x, and the box is the farthest in z.
	 */
	@Test
	public void testDrawingReachesAsFarAsTheObstacles() {
		ContainerAsciiArt art = newBuilder(10, 10, 10, place("A", 1, 1, 2, 0, 0, 0))
				.withObstacles(List.of(place("X", 2, 1, 1, 3, 0, 0)))
				.build();
		ContainerAsciiArt exact = newBuilder(5, 1, 2, place("A", 1, 1, 2, 0, 0, 0))
				.withObstacles(List.of(place("X", 2, 1, 1, 3, 0, 0)))
				.build();

		assertThat(art.front().toString()).isEqualTo(exact.front().toString());
		assertThat(art.top().toString()).isEqualTo(exact.top().toString());
		assertThat(art.side().toString()).isEqualTo(exact.side().toString());
		assertThat(art.oblique().toString()).isEqualTo(exact.oblique().toString());

		assertThat(art.front().toString()).isEqualTo("""
				z
				2 ┌───┐
				  │   │
				1 │ A │       ┌───────┐
				  │   │       │#######│
				0 └───┘       └───────┘
				  0   1       3       5   x
				""");
	}

	/**
	 * The same container and boxes, with the container outline: every view is drawn to the size of the container, which is written at the axes.
	 */
	@Test
	public void testContainerOutlineKeepsTheSizeOfTheContainerInEveryView() {
		ContainerAsciiArt art = newBuilder(6, 4, 3,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 1, 1, 2, 0, 0))
				.withContainerOutline(true)
				.build();
		ContainerAsciiArt cropped = newBuilder(6, 4, 3,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 1, 1, 2, 0, 0))
				.build();

		assertThat(art.front().getWidth()).isGreaterThan(cropped.front().getWidth());
		assertThat(art.top().getHeight()).isGreaterThan(cropped.top().getHeight());
		assertThat(art.side().getWidth()).isGreaterThan(cropped.side().getWidth());
		assertThat(art.oblique().getHeight()).isGreaterThan(cropped.oblique().getHeight());

		assertThat(art.front().toString()).isEqualTo("""
				z
				3 .................................................
				  .                                               .
				  .                                               .
				  .                                               .
				2 ┌───────────────┐                               .
				  │               │                               .
				  │               │                               .
				  │               │                               .
				1 │       A       ├───────────────┐               .
				  │               │               │               .
				  │               │       B       │               .
				  │               │               │               .
				0 └───────────────┴───────────────┘................
				  0               2               4               6   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4 .................................................
				  .                                               .
				  .                                               .
				  .                                               .
				  .                                               .
				  .                                               .
				  .                                               .
				  .                                               .
				2 ┌───────────────┐                               .
				  │               │                               .
				  │               │                               .
				  │               │                               .
				1 │       A       ├───────────────┐               .
				  │               │               │               .
				  │               │       B       │               .
				  │               │               │               .
				0 └───────────────┴───────────────┘................
				  0               2               4               6   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				3 .................................
				  .                               .
				  .                               .
				  .                               .
				2 ┌───────────────┐               .
				  │               │               .
				  │       A       │               .
				  │               │               .
				1 ├───────┐       │               .
				  │       │       │               .
				  │   B   │       │               .
				  │       │       │               .
				0 └───────┴───────┘................
				  0       1       2               4   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				          .................................................
				         ..                                              ..
				        . .                                             . .
				       .  .                                            .  .
				  z   .   .                                           .   .
				     .    .                                          .    .
				  │ .     .                                         .     .
				  │.      .                                        .      .
				3 │...┌───────────────┐............................       .   y
				  │  ╱               ╱│                           .       .
				  │ ╱               ╱ │                           .       . ╱
				  │╱               ╱  │                           .       .╱
				2 ┌───────────────┐   │...................................╱ 4
				  │               │   │                           .      ╱
				  │               │ ┌───────────────┐             .     ╱
				  │               │╱               ╱│             .    ╱
				1 │       A       ├───────────────┐ │             .   ╱ 2
				  │               │               │ │             .  ╱
				  │               │       B       │ │             . ╱ 1
				  │               │               │╱              .╱
				0 └───────────────┴───────────────┘────────────────── x
				  0               2               4               6
				""");
	}

	/**
	 * The end of the drawing is labelled with the size minus one, with the inclusive coordinates: also when it is where the boxes end, not the
	 * size of the container.
	 */
	@Test
	public void testInclusiveCoordinatesOfTheEndOfTheBoxes() {
		ContainerAsciiArt art = newBuilder(10, 10, 10, place("A", 3, 1, 2, 0, 0, 0))
				.withInclusiveCoordinates(true)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────────┐
				  │           │
				  │     A     │
				  │           │
				0 └───────────┘
				  0           2   x
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
				  │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
	}

	@Test
	public void testSceneTop() {
		assertThat(scene().build().top().toString()).isEqualTo("""
				y
				4 ┌───────────────┐
				  │               │
				  │       C       │
				  │               │
				2 ├───────┬───────┤
				  │       │       │
				  │   A   │   D   │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
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
				  │   B   │       │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   y
				""");
	}

	@Test
	public void testSceneOblique() {
		assertThat(scene().build().oblique().toString()).isEqualTo("""
				  z

				  │   ┌─────┌───────┐─┐
				  │  ╱     ╱       ╱│╱│
				4 │ ┌─────┌───────┐ │ │   y
				  │ │     │       │ │ │
				  │ └─────│   D   │ │C│ ╱
				  │╱      │       │╱│ │╱
				2 ┌───────┼───────┤ │ │ 4
				  │       │       │ │╱
				  │   A   │   B   │ │ 2
				  │       │       │╱
				0 └───────┴───────┘── x
				  0       2       4
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
				    ┌─────┌───────┐─┐
				   ╱     ╱       ╱│╱│
				  ┌─────┌───────┐ │ │
				  │     │       │ │ │
				  └─────│   D   │ │C│
				 ╱      │       │╱│ │
				┌───────┼───────┤ │ │
				│       │       │ │╱
				│   A   │   B   │ │
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
				  │   A   │   B   │
				  │       │       │
				0 └───────┴───────┘
				  0       2       4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z   .................
				     ..              ..
				  │ . ┌─────┌───────┐─┐
				  │. ╱     ╱       ╱│╱│
				4 │.┌─────┌───────┐ │ │   y
				  │ │     │       │ │ │
				  │ └─────│   D   │ │C│ ╱
				  │╱      │       │╱│ │╱
				2 ┌───────┼───────┤ │ │ 4
				  │       │       │ │╱
				  │   A   │   B   │ │ 2
				  │       │       │╱
				0 └───────┴───────┘── x
				  0       2       4
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
				  . ┌─────┌───────┐─┐
				 . ╱     ╱       ╱│╱│
				..┌─────┌───────┐ │ │
				. │     │       │ │ │
				. └─────│   D   │ │C│
				.╱      │       │╱│ │
				┌───────┼───────┤ │ │
				│       │       │ │╱
				│   A   │   B   │ │
				│       │       │╱
				└───────┴───────┘
				""");
	}

	@Test
	public void testComment() {
		ContainerAsciiArt art = newBuilder(3, 2, 2, place("A", 3, 2, 2, 0, 0, 0)).withComment("// ").build();

		assertThat(art.oblique().toString()).isEqualTo("""
				//   z
				//
				//   │ ┌───────────┐   y
				//   │╱           ╱│
				// 2 ┌───────────┐ │ ╱
				//   │           │ │╱
				//   │     A     │ │ 2
				//   │           │╱
				// 0 └───────────┘── x
				//   0           3
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
				// ┌───────────────┐
				// │               │
				// │       B       │
				// │               │
				// └───────────────┘
				//
				//
				//
				// ┌───────────────┐
				// │               │
				// │       A       │
				// │               │
				// └───────────────┘
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
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
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
				  │   B   │
				  │       │
				0 └───────┘
				  0       2   x
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
				  │   B   │
				  │       │
				0 └───────┘
				  0       2   y
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
				          │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2       4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4         ┌───────┐
				          │       │
				          │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2       4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4         ┌───────┐
				          │       │
				          │   B   │
				          │       │
				2 ┌───────┼───────┘
				  │       │
				  │   A   │
				  │       │
				0 └───────┘
				  0       2       4   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z           ┌───────┐
				             ╱       ╱│
				  │         ┌───────┐ │
				  │         │       │ │
				4 │         │   B   │ │   y
				  │         │       │╱
				  │ ┌───────┼───────┘   ╱
				  │╱       ╱│          ╱
				2 ┌───────┐ │         ╱ 4
				  │       │ │        ╱
				  │   A   │ │       ╱ 2
				  │       │╱       ╱
				0 └───────┘────────── x
				  0       2       4
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

	/**
	 * The coordinates are 0, where the boxes start and end (the farthest is the end of the drawing): not at every unit. The values on the left are
	 * aligned to the right.
	 */
	@Test
	public void testCoordinatesWithSeveralDigits() {
		ContainerAsciiArt art = newBuilder(11, 1, 10,
				place("A", 5, 1, 10, 0, 0, 0),
				place("B", 6, 1, 3, 5, 0, 0))
				.withScale(3, 1)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				10 ┌──────────────┐
				   │              │
				   │              │
				   │              │
				   │              │
				   │      A       │
				   │              │
				 3 │              ├─────────────────┐
				   │              │        B        │
				   │              │                 │
				 0 └──────────────┴─────────────────┘
				   0              5                 11   x
				""");
	}

	/**
	 * Only the coordinates where the boxes start and end are written, with 0: in the front view for x 0, 3, 5 and 7, and for z 0, 2 and 4. The
	 * coordinates of the box behind the others are also written. The container (10 x 4 x 10) is larger than the boxes, and not drawn.
	 */
	@Test
	public void testCoordinatesAreWhereTheBoxesStartAndEnd() {
		ContainerAsciiArt art = newBuilder(10, 4, 10,
				place("A", 2, 4, 2, 3, 0, 0),
				place("B", 2, 2, 2, 3, 0, 2),
				place("C", 4, 2, 2, 3, 2, 2))
				.withScale(3, 1)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4          ┌─────┬─────┐
				           │  B  │  C  │
				2          ├─────┼─────┘
				           │  A  │
				0          └─────┘
				  0        3     5     7   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4          ┌───────────┐
				           │     C     │
				2          ├─────┬─────┘
				           │  B  │
				0          └─────┘
				  0        3     5     7   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4 ┌─────┬─────┐
				  │  B  │  C  │
				2 ├─────┴─────┤
				  │     A     │
				0 └───────────┘
				  0     2     4   y
				""");
	}

	/**
	 * The obstacles also have coordinates.
	 */
	@Test
	public void testCoordinatesOfObstacles() {
		ContainerAsciiArt art = newBuilder(6, 1, 2, place("A", 2, 1, 2, 4, 0, 0))
				.withObstacles(List.of(place("X", 3, 1, 1, 0, 0, 0)))
				.withScale(3, 2)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				2             ┌─────┐
				              │     │
				1 ┌────────┐  │  A  │
				  │########│  │     │
				0 └────────┘  └─────┘
				  0        3  4     6   x
				""");
	}

	/**
	 * The boxes are 1 unit wide, in 11 columns for the 10 units, so a coordinate next to another coordinate is left out: the coordinates of the
	 * boxes are 0, 1, 2, 3 and 10. The coordinates 0 and 10 (the end of the boxes) are kept, and then the others from the left to the right as
	 * long as there is a blank column in between: 2, but not 1 and 3.
	 */
	@Test
	public void testCoordinatesWhichAreTooCloseAreLeftOut() {
		ContainerAsciiArt art = newBuilder(10, 1, 1,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 1, 0, 0),
				place("C", 1, 1, 1, 2, 0, 0),
				place("D", 7, 1, 1, 3, 0, 0))
				.withScale(1, 1)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌┬┬┬──────┐
				0 └┴┴┴──────┘
				  0 2       10   x
				""");
	}

	/**
	 * The coordinate 0 and the end of the boxes (9) have priority over the coordinate where the first box ends (8), which is next to the end,
	 * although it is further to the left.
	 */
	@Test
	public void testCoordinatesOfTheEndOfTheBoxesHavePriority() {
		ContainerAsciiArt art = newBuilder(10, 1, 1,
				place("A", 8, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 8, 0, 0))
				.withScale(1, 1)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌───────┬┐
				0 └───────┴┘
				  0        9   x
				""");

		// the box ends next to the origin: the coordinate 1 is left out, also though it is the end of the drawing, because 0 has priority
		ContainerAsciiArt origin = newBuilder(10, 1, 1, place("A", 1, 1, 1, 0, 0, 0))
				.withScale(1, 1)
				.build();

		assertThat(origin.front().toString()).isEqualTo("""
				z
				1 ┌┐
				0 └┘
				  0    x
				""");
	}

	/**
	 * With the container outline, the container is drawn to its size (10), which has priority over the coordinate where the box ends (9), which is
	 * next to the size of the container, although it is further to the left.
	 */
	@Test
	public void testCoordinatesOfTheContainerHavePriorityWithTheOutline() {
		ContainerAsciiArt art = newBuilder(10, 1, 1, place("A", 9, 1, 1, 0, 0, 0))
				.withScale(1, 1)
				.withContainerOutline(true)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				1 ┌────────┐.
				0 └────────┘.
				  0         10   x
				""");
	}

	/**
	 * With half a line per unit, the coordinates 1 and 2 are on the same line, as are 9 and 10. There is one value per line: 0, the end of the
	 * boxes (10), and then from the bottom to the top.
	 */
	@Test
	public void testCoordinatesOnTheSameLine() {
		ContainerAsciiArt art = newBuilder(8, 1, 10,
				place("A", 8, 1, 1, 0, 0, 0),
				place("B", 8, 1, 1, 0, 0, 1),
				place("C", 8, 1, 7, 0, 0, 2),
				place("D", 8, 1, 1, 0, 0, 9))
				.withWidth(16)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				10 ┌───────┐
				   │       │
				   │   C   │
				   │       │
				 1 ├───────┤
				 0 └───────┘
				   0       8   x
				""");
	}

	/**
	 * The axes of the oblique view are along the edges of the drawing, with coordinates. The x and z axes have the coordinates of the boxes with
	 * their front at y = 0: the box C is further back, so its height of 3 is not written next to the z axis. The y axis has the coordinates in y of
	 * all boxes, the end of the boxes, but not 0.
	 */
	@Test
	public void testObliqueCoordinates() {
		ContainerAsciiArt art = newBuilder(4, 4, 4,
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0),
				place("C", 4, 2, 3, 0, 2, 0),
				place("D", 2, 2, 2, 2, 0, 2))
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │   ┌─────┌───────┐─┐
				  │  ╱     ╱       ╱│╱│
				4 │ ┌─────┌───────┐ │ │   y
				  │ │     │       │ │ │
				  │ └─────│   D   │ │C│ ╱
				  │╱      │       │╱│ │╱
				2 ┌───────┼───────┤ │ │ 4
				  │       │       │ │╱
				  │   A   │   B   │ │ 2
				  │       │       │╱
				0 └───────┴───────┘── x
				  0       2       4
				""");
	}

	@Test
	public void testObliqueCoordinatesOfAnEmptyContainer() {
		ContainerAsciiArt art = newBuilder(4, 2, 2).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │                     y
				  │
				2 │                   ╱
				  │                  ╱
				  │                 ╱ 2
				  │                ╱
				0 └────────────────── x
				  0               4
				""");
	}

	/**
	 * The values of x are centered below the x axis; they are left out if they are too close, as for the other views: here 1 and 3 are next to
	 * 0 and 2, the value of 10 is two digits wide.
	 */
	@Test
	public void testObliqueCoordinatesWhichAreTooCloseAreLeftOut() {
		ContainerAsciiArt art = newBuilder(10, 1, 1,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 1, 0, 0),
				place("C", 1, 1, 1, 2, 0, 0),
				place("D", 7, 1, 1, 3, 0, 0))
				.withScale(1, 1)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				                  y
				  z
				                ╱
				  │ ┌─────────┐╱
				  │╱╱╱╱  D   ╱│ 1
				1 ┌┬┬┬──────┐╱
				0 └┴┴┴──────┘── x
				  0 2      10
				""");
	}

	@Test
	public void testObliqueCoordinatesOfTheEndOfTheBoxesHavePriority() {
		// the first box ends at 8, next to the end of the boxes (9)
		ContainerAsciiArt art = newBuilder(10, 1, 1,
				place("A", 8, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 8, 0, 0))
				.withScale(1, 1)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				                 y
				  z
				               ╱
				  │ ┌────────┐╱
				  │╱   A   ╱╱│ 1
				1 ┌───────┬┐╱
				0 └───────┴┘── x
				  0        9
				""");
	}

	/**
	 * With the container outline, the axes reach the size of the container (10), which has priority over the coordinate where the box ends (9).
	 */
	@Test
	public void testObliqueCoordinatesOfTheContainerHavePriorityWithTheOutline() {
		ContainerAsciiArt art = newBuilder(10, 1, 1, place("A", 9, 1, 1, 0, 0, 0))
				.withScale(1, 1)
				.withContainerOutline(true)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				                  y
				  z
				                ╱
				  │ ┌────────┐.╱
				  │╱   A    ╱│╱ 1
				1 ┌────────┐╱╱
				0 └────────┘─── x
				  0        10
				""");
	}

	/**
	 * With half a line per unit, the coordinates 1 and 2 are on the same line along the y axis, as are 9 and 10. There is one value per line:
	 * the end of the boxes (10), and then from low to high.
	 */
	@Test
	public void testObliqueCoordinatesOnTheSameLine() {
		ContainerAsciiArt art = newBuilder(8, 10, 1,
				place("A", 8, 1, 1, 0, 0, 0),
				place("B", 8, 1, 1, 0, 1, 0),
				place("C", 8, 7, 1, 0, 2, 0),
				place("D", 8, 1, 1, 0, 9, 0))
				.withScale(1, 0.5)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				                                  y

				                                ╱
				                      ┌───────┐╱
				                     ╱   D   ╱│ 10
				                    ╱───────┐╱
				                   ╱       ╱│ 9
				                  ╱       ╱╱
				                 ╱       ╱╱
				                ╱       ╱╱
				               ╱       ╱╱
				              ╱       ╱╱
				             ╱   C   ╱╱
				            ╱       ╱╱
				           ╱       ╱╱
				          ╱       ╱╱
				         ╱       ╱╱
				        ╱       ╱╱
				       ╱       ╱╱
				  z   ╱───────┐╱
				     ╱   B   ╱│ 2
				  │ ╱───────┐╱
				  │╱   A   ╱│ 1
				1 ┌───────┐╱
				0 └───────┘── x
				  0       8
				""");
	}

	@Test
	public void testObliqueWithoutAxesThereAreNoNamesAndNoCoordinates() {
		ContainerAsciiArt art = newBuilder(2, 1, 2, place("A", 2, 1, 2, 0, 0, 0))
				.withScale(4, 2)
				.withAxes(false)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  ┌───────┐
				 ╱       ╱│
				┌───────┐ │
				│       │ │
				│   A   │ │
				│       │╱
				└───────┘
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
				  │#######│   A   │
				  │#######│       │
				0 └───────┴───────┘
				  0       2       4   x
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
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
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
				  │   A   │
				  │       │
				0 └───────┘
				  0       2   x
				""");
	}

	/**
	 * One unit of y is drawn as half of the lines per unit of z: here 4 lines per unit of z, so 2 steps up and to the right per unit of y, and the box
	 * which is 2 units deep gets 4 steps.
	 */
	@Test
	public void testObliqueDepthIsHalfTheLinesPerUnit() {
		ContainerAsciiArt art = newBuilder(2, 2, 1, place("A", 2, 2, 1, 0, 0, 0)).withScale(4, 4).withAxes(false).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				    ┌───────┐
				   ╱       ╱│
				  ╱       ╱ │
				 ╱       ╱  │
				┌───────┐   │
				│       │  ╱
				│   A   │ ╱
				│       │╱
				└───────┘
				""");
	}

	/**
	 * Half of the lines per unit of z would be 1 step per unit of y, but the thinnest box along y (B, which is 1 deep) gets at least 2 steps: A is
	 * 2 units deep and gets 4 steps.
	 */
	@Test
	public void testObliqueThinnestBoxAlongYGetsTwoSteps() {
		ContainerAsciiArt art = newBuilder(2, 3, 1,
				place("A", 2, 2, 1, 0, 0, 0),
				place("B", 2, 1, 1, 0, 2, 0)).withScale(4, 2).withAxes(false).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				      ┌───────┐
				     ╱   B   ╱│
				    ╱───────┐ │
				   ╱       ╱│╱
				  ╱       ╱ │
				 ╱       ╱ ╱
				┌───────┐ ╱
				│   A   │╱
				└───────┘
				""");
	}

	/**
	 * The thinnest box along y is 3 deep: 1 step per unit of y is enough for 3 steps, so the depth is half of the lines per unit.
	 */
	@Test
	public void testObliqueDepthIsHalfTheLinesPerUnitWhenAllBoxesAreDeep() {
		ContainerAsciiArt art = newBuilder(2, 3, 1, place("A", 2, 3, 1, 0, 0, 0)).withScale(4, 2).withAxes(false).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				   ┌───────┐
				  ╱       ╱│
				 ╱       ╱ │
				┌───────┐ ╱
				│   A   │╱
				└───────┘
				""");
	}

	/**
	 * Seven boxes in a row along y in a container 1 x 7 x 1: each box gets 2 steps, so the oblique view is not a long staircase.
	 */
	@Test
	public void testObliqueBoxesInARowAlongY() {
		ContainerAsciiArt art = newBuilder(1, 7, 1,
				place("A", 1, 1, 1, 0, 0, 0),
				place("B", 1, 1, 1, 0, 1, 0),
				place("C", 1, 1, 1, 0, 2, 0),
				place("D", 1, 1, 1, 0, 3, 0),
				place("E", 1, 1, 1, 0, 4, 0),
				place("F", 1, 1, 1, 0, 5, 0),
				place("G", 1, 1, 1, 0, 6, 0)).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				                ┌───────┐   y
				               ╱   G   ╱│
				              ╱───────┐ │ ╱
				             ╱   F   ╱│ │╱
				            ╱───────┐ │ │ 7
				           ╱   E   ╱│ │╱
				          ╱───────┐ │ │ 6
				         ╱   D   ╱│ │╱
				        ╱───────┐ │ │ 5
				       ╱   C   ╱│ │╱
				  z   ╱───────┐ │ │ 4
				     ╱   B   ╱│ │╱
				  │ ╱───────┐ │ │ 3
				  │╱       ╱│ │╱
				1 ┌───────┐ │ │ 2
				  │       │ │╱
				  │   A   │ │ 1
				  │       │╱
				0 └───────┘── x
				  0       1
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
				  0     2     4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				2 ┌─────┬─────┐
				  │  A  │  B  │
				0 └─────┴─────┘
				  0     2     4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				2 ┌─────┐
				1 ├─────┤
				0 └─────┘
				  0     2   y
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z                 y

				  │ ┌─────┐       ╱
				  │╱     ╱└─────┐╱
				2 ┌─────┐╱  B  ╱│ 2
				1 │  A  ├─────┐╱
				0 └─────┴─────┘── x
				  0     2     4
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
	public void testWidthWithoutAxesIsTheWidthOfTheContainer() {
		ContainerAsciiArt art = newBuilder(1000, 500, 400, place("A", 1000, 500, 400, 0, 0, 0))
				.withWidth(31)
				.withAxes(false)
				.build();

		assertThat(art.front().getWidth()).isEqualTo(31);
		assertThat(art.top().getWidth()).isEqualTo(31);
		assertThat(art.side().getWidth()).isEqualTo(31);
		// the depth is part of the width of the oblique view
		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(31).isGreaterThan(28);

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

	/**
	 * The width is that of the widest line of the view: with the values of the axes, their names and the depth of the oblique view.
	 */
	@Test
	public void testWidthCountsEverythingInTheView() {
		for (int columns : new int[] { 20, 31, 40, 60, 100, 160 }) {
			ContainerAsciiArt art = newBuilder(1000, 500, 400,
					place("A", 500, 500, 100, 0, 0, 0),
					place("B", 500, 250, 400, 500, 0, 0),
					place("C", 1000, 250, 200, 0, 250, 0))
					.withWidth(columns)
					.build();

			assertThat(art.front().getWidth()).as("front at %d columns", columns).isLessThanOrEqualTo(columns).isGreaterThan(columns - 6);
			assertThat(art.top().getWidth()).as("top at %d columns", columns).isLessThanOrEqualTo(columns).isGreaterThan(columns - 6);
			assertThat(art.side().getWidth()).as("side at %d columns", columns).isLessThanOrEqualTo(columns).isGreaterThan(columns - 6);
			assertThat(art.oblique().getWidth()).as("oblique at %d columns", columns).isLessThanOrEqualTo(columns).isGreaterThan(columns - 6);
		}
	}

	@Test
	public void testWidthIsTheMaximumWidthOfAViewWithManyDigits() {
		// 5 digits at the z axis and at the x axis
		ContainerAsciiArt art = newBuilder(12000, 2000, 12031, place("A", 10000, 2000, 12031, 0, 0, 0))
				.withWidth(37)
				.build();

		assertThat(art.front().getWidth()).isLessThanOrEqualTo(37).isGreaterThan(31);
		assertThat(art.top().getWidth()).isLessThanOrEqualTo(37).isGreaterThan(31);
		assertThat(art.side().getWidth()).isLessThanOrEqualTo(37).isGreaterThan(31);
		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(37).isGreaterThan(31);
	}

	/**
	 * The thinnest box along y keeps its two steps in the oblique view as long as that fits in the width: here only about 4 columns are left for
	 * the 3 units of x, so the width wins.
	 */
	@Test
	public void testWidthWinsOverTheMinimumDepthWhenTheViewIsTooNarrow() {
		ContainerAsciiArt art = newBuilder(3, 10, 3, place("A", 3, 1, 3, 0, 0, 0)).withWidth(14).build();

		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(14);
		assertThat(art.front().getWidth()).isLessThanOrEqualTo(14);
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
