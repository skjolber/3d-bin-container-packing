package com.github.skjolber.packing.virtualbox.bounds;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.virtualbox.bounds.SingleObjectiveBruteForceVirtualBoxBoundsSearch;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsObjective;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult.Termination;

class BruteForceVirtualBoxBoundsInterruptTest {

	@Test
	void inventoryAndRotationPreparationDoNotPollInterrupts() {
		AtomicInteger checks = new AtomicInteger();
		SingleObjectiveBruteForceVirtualBoxBoundsSearch search = new SingleObjectiveBruteForceVirtualBoxBoundsSearch(
				items(), container(), new VirtualBoxBoundsObjective("volume", null, VirtualBoxBounds.MIN_VOLUME),
				() -> { checks.incrementAndGet(); return true; }, false);
		assertThat(search.prepare()).isTrue();
		search.prepareRotation();
		assertThat(checks.get()).isZero();

		// The operation entry still rejects an already interrupted request.
		VirtualBoxBoundsResult result = search.pack(System.nanoTime());
		assertThat(checks.get()).isEqualTo(1);
		assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
		assertThat(result.getResults()).isEmpty();
	}

	@Test
	void interruptionDuringPreparationStopsBeforePlacementSearch() {
		AtomicBoolean stop = new AtomicBoolean();
		SingleObjectiveBruteForceVirtualBoxBoundsSearch search = new SingleObjectiveBruteForceVirtualBoxBoundsSearch(
				items(), container(), new VirtualBoxBoundsObjective("volume", null, VirtualBoxBounds.MIN_VOLUME), stop::get, false) {
			@Override
			protected boolean prepare() {
				boolean prepared = super.prepare();
				stop.set(true);
				return prepared;
			}
		};
		VirtualBoxBoundsResult result = search.pack(System.nanoTime());
		assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
		assertThat(result.getResults()).isEmpty();
	}

	protected List<BoxItem> items() {
		return List.of(
				new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).build(), 2),
				new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build(), 2));
	}

	protected Container container() {
		return Container.newBuilder().withSize(3, 3, 3).withEmptyWeight(0).withMaxLoadWeight(10).build();
	}
}
