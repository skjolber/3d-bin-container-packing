package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;

public class ContainerAsciiArtStyleTest {

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
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0),
				place("C", 4, 2, 3, 0, 2, 0),
				place("D", 2, 2, 2, 2, 0, 2)), 4, 4, 4);
	}

	/**
	 * Container 4 x 4 x 4 with four boxes. The top face of A has its back edge at the height of the top of D, behind D: this edge is not joined
	 * to the front top left corner of D. The back edge of the top face of A touches the front face of C, where it is joined to the front left edge
	 * of C.
	 */
	private static ContainerAsciiArt.Builder depthScene() {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0),
				place("D", 2, 2, 2, 2, 0, 2),
				place("C", 3, 2, 4, 0, 2, 0)), 4, 4, 4);
	}

	/**
	 * Container 4 x 4 x 4 with two boxes: the box B is behind the box A, which is above the floor. The back edge of the top face of A touches the
	 * front face of B. The right edge of the top face of A is a diagonal edge which ends on the horizontal edge at the top of the front face of
	 * B, which continues at both sides.
	 */
	private static ContainerAsciiArt.Builder teeScene() {
		// the scale at which the labels fit in the front view
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(
				place("A", 2, 1, 2, 0, 0, 2),
				place("B", 2, 3, 3, 1, 1, 1)), 4, 4, 4).withScale(4, 2);
	}

	private static ContainerAsciiArt.Builder single() {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(place("box", 2, 3, 2, 0, 0, 0)), 2, 3, 2);
	}

	@Test
	public void testLightIsTheDefault() {
		assertThat(scene().build().front().toString()).isEqualTo(scene().withStyle(Style.LIGHT).build().front().toString());
		assertThat(scene().build().oblique().toString()).isEqualTo(scene().withStyle(Style.LIGHT).build().oblique().toString());
	}

	@Test
	public void testLight() {
		ContainerAsciiArt art = scene().withStyle(Style.LIGHT).build();

		assertThat(art.front().toString()).isEqualTo("""
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
	public void testRounded() {
		ContainerAsciiArt art = scene().withStyle(Style.ROUNDED).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4         ╭───────╮
				          │       │
				3 ╭───────┤   D   │
				  │   C   │       │
				2 ├───────┼───────┤
				  │       │       │
				  │   A   │   B   │
				  │       │       │
				0 ╰───────┴───────╯
				  0       2       4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  │   ╭─────╭───────╮─╮
				  │  ╱     ╱       ╱│╱│
				4 │ ╭─────╭───────╮ │ │   y
				  │ │     │       │ │ │
				  │ ╰─────│   D   │ │C│ ╱
				  │╱      │       │╱│ │╱
				2 ╭───────┼───────┤ │ │ 4
				  │       │       │ │╱
				  │   A   │   B   │ │ 2
				  │       │       │╱
				0 ╰───────┴───────╯── x
				  0       2       4
				""");
	}

	@Test
	public void testHeavy() {
		ContainerAsciiArt art = scene().withStyle(Style.HEAVY).build();

		assertThat(art.front().toString()).isEqualTo("""
				z
				4         ┏━━━━━━━┓
				          ┃       ┃
				3 ┏━━━━━━━┫   D   ┃
				  ┃   C   ┃       ┃
				2 ┣━━━━━━━╋━━━━━━━┫
				  ┃       ┃       ┃
				  ┃   A   ┃   B   ┃
				  ┃       ┃       ┃
				0 ┗━━━━━━━┻━━━━━━━┛
				  0       2       4   x
				""");
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  ┃   ┏━━━━━┏━━━━━━━┓━┓
				  ┃  ╱     ╱       ╱┃╱┃
				4 ┃ ┏━━━━━┏━━━━━━━┓ ┃ ┃   y
				  ┃ ┃     ┃       ┃ ┃ ┃
				  ┃ ┗━━━━━┃   D   ┃ ┃C┃ ╱
				  ┃╱      ┃       ┃╱┃ ┃╱
				2 ┏━━━━━━━╋━━━━━━━┫ ┃ ┃ 4
				  ┃       ┃       ┃ ┃╱
				  ┃   A   ┃   B   ┃ ┃ 2
				  ┃       ┃       ┃╱
				0 ┗━━━━━━━┻━━━━━━━┛━━ x
				  0       2       4
				""");
	}

	@Test
	public void testAscii() {
		ContainerAsciiArt art = scene().withStyle(Style.ASCII).build();

		// the corners and junctions are '+' in the front, top and side views
		assertThat(art.front().toString()).isEqualTo("""
				z
				4         +-------+
				          |       |
				3 +-------+   D   |
				  |   C   |       |
				2 +-------+-------+
				  |       |       |
				  |   A   |   B   |
				  |       |       |
				0 +-------+-------+
				  0       2       4   x
				""");
		assertThat(art.top().toString()).isEqualTo("""
				y
				4 +---------------+
				  |               |
				  |       C       |
				  |               |
				2 +-------+-------+
				  |       |       |
				  |   A   |   D   |
				  |       |       |
				0 +-------+-------+
				  0       2       4   x
				""");
		assertThat(art.side().toString()).isEqualTo("""
				z
				4 +-------+
				  |       |
				3 |   D   +-------+
				  |       |       |
				2 +-------+       |
				  |       |   C   |
				  |   B   |       |
				  |       |       |
				0 +-------+-------+
				  0       2       4   y
				""");
		// vertical edges are drawn through corners and junctions, then diagonal edges
		assertThat(art.oblique().toString()).isEqualTo("""
				  z

				  |   /-----/-------|-|
				  |  /     /       /|/|
				4 | |-----|-------| | |   y
				  | |     |       | | |
				  | |-----|   D   | |C| /
				  |/      |       |/| |/
				2 |-------|-------| | | 4
				  |       |       | |/
				  |   A   |   B   | | 2
				  |       |       |/
				0 |-------|-------|-- x
				  0       2       4
				""");
	}

	@Test
	public void testTopAndSideInLightStyle() {
		ContainerAsciiArt art = scene().build();

		assertThat(art.top().toString()).isEqualTo("""
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
		assertThat(art.side().toString()).isEqualTo("""
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
	public void testSingleBoxInEveryStyle() {
		// the back top left vertex, where the diagonal edge ends on a horizontal edge which continues to the right, is the top left corner
		assertThat(single().withStyle(Style.LIGHT).build().oblique().toString()).isEqualTo("""
				  z
				     ┌───────┐   y
				  │ ╱       ╱│
				  │╱       ╱ │ ╱
				2 ┌───────┐  │╱
				  │       │  │ 3
				  │  box  │ ╱
				  │       │╱
				0 └───────┘── x
				  0       2
				""");
		assertThat(single().withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				  z
				     ╭───────╮   y
				  │ ╱       ╱│
				  │╱       ╱ │ ╱
				2 ╭───────╮  │╱
				  │       │  │ 3
				  │  box  │ ╱
				  │       │╱
				0 ╰───────╯── x
				  0       2
				""");
		assertThat(single().withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				  z
				     ┏━━━━━━━┓   y
				  ┃ ╱       ╱┃
				  ┃╱       ╱ ┃ ╱
				2 ┏━━━━━━━┓  ┃╱
				  ┃       ┃  ┃ 3
				  ┃  box  ┃ ╱
				  ┃       ┃╱
				0 ┗━━━━━━━┛━━ x
				  0       2
				""");
		assertThat(single().withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				  z
				     /-------|   y
				  | /       /|
				  |/       / | /
				2 |-------|  |/
				  |       |  | 3
				  |  box  | /
				  |       |/
				0 |-------|-- x
				  0       2
				""");
	}

	@Test
	public void testEdgesAtAnotherDepthAreNotJoined() {
		// the horizontal edge from the left which is the back edge of the top face of A, runs up to the front left corner of D
		// but does not join it, as D is nearer: the corner stays a corner
		assertThat(depthScene().withAxes(false).build().oblique().toString()).isEqualTo("""
				    ┌───────────┐
				   ╱     C     ╱│
				  ┌───────┬───────┐
				  │      ╱       ╱│
				  │     ┌───────┐ │
				  │     │       │ │
				  └─────│   D   │ │
				 ╱      │       │╱│
				┌───────┼───────┤ │
				│       │       │ │
				│   A   │   B   │ │
				│       │       │╱
				└───────┴───────┘
				""");
		assertThat(depthScene().withAxes(false).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				    ┏━━━━━━━━━━━┓
				   ╱     C     ╱┃
				  ┏━━━━━━━┳━━━━━━━┓
				  ┃      ╱       ╱┃
				  ┃     ┏━━━━━━━┓ ┃
				  ┃     ┃       ┃ ┃
				  ┗━━━━━┃   D   ┃ ┃
				 ╱      ┃       ┃╱┃
				┏━━━━━━━╋━━━━━━━┫ ┃
				┃       ┃       ┃ ┃
				┃   A   ┃   B   ┃ ┃
				┃       ┃       ┃╱
				┗━━━━━━━┻━━━━━━━┛
				""");
		assertThat(depthScene().withAxes(false).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				    ╭───────────╮
				   ╱     C     ╱│
				  ╭───────┬───────╮
				  │      ╱       ╱│
				  │     ╭───────╮ │
				  │     │       │ │
				  ╰─────│   D   │ │
				 ╱      │       │╱│
				╭───────┼───────┤ │
				│       │       │ │
				│   A   │   B   │ │
				│       │       │╱
				╰───────┴───────╯
				""");
	}

	@Test
	public void testEdgesAtTheSameDepthAreJoined() {
		// A and B side by side: the front face of A and the front face of B are joined in a junction, as are their top faces
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 0, 0, 0), place("B", 2, 2, 2, 2, 0, 0), place("C", 2, 2, 2, 2, 0, 2)), 4, 2, 4)
				.withAxes(false)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				          ┌───────┐
				         ╱       ╱│
				        ┌───────┐ │
				        │       │ │
				  ┌─────│   C   │ │
				 ╱      │       │╱│
				┌───────┼───────┤ │
				│       │       │ │
				│   A   │   B   │ │
				│       │       │╱
				└───────┴───────┘
				""");
	}

	@Test
	public void testVertexOnAnEdgeWhichContinuesAtTheSameDepth() {
		// the diagonal edge ends on a horizontal edge which continues at both sides at the same depth: a T junction
		assertThat(teeScene().withAxes(false).build().oblique().toString()).isEqualTo("""
				            ┌───────┐
				           ╱       ╱│
				          ╱       ╱ │
				         ╱   B   ╱  │
				        ╱       ╱   │
				       ╱       ╱    │
				  ┌───────┬───┐     │
				 ╱       ╱│   │    ╱
				┌───────┐ │   │   ╱
				│       │ │   │  ╱
				│   A   │ │   │ ╱
				│       │╱    │╱
				└───────┘─────┘
				""");
		assertThat(teeScene().withAxes(false).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				            ┏━━━━━━━┓
				           ╱       ╱┃
				          ╱       ╱ ┃
				         ╱   B   ╱  ┃
				        ╱       ╱   ┃
				       ╱       ╱    ┃
				  ┏━━━━━━━┳━━━┓     ┃
				 ╱       ╱┃   ┃    ╱
				┏━━━━━━━┓ ┃   ┃   ╱
				┃       ┃ ┃   ┃  ╱
				┃   A   ┃ ┃   ┃ ╱
				┃       ┃╱    ┃╱
				┗━━━━━━━┛━━━━━┛
				""");
		assertThat(teeScene().withAxes(false).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				            ╭───────╮
				           ╱       ╱│
				          ╱       ╱ │
				         ╱   B   ╱  │
				        ╱       ╱   │
				       ╱       ╱    │
				  ╭───────┬───╮     │
				 ╱       ╱│   │    ╱
				╭───────╮ │   │   ╱
				│       │ │   │  ╱
				│   A   │ │   │ ╱
				│       │╱    │╱
				╰───────╯─────╯
				""");
	}

	/**
	 * B is directly behind A: the top face of A and the top face of B are next to each other, and the diagonal edge continues through the cell where
	 * the horizontal edge between them starts: it stays a diagonal edge in every style, so that the diagonal edge is not broken.
	 */
	@Test
	public void testDiagonalEdgeContinuesThroughTheStartOfAHorizontalEdge() {
		List<Placement> placements = List.of(place("A", 2, 2, 2, 0, 0, 0), place("B", 2, 2, 2, 0, 2, 0));

		assertThat(chain(placements, Style.LIGHT).oblique().toString()).isEqualTo("""
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
		assertThat(chain(placements, Style.ROUNDED).oblique().toString()).isEqualTo("""
				    ╭───────╮
				   ╱   B   ╱│
				  ╱───────╮ │
				 ╱       ╱│ │
				╭───────╮ │ │
				│       │ │╱
				│   A   │ │
				│       │╱
				╰───────╯
				""");
		assertThat(chain(placements, Style.HEAVY).oblique().toString()).isEqualTo("""
				    ┏━━━━━━━┓
				   ╱   B   ╱┃
				  ╱━━━━━━━┓ ┃
				 ╱       ╱┃ ┃
				┏━━━━━━━┓ ┃ ┃
				┃       ┃ ┃╱
				┃   A   ┃ ┃
				┃       ┃╱
				┗━━━━━━━┛
				""");
		assertThat(chain(placements, Style.ASCII).oblique().toString()).isEqualTo("""
				    /-------|
				   /   B   /|
				  /-------| |
				 /       /| |
				|-------| | |
				|       | |/
				|   A   | |
				|       |/
				|-------|
				""");
	}

	/**
	 * Three boxes next to each other, and three boxes behind them: the diagonal edges between the boxes go on through the horizontal edge where the
	 * top faces of the front row meet the top faces of the back row, at the start, in the middle and at the end of the horizontal edge. At the end,
	 * the vertical edge of the right faces makes it a corner.
	 */
	@Test
	public void testDiagonalEdgesContinueThroughAHorizontalEdgeWhichContinues() {
		List<Placement> placements = List.of(
				place("A", 1, 1, 1, 0, 0, 0), place("B", 1, 1, 1, 1, 0, 0), place("C", 1, 1, 1, 2, 0, 0),
				place("D", 1, 1, 1, 0, 1, 0), place("E", 1, 1, 1, 1, 1, 0), place("F", 1, 1, 1, 2, 1, 0));

		ContainerAsciiArt art = ContainerAsciiArt.newBuilder().withPlacements(placements, 3, 2, 1).withAxes(false).withScale(4, 2).build();

		assertThat(art.oblique().toString()).isEqualTo("""
				    ┌───────────┐
				   ╱ D ╱ E ╱ F ╱│
				  ╱───╱───╱───┐ │
				 ╱   ╱   ╱   ╱│╱
				┌───┬───┬───┐ │
				│ A │ B │ C │╱
				└───┴───┴───┘
				""");
	}

	private static ContainerAsciiArt chain(List<Placement> placements, Style style) {
		return ContainerAsciiArt.newBuilder().withPlacements(placements, 2, 4, 2).withAxes(false).withStyle(style).build();
	}

	/**
	 * The diagonal axis (y) starts in the cell after the end of the straight axis (x): in the cell where it starts, the x axis goes on. Here the
	 * box ends before the right edge of the container (drawn with the container outline), so the x axis is visible at the end of the box, and
	 * continues to the right edge of the container and past it.
	 */
	@Test
	public void testTheDiagonalAxisStartsAfterTheStraightAxis() {
		List<Placement> placements = List.of(place("A", 9, 1, 1, 0, 0, 0));

		assertThat(ContainerAsciiArt.newBuilder().withPlacements(placements, 10, 1, 1).withScale(1, 1).withContainerOutline(true).withStyle(Style.LIGHT).build().oblique().toString()).isEqualTo("""
				                  y
				  z
				                ╱
				  │ ┌────────┐.╱
				  │╱   A    ╱│╱ 1
				1 ┌────────┐╱╱
				0 └────────┘─── x
				  0        10
				""");
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(placements, 10, 1, 1).withScale(1, 1).withContainerOutline(true).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				                  y
				  z
				                ╱
				  │ ╭────────╮.╱
				  │╱   A    ╱│╱ 1
				1 ╭────────╮╱╱
				0 ╰────────╯─── x
				  0        10
				""");
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(placements, 10, 1, 1).withScale(1, 1).withContainerOutline(true).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				                  y
				  z
				                ╱
				  ┃ ┏━━━━━━━━┓.╱
				  ┃╱   A    ╱┃╱ 1
				1 ┏━━━━━━━━┓╱╱
				0 ┗━━━━━━━━┛━━━ x
				  0        10
				""");
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(placements, 10, 1, 1).withScale(1, 1).withContainerOutline(true).withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				                  y
				  z
				                /
				  | /--------|./
				  |/   A    /|/ 1
				1 |--------|//
				0 |--------|--- x
				  0        10
				""");
	}

	@Test
	public void testTheAxesOfAnEmptyContainerOnlyHaveTheirOwnStraightCharacters() {
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(List.of(), 4, 1, 1).withScale(2, 1).withStyle(Style.LIGHT).build().oblique().toString()).isEqualTo("""
				  z            y

				  │          ╱
				  │         ╱
				1 │        ╱ 1
				0 └────────── x
				  0       4
				""");
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(List.of(), 4, 1, 1).withScale(2, 1).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				  z            y

				  ┃          ╱
				  ┃         ╱
				1 ┃        ╱ 1
				0 ┗━━━━━━━━━━ x
				  0       4
				""");
		assertThat(ContainerAsciiArt.newBuilder().withPlacements(List.of(), 4, 1, 1).withScale(2, 1).withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				  z            y

				  |          /
				  |         /
				1 |        / 1
				0 |---------- x
				  0       4
				""");
	}

	@Test
	public void testShading() {
		assertThat(scene().withShading(true).build().oblique().toString()).isEqualTo("""
				  z

				  │   ┌─────┌───────┐─┐
				  │  ╱░░░░░╱░░░░░░░╱│╱│
				4 │ ┌─────┌───────┐▒│▒│   y
				  │ │     │       │▒│▒│
				  │ └─────│   D   │▒│C│ ╱
				  │╱░░░░░░│       │╱│▒│╱
				2 ┌───────┼───────┤▒│▒│ 4
				  │       │       │▒│╱
				  │   A   │   B   │▒│ 2
				  │       │       │╱
				0 └───────┴───────┘── x
				  0       2       4
				""");
		assertThat(scene().withShading(true).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				  z

				  ┃   ┏━━━━━┏━━━━━━━┓━┓
				  ┃  ╱░░░░░╱░░░░░░░╱┃╱┃
				4 ┃ ┏━━━━━┏━━━━━━━┓▒┃▒┃   y
				  ┃ ┃     ┃       ┃▒┃▒┃
				  ┃ ┗━━━━━┃   D   ┃▒┃C┃ ╱
				  ┃╱░░░░░░┃       ┃╱┃▒┃╱
				2 ┏━━━━━━━╋━━━━━━━┫▒┃▒┃ 4
				  ┃       ┃       ┃▒┃╱
				  ┃   A   ┃   B   ┃▒┃ 2
				  ┃       ┃       ┃╱
				0 ┗━━━━━━━┻━━━━━━━┛━━ x
				  0       2       4
				""");
		assertThat(scene().withShading(true).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				  z

				  │   ╭─────╭───────╮─╮
				  │  ╱░░░░░╱░░░░░░░╱│╱│
				4 │ ╭─────╭───────╮▒│▒│   y
				  │ │     │       │▒│▒│
				  │ ╰─────│   D   │▒│C│ ╱
				  │╱░░░░░░│       │╱│▒│╱
				2 ╭───────┼───────┤▒│▒│ 4
				  │       │       │▒│╱
				  │   A   │   B   │▒│ 2
				  │       │       │╱
				0 ╰───────┴───────╯── x
				  0       2       4
				""");
		assertThat(scene().withShading(true).withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				  z

				  |   /-----/-------|-|
				  |  /...../......./|/|
				4 | |-----|-------|:|:|   y
				  | |     |       |:|:|
				  | |-----|   D   |:|C| /
				  |/......|       |/|:|/
				2 |-------|-------|:|:| 4
				  |       |       |:|/
				  |   A   |   B   |:| 2
				  |       |       |/
				0 |-------|-------|-- x
				  0       2       4
				""");
	}

	@Test
	public void testNoShadingByDefault() {
		assertThat(scene().build().oblique().toString()).isEqualTo(scene().withShading(false).build().oblique().toString());
		assertThat(scene().build().oblique().toString()).doesNotContain("░").doesNotContain("▒");
	}

	@Test
	public void testShadingIsForTheObliqueViewOnly() {
		ContainerAsciiArt shaded = scene().withShading(true).build();
		ContainerAsciiArt plain = scene().build();

		assertThat(shaded.front().toString()).isEqualTo(plain.front().toString());
		assertThat(shaded.top().toString()).isEqualTo(plain.top().toString());
		assertThat(shaded.side().toString()).isEqualTo(plain.side().toString());
	}

	@Test
	public void testObstaclesAreFilledWhenShading() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 2, 0, 0)), 4, 2, 2)
				.withObstacles(List.of(place("X", 2, 2, 2, 0, 0, 0)))
				.withAxes(false)
				.withShading(true)
				.build();

		assertThat(art.oblique().toString()).isEqualTo("""
				  ┌───────────────┐
				 ╱#######╱░░░░░░░╱│
				┌───────┬───────┐▒│
				│#######│       │▒│
				│#######│   A   │▒│
				│#######│       │╱
				└───────┴───────┘
				""");
	}

	/**
	 * Straight lines run right up to the diagonal lines: there are no characters for the end of a line.
	 */
	@Test
	public void testNoCharactersForTheEndOfALine() {
		Random random = new Random(1);
		for (int i = 0; i < 100; i++) {
			List<Placement> placements = randomPlacements(random, 4, 4, 4);
			for (Style style : Style.values()) {
				for (boolean axes : new boolean[] { true, false }) {
					ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
							.withPlacements(placements, 4, 4, 4)
							.withStyle(style)
							.withAxes(axes)
							.withContainerOutline(i % 2 == 0)
							.build();

					for (String drawing : new String[] { art.front().toString(), art.top().toString(), art.side().toString(), art.oblique().toString() }) {
						assertThat(drawing).doesNotContain("╴").doesNotContain("╶").doesNotContain("╵").doesNotContain("╷");
						assertThat(drawing).doesNotContain("╸").doesNotContain("╺").doesNotContain("╹").doesNotContain("╻");
					}
				}
			}
		}
	}

	/**
	 * A style only uses its own characters (and the labels).
	 */
	@Test
	public void testStylesOnlyUseTheirOwnCharacters() {
		String light = "─│╱┌┐└┘├┤┬┴┼";
		String rounded = "─│╱╭╮╰╯├┤┬┴┼";
		String heavy = "━┃╱┏┓┗┛┣┫┳┻╋";
		String ascii = "-|/+";

		Random random = new Random(2);
		for (int i = 0; i < 50; i++) {
			List<Placement> placements = randomPlacements(random, 4, 4, 4);
			for (Style style : Style.values()) {
				String allowed = style == Style.LIGHT ? light : style == Style.ROUNDED ? rounded : style == Style.HEAVY ? heavy : ascii;
				ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
						.withPlacements(placements, 4, 4, 4)
						.withStyle(style)
						.withContainerOutline(true)
						.build();

				for (String drawing : new String[] { art.front().toString(), art.top().toString(), art.side().toString(), art.oblique().toString() }) {
					for (int j = 0; j < drawing.length(); j++) {
						char c = drawing.charAt(j);
						boolean plain = c == ' ' || c == '\n' || c == '.' || Character.isLetterOrDigit(c) && c < 128;
						assertThat(plain || allowed.indexOf(c) != -1).as("character %s in %s %s", c, style, drawing).isTrue();
					}
				}
			}
		}
	}

	/**
	 * Boxes in random places which do not overlap, with a size of one to three units.
	 */
	private static List<Placement> randomPlacements(Random random, int dx, int dy, int dz) {
		List<int[]> boxes = new ArrayList<>();
		List<Placement> placements = new ArrayList<>();
		for (int i = 0; i < 40 && boxes.size() < 6; i++) {
			int sx = 1 + random.nextInt(3);
			int sy = 1 + random.nextInt(3);
			int sz = 1 + random.nextInt(3);
			int x = random.nextInt(dx - sx + 1);
			int y = random.nextInt(dy - sy + 1);
			int z = random.nextInt(dz - sz + 1);

			boolean overlaps = false;
			for (int[] box : boxes) {
				if (x < box[0] + box[3] && box[0] < x + sx && y < box[1] + box[4] && box[1] < y + sy && z < box[2] + box[5] && box[2] < z + sz) {
					overlaps = true;
					break;
				}
			}
			if (!overlaps) {
				boxes.add(new int[] { x, y, z, sx, sy, sz });
				placements.add(place(String.valueOf((char) ('A' + boxes.size() - 1)), sx, sy, sz, x, y, z));
			}
		}
		return placements;
	}
}
