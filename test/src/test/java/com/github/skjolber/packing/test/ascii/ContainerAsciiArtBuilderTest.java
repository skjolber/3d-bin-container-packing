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
				┌───────────────┐
				│               │
				│       A       │
				│               │
				└───────────────┘
				""");
	}

	@Test
	public void testTooLargeDrawing() {
		// units of millimeters: the default scale would be thousands of columns
		ContainerAsciiArt art = ContainerAsciiArt.newBuilder()
				.withPlacements(List.of(place("A", 1000, 1000, 1000, 0, 0, 0)), 6000, 2400, 2400)
				.build();

		assertThatThrownBy(() -> art.front()).isInstanceOf(IllegalStateException.class).hasMessageContaining("withWidth");
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
