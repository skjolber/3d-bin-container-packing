package com.github.skjolber.packing.accelerator;

import com.github.skjolber.packing.accelerator.test.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.accelerator.test.FlatTestInstances.Instance;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;

class FlatProblemTest {

	/** A search places the leading boxes up to the limit; an accelerator in native code has no array bounds to stop it at a negative one. */
	@Test
	void aProblemWithANegativeLimitCannotBeBuilt() {
		Instance instance = FlatTestInstances.random(1, 5, 5);
		Container container = instance.container;
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(instance.copyBoxItems())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		assertThatThrownBy(() -> FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), -1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), 0).ints()[FlatProblem.LIMIT])
				.isZero();
	}
}
