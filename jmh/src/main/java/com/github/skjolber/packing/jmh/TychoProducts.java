package com.github.skjolber.packing.jmh;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

/** Tycho product sets and container, shared by benchmarks in other packages. */
public class TychoProducts {

	private TychoProducts() {
	}

	/** @param boxes product set: "22", "33" or "93" */
	public static List<BoxItem> getProducts(String boxes) {
		return TychoBenchmark.getProducts(boxes);
	}

	/** The container used by {@link TychoPackagerState}. */
	public static List<ContainerItem> getContainers() {
		return ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("1")
						.withEmptyWeight(1)
						.withSize(1500, 1900, 4000)
						.withMaxLoadWeight(100)
						.build())
				.build();
	}
}
