package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

class BruteForceBoundingBoxResultBuilderTest {

	/*
	 * One rotating box gives independent dimension winners:
	 *
	 *     width                 depth                 height
	 *     1 x 3 x 2             3 x 1 x 2              3 x 2 x 1
	 *
	 * No fourth objective is implicitly included.
	 */
	@Test
	void dimensionHelpersRegisterOnlyTheirNamedObjectives() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResultBuilder builder = builder(search).withMinimumDimensions();
			BruteForceBoundingBoxResult result = builder.build();
			assertThat(builder.objectives).hasSize(3);
			assertThat(result.getObjectiveResults().keySet()).containsExactly("x", "y", "z");
			assertThat(result.getObjectiveResults().get("x").getBoundingBox().dx()).isEqualTo(1);
			assertThat(result.getObjectiveResults().get("y").getBoundingBox().dy()).isEqualTo(1);
			assertThat(result.getObjectiveResults().get("z").getBoundingBox().dz()).isEqualTo(1);
		}
	}

	@Test
	void shorthandAndNamedObjectivesComposeWithoutClearingOrReordering() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			Predicate<BoundingBox> goal = bounds -> true;
			BruteForceBoundingBoxResultBuilder builder = builder(search)
					.withGoal(goal).withObjective("width", goal, BoundingBox.MIN_X)
					.withComparator(BoundingBox.MIN_Y);
			assertThat(builder.findObjective("default").goal()).isSameAs(goal);
			assertThat(builder.findObjective("default").comparator()).isSameAs(BoundingBox.MIN_Y);
			// The name "default" is not reserved. Replace it through the same path as any other name.
			builder.withObjective("default", bounds -> false, BoundingBox.MIN_Z).withGoal(goal);
			assertThat(builder.findObjective("default").comparator()).isSameAs(BoundingBox.MIN_Z);
			BruteForceBoundingBoxResult result = builder.build();
			assertThat(result.getObjectiveResults().keySet()).containsExactly("default", "width");
			assertThat(result.getReachedGoals()).containsExactly("default", "width");
			assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);

			BruteForceBoundingBoxResult reversed = builder(search).withObjective("width", goal, BoundingBox.MIN_X)
					.withComparator(BoundingBox.MIN_Z).withGoal(goal).build();
			assertThat(reversed.getObjectiveResults().keySet()).containsExactly("width", "default");
			assertThat(reversed.getReachedGoals()).containsExactly("width", "default");
		}
	}

	@Test
	void fallbackDoesNotBecomeARegisteredObjectiveWhenBuilderIsReused() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResultBuilder builder = builder(search);
			BruteForceBoundingBoxResult fallback = builder.build();
			assertThat(fallback.getObjectiveResults().keySet()).containsExactly("default");
			assertThat(builder.objectives).isNull();
			BruteForceBoundingBoxResult width = builder.withMinimumX().build();
			assertThat(width.getObjectiveResults().keySet()).containsExactly("x");
			assertThat(fallback.getObjectiveResults().keySet()).containsExactly("default");
		}
	}

	protected BruteForceBoundingBoxResultBuilder builder(BruteForceBoundingBox search) {
		BoxItem item = new BoxItem(Box.newBuilder().withSize(3, 2, 1).withRotate3D().withWeight(1).build());
		Container container = Container.newBuilder().withSize(3, 3, 3).withEmptyWeight(0).withMaxLoadWeight(10).build();
		return search.newResultBuilder().withBoxItems(item).withContainer(container);
	}
}
