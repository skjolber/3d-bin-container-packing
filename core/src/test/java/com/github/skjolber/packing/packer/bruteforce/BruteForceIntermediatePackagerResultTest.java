package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;

class BruteForceIntermediatePackagerResultTest {

	@Test
	void rebuildsLoadsIndependentOfPlacementOrder() {
		Placement bottom = placement("bottom", 2, 0);
		Placement middle = placement("middle", 3, 1);
		Placement top = placement("top", 5, 2);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(null, null, 0, null);

		result.rebuildLoads(List.of(middle, top, bottom));

		assertThat(top.getLoadWeight()).isZero();
		assertThat(middle.getLoadWeight()).isEqualTo(5.0);
		assertThat(bottom.getLoadWeight()).isEqualTo(8.0);
		assertThat(middle.getSupporters()).singleElement()
				.extracting(load -> load.getPlacement()).isSameAs(bottom);
	}

	private static Placement placement(String id, int weight, int z) {
		Box box = Box.newBuilder().withId(id).withSize(10, 10, 1).withWeight(weight).build();
		new BoxItem(box);
		return new Placement(box.getStackValues()[0], 0, 0, 0, z);
	}
}
