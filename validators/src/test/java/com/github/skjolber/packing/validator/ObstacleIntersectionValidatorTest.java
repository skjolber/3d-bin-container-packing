package com.github.skjolber.packing.validator;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.test.assertj.ValidatorResultAssert;
import com.github.skjolber.packing.validator.reasons.BoxIntersectsObstacleReason;

/**
 * The obstacles of a container are boxes which are already in it: no placed box may take up any of their space. The container is 2 x 2 x 2,
 * with an obstacle which runs from the floor to the roof at x = 1, y = 1.
 */
public class ObstacleIntersectionValidatorTest {

	private final ObstacleIntersectionValidator validator = new ObstacleIntersectionValidator();

	private static final Box CUBE = Box.newBuilder().withId("cube").withSize(1, 1, 1).withWeight(1).build();

	/**
	 * Boxes next to the obstacle, on the floor and above it, do not take up its space.
	 */
	@Test
	void boxesAroundAnObstacleAreValid() {
		Container container = container(List.of(new Placement(CUBE.getStackValue(0), 0, 0, 0, 0), new Placement(CUBE.getStackValue(0), 0, 1, 0, 0),
				new Placement(CUBE.getStackValue(0), 0, 0, 1, 1)));
		// <figure>
		//   z   /-------/-------|       z                         y                         z
		//      /       /#######/|       2 +-------+-------+       2 +-------+-------+       2         +-------+
		//   | |-------|-------|#|         |       |#######|         |       |#######|                 |#######|
		//   | |       |#######|#|         | cube  |#######|         | cube  |#######|                 |#######|
		// 2 | | cube  |#######|#|   y     |       |#######|         |       |#######|                 |#######|
		//   | |       |#######|#|       1 +-------+-------+       1 +-------+-------+       1 +-------+#######|
		//   | |-------|-------|#| /       |       |       |         |       |       |         |       |#######|
		//   |/       /       /|#|/        | cube  | cube  |         | cube  | cube  |         | cube  |#######|
		// 1 |-------|-------| |#| 2       |       |       |         |       |       |         |       |#######|
		//   |       |       | |/        0 +-------+-------+       0 +-------+-------+       0 +-------+-------+
		//   | cube  | cube  | | 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(new PackagerResult(List.of(container), 0, false));

		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(validator.validate(container, reasons)).isTrue();
		assertThat(reasons).isEmpty();
	}

	/**
	 * A box in the obstacle's column is rejected, wherever it is in the column.
	 */
	@Test
	void boxInTheSpaceOfAnObstacleIsRejected() {
		Placement ok = new Placement(CUBE.getStackValue(0), 0, 0, 0, 0);
		Placement inObstacle = new Placement(CUBE.getStackValue(0), 0, 1, 1, 1);
		Container container = container(List.of(ok, inObstacle));
		// <figure>
		//   z           /-------|       z                         y                         z
		//              /       /|       2         +-------+       2         +-------+       2         +-------+
		//   |         |-------| |                 |       |                 |       |                 |       |
		//   |         |       | |                 | cube  |                 | cube  |                 | cube  |
		// 2 |         | cube  | |   y             |       |                 |       |                 |       |
		//   |         |       |/|       1 +-------+-------+       1 +-------+-------+       1 +-------+-------+
		//   | /-------|-------|#| /       |       |#######|         |       |                 |       |#######|
		//   |/       /|#######|#|/        | cube  |#######|         | cube  |                 | cube  |#######|
		// 1 |-------| |#######|#| 2       |       |#######|         |       |                 |       |#######|
		//   |       | |#######|/        0 +-------+-------+       0 +-------+               0 +-------+-------+
		//   | cube  | |-------| 1         0       1       2   x     0       1       2   x     0       1       2   y
		//   |       |/       /
		// 0 |-------|---------- x
		//   0       1       2
		// </figure>
		figure(new PackagerResult(List.of(container), 0, false));

		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(validator.validate(container, reasons)).isFalse();
		assertThat(reasons).singleElement().isInstanceOfSatisfying(BoxIntersectsObstacleReason.class, reason -> {
			assertThat(reason.getPlacement()).isSameAs(inObstacle);
			assertThat(reason.getObstacle()).isSameAs(container.getObstacles().get(0));
		});
	}

	/**
	 * Every box which takes up space of an obstacle is reported.
	 */
	@Test
	void everyBoxInTheSpaceOfAnObstacleIsReported() {
		Container container = container(List.of(new Placement(CUBE.getStackValue(0), 0, 1, 1, 0), new Placement(CUBE.getStackValue(0), 0, 1, 1, 1)));

		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(validator.validate(container, reasons)).isFalse();
		assertThat(reasons).hasSize(2).allSatisfy(reason -> assertThat(reason).isInstanceOf(BoxIntersectsObstacleReason.class));
	}

	@Test
	void containerWithoutObstaclesIsValid() {
		Stack stack = new Stack();
		stack.add(new Placement(CUBE.getStackValue(0), 0, 1, 1, 1));
		Container container = Container.newBuilder().withId("container").withSize(2, 2, 2).withMaxLoadWeight(10).withStack(stack).build();

		assertThat(validator.validate(container, new ArrayList<>())).isTrue();
	}

	/**
	 * The default validator includes the check: a result with a box in the space of an obstacle is rejected, without other reasons.
	 */
	@Test
	void defaultValidatorRejectsABoxInTheSpaceOfAnObstacle() throws IOException {
		try (DefaultValidator defaultValidator = new DefaultValidator()) {
			Container valid = container(List.of(new Placement(CUBE.getStackValue(0), 0, 0, 0, 0)));
			Container invalid = container(List.of(new Placement(CUBE.getStackValue(0), 0, 1, 1, 0)));

			assertThat(validate(defaultValidator, valid).isValid()).isTrue();

			ValidatorResult result = validate(defaultValidator, invalid);
			ValidatorResultAssert.assertThat(result).isNotValid().hasReasons(BoxIntersectsObstacleReason.class);
		}
	}

	private static ValidatorResult validate(DefaultValidator defaultValidator, Container resultContainer) {
		Container reference = Container.newBuilder().withId("container").withSize(2, 2, 2).withMaxLoadWeight(10).build();
		return defaultValidator.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(reference, 1)))
				.withMaxContainerCount(1)
				.withPackagerResult(new PackagerResult(List.of(resultContainer), 0, false))
				.withBoxItems(List.of(new BoxItem(CUBE, resultContainer.getStack().size())))
				.build();
	}

	/**
	 * @param placements the placed boxes
	 * @return a container with the placements, and an obstacle from the floor to the roof at x = 1, y = 1
	 */
	private static Container container(List<Placement> placements) {
		Stack stack = new Stack();
		for (Placement placement : placements) {
			stack.add(placement);
		}
		Box column = Box.newBuilder().withId("obstacle").withSize(1, 1, 2).withWeight(0).build();
		Placement obstacle = new Placement(column.getStackValue(0), 0, 1, 1, 0);
		return Container.newBuilder().withId("container").withSize(2, 2, 2).withMaxLoadWeight(10).withStack(stack).build().withObstacles(List.of(obstacle));
	}
}
