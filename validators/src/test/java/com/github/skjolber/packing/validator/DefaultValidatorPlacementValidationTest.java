package com.github.skjolber.packing.validator;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.PlacementValidator;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilder;
import com.github.skjolber.packing.api.validator.placement.PlacementValidatorBuilderFactory;
import com.github.skjolber.packing.api.validator.placement.StabilityValidator;
import com.github.skjolber.packing.test.assertj.ValidatorResultAssert;
import com.github.skjolber.packing.validator.load.CompositeLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;
import com.github.skjolber.packing.validator.reasons.TooFewBoxItemIdsReason;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;

/**
 * The load constraints which the boxes declare are validated by default, also for a container item which has no placement validator builder
 * factory, so that a result which breaks one cannot be accepted without the caller noticing. A stability rule is a policy, and is only
 * validated with a factory which has a stability validator. The result says which of the two were validated, see
 * {@linkplain ValidatorResult#isLoadValidated()}, {@linkplain ValidatorResult#isStabilityValidated()} and
 * {@linkplain ValidatorResult#hasLoadConstraints()}.
 * <p>
 * The container is 2 x 1 x 4. The results are laid out by hand, as a packager does not build layouts which break the constraints. Most of
 * them are a tower of boxes at x = 0, bottom up.
 */
public class DefaultValidatorPlacementValidationTest {

	private final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws IOException {
		validator.close();
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// the load constraints are validated without a factory
	// ------------------------------------------------------------------------------------------------------------------------------

	/**
	 * The base carries at most 5; the box on top weighs 10. With a lighter box on top, the result is accepted.
	 */
	@Test
	void maxLoadWeightIsValidatedWithoutAFactory() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build());
		BoxItem heavy = new BoxItem(box("heavy", 10));
		BoxItem light = new BoxItem(box("light", 4));

		PackagerResult tooHeavy = tower(base, heavy);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | heavy | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(tooHeavy);
		ValidatorResult rejected = validation(base, heavy).withPackagerResult(tooHeavy).build();
		ValidatorResultAssert.assertThat(rejected).isNotValid().hasReasons(ExcessiveLoadWeightReason.class);
		assertCoverage(rejected, true, false, true);

		PackagerResult withinLimit = tower(base, light);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | light |         | light |         | light |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | light | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(withinLimit);
		ValidatorResult accepted = validation(base, light).withPackagerResult(withinLimit).build();
		ValidatorResultAssert.assertThat(accepted).isValid();
		assertCoverage(accepted, true, false, true);
	}

	/**
	 * The base carries boxes of its own type only; a cup is on top of it. Two boxes of the base's type on each other are accepted.
	 */
	@Test
	void identicalBoxOnlyIsValidatedWithoutAFactory() {
		Box identical = Box.newBuilder().withId("identical").withSize(1, 1, 1).withWeight(1).withMaxLoadIdenticalBoxCount(-1).build();
		BoxItem base = new BoxItem(identical);
		BoxItem cup = new BoxItem(box("cup", 1));
		BoxItem twice = new BoxItem(identical, 2);

		PackagerResult mixed = tower(base, cup);
		// <figure>
		//   z                     z                     y                     z
		//                         2 +-----------+       1 +-----------+       2 +-----------+
		//   | /-----------|         |           |         |           |         |           |
		//   |/           /|         |    cup    |         |    cup    |         |    cup    |
		// 2 |-----------| |         |           |         |           |         |           |
		//   |           | |       1 +-----------+       0 +-----------+       1 +-----------+
		//   |    cup    | |   y     |           |         0           1   x     |           |
		//   |           |/|         | identical |                               | identical |
		// 1 |-----------| | /       |           |                               |           |
		//   |           | |/      0 +-----------+                             0 +-----------+
		//   | identical | | 1       0           1   x                           0           1   y
		//   |           |/
		// 0 |-----------|-- x
		//   0           1
		// </figure>
		figure(mixed);
		ValidatorResult rejected = validation(base, cup).withPackagerResult(mixed).build();
		ValidatorResultAssert.assertThat(rejected).isNotValid().hasReasons(NonIdenticalLoadBoxReason.class);
		assertCoverage(rejected, true, false, true);

		PackagerResult same = tower(twice, twice);
		// <figure>
		//   z                     z                     y                     z
		//                         2 +-----------+       1 +-----------+       2 +-----------+
		//   | /-----------|         |           |         |           |         |           |
		//   |/           /|         | identical |         | identical |         | identical |
		// 2 |-----------| |         |           |         |           |         |           |
		//   |           | |       1 +-----------+       0 +-----------+       1 +-----------+
		//   | identical | |   y     |           |         0           1   x     |           |
		//   |           |/|         | identical |                               | identical |
		// 1 |-----------| | /       |           |                               |           |
		//   |           | |/      0 +-----------+                             0 +-----------+
		//   | identical | | 1       0           1   x                           0           1   y
		//   |           |/
		// 0 |-----------|-- x
		//   0           1
		// </figure>
		figure(same);
		ValidatorResult accepted = validation(twice).withPackagerResult(same).build();
		ValidatorResultAssert.assertThat(accepted).isValid();
		assertCoverage(accepted, true, false, true);
	}

	/**
	 * The base carries one level of boxes at most; two boxes are stacked on it. With one box on top, the result is accepted.
	 */
	@Test
	void maxLoadBoxCountIsValidatedWithoutAFactory() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadBoxCount(1).build());
		BoxItem first = new BoxItem(box("first", 1));
		BoxItem second = new BoxItem(box("second", 1));

		PackagerResult twoLevels = tower(base, first, second);
		// <figure>
		//   z                  z                  y                  z
		//                      3 +--------+       1 +--------+       3 +--------+
		//   | /--------|         |        |         |        |         |        |
		//   |/        /|         | second |         | second |         | second |
		// 3 |--------| |         |        |         |        |         |        |
		//   |        | |       2 +--------+       0 +--------+       2 +--------+
		//   | second | |         |        |         0        1   x     |        |
		//   |        |/|         | first  |                            | first  |
		// 2 |--------| |         |        |                            |        |
		//   |        | |       1 +--------+                          1 +--------+
		//   | first  | |   y     |        |                            |        |
		//   |        |/|         |  base  |                            |  base  |
		// 1 |--------| | /       |        |                            |        |
		//   |        | |/      0 +--------+                          0 +--------+
		//   |  base  | | 1       0        1   x                        0        1   y
		//   |        |/
		// 0 |--------|-- x
		//   0        1
		// </figure>
		figure(twoLevels);
		ValidatorResult rejected = validation(base, first, second).withPackagerResult(twoLevels).build();
		ValidatorResultAssert.assertThat(rejected).isNotValid().hasReasons(ExcessiveLoadBoxCountReason.class);
		assertCoverage(rejected, true, false, true);

		PackagerResult oneLevel = tower(base, first);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | first |         | first |         | first |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | first | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(oneLevel);
		ValidatorResult accepted = validation(base, first).withPackagerResult(oneLevel).build();
		ValidatorResultAssert.assertThat(accepted).isValid();
		assertCoverage(accepted, true, false, true);
	}

	/**
	 * The groups are validated like the box items.
	 */
	@Test
	void loadConstraintsOfBoxItemGroupsAreValidatedWithoutAFactory() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build());
		BoxItem heavy = new BoxItem(box("heavy", 10));
		BoxItemGroup group = new BoxItemGroup("group", List.of(base, heavy));

		PackagerResult tooHeavy = tower(base, heavy);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | heavy | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(tooHeavy);
		ValidatorResult rejected = validator.newResultBuilder()
				.withContainerItems(List.of(item()))
				.withMaxContainerCount(1)
				.withBoxItemGroups(List.of(group))
				.withPackagerResult(tooHeavy)
				.build();

		ValidatorResultAssert.assertThat(rejected).isNotValid().hasReasons(ExcessiveLoadWeightReason.class);
		assertCoverage(rejected, true, false, true);
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// coverage
	// ------------------------------------------------------------------------------------------------------------------------------

	/**
	 * No box declares a load constraint: the load validation passes vacuously, which the result tells (it validated the loads, and had no load
	 * constraints to check), and there is no stability validation. No load validator is created, so no support graph is built.
	 */
	@Test
	void unconstrainedInputIsLoadValidatedVacuously() {
		BoxItem bottom = new BoxItem(box("bottom", 1));
		BoxItem top = new BoxItem(box("top", 10));

		PackagerResult result = tower(bottom, top);
		// <figure>
		//   z                  z                  y                  z
		//                      2 +--------+       1 +--------+       2 +--------+
		//   | /--------|         |        |         |        |         |        |
		//   |/        /|         |  top   |         |  top   |         |  top   |
		// 2 |--------| |         |        |         |        |         |        |
		//   |        | |       1 +--------+       0 +--------+       1 +--------+
		//   |  top   | |   y     |        |         0        1   x     |        |
		//   |        |/|         | bottom |                            | bottom |
		// 1 |--------| | /       |        |                            |        |
		//   |        | |/      0 +--------+                          0 +--------+
		//   | bottom | | 1       0        1   x                        0        1   y
		//   |        |/
		// 0 |--------|-- x
		//   0        1
		// </figure>
		figure(result);
		ValidatorResult accepted = validation(bottom, top).withPackagerResult(result).build();

		ValidatorResultAssert.assertThat(accepted).isValid();
		assertCoverage(accepted, true, false, false);
		assertThat(accepted.isLoadValidated() && !accepted.hasLoadConstraints()).as("vacuous").isTrue();
	}

	/**
	 * The default placement validator without a load constraint has no load validators to run (an empty composite): a support graph is
	 * built by the load validators of the constraints only.
	 */
	@Test
	void placementValidatorWithoutLoadConstraintsHasNothingToRun() {
		Container container = tower(new BoxItem(box("bottom", 1)), new BoxItem(box("top", 10))).getContainers().get(0);
		List<Placement> placements = container.getStack().getPlacements();

		DefaultPlacementValidator unconstrained = (DefaultPlacementValidator)new DefaultPlacementValidatorBuilder().withContainer(container).withPlacements(placements).build();
		assertThat(unconstrained.getLoadValidator()).isInstanceOfSatisfying(CompositeLoadValidator.class, composite -> assertThat(composite.getValidators()).isEmpty());
		assertThat(unconstrained.isLoadValidated()).isTrue();
		assertThat(unconstrained.isStabilityValidated()).isFalse();

		Box constrained = Box.newBuilder().withId("constrained").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build();
		Container other = tower(new BoxItem(constrained), new BoxItem(box("top", 10))).getContainers().get(0);
		DefaultPlacementValidator weight = (DefaultPlacementValidator)new DefaultPlacementValidatorBuilder().withContainer(other).withPlacements(other.getStack().getPlacements()).build();
		assertThat(weight.getLoadValidator()).isInstanceOf(WeightLoadValidator.class);
	}

	/**
	 * Opting out restores the previous behavior: the placements are not validated, so a result which breaks a load constraint is accepted, and the
	 * result says that the loads were not validated (and that it did not look for load constraints).
	 */
	@Test
	void optOutSkipsTheLoadValidationAndTheResultSaysSo() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build());
		BoxItem heavy = new BoxItem(box("heavy", 10));

		PackagerResult tooHeavy = tower(base, heavy);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | heavy | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(tooHeavy);
		ValidatorResult skipped = validation(base, heavy).withDefaultPlacementValidation(false).withPackagerResult(tooHeavy).build();

		ValidatorResultAssert.assertThat(skipped).isValid();
		assertCoverage(skipped, false, false, false);

		// the default is to validate
		ValidatorResult validated = validation(base, heavy).withDefaultPlacementValidation(true).withPackagerResult(tooHeavy).build();
		ValidatorResultAssert.assertThat(validated).isNotValid().hasReasons(ExcessiveLoadWeightReason.class);
		assertCoverage(validated, true, false, true);
	}

	/**
	 * With a factory, the factory decides: the load constraints are still validated, and the stability validator is applied. A box which
	 * overhangs is accepted by default, as stability is a policy, and rejected with full support required.
	 */
	@Test
	void explicitFactoryDecidesAndKeepsStability() {
		BoxItem base = new BoxItem(box("base", 1));
		BoxItem board = new BoxItem(Box.newBuilder().withId("board").withSize(2, 1, 1).withWeight(1).build());

		PackagerResult overhanging = tower(base, board);
		// <figure>
		//   z                         z                         y                         z
		//                             2 +---------------+       1 +---------------+       2 +-------+
		//   | /---------------|         |               |         |               |         |       |
		//   |/               /|         |     board     |         |     board     |         | board |
		// 2 |---------------| |         |               |         |               |         |       |
		//   |               | |       1 +-------+-------+       0 +---------------+       1 +-------+
		//   |     board     | |   y     |       |                 0       1       2   x     |       |
		//   |               |/          | base  |                                           | base  |
		// 1 |-------|-------|   /       |       |                                           |       |
		//   |       | |        /      0 +-------+                                         0 +-------+
		//   | base  | |       / 1       0       1       2   x                               0       1   y
		//   |       |/       /
		// 0 |-------|---------- x
		//   0       1       2
		// </figure>
		figure(overhanging);

		ValidatorResult byDefault = validation(base, board).withPackagerResult(overhanging).build();
		ValidatorResultAssert.assertThat(byDefault).isValid();
		assertCoverage(byDefault, true, false, false);

		ValidatorResult withFactory = validation(factory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator())), base, board).withPackagerResult(overhanging).build();
		ValidatorResultAssert.assertThat(withFactory).isNotValid().hasReasons(InsufficientSupportAreaReason.class);
		assertCoverage(withFactory, true, true, false);

		// supported by two boxes: accepted, and the result says that the stability was validated
		BoxItem pair = new BoxItem(box("base", 1), 2);
		PackagerResult supported = layout(place(pair, 0, 0, 0), place(pair, 1, 0, 0), place(board, 0, 0, 1));
		// <figure>
		//   z                         z                         y                         z
		//                             2 +---------------+       1 +---------------+       2 +-------+
		//   | /---------------|         |               |         |               |         |       |
		//   |/               /|         |     board     |         |     board     |         | board |
		// 2 |---------------| |         |               |         |               |         |       |
		//   |               | |       1 +-------+-------+       0 +---------------+       1 +-------+
		//   |     board     | |   y     |       |       |         0       1       2   x     |       |
		//   |               |/|         | base  | base  |                                   | base  |
		// 1 |-------|-------| | /       |       |       |                                   |       |
		//   |       |       | |/      0 +-------+-------+                                 0 +-------+
		//   | base  | base  | | 1       0       1       2   x                               0       1   y
		//   |       |       |/
		// 0 |-------|-------|-- x
		//   0       1       2
		// </figure>
		figure(supported);
		ValidatorResult stable = validation(factory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator())), pair, board).withPackagerResult(supported).build();
		ValidatorResultAssert.assertThat(stable).isValid();
		assertCoverage(stable, true, true, false);

		// a factory without a stability validator validates the loads only
		ValidatorResult loadOnly = validation(factory(f -> {
		}), base, board).withPackagerResult(overhanging).build();
		ValidatorResultAssert.assertThat(loadOnly).isValid();
		assertCoverage(loadOnly, true, false, false);
	}

	/**
	 * A factory is not affected by the opt-out: that is for the container items which have none.
	 */
	@Test
	void optOutDoesNotAffectAContainerItemWithAFactory() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build());
		BoxItem heavy = new BoxItem(box("heavy", 10));

		PackagerResult tooHeavy = tower(base, heavy);
		// <figure>
		//   z                 z                 y                 z
		//                     2 +-------+       1 +-------+       2 +-------+
		//   | /-------|         |       |         |       |         |       |
		//   |/       /|         | heavy |         | heavy |         | heavy |
		// 2 |-------| |         |       |         |       |         |       |
		//   |       | |       1 +-------+       0 +-------+       1 +-------+
		//   | heavy | |   y     |       |         0       1   x     |       |
		//   |       |/|         | base  |                           | base  |
		// 1 |-------| | /       |       |                           |       |
		//   |       | |/      0 +-------+                         0 +-------+
		//   | base  | | 1       0       1   x                       0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(tooHeavy);
		ValidatorResult result = validation(factory(f -> {
		}), base, heavy).withDefaultPlacementValidation(false).withPackagerResult(tooHeavy).build();

		ValidatorResultAssert.assertThat(result).isNotValid().hasReasons(ExcessiveLoadWeightReason.class);
		assertCoverage(result, true, false, true);
	}

	/**
	 * The result covers all of the containers: with one container item which has a stability validator and one which has not, the loads are
	 * validated, but the stability is not.
	 */
	@Test
	void stabilityIsOnlyValidatedIfEveryContainerHasIt() {
		BoxItem cubes = new BoxItem(box("cube", 1), 2);
		Container plain = container("plain", 2, 1, 4);
		Container supported = container("supported", 2, 1, 4);

		PackagerResult result = new PackagerResult(List.of(with(plain, place(cubes, 0, 0, 0)), with(supported, place(cubes, 0, 0, 0))), 0, false);
		// <figure>
		// container 1 of 2: plain
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | cube  |         | cube  |         | cube  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | cube  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		//
		// container 2 of 2: supported
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | cube  |         | cube  |         | cube  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | cube  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(result);

		ValidatorResult mixed = validator.newResultBuilder()
				.withContainerItem(b -> b.withContainerItem(plain, 1))
				.withContainerItem(b -> b.withContainerItem(supported, 1).withPlacementValidatorBuilderFactory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator())))
				.withMaxContainerCount(2)
				.withBoxItems(cubes)
				.withPackagerResult(result)
				.build();
		ValidatorResultAssert.assertThat(mixed).isValid();
		assertCoverage(mixed, true, false, false);

		ValidatorResult both = validator.newResultBuilder()
				.withContainerItem(b -> b.withContainerItem(plain, 1).withPlacementValidatorBuilderFactory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator())))
				.withContainerItem(b -> b.withContainerItem(supported, 1).withPlacementValidatorBuilderFactory(f -> f.withStabilityValidator(new FullySupportedStabilityValidator())))
				.withMaxContainerCount(2)
				.withBoxItems(cubes)
				.withPackagerResult(result)
				.build();
		ValidatorResultAssert.assertThat(both).isValid();
		assertCoverage(both, true, true, false);
	}

	/**
	 * The flags say what was validated, so a result which was not validated as far as the placements does not claim it was: here the box counts
	 * are wrong, which is found first, and when the validation is interrupted.
	 */
	@Test
	void resultWhichDidNotGetToThePlacementsClaimsNothing() {
		BoxItem base = new BoxItem(Box.newBuilder().withId("base").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(5).build());
		BoxItem missing = new BoxItem(box("missing", 1));

		PackagerResult onlyBase = tower(base);
		// <figure>
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | base  |         | base  |         | base  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | base  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(onlyBase);
		ValidatorResult wrongCounts = validation(base, missing).withPackagerResult(onlyBase).build();
		ValidatorResultAssert.assertThat(wrongCounts).isNotValid().hasReasons(TooFewBoxItemIdsReason.class);
		assertCoverage(wrongCounts, false, false, false);

		ValidatorResult interrupted = validation(base).withInterrupt(() -> true).withPackagerResult(onlyBase).build();
		assertThat(interrupted.isTimeout()).isTrue();
		assertCoverage(interrupted, false, false, false);
	}

	/**
	 * A placement validator says itself what it validates; one which does not say is not claimed to validate anything.
	 */
	@Test
	void customPlacementValidatorReportsItsOwnCoverage() {
		BoxItem cube = new BoxItem(box("cube", 1));
		PackagerResult result = tower(cube);
		// <figure>
		//   z                 z                 y                 z
		//                     1 +-------+       1 +-------+       1 +-------+
		//   | /-------|   y     |       |         |       |         |       |
		//   |/       /|         | cube  |         | cube  |         | cube  |
		// 1 |-------| | /       |       |         |       |         |       |
		//   |       | |/      0 +-------+       0 +-------+       0 +-------+
		//   | cube  | | 1       0       1   x     0       1   x     0       1   y
		//   |       |/
		// 0 |-------|-- x
		//   0       1
		// </figure>
		figure(result);

		PlacementValidator silent = (placements, reasons) -> true;
		ValidatorResult undeclared = validation(custom(silent), cube).withPackagerResult(result).build();
		ValidatorResultAssert.assertThat(undeclared).isValid();
		assertCoverage(undeclared, false, false, false);

		PlacementValidator loads = new PlacementValidator() {

			@Override
			public boolean isValid(List<Placement> list, List<ValidatorResultReason> reasons) {
				return true;
			}

			@Override
			public boolean isLoadValidated() {
				return true;
			}
		};
		ValidatorResult declared = validation(custom(loads), cube).withPackagerResult(result).build();
		ValidatorResultAssert.assertThat(declared).isValid();
		assertCoverage(declared, true, false, false);
	}

	/**
	 * A result which is created without coverage (for example by a validator which has no placement checks) does not claim any.
	 */
	@Test
	void resultWithoutCoverageClaimsNothing() {
		assertCoverage(new ValidatorResult(0, true, false, List.of()), false, false, false);
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// input
	// ------------------------------------------------------------------------------------------------------------------------------

	private static void assertCoverage(ValidatorResult result, boolean loadValidated, boolean stabilityValidated, boolean hasLoadConstraints) {
		assertThat(result.isLoadValidated()).as("load validated").isEqualTo(loadValidated);
		assertThat(result.isStabilityValidated()).as("stability validated").isEqualTo(stabilityValidated);
		assertThat(result.hasLoadConstraints()).as("has load constraints").isEqualTo(hasLoadConstraints);
	}

	private static Box box(String id, int weight) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(weight).build();
	}

	private static Container container(String id, int dx, int dy, int dz) {
		return Container.newBuilder().withId(id).withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}

	/** The container item of the input: the container is 2 x 1 x 4 */
	private static ContainerItem item() {
		return new ContainerItem(container("container", 2, 1, 4), 1);
	}

	/** The validator for the input: one container, and the box items (the result is added by the test) */
	private DefaultValidator.DefaultValidatorResultBuilder validation(BoxItem... boxItems) {
		return validator.newResultBuilder().withContainerItems(List.of(item())).withMaxContainerCount(1).withBoxItems(boxItems);
	}

	/** As above, but the container item has the placement validator builder factory */
	private DefaultValidator.DefaultValidatorResultBuilder validation(PlacementValidatorBuilderFactory factory, BoxItem... boxItems) {
		return validator.newResultBuilder()
				.withContainerItem(b -> b.withContainerItem(item()).withPlacementValidatorBuilderFactory(factory))
				.withMaxContainerCount(1)
				.withBoxItems(boxItems);
	}

	private static PlacementValidatorBuilderFactory factory(Consumer<ValidatorResultBuilder.PlacementValidatorBuilderFactoryBuilder> consumer) {
		AbstractValidatorResultBuilder.DefaultPlacementValidatorBuilderFactoryBuilder builder = new AbstractValidatorResultBuilder.DefaultPlacementValidatorBuilderFactoryBuilder();
		consumer.accept(builder);
		return builder.build();
	}

	/** A factory which creates the placement validator */
	private static PlacementValidatorBuilderFactory custom(PlacementValidator placementValidator) {
		return () -> new PlacementValidatorBuilder() {

			@Override
			public PlacementValidatorBuilder withContainer(Container container) {
				return this;
			}

			@Override
			public PlacementValidatorBuilder withPlacements(List<Placement> placements) {
				return this;
			}

			@Override
			public PlacementValidatorBuilder withStabilityValidator(StabilityValidator stabilityValidator) {
				return this;
			}

			@Override
			public PlacementValidator build() {
				return placementValidator;
			}
		};
	}

	/** A box in the container, in its first position (rotation) */
	private static Placement place(BoxItem boxItem, int x, int y, int z) {
		return new Placement(boxItem, boxItem.getBox().getStackValue(0), 0, x, y, z);
	}

	private static Container with(Container container, Placement... placements) {
		Stack stack = new Stack();
		for (Placement placement : placements) {
			stack.add(placement);
		}
		return Container.newBuilder().withId(container.getId()).withSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz()).withMaxLoadWeight(100).withStack(stack).build();
	}

	/** A result with a single container with the placements, in the order of the stack: from the bottom up */
	private static PackagerResult layout(Placement... placements) {
		return new PackagerResult(List.of(with(item().getContainer(), placements)), 0, false);
	}

	/** A tower of boxes in the container, at x = 0, from the floor up. A box item which is listed more than once is placed that many times. */
	private static PackagerResult tower(BoxItem... boxItems) {
		Placement[] placements = new Placement[boxItems.length];
		for (int i = 0; i < boxItems.length; i++) {
			placements[i] = place(boxItems[i], 0, 0, i);
		}
		return layout(placements);
	}
}
