package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;

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

	@Test
	void freshAndForkKeepSessionStateIndependent() {
		BoxItem item = new BoxItem(box("item"));
		TestAdapter adapter = new TestAdapter(item);
		TestAdapter fresh = (TestAdapter) adapter.fresh();
		TestAdapter fork = (TestAdapter) adapter.fork();

		Stack stack = new Stack();
		stack.add(new Placement(item.getBox().getStackValue(0), 0, 0, 0, 0));
		fresh.accept(new DefaultIntermediatePackagerResult(fresh.getContainerItem(0), stack));
		fork.accept(new DefaultIntermediatePackagerResult(fork.getContainerItem(0), stack));

		assertThat(fresh.countRemainingBoxes()).isZero();
		assertThat(fork.countRemainingBoxes()).isZero();
		assertThat(adapter.countRemainingBoxes()).isEqualTo(1);

		PackagerSession freshOfFresh = fresh.fresh();
		assertThat(freshOfFresh.countRemainingBoxes()).isEqualTo(1);
		assertThat(freshOfFresh.getContainerItem(0).getCount()).isEqualTo(1);

		PackagerSession freshOfFork = fork.fresh();
		assertThat(freshOfFork.countRemainingBoxes()).isEqualTo(1);
		assertThat(freshOfFork.getContainerItem(0).getCount()).isEqualTo(1);
		assertThat(fork.countRemainingBoxes()).isZero();
	}

	@Test
	void rejectsDuplicateGlobalBoxItemIndexes() {
		BoxItem first = new BoxItem(box("first"));
		BoxItem second = new BoxItem(box("second"));
		first.setGlobalIndex(2);
		second.setGlobalIndex(2);

		assertThatThrownBy(() -> AbstractPackagerAdapter.initializeGlobalIndexes(List.of(first, second)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Duplicate box item global index 2");
	}

	private static Box box(String id) {
		return Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build();
	}

	private static final class TestAdapter extends AbstractBoxItemAdapter {

		private TestAdapter(BoxItem item) {
			this(item, List.of(new ContainerItem(Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).build(), 1)), 1);
		}

		private TestAdapter(BoxItem item, List<ContainerItem> containers, int containerCount) {
			super(List.of(item), Order.CRONOLOGICAL, containers, containerCount, () -> false);
		}

		private TestAdapter(TestAdapter source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new TestAdapter(this);
		}

		@Override
		protected TestAdapter fresh(List<ContainerItem> containers, int containerCount) {
			return new TestAdapter(copyBoxItems(initialBoxItems).get(0), containers, containerCount);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected IntermediatePackagerResult copy(ContainerItem peek, IntermediatePackagerResult result, int index) {
			throw new UnsupportedOperationException();
		}

	}
}
