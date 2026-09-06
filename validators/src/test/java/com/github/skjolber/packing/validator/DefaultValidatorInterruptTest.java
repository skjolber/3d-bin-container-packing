package com.github.skjolber.packing.validator;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

public class DefaultValidatorInterruptTest {

	private final DefaultValidator validator = new DefaultValidator();

	@AfterEach
	void closeValidator() throws IOException {
		validator.close();
	}

	@Test
	void honorsPastDeadline() {
		ValidatorResult result = resultBuilder().withDeadline(System.currentTimeMillis() - 1L).build();

		assertTrue(result.isTimeout());
	}

	@Test
	void acceptsFutureDeadlineWithoutThrowing() {
		ValidatorResult result = resultBuilder().withDeadline(System.currentTimeMillis() + 60_000L).build();

		assertTrue(result.isValid());
	}

	@Test
	void honorsCustomInterrupt() {
		ValidatorResult result = resultBuilder().withInterrupt(() -> true).build();

		assertTrue(result.isTimeout());
	}

	private DefaultValidator.DefaultValidatorResultBuilder resultBuilder() {
		Box box = Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build();
		Stack stack = new Stack();
		stack.add(new Placement(box.getStackValue(0), 0, 0, 0, 0));
		Container container = Container.newBuilder().withId("container").withSize(1, 1, 1).withMaxLoadWeight(10).withStack(stack).build();

		return validator.newResultBuilder()
				.withContainerItems(List.of(new ContainerItem(container, 1)))
				.withMaxContainerCount(1)
				.withPackagerResult(new PackagerResult(List.of(container), 0L, false))
				.withBoxItems(List.of(new BoxItem(box)));
	}
}
