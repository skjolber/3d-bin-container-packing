package com.github.skjolber.packing.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public class BoxCountValidatorTest {

	private final BoxCountValidator validator = new BoxCountValidator();

	@Test
	void rejectsUnexpectedBoxId() {
		Box expected = box("expected");
		Box unexpected = box("unexpected");

		assertFalse(validator.validate(List.of(new BoxItem(expected)), result(container(expected, unexpected)), reasons()));
	}

	@Test
	void rejectsMissingGroup() {
		BoxItemGroup group = new BoxItemGroup("group", List.of(new BoxItem(box("expected"))));

		assertFalse(validator.validateBoxItemGroupsCounts(List.of(group), result(container()), reasons()));
	}

	@Test
	void rejectsGroupRepeatedAcrossContainers() {
		Box expected = box("expected");
		BoxItemGroup group = new BoxItemGroup("group", List.of(new BoxItem(expected)));

		assertFalse(validator.validateBoxItemGroupsCounts(List.of(group), result(container(expected), container(expected)), reasons()));
	}

	private List<ValidatorResultReason> reasons() {
		return new ArrayList<>();
	}

	private PackagerResult result(Container... containers) {
		return new PackagerResult(List.of(containers), 0L, false);
	}

	private Box box(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private Container container(Box... boxes) {
		Stack stack = new Stack();
		for (int i = 0; i < boxes.length; i++) {
			stack.add(new Placement(boxes[i].getStackValue(0), 0, i, 0, 0));
		}
		return Container.newBuilder().withId("container").withSize(Math.max(1, boxes.length), 1, 1).withMaxLoadWeight(100).withStack(stack).build();
	}
}
