package com.github.skjolber.packing.virtualbox.bounds;

import java.util.List;
import java.util.function.Predicate;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/** Single-objective specialization with complete-layout load validation. */
public class LoadBruteForceVirtualBoxBoundsSearch extends SingleObjectiveBruteForceVirtualBoxBoundsSearch {
	protected LoadBruteForceVirtualBoxBoundsSearch(List<BoxItem> items, Container container, VirtualBoxBoundsComparator comparator,
			Predicate<VirtualBoxBounds> goal, PackagerInterruptSupplier interrupt) {
		this(items, container, new VirtualBoxBoundsObjective("default", goal, comparator), interrupt);
	}

	protected LoadBruteForceVirtualBoxBoundsSearch(List<BoxItem> items, Container container, VirtualBoxBoundsObjective objective, PackagerInterruptSupplier interrupt) {
		super(items, container, objective, interrupt, true);
	}
}
