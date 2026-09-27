package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

class BruteForceBoundingBoxInterruptTest {

	@Test
	void inventoryAndRotationPreparationDoNotPollInterrupts() {
		AtomicInteger checks = new AtomicInteger();
		SingleObjectiveBruteForceBoundingBoxSearch search = new SingleObjectiveBruteForceBoundingBoxSearch(
				items(), container(), new BoundingBoxObjective("volume", null, BoundingBox.MIN_VOLUME),
				() -> { checks.incrementAndGet(); return true; }, false);
		assertThat(search.prepare()).isTrue();
		search.prepareRotation();
		assertThat(checks.get()).isZero();

		// The operation entry still rejects an already interrupted request.
		BruteForceBoundingBoxResult result = search.pack(System.nanoTime());
		assertThat(checks.get()).isEqualTo(1);
		assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
		assertThat(result.getResults()).isEmpty();
	}

	@Test
	void interruptionDuringPreparationStopsBeforePlacementSearch() {
		AtomicBoolean stop = new AtomicBoolean();
		SingleObjectiveBruteForceBoundingBoxSearch search = new SingleObjectiveBruteForceBoundingBoxSearch(
				items(), container(), new BoundingBoxObjective("volume", null, BoundingBox.MIN_VOLUME), stop::get, false) {
			@Override
			protected boolean prepare() {
				boolean prepared = super.prepare();
				stop.set(true);
				return prepared;
			}
		};
		BruteForceBoundingBoxResult result = search.pack(System.nanoTime());
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
