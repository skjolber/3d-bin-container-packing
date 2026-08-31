package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.impl.ValidatingStack;

/**
 * Shared utilities for packager constraint integration tests.
 * <p>
 * Subclasses inherit helper methods but create their own packager instances
 * directly in each {@code @Test} method (same pattern as
 * {@code PlainPackagerLoadTest}).
 */
public abstract class AbstractPackagerConstraintTest {

	/**
	 * Creates a container sized {@code dx × dy × dz}.
	 * Uses a large {@code maxLoadWeight} so the container-level limit
	 * never interferes with box-level constraint tests.
	 */
	protected Container container(int dx, int dy, int dz) {
		return Container.newBuilder()
				.withDescription("container")
				.withEmptyWeight(0)
				.withSize(dx, dy, dz)
				.withMaxLoadWeight(1_000_000)
				.withStack(new ValidatingStack())
				.build();
	}

	/** Returns the placement whose {@code absoluteZ} equals {@code z}. */
	protected Placement placementAt(List<Placement> placements, int z) {
		return placements.stream()
				.filter(p -> p.getAbsoluteZ() == z)
				.findFirst()
				.orElseThrow(() -> new AssertionError("No placement at z=" + z));
	}

	/** Asserts that the result succeeded and used the expected number of containers. */
	protected void assertContainers(PackagerResult result, int expectedCount) {
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getContainers()).hasSize(expectedCount);
	}

	/** Asserts the stack size (number of placed boxes) inside a specific container. */
	protected void assertStackSize(PackagerResult result, int containerIndex, int expectedSize) {
		assertThat(result.getContainers().get(containerIndex).getStack().size())
				.isEqualTo(expectedSize);
	}
}
