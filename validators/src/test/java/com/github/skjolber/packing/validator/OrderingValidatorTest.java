package com.github.skjolber.packing.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BlockedExtractionReason;
import com.github.skjolber.packing.validator.reasons.ContainerPriorityReason;

/**
 * Container priority and extraction order validation.
 */
public class OrderingValidatorTest {

	private static Placement place(String id, int x, int z, int containerPriority, int extractionOrder) {
		Box box = Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
		new BoxItem(box).withContainerPriority(containerPriority).withExtractionOrder(extractionOrder);
		return new Placement(box.getStackValue(0), 0, x, 0, z);
	}

	private static Container container(ContainerAccess access, Placement... placements) {
		Stack stack = new Stack();
		for (Placement placement : placements) {
			stack.add(placement);
		}
		return Container.newBuilder().withId("c").withSize(3, 1, 3).withMaxLoadWeight(100).withAccess(access).withStack(stack).build();
	}

	@Test
	void boxesAreInContainersInOrderOfPriority() {
		ContainerPriorityValidator validator = new ContainerPriorityValidator();

		// priorities 0 and 1 in the first container, 1 and 2 in the second
		Container first = container(ContainerAccess.ANY, place("a", 0, 0, 0, 0), place("b", 1, 0, 1, 0));
		Container second = container(ContainerAccess.ANY, place("c", 0, 0, 1, 0), place("d", 1, 0, 2, 0));
		assertThat(validator.validate(List.of(first, second), new ArrayList<>())).isTrue();

		// priority 0 in the second container, after priority 1 in the first
		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(validator.validate(List.of(second, first), reasons)).isFalse();
		assertThat(reasons).allMatch(reason -> reason instanceof ContainerPriorityReason);
		assertThat(((ContainerPriorityReason)reasons.get(0)).getContainer()).isEqualTo(1);
	}

	//
	//  z
	//  1 +---+
	//    | b |   b is extracted after a, but rests on it
	//  0 +---+
	//    | a |
	//    0   1  x
	//
	@Test
	void boxIsNotExtractedFirstWhenALaterBoxRestsOnIt() {
		ExtractionOrderValidator validator = new ExtractionOrderValidator();

		Placement a = place("a", 0, 0, 0, 1);
		Placement b = place("b", 0, 1, 0, 2);
		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(validator.validate(container(ContainerAccess.ANY, a, b), reasons)).isFalse();
		assertThat(reasons).singleElement().isInstanceOf(BlockedExtractionReason.class);
		assertThat(((BlockedExtractionReason)reasons.get(0)).getPlacement()).isSameAs(a);

		// b is extracted first: fine
		Placement c = place("c", 0, 0, 0, 2);
		Placement d = place("d", 0, 1, 0, 1);
		assertThat(validator.validate(container(ContainerAccess.ANY, c, d), new ArrayList<>())).isTrue();
	}

	//
	//  side view, door at x = 2
	//
	//  0 +---+---+
	//    | a | b |   door ->   a is extracted first, but b is in its path
	//    0   1   2  x
	//
	@Test
	void boxIsNotExtractedFirstWhenALaterBoxIsInItsPath() {
		ExtractionOrderValidator validator = new ExtractionOrderValidator();

		Placement a = place("a", 0, 0, 0, 1);
		Placement b = place("b", 1, 0, 0, 2);
		assertThat(validator.validate(container(ContainerAccess.FRONT, a, b), new ArrayList<>())).isFalse();
		// from above, side by side boxes do not block each other
		assertThat(validator.validate(container(ContainerAccess.TOP, a, b), new ArrayList<>())).isTrue();
	}
}
