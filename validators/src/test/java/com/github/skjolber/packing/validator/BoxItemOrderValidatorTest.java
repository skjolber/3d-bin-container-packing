package com.github.skjolber.packing.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

public class BoxItemOrderValidatorTest {

	private final BoxItemOrderValidator validator = new BoxItemOrderValidator();

	@Test
	void rejectsSkippedItemsInReverseOrder() {
		BoxItem first = item("first");
		BoxItem second = item("second");
		Container container = container(second.getBox(), first.getBox());

		assertFalse(validator.validate(List.of(first, second), Order.CRONOLOGICAL_ALLOW_SKIPPING, List.of(container)));
	}

	@Test
	void rejectsSkippedGroupsInReverseOrder() {
		BoxItem first = item("first");
		BoxItem second = item("second");
		BoxItemGroup firstGroup = new BoxItemGroup("first-group", List.of(first));
		BoxItemGroup secondGroup = new BoxItemGroup("second-group", List.of(second));
		PackagerResult result = new PackagerResult(List.of(container(second.getBox(), first.getBox())), 0L, false);

		assertFalse(validator.validate(List.of(firstGroup, secondGroup), Order.CRONOLOGICAL_ALLOW_SKIPPING, result));
	}

	private BoxItem item(String id) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build());
	}

	private Container container(Box... boxes) {
		Stack stack = new Stack();
		for (int i = 0; i < boxes.length; i++) {
			stack.add(new Placement(boxes[i].getStackValue(0), 0, i, 0, 0));
		}
		return Container.newBuilder().withId("container").withSize(boxes.length, 1, 1).withMaxLoadWeight(100).withStack(stack).build();
	}
}
