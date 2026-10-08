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
	 * Container 4 x 4 x 4 with four boxes, with one vertex where a diagonal ends on a horizontal edge which continues at both sides
	 * (on the edge of another box).
	 */
	private static ContainerAsciiArt.Builder vertexScene() {
		return ContainerAsciiArt.newBuilder().withPlacements(List.of(
				place("A", 2, 2, 2, 0, 0, 0),
				place("B", 2, 2, 2, 2, 0, 0),
				place("D", 2, 2, 2, 2, 0, 2),
				place("C", 3, 2, 4, 0, 2, 0)), 4, 4, 4);
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
				z                   y
				│                  ╱
				│                 ╱
				│          ╭───────────────╮
				│         ╱       C       ╱│
				│        ╱     ╭───────╮ ╱ │
				│       ╱     ╱       ╱│╱  │
				│      ╭─────╱       ╱ │   │
				│      │    ╱       ╱  │   │
				│      ╰───┬───────╮   │   │
				│     ╱    │       │  ╱│  ╱
				│    ╱     │   D   │ ╱ │ ╱
				│   ╱      │       │╱  │╱
				│  ╭───────┼───────┤   │
				│  │       │       │  ╱
				│  │   A   │   B   │ ╱
				│  │       │       │╱
				│  ╰───────┴───────╯
				│ ╱
				│╱
				╰─────────────────────────── x
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
				z                   y
				┃                  ╱
				┃                 ╱
				┃          ┏━━━━━━━━━━━━━━━┓
				┃         ╱       C       ╱┃
				┃        ╱     ┏━━━━━━━┓ ╱ ┃
				┃       ╱     ╱       ╱┃╱  ┃
				┃      ┏━━━━━╱       ╱ ┃   ┃
				┃      ┃    ╱       ╱  ┃   ┃
				┃      ┗━━━┳━━━━━━━┓   ┃   ┃
				┃     ╱    ┃       ┃  ╱┃  ╱
				┃    ╱     ┃   D   ┃ ╱ ┃ ╱
				┃   ╱      ┃       ┃╱  ┃╱
				┃  ┏━━━━━━━╋━━━━━━━┫   ┃
				┃  ┃       ┃       ┃  ╱
				┃  ┃   A   ┃   B   ┃ ╱
				┃  ┃       ┃       ┃╱
				┃  ┗━━━━━━━┻━━━━━━━┛
				┃ ╱
				┃╱
				┗━━━━━━━━━━━━━━━━━━━━━━━━━━━ x
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
				z                   y
				|                  /
				|                 /
				|          /---------------|
				|         /       C       /|
				|        /     /-------| / |
				|       /     /       /|/  |
				|      |-----/       / |   |
				|      |    /       /  |   |
				|      |---|-------|   |   |
				|     /    |       |  /|  /
				|    /     |   D   | / | /
				|   /      |       |/  |/
				|  |-------|-------|   |
				|  |       |       |  /
				|  |   A   |   B   | /
				|  |       |       |/
				|  |-------|-------|
				| /
				|/
				|--------------------------- x
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
				z             y
				│        ┌───────┐
				│       ╱       ╱│
				│      ╱       ╱ │
				│     ╱       ╱  │
				│    ╱       ╱   │
				│   ╱       ╱   ╱
				│  ┌───────┐   ╱
				│  │       │  ╱
				│  │  box  │ ╱
				│  │       │╱
				│  └───────┘
				│ ╱
				│╱
				└───────────────── x
				""");
		assertThat(single().withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				z             y
				│        ╭───────╮
				│       ╱       ╱│
				│      ╱       ╱ │
				│     ╱       ╱  │
				│    ╱       ╱   │
				│   ╱       ╱   ╱
				│  ╭───────╮   ╱
				│  │       │  ╱
				│  │  box  │ ╱
				│  │       │╱
				│  ╰───────╯
				│ ╱
				│╱
				╰───────────────── x
				""");
		assertThat(single().withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				z             y
				┃        ┏━━━━━━━┓
				┃       ╱       ╱┃
				┃      ╱       ╱ ┃
				┃     ╱       ╱  ┃
				┃    ╱       ╱   ┃
				┃   ╱       ╱   ╱
				┃  ┏━━━━━━━┓   ╱
				┃  ┃       ┃  ╱
				┃  ┃  box  ┃ ╱
				┃  ┃       ┃╱
				┃  ┗━━━━━━━┛
				┃ ╱
				┃╱
				┗━━━━━━━━━━━━━━━━━ x
				""");
		assertThat(single().withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				z             y
				|        /-------|
				|       /       /|
				|      /       / |
				|     /       /  |
				|    /       /   |
				|   /       /   /
				|  |-------|   /
				|  |       |  /
				|  |  box  | /
				|  |       |/
				|  |-------|
				| /
				|/
				|----------------- x
				""");
	}

	@Test
	public void testVertexOnTheEdgeOfAnotherBox() {
		// the diagonal edge ends on a horizontal edge which continues at both sides: a T junction
		assertThat(vertexScene().withAxes(false).build().oblique().toString()).isEqualTo("""
				        ┌───────────┐
				       ╱           ╱│
				      ╱     C     ╱ │
				     ╱           ╱  │
				    ┌───────┬───────┤
				    │      ╱       ╱│
				    │     ╱       ╱ │
				    │    ╱       ╱  │
				    └───┬───────┐   │
				   ╱    │       │  ╱│
				  ╱     │   D   │ ╱ │
				 ╱      │       │╱  │
				┌───────┼───────┤   │
				│       │       │  ╱
				│   A   │   B   │ ╱
				│       │       │╱
				└───────┴───────┘
				""");
		assertThat(vertexScene().withAxes(false).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				        ┏━━━━━━━━━━━┓
				       ╱           ╱┃
				      ╱     C     ╱ ┃
				     ╱           ╱  ┃
				    ┏━━━━━━━┳━━━━━━━┫
				    ┃      ╱       ╱┃
				    ┃     ╱       ╱ ┃
				    ┃    ╱       ╱  ┃
				    ┗━━━┳━━━━━━━┓   ┃
				   ╱    ┃       ┃  ╱┃
				  ╱     ┃   D   ┃ ╱ ┃
				 ╱      ┃       ┃╱  ┃
				┏━━━━━━━╋━━━━━━━┫   ┃
				┃       ┃       ┃  ╱
				┃   A   ┃   B   ┃ ╱
				┃       ┃       ┃╱
				┗━━━━━━━┻━━━━━━━┛
				""");
		assertThat(vertexScene().withAxes(false).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				        ╭───────────╮
				       ╱           ╱│
				      ╱     C     ╱ │
				     ╱           ╱  │
				    ╭───────┬───────┤
				    │      ╱       ╱│
				    │     ╱       ╱ │
				    │    ╱       ╱  │
				    ╰───┬───────╮   │
				   ╱    │       │  ╱│
				  ╱     │   D   │ ╱ │
				 ╱      │       │╱  │
				╭───────┼───────┤   │
				│       │       │  ╱
				│   A   │   B   │ ╱
				│       │       │╱
				╰───────┴───────╯
				""");
	}

	@Test
	public void testHorizontalEdgeRunsUpToTheDiagonalEdge() {
		// B is directly behind A: the top face of A and the top face of B are next to each other, and the diagonal edge continues
		// through the horizontal edge where they meet. The horizontal edge runs up to the diagonal edge.
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 0, 0, 0), place("B", 2, 2, 2, 0, 2, 0)), 2, 4, 2)
				.withAxes(false)
				.build();

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

		ContainerAsciiArt ascii = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 0, 0, 0), place("B", 2, 2, 2, 0, 2, 0)), 2, 4, 2)
				.withAxes(false)
				.withStyle(Style.ASCII)
				.build();

		assertThat(ascii.oblique().toString()).isEqualTo("""
				        /-------|
				       /       /|
				      /   B   / |
				     /       /  |
				    /-------|   |
				   /       /|  /
				  /       / | /
				 /       /  |/
				|-------|   |
				|       |  /
				|   A   | /
				|       |/
				|-------|
				""");
	}

	@Test
	public void testShading() {
		assertThat(scene().withShading(true).build().oblique().toString()).isEqualTo("""
				z                   y
				│                  ╱
				│                 ╱
				│          ┌───────────────┐
				│         ╱░░░░░░░C░░░░░░░╱│
				│        ╱░░░░░┌───────┐░╱▒│
				│       ╱░░░░░╱░░░░░░░╱│╱▒▒│
				│      ┌─────╱░░░░░░░╱▒│▒▒▒│
				│      │    ╱░░░░░░░╱▒▒│▒▒▒│
				│      └───┬───────┐▒▒▒│▒▒▒│
				│     ╱░░░░│       │▒▒╱│▒▒╱
				│    ╱░░░░░│   D   │▒╱▒│▒╱
				│   ╱░░░░░░│       │╱▒▒│╱
				│  ┌───────┼───────┤▒▒▒│
				│  │       │       │▒▒╱
				│  │   A   │   B   │▒╱
				│  │       │       │╱
				│  └───────┴───────┘
				│ ╱
				│╱
				└─────────────────────────── x
				""");
		assertThat(scene().withShading(true).withStyle(Style.HEAVY).build().oblique().toString()).isEqualTo("""
				z                   y
				┃                  ╱
				┃                 ╱
				┃          ┏━━━━━━━━━━━━━━━┓
				┃         ╱░░░░░░░C░░░░░░░╱┃
				┃        ╱░░░░░┏━━━━━━━┓░╱▒┃
				┃       ╱░░░░░╱░░░░░░░╱┃╱▒▒┃
				┃      ┏━━━━━╱░░░░░░░╱▒┃▒▒▒┃
				┃      ┃    ╱░░░░░░░╱▒▒┃▒▒▒┃
				┃      ┗━━━┳━━━━━━━┓▒▒▒┃▒▒▒┃
				┃     ╱░░░░┃       ┃▒▒╱┃▒▒╱
				┃    ╱░░░░░┃   D   ┃▒╱▒┃▒╱
				┃   ╱░░░░░░┃       ┃╱▒▒┃╱
				┃  ┏━━━━━━━╋━━━━━━━┫▒▒▒┃
				┃  ┃       ┃       ┃▒▒╱
				┃  ┃   A   ┃   B   ┃▒╱
				┃  ┃       ┃       ┃╱
				┃  ┗━━━━━━━┻━━━━━━━┛
				┃ ╱
				┃╱
				┗━━━━━━━━━━━━━━━━━━━━━━━━━━━ x
				""");
		assertThat(scene().withShading(true).withStyle(Style.ROUNDED).build().oblique().toString()).isEqualTo("""
				z                   y
				│                  ╱
				│                 ╱
				│          ╭───────────────╮
				│         ╱░░░░░░░C░░░░░░░╱│
				│        ╱░░░░░╭───────╮░╱▒│
				│       ╱░░░░░╱░░░░░░░╱│╱▒▒│
				│      ╭─────╱░░░░░░░╱▒│▒▒▒│
				│      │    ╱░░░░░░░╱▒▒│▒▒▒│
				│      ╰───┬───────╮▒▒▒│▒▒▒│
				│     ╱░░░░│       │▒▒╱│▒▒╱
				│    ╱░░░░░│   D   │▒╱▒│▒╱
				│   ╱░░░░░░│       │╱▒▒│╱
				│  ╭───────┼───────┤▒▒▒│
				│  │       │       │▒▒╱
				│  │   A   │   B   │▒╱
				│  │       │       │╱
				│  ╰───────┴───────╯
				│ ╱
				│╱
				╰─────────────────────────── x
				""");
		assertThat(scene().withShading(true).withStyle(Style.ASCII).build().oblique().toString()).isEqualTo("""
				z                   y
				|                  /
				|                 /
				|          /---------------|
				|         /.......C......./|
				|        /...../-------|./:|
				|       /...../......./|/::|
				|      |-----/......./:|:::|
				|      |    /......./::|:::|
				|      |---|-------|:::|:::|
				|     /....|       |::/|::/
				|    /.....|   D   |:/:|:/
				|   /......|       |/::|/
				|  |-------|-------|:::|
				|  |       |       |::/
				|  |   A   |   B   |:/
				|  |       |       |/
				|  |-------|-------|
				| /
				|/
				|--------------------------- x
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
				  ╱#######╱░░░░░░░╱▒│
				 ╱#######╱░░░░░░░╱▒▒│
				┌───────┬───────┐▒▒▒│
				│#######│       │▒▒╱
				│#######│   A   │▒╱
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
