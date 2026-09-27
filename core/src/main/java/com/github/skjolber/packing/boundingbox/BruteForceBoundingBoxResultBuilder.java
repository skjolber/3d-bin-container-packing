package com.github.skjolber.packing.boundingbox;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;

/**
 * Per-operation settings for bounding-box search. The container's
 * load dimensions and load weight are upper bounds, not the objective. All
 * supplied boxes must be placed. Input inventories and indexes are not changed.
 *
 * <p>This operation is sequential and intended for small assemblies. It searches
 * the extreme-point placement space, not arbitrary coordinates.
 * Groups, obstacles and existing placements are not supported. Per-box load
 * constraints are supported when created by {@link LoadBruteForceBoundingBox}.
 * No full-support/stability constraint is implied.</p>
 */
public class BruteForceBoundingBoxResultBuilder {

	protected final ScheduledThreadPoolExecutor scheduler;
	protected final boolean load;
	protected List<BoxItem> items;
	protected Container container;
	protected BoundingBoxComparator comparator = BoundingBox.MIN_VOLUME;
	protected Map<String, BoundingBoxObjective> additionalObjectives = new LinkedHashMap<>();
	protected Predicate<BoundingBox> goal;
	protected boolean explicitObjectives;
	protected PackagerInterruptSupplier interrupt;
	protected long deadline = -1;

	protected BruteForceBoundingBoxResultBuilder(ScheduledThreadPoolExecutor scheduler, boolean load) {
		this.scheduler = scheduler;
		this.load = load;
	}

	public BruteForceBoundingBoxResultBuilder withBoxItems(List<BoxItem> items) {
		this.items = List.copyOf(items);
		return this;
	}

	public BruteForceBoundingBoxResultBuilder withBoxItems(BoxItem... items) {
		return withBoxItems(Arrays.asList(items));
	}

	/** Supply an empty container defining the search limits. */
	public BruteForceBoundingBoxResultBuilder withContainer(Container container) {
		this.container = Objects.requireNonNull(container);
		return this;
	}

	/** Normal comparator convention: negative means the first envelope is preferred. */
	public BruteForceBoundingBoxResultBuilder withComparator(BoundingBoxComparator comparator) {
		checkImplicitObjective();
		this.comparator = Objects.requireNonNull(comparator);
		return this;
	}

	/**
	 * Register a first-class objective. The first call replaces the implicit primary
	 * objective; subsequent calls add objectives. Duplicate names replace in place.
	 * Results are returned by name; legacy primary getters refer to the first objective.
	 */
	public BruteForceBoundingBoxResultBuilder withObjective(BoundingBoxObjective objective) {
		validateObjective(objective);
		if(!explicitObjectives) {
			additionalObjectives.clear();
			explicitObjectives = true;
		}
		additionalObjectives.put(objective.name(), objective);
		return this;
	}

	public BruteForceBoundingBoxResultBuilder withObjective(String name, Predicate<BoundingBox> goal, BoundingBoxComparator comparator) {
		return withObjective(new BoundingBoxObjective(name, goal, comparator));
	}

	/** Add an independent objective with its own stopping goal. */
	public BruteForceBoundingBoxResultBuilder withAdditionalObjective(String name, Predicate<BoundingBox> goal, BoundingBoxComparator comparator) {
		BoundingBoxObjective objective = new BoundingBoxObjective(name, goal, comparator);
		validateObjective(objective);
		if(!explicitObjectives && name.equals("primary")) {
			throw new IllegalArgumentException("The name primary is reserved for the implicit objective");
		}
		additionalObjectives.put(name, objective);
		return this;
	}

	/** Add an objective which optimizes until exhaustion or interruption. */
	public BruteForceBoundingBoxResultBuilder withAdditionalObjective(String name, BoundingBoxComparator comparator) {
		return withAdditionalObjective(name, null, comparator);
	}

	/** Retain minimum width under the objective name {@code x}. */
	public BruteForceBoundingBoxResultBuilder withMinimumX() {
		return withAdditionalObjective("x", BoundingBox.MIN_X);
	}

	/** Retain minimum depth under the objective name {@code y}. */
	public BruteForceBoundingBoxResultBuilder withMinimumY() {
		return withAdditionalObjective("y", BoundingBox.MIN_Y);
	}

	/** Retain minimum height under the objective name {@code z}. */
	public BruteForceBoundingBoxResultBuilder withMinimumZ() {
		return withAdditionalObjective("z", BoundingBox.MIN_Z);
	}

	/** Retain all three independent dimension minima, in addition to the primary result. */
	public BruteForceBoundingBoxResultBuilder withMinimumDimensions() {
		return withMinimumX().withMinimumY().withMinimumZ();
	}

	/**
	 * Retain the first complete layout satisfying the implicit primary goal, even
	 * if a previous layout ranked better under the comparator. A null goal searches
	 * to exhaustion or interruption. Called only for complete layouts.
	 * Search stops early only after every objective reaches its own goal.
	 * Use withObjective instead when configuring explicitly named objectives.
	 */
	public BruteForceBoundingBoxResultBuilder withGoal(Predicate<BoundingBox> goal) {
		checkImplicitObjective();
		this.goal = goal;
		return this;
	}

	public BruteForceBoundingBoxResultBuilder withInterrupt(PackagerInterruptSupplier interrupt) {
		this.interrupt = interrupt;
		return this;
	}

	/** Epoch-millisecond deadline shared with any other work in the caller's operation. */
	public BruteForceBoundingBoxResultBuilder withInterruptDeadline(long deadline) {
		this.deadline = deadline;
		return this;
	}

	/** Set a deadline relative to this call, matching the ordinary result builder. */
	public BruteForceBoundingBoxResultBuilder withInterruptDuration(long duration) {
		if(duration < -1) {
			throw new IllegalArgumentException("Expected a non-negative duration or -1");
		}
		this.deadline = duration == -1 ? -1 : System.currentTimeMillis() + duration;
		return this;
	}

	public BruteForceBoundingBoxResult build() {
		long start = System.nanoTime();
		validate();
		PackagerInterruptSupplier operationInterrupt = PackagerInterruptSupplierBuilder.builder()
				.withScheduledThreadPoolExecutor(scheduler).withDeadline(deadline).withInterrupt(interrupt).build();
		try {
			List<BoundingBoxObjective> objectives = new java.util.ArrayList<>();
			if(!explicitObjectives) {
				objectives.add(new BoundingBoxObjective("primary", goal, comparator));
			}
			objectives.addAll(additionalObjectives.values());
			BruteForceBoundingBoxSearch search;
			if(objectives.size() == 1) {
				search = load ? new LoadBruteForceBoundingBoxSearch(items, container, objectives.get(0), operationInterrupt)
						: new SingleObjectiveBruteForceBoundingBoxSearch(items, container, objectives.get(0), operationInterrupt, false);
			} else {
				search = new MultiObjectiveBruteForceBoundingBoxSearch(items, container, objectives, operationInterrupt, load);
			}
			return search.pack(start);
		} finally {
			operationInterrupt.close();
		}
	}

	protected void checkImplicitObjective() {
		if(explicitObjectives) {
			throw new IllegalStateException("Configure the comparator and goal via withObjective for explicitly named objectives");
		}
	}

	protected void validateObjective(BoundingBoxObjective objective) {
		Objects.requireNonNull(objective);
		Objects.requireNonNull(objective.name());
		Objects.requireNonNull(objective.comparator());
		if(objective.name().isBlank()) {
			throw new IllegalArgumentException("Expected a non-blank objective name");
		}
	}

	protected void validate() {
		if(items == null || items.isEmpty() || container == null) {
			throw new IllegalStateException("Expected box items and a container");
		}
		BoundingBox.validateDimensions(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(container.getMaxLoadWeight() < 0 || !container.getStack().isEmpty()) {
			throw new IllegalArgumentException("Expected an empty container with a non-negative load weight");
		}
		long count = 0;
		for(BoxItem item : items) {
			if(item.getCount() <= 0) {
				throw new IllegalArgumentException("Expected positive box item counts");
			}
			count += item.getCount();
			Box box = item.getBox();
			if(box.getVolume() <= 0 || box.getStackValues().length == 0) {
				throw new IllegalArgumentException("Expected a positive box volume and at least one orientation");
			}
			if(box.getWeight() < 0) {
				throw new IllegalArgumentException("Expected non-negative box weights");
			}
			if(!load && (box.isMaxLoad() || box.isLoadIdenticalBoxOnly())) {
				throw new IllegalArgumentException("Bounding-box search requires non-negative weights and no per-box load constraints");
			}
			if(item.getGroup() != null) {
				throw new IllegalArgumentException("Bounding-box search does not support box-item groups");
			}
			for(BoxStackValue value : box.getStackValues()) {
				BoundingBox.validateDimensions(value.getDx(), value.getDy(), value.getDz());
				if(value.getVolume() != box.getVolume()) {
					throw new IllegalArgumentException("Expected all box orientations to have the box volume");
				}
			}
		}
		if(count >= Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Too many boxes for a permutation iterator");
		}
	}
}
