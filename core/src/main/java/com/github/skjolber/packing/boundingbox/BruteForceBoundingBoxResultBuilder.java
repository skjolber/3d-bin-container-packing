package com.github.skjolber.packing.boundingbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Objectives are registered by name and have equal standing. If none are registered,
 * a minimum-volume objective named {@code default} is used for that build only.
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
	protected List<BoundingBoxObjective> objectives;
	protected PackagerInterruptSupplier interrupt;
	protected long deadline = -1;

	protected BruteForceBoundingBoxResultBuilder(ScheduledThreadPoolExecutor scheduler, boolean load) {
		this.scheduler = scheduler;
		this.load = load;
	}

	/** Retain the input list directly. Do not modify it or its items while this builder/search is in use. */
	public BruteForceBoundingBoxResultBuilder withBoxItems(List<BoxItem> items) {
		this.items = Objects.requireNonNull(items);
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

	/**
	 * Configure the comparator of the objective named {@code default}, preserving its goal.
	 * Adds that objective if absent. Negative means the first envelope is preferred.
	 */
	public BruteForceBoundingBoxResultBuilder withComparator(BoundingBoxComparator comparator) {
		BoundingBoxObjective objective = findObjective("default");
		return withObjective("default", objective == null ? null : objective.goal(), comparator);
	}

	/**
	 * Register an objective, replacing an existing objective of the same name in place.
	 * Other objectives are unchanged. All objectives have equal standing.
	 * The objective is retained directly and must not be modified after registration.
	 */
	public BruteForceBoundingBoxResultBuilder withObjective(BoundingBoxObjective objective) {
		validateObjective(objective);
		registerObjective(objective);
		return this;
	}

	public BruteForceBoundingBoxResultBuilder withObjective(String name, Predicate<BoundingBox> goal, BoundingBoxComparator comparator) {
		return withObjective(new BoundingBoxObjective(name, goal, comparator));
	}

	/** Add an objective which optimizes until exhaustion or interruption. */
	public BruteForceBoundingBoxResultBuilder withObjective(String name, BoundingBoxComparator comparator) {
		return withObjective(name, null, comparator);
	}

	/** Retain minimum width under the objective name {@code x}. */
	public BruteForceBoundingBoxResultBuilder withMinimumX() {
		return withObjective("x", BoundingBox.MIN_X);
	}

	/** Retain minimum depth under the objective name {@code y}. */
	public BruteForceBoundingBoxResultBuilder withMinimumY() {
		return withObjective("y", BoundingBox.MIN_Y);
	}

	/** Retain minimum height under the objective name {@code z}. */
	public BruteForceBoundingBoxResultBuilder withMinimumZ() {
		return withObjective("z", BoundingBox.MIN_Z);
	}

	/** Register the three independent dimension objectives named x, y and z. */
	public BruteForceBoundingBoxResultBuilder withMinimumDimensions() {
		return withMinimumX().withMinimumY().withMinimumZ();
	}

	/**
	 * Configure the goal of the objective named {@code default}, preserving its comparator.
	 * Adds that objective with minimum-volume ordering if absent. A goal accepts the first
	 * matching complete layout, even if a previous layout ranked better. A null goal
	 * optimizes until exhaustion or interruption. Search stops early only when all goals are met.
	 */
	public BruteForceBoundingBoxResultBuilder withGoal(Predicate<BoundingBox> goal) {
		BoundingBoxObjective objective = findObjective("default");
		return withObjective("default", goal, objective == null ? BoundingBox.MIN_VOLUME : objective.comparator());
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
			BoundingBoxObjective single = null;
			if(objectives == null || objectives.isEmpty()) {
				// A fallback only for an unconfigured builder; it is not inserted alongside named objectives.
				single = new BoundingBoxObjective("default", null, BoundingBox.MIN_VOLUME);
			} else if(objectives.size() == 1) {
				single = objectives.get(0);
			}
			BruteForceBoundingBoxSearch search;
			if(single != null) {
				search = load ? new LoadBruteForceBoundingBoxSearch(items, container, single, operationInterrupt)
						: new SingleObjectiveBruteForceBoundingBoxSearch(items, container, single, operationInterrupt, false);
			} else {
				search = new MultiObjectiveBruteForceBoundingBoxSearch(items, container, objectives.toArray(BoundingBoxObjective[]::new), operationInterrupt, load);
			}
			return search.pack(start);
		} finally {
			operationInterrupt.close();
		}
	}

	/** Small objective lists do not need a second, name-indexed collection. */
	protected void registerObjective(BoundingBoxObjective objective) {
		if(objectives == null) {
			objectives = new ArrayList<>(4);
		} else {
			for(int i = 0; i < objectives.size(); i++) {
				if(objectives.get(i).name().equals(objective.name())) {
					objectives.set(i, objective);
					return;
				}
			}
		}
		objectives.add(objective);
	}

	protected BoundingBoxObjective findObjective(String name) {
		if(objectives != null) {
			for(BoundingBoxObjective objective : objectives) {
				if(objective.name().equals(name)) {
					return objective;
				}
			}
		}
		return null;
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
