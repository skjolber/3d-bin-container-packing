package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

public class FiguresTest {

	@Test
	public void testFigureOfRemovesTrailingBlanks() {
		Figure figure = Figure.of("abc  ", "", "d ");

		assertThat(figure.getLines()).containsExactly("abc", "", "d");
		assertThat(figure.getWidth()).isEqualTo(3);
		assertThat(figure.getHeight()).isEqualTo(3);
		assertThat(figure.toString()).isEqualTo("abc\n\nd\n");
	}

	@Test
	public void testFigureLinesCannotBeChanged() {
		Figure figure = Figure.of("abc");

		assertThatThrownBy(() -> figure.getLines().add("def")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void testFigureComment() {
		Figure figure = Figure.of("abc", "", "d").withComment("// ");

		assertThat(figure.getComment()).isEqualTo("// ");
		// the width does not include the comment, and blank lines have no trailing blank
		assertThat(figure.getWidth()).isEqualTo(3);
		assertThat(figure.toString()).isEqualTo("""
				// abc
				//
				// d
				""");
	}

	/**
	 * The figures are aligned at the top.
	 */
	@Test
	public void testHorizontal() {
		Figure figure = Figures.horizontal(2,
				Figure.of("aaa", "bbb", "ccc"),
				Figure.of("1", "2"),
				Figure.of("xx"));

		assertThat(figure.toString()).isEqualTo("""
				aaa  1  xx
				bbb  2
				ccc
				""");
		assertThat(figure.getHeight()).isEqualTo(3);
		assertThat(figure.getWidth()).isEqualTo(10);
	}

	@Test
	public void testHorizontalWithoutGap() {
		Figure figure = Figures.horizontal(0, Figure.of("a ", "bb"), Figure.of("c", "d"));

		assertThat(figure.toString()).isEqualTo("""
				a c
				bbd
				""");
	}

	@Test
	public void testHorizontalKeepsLeadingBlanks() {
		Figure figure = Figures.horizontal(1, Figure.of("  a", "b"), Figure.of("  c"));

		assertThat(figure.toString()).isEqualTo("""
				  a   c
				b
				""");
	}

	/**
	 * The figures are aligned at the left.
	 */
	@Test
	public void testVertical() {
		Figure figure = Figures.vertical(1,
				Figure.of("aaa", "b"),
				Figure.of(" c"),
				Figure.of("dd"));

		assertThat(figure.toString()).isEqualTo("""
				aaa
				b

				 c

				dd
				""");
		assertThat(figure.getHeight()).isEqualTo(6);
		assertThat(figure.getWidth()).isEqualTo(3);
	}

	@Test
	public void testVerticalWithoutGap() {
		Figure figure = Figures.vertical(0, Figure.of("aaa"), Figure.of("b"));

		assertThat(figure.toString()).isEqualTo("""
				aaa
				b
				""");
	}

	@Test
	public void testNoFigures() {
		assertThat(Figures.horizontal(2).toString()).isEmpty();
		assertThat(Figures.vertical(2).toString()).isEmpty();
	}

	@Test
	public void testSingleFigure() {
		Figure figure = Figure.of("a", "b");

		assertThat(Figures.horizontal(5, figure).toString()).isEqualTo(figure.toString());
		assertThat(Figures.vertical(5, figure).toString()).isEqualTo(figure.toString());
	}

	@Test
	public void testCombinedFigureHasTheCommentOfTheFirstFigureWithComment() {
		Figure plain = Figure.of("a");
		Figure commented = Figure.of("b").withComment("# ");
		Figure other = Figure.of("c").withComment("// ");

		assertThat(Figures.horizontal(1, plain, commented, other).toString()).isEqualTo("# a b c\n");
		assertThat(Figures.vertical(0, plain, commented, other).toString()).isEqualTo("# a\n# b\n# c\n");
		assertThat(Figures.vertical(0, plain, plain).getComment()).isNull();
	}

	@Test
	public void testArguments() {
		Figure figure = Figure.of("a");

		assertThatThrownBy(() -> Figures.horizontal(-1, figure)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Figures.vertical(-1, figure)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Figures.horizontal(1, figure, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Figures.vertical(1, null, figure)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testDrawingsNextToEachOther() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 3, 2, 2, 0, 0, 0)), 3, 2, 2)
				.build();

		assertThat(Figures.horizontal(3, art.front(), art.side()).toString()).isEqualTo("""
				z                     z
				2 ┌───────────┐       2 ┌───────┐
				  │           │         │       │
				1 │     A     │       1 │   A   │
				  │           │         │       │
				0 └───────────┘       0 └───────┘
				  0   1   2   3   x     0   1   2   y
				""");
	}

	@Test
	public void testDrawingsBelowEachOther() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 3, 2, 2, 0, 0, 0)), 3, 2, 2)
				.build();

		assertThat(Figures.vertical(1, art.front(), art.side()).toString()).isEqualTo("""
				z
				2 ┌───────────┐
				  │           │
				1 │     A     │
				  │           │
				0 └───────────┘
				  0   1   2   3   x

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
	public void testDrawingsWithDifferentHeightsAreAlignedAtTheTop() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 0, 0, 0), place("B", 2, 2, 2, 2, 0, 0)), 4, 2, 2)
				.withAxes(false)
				.build();

		// the oblique view is higher than the front view
		assertThat(Figures.horizontal(2, art.front(), art.oblique()).toString()).isEqualTo("""
				┌───────┬───────┐      ┌───────────────┐
				│       │       │     ╱       ╱       ╱│
				│   A   │   B   │    ╱       ╱       ╱ │
				│       │       │   ╱       ╱       ╱  │
				└───────┴───────┘  ┌───────┬───────┐   │
				                   │       │       │  ╱
				                   │   A   │   B   │ ╱
				                   │       │       │╱
				                   └───────┴───────┘
				""");
	}

	@Test
	public void testDrawingsWithComment() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 2, 2, 2, 0, 0, 0)), 4, 2, 2)
				.withAxes(false)
				.withComment("// ")
				.build();

		assertThat(Figures.horizontal(2, art.front(), art.top()).toString()).isEqualTo("""
				// ┌───────┐  ┌───────┐
				// │       │  │       │
				// │   A   │  │   A   │
				// │       │  │       │
				// └───────┘  └───────┘
				""");
	}
}
