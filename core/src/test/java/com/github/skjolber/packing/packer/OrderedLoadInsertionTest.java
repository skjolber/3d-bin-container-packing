package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.InsertionOrderValidator;

/**
 * With a box item order, each box is inserted after the boxes before it, also with load limits (which use the
 * load-aware placement controls). Container 2 x 1 x 2, boxes in order b, c (2 wide), d:
 *
 * <pre>
 *   [ c  ]          c can only rest on b, overhanging the empty half; d fits under the overhang,
 *   [b]             but then c rests on d, which is inserted after it
 * </pre>
 */
public class OrderedLoadInsertionTest {

	private static PackagerResult pack(Order order) {
		List<BoxItem> boxItems = new ArrayList<>();
		// a load limit, so that the load-aware placement controls are used
		boxItems.add(new BoxItem(Box.newBuilder().withId("b").withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(10).build(), 1));
		boxItems.add(new BoxItem(Box.newBuilder().withId("c").withSize(2, 1, 1).withWeight(1).build(), 1));
		boxItems.add(new BoxItem(Box.newBuilder().withId("d").withSize(1, 1, 1).withWeight(1).build(), 1));
		Container container = Container.newBuilder().withId("container").withSize(2, 1, 2).withMaxLoadWeight(100).build();
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			return packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(container, 3)))
					.withBoxItems(boxItems)
					.withOrder(order)
					.withMaxContainerCount(3)
					.build();
		}
	}

	private static void assertInsertionOrder(PackagerResult result) {
		assertThat(result.isSuccess()).isTrue();
		InsertionOrderValidator validator = new InsertionOrderValidator();
		for (Container container : result.getContainers()) {
			List<ValidatorResultReason> reasons = new ArrayList<>();
			assertThat(validator.validate(container, reasons)).as("%s", reasons).isTrue();
		}
	}

	@Test
	public void boxesAreInsertedInTheBoxItemOrder() {
		assertInsertionOrder(pack(Order.CHRONOLOGICAL));
	}

	@Test
	public void boxesAreInsertedInTheBoxItemOrderWhenSkipping() {
		assertInsertionOrder(pack(Order.CHRONOLOGICAL_ALLOW_SKIPPING));
	}
}
