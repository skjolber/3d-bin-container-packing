package com.github.skjolber.packing.test.assertj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.validator.DefaultValidator;

/**
 * Assertions which validate packager results, tested here because the {@code validators} module provides a validator.
 */
public class PackagerResultAssertTest {

	private static class FirstReason implements ValidatorResultReason {
		@Override
		public int getCode() {
			return 1;
		}

		@Override
		public String getMessage() {
			return "first";
		}
	}

	private static class SecondReason extends FirstReason {
	}

	private final DefaultValidator validator = new DefaultValidator();

	private final BoxItem a = new BoxItem(Box.newBuilder().withId("A").withSize(1, 1, 1).withWeight(1).build());
	private final BoxItem b = new BoxItem(Box.newBuilder().withId("B").withSize(1, 1, 1).withWeight(1).build());

	@AfterEach
	void closeValidator() throws IOException {
		validator.close();
	}

	//
	// +---+---+
	// | A | B |
	// +---+---+
	//
	private PackagerResult sideBySide(int xOfB) {
		Stack stack = new Stack();
		stack.add(new Placement(a, a.getBox().getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(b, b.getBox().getStackValue(0), 0, xOfB, 0, 0));
		Container container = Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(10).withStack(stack).build();
		return new PackagerResult(List.of(container), 0L, false, -1);
	}

	private List<ContainerItem> trusted() {
		Container container = Container.newBuilder().withId("container").withSize(2, 1, 1).withMaxLoadWeight(10).withStack(new Stack()).build();
		return List.of(new ContainerItem(container, 1));
	}

	@Test
	void acceptsValidResult() {
		PackagerResultAssert.assertThat(sideBySide(1))
				.isStackedWithinConstraints()
				.placesExactly(List.of(a, b))
				.isAcceptedBy(validator.newResultBuilder()
						.withContainerItems(trusted())
						.withMaxContainerCount(1)
						.withBoxItems(List.of(a, b)))
				.isAcceptedBy((LoadValidator)(placements, reasons) -> true);
	}

	//
	// +---+
	// |A/B|  (same position)
	// +---+
	//
	@Test
	void rejectsIntersectingPlacements() {
		PackagerResult result = sideBySide(0);

		assertThatThrownBy(() -> PackagerResultAssert.assertThat(result).isStackedWithinConstraints()).isInstanceOf(AssertionError.class).hasMessageContaining("intersects");
		assertThatThrownBy(() -> PackagerResultAssert.assertThat(result).isAcceptedBy(validator.newResultBuilder()
				.withContainerItems(trusted())
				.withMaxContainerCount(1)
				.withBoxItems(List.of(a, b)))).isInstanceOf(AssertionError.class).hasMessageContaining("reasons");

		PackagerResultAssert.assertThat(result).isRejectedBy(validator.newResultBuilder()
				.withContainerItems(trusted())
				.withMaxContainerCount(1)
				.withBoxItems(List.of(a, b)));
	}

	@Test
	void rejectsValidResultAsInvalid() {
		assertThatThrownBy(() -> PackagerResultAssert.assertThat(sideBySide(1)).isRejectedBy(validator.newResultBuilder()
				.withContainerItems(trusted())
				.withMaxContainerCount(1)
				.withBoxItems(List.of(a, b)))).isInstanceOf(AssertionError.class);
	}

	@Test
	void requiresAContainer() {
		PackagerResult empty = new PackagerResult(List.of(), 0L, false, -1);

		assertThatThrownBy(() -> PackagerResultAssert.assertThat(empty).isStackedWithinConstraints()).isInstanceOf(AssertionError.class);
	}

	@Test
	void reportsLoadValidatorRejection() {
		LoadValidator rejecting = (placements, reasons) -> {
			reasons.add(new FirstReason());
			return false;
		};
		assertThatThrownBy(() -> PackagerResultAssert.assertThat(sideBySide(1)).isAcceptedBy(rejecting)).isInstanceOf(AssertionError.class).hasMessageContaining("container 0");
	}

	@Test
	void countsPlacementsPerBoxItem() {
		BoxItem twoOfA = new BoxItem(Box.newBuilder().withId("A").withSize(1, 1, 1).withWeight(1).build(), 2);

		assertThatThrownBy(() -> PackagerResultAssert.assertThat(sideBySide(1)).placesExactly(List.of(a))).isInstanceOf(AssertionError.class).hasMessageContaining("only the given box items");
		assertThatThrownBy(() -> PackagerResultAssert.assertThat(sideBySide(1)).placesExactly(List.of(a, b, twoOfA))).isInstanceOf(AssertionError.class).hasMessageContaining("placement(s)");
	}

	@Test
	void comparesReasonTypesInOrder() {
		ValidatorResult result = new ValidatorResult(0, false, false, List.of(new FirstReason(), new SecondReason()));

		ValidatorResultAssert.assertThat(result).isNotValid().hasReasons(FirstReason.class, SecondReason.class);
		assertThatThrownBy(() -> ValidatorResultAssert.assertThat(result).hasReasons(SecondReason.class, FirstReason.class)).isInstanceOf(AssertionError.class);
		assertThatThrownBy(() -> ValidatorResultAssert.assertThat(result).hasReasons(FirstReason.class)).isInstanceOf(AssertionError.class);
		assertThatThrownBy(() -> ValidatorResultAssert.assertThat(result).isValid()).isInstanceOf(AssertionError.class).hasMessageContaining("first");
	}
}
