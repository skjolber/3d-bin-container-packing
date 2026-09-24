package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

class CrossPackagerAdapterAcceptanceTest {

	@Test
	void acceptsResultUsingTheReceiverBoxItemWithTheSameGlobalIndex() {
		BoxItem sourceItem = new BoxItem(box("source"));
		BoxItem receiverItem = new BoxItem(box("receiver"));
		TestAdapter source = new TestAdapter(sourceItem);
		TestAdapter receiver = new TestAdapter(receiverItem);

		Stack stack = new Stack();
		stack.add(new Placement(sourceItem.getBox().getStackValue(0), 0, 0, 0, 0));
		receiver.accept(new DefaultIntermediatePackagerResult(source.getContainerItem(0), stack));

		assertThat(receiver.countRemainingBoxes()).isZero();
		assertThat(receiver.getContainerItem(0).getCount()).isZero();
		assertThat(source.countRemainingBoxes()).isEqualTo(1);
	}

	private static Box box(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private static final class TestAdapter extends AbstractBoxItemAdapter {

		private TestAdapter(BoxItem item) {
			super(List.of(item), Order.CRONOLOGICAL,
					List.of(new ControlledContainerItem(Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build(), 1)),
					1, () -> false);
		}

		@Override
		public PackagerAdapter fork() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected TestAdapter fresh(List<ControlledContainerItem> containers, int containerCount) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected IntermediatePackagerResult copy(ControlledContainerItem peek, IntermediatePackagerResult result, int index) {
			throw new UnsupportedOperationException();
		}

	}
}
