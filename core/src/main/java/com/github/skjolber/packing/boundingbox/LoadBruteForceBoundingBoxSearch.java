package com.github.skjolber.packing.boundingbox;

import java.util.List;
import java.util.function.Predicate;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/** Single-objective specialization with complete-layout load validation. */
public class LoadBruteForceBoundingBoxSearch extends SingleObjectiveBruteForceBoundingBoxSearch {
	protected LoadBruteForceBoundingBoxSearch(List<BoxItem> items, Container container, BoundingBoxComparator comparator,
			Predicate<BoundingBox> goal, PackagerInterruptSupplier interrupt) {
		this(items, container, new BoundingBoxObjective("primary", goal, comparator), interrupt);
	}

	protected LoadBruteForceBoundingBoxSearch(List<BoxItem> items, Container container, BoundingBoxObjective objective, PackagerInterruptSupplier interrupt) {
		super(items, container, objective, interrupt, true);
	}
}
