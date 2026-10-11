package com.github.skjolber.packing.test.ascii;

import static com.github.skjolber.packing.test.ascii.TestPlacements.place;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ContainerAsciiArtBuilderTest {

	@Test
	public void testContainerIsRequired() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().build()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testContainerSizeMustBePositive() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withPlacements(List.of(), 0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withPlacements(List.of(), 1, 0, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withPlacements(List.of(), 1, 1, -1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testScaleMustBePositive() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withScale(0, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withScale(1, -1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withScale(Double.NaN, 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testWidthMustBeAtLeastTwo() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withWidth(1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testHeightMustBeAtLeastTwo() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withHeight(1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withHeight(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testStyleMustNotBeNull() {
		assertThatThrownBy(() -> ContainerAsciiArt.newBuilder().withStyle(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testTheLastOfScaleAndWidthIsUsed() {
		ContainerAsciiArt.Builder builder = ContainerAsciiArt.newBuilder().withPlacements(List.of(place("A", 4, 1, 1, 0, 0, 0)), 4, 1, 1);

		String scaled = builder.withScale(3, 2).build().front().toString();
		String width = builder.withWidth(41).build().front().toString();
		assertThat(width).isNotEqualTo(scaled);

		assertThat(builder.withScale(3, 2).build().front().toString()).isEqualTo(scaled);
		assertThat(builder.withWidth(41).build().front().toString()).isEqualTo(width);
	}

	@Test
	public void testBoxesOutsideTheContainerAreDrawn() {
		// the container is 2 wide, but the box is 4 wide
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 4, 1, 1, 0, 0, 0)), 2, 1, 1)
				.withAxes(false)
				.build();

		assertThat(art.front().toString()).isEqualTo("""
				┌───────────────────────────────┐
				│                               │
				│               A               │
				│                               │
				└───────────────────────────────┘
				""");
	}

	@Test
	public void testExplicitScaleWhichIsTooLargeIsRefused() {
		// units of millimeters at a scale of several columns per unit: a drawing of millions of characters
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1000, 1000, 1000, 0, 0, 0)), 6000, 2400, 2400)
				.withScale(4, 2)
				.build();

		assertThatThrownBy(() -> art.front()).isInstanceOf(IllegalStateException.class).hasMessageContaining("withWidth");
	}

	/**
	 * Units of millimeters: the scale at which the labels fit would be thousands of columns, so the views are drawn smaller instead: none of them
	 * is wider than 80 columns or higher than 40 lines.
	 */
	@Test
	public void testDefaultScaleOfALargeContainerKeepsEachViewWithinTheDefaultSize() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1000, 1000, 1000, 0, 0, 0), place("B", 2000, 1000, 1000, 1000, 0, 0)), 6000, 2400, 2400)
				.build();

		assertThat(art.front().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.top().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.side().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.oblique().getWidth()).isLessThanOrEqualTo(80);
		assertThat(art.front().getHeight()).isLessThanOrEqualTo(40);
		assertThat(art.top().getHeight()).isLessThanOrEqualTo(40);
		assertThat(art.side().getHeight()).isLessThanOrEqualTo(40);
		assertThat(art.oblique().getHeight()).isLessThanOrEqualTo(40);

		// as large as possible: the widest view is nearly 80 columns wide
		assertThat(art.front().getWidth()).isGreaterThan(70);
		assertThat(art.front().toString()).contains("A").contains("B");
	}

	/**
	 * A drawing which is scaled down to fit keeps its aspect: a line is twice as tall as a character is wide, also for a cube in units of millimeters.
	 */
	@Test
	public void testDefaultScaleOfALargeCubeKeepsTwiceAsManyColumnsAsLines() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1000, 1000, 1000, 0, 0, 0)), 1000, 1000, 1000)
				.withAxes(false)
				.build();

		int columns = art.front().getWidth() - 1;
		int lines = art.front().getHeight() - 1;
		assertThat(columns).isGreaterThan(70);
		assertThat(columns).isBetween(2 * lines - 1, 2 * lines + 1);
	}

	/**
	 * The same when fitting to a width (a cube that is larger than the default scale at 41 columns).
	 */
	@Test
	public void testWidthKeepsTwiceAsManyColumnsAsLines() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 100, 100, 100, 0, 0, 0)), 100, 100, 100)
				.withAxes(false)
				.withWidth(41)
				.build();

		assertThat(art.front().getWidth()).isEqualTo(41);
		assertThat(art.front().getHeight()).isEqualTo(21);
	}

	/**
	 * A container which is much higher than it is wide (here 2352 x 2394 x 12031, drawn to its size with the container outline): the height limits
	 * the scale, so that no view is higher than 40 lines.
	 */
	@Test
	public void testDefaultScaleOfATallContainerKeepsEachViewWithinTheDefaultSize() {
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1200, 750, 2280, 0, 0, 0), place("B", 1200, 450, 2280, 0, 750, 0)), 2352, 2394, 12031)
				.withContainerOutline(true)
				.build();

		for (Figure figure : List.of(art.front(), art.top(), art.side(), art.oblique())) {
			assertThat(figure.getWidth()).isLessThanOrEqualTo(80);
			assertThat(figure.getHeight()).isLessThanOrEqualTo(40);
		}
		assertThat(art.front().getHeight()).isGreaterThan(35);
	}

	@Test
	public void testBuilderCanBeReused() {
		ContainerAsciiArt.Builder builder = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1, 1, 1, 0, 0, 0)), 1, 1, 1)
				.withAxes(false);

		ContainerAsciiArt first = builder.build();
		ContainerAsciiArt second = builder.withPlacements(List.of(place("B", 1, 1, 1, 0, 0, 0)), 1, 1, 1).build();

		assertThat(first.front().toString()).contains("A").doesNotContain("B");
		assertThat(second.front().toString()).contains("B").doesNotContain("A");
	}
}
