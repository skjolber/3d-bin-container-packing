package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;

/**
 * Per-operation virtual-box settings. Identical boxes form direct rectangular grids,
 * split into container-sized blocks when the whole count does not fit. Equal blocks
 * are handed to the delegate as one counted item. Selective refinement splits grids
 * along an axis and reuses operation-local layouts without permutation searches.
 * Ungrouped fallback is attempted when aggregation and refinement fail.
 */
public class VirtualBoxPackagerResultBuilder extends AbstractPackagerResultBuilder<VirtualBoxPackagerResultBuilder> {
	protected final Packager<? extends PackagerResultBuilder> delegate;
	protected final ScheduledThreadPoolExecutor scheduler;
	protected int maxLayouts = 8;
	protected int maxGridBoxes = 10_000;
	protected boolean aggregation = true;
	protected boolean compareUngrouped;
	protected int maxRefinements = 4;
	protected int maxDelegateBoxes = Integer.MAX_VALUE;

	protected VirtualBoxPackagerResultBuilder(Packager<? extends PackagerResultBuilder> delegate, ScheduledThreadPoolExecutor scheduler) {
		this.delegate = delegate;
		this.scheduler = scheduler;
	}

	/**
	 * Bound the number of alternative layouts (delegate stack values) per virtual box.
	 * Each container type which can hold the assembly keeps one layout of its own, even
	 * if there are more such container types than this limit.
	 */
	public VirtualBoxPackagerResultBuilder withMaxLayouts(int count) {
		maxLayouts = positive(count);
		return this;
	}

	/**
	 * Bound the physical size of a directly constructed grid. Larger items, and items
	 * whose whole count does not fit a container, are split into container-sized grids.
	 */
	public VirtualBoxPackagerResultBuilder withMaxGridBoxes(int count) {
		maxGridBoxes = positive(count);
		return this;
	}

	/**
	 * Enable filled-envelope preprocessing (default true). Disable when custom
	 * delegate controls require original physical boxes or child surfaces. Disabled
	 * operations are forwarded unchanged, without layout preparation, refinement or
	 * expansion; preprocessing limits such as maxDelegateBoxes do not apply.
	 */
	public VirtualBoxPackagerResultBuilder withAggregation(boolean enabled) {
		aggregation = enabled;
		return this;
	}

	/**
	 * Also run ungrouped packing after a successful aggregated attempt and retain
	 * the lower cost (if available), then fewer containers, then less container volume.
	 * Disabled by default to avoid doing every successful packing twice.
	 */
	public VirtualBoxPackagerResultBuilder withCompareUngrouped(boolean enabled) {
		compareUngrouped = enabled;
		return this;
	}

	/** Maximum selective splits after coarse packing; zero disables refinement. */
	public VirtualBoxPackagerResultBuilder withMaxRefinements(int count) {
		if(count < 0) {
			throw new IllegalArgumentException("Expected a non-negative refinement limit");
		}
		maxRefinements = count;
		return this;
	}

	/** Limit physical delegate items across coarse, refined and ungrouped attempts. */
	public VirtualBoxPackagerResultBuilder withMaxDelegateBoxes(int count) {
		maxDelegateBoxes = positive(count);
		return this;
	}

	protected static int positive(int value) {
		if(value <= 0) {
			throw new IllegalArgumentException("Expected a positive limit");
		}
		return value;
	}

	@Override
	public PackagerResult build() {
		validate();
		long start = System.nanoTime();
		// One deadline supplier spans preprocessing, refinement and all
		// delegate attempts. Borrowed views prevent nested builders from closing it.
		try(PackagerInterruptSupplier stop = PackagerInterruptSupplierBuilder.builder()
				.withScheduledThreadPoolExecutor(scheduler).withDeadline(deadline)
				.withInterrupt(interrupt == null ? null : interrupt::getAsBoolean).build()) {
			if(stop.getAsBoolean()) {
				return new PackagerResult(List.of(), VirtualBoxPacking.elapsed(start), true);
			}
			if(!supportsAggregation()) {
				// Forward unsupported features unchanged; never silently discard their semantics.
				return configured(stop).withBoxItems(items).withBoxItemGroups(itemGroups).build();
			}
			VirtualBoxPlan plan = prepare(stop);
			if(stop.getAsBoolean()) {
				return new PackagerResult(List.of(), VirtualBoxPacking.elapsed(start), true);
			}
			VirtualBoxPacking grouped = plan.packing();
			PackagerResult best = new PackagerResult(List.of(), VirtualBoxPacking.elapsed(start), false);
			if(plan.delegateCount() <= maxDelegateBoxes) {
				best = attempt(grouped, stop, start);
			}
			if(!grouped.hasVirtualBoxes() && plan.delegateCount() <= maxDelegateBoxes) {
				return best;
			}
			// Refinement is useful after failure or when container count/cost may improve.
			long finalContainerCount = getFinalContainerCount();
			for(int i = 0; i < maxRefinements && shouldRefine(best, finalContainerCount) && !stop.getAsBoolean(); i++) {
				if(!plan.refine(stop)) {
					break;
				}
				PackagerResult candidate = attempt(plan.packing(), stop, start);
				if(!best.isSuccess() || better(candidate, best)) {
					best = candidate;
				}
			}
			if((!best.isSuccess() || compareUngrouped) && !stop.getAsBoolean()) {
				long physicalCount = 0;
				for(BoxItem item : items) {
					physicalCount += item.getCount();
				}
				if(physicalCount <= maxDelegateBoxes && plan.delegateCount() < physicalCount) {
					VirtualBoxPacking plain = new VirtualBoxPacking();
					for(BoxItem item : items) {
						plain.add(item);
					}
					PackagerResult fallback = attempt(plain, stop, start);
					if(!best.isSuccess() || better(fallback, best)) {
						best = fallback;
					}
				}
			}
			return new PackagerResult(best.getContainers(), VirtualBoxPacking.elapsed(start), best.isTimeout() || stop.getAsBoolean(), best.getCost(), best.isInsertionOrder());
		} finally {
			if(interrupt != null) {
				interrupt.close();
			}
		}
	}

	protected PackagerResult attempt(VirtualBoxPacking packing, PackagerInterruptSupplier stop, long start) {
		// Filled envelopes are ordinary delegate items; equal envelopes share one counted item. Keep expansion
		// outside the delegate's search/point loops, including the no-load path.
		// the expanded result is put in insertion order, so the delegate's result need not be
		PackagerResult result = configured(stop).withInsertionOrder(false).withBoxItems(packing.getItems()).build();
		return packing.expand(result, items, start, insertionOrder);
	}

	protected PackagerResultBuilder configured(PackagerInterruptSupplier stop) {
		List<ContainerItem> copies = new ArrayList<>();
		for(ContainerItem item : containers) {
			copies.add(new ContainerItem(item));
		}
		return delegate.newResultBuilder().withContainerItems(copies).withMaxContainerCount(maxContainerCount)
				.withOrder(order).withInsertionOrder(insertionOrder).withInterruptDeadline(-1).withInterrupt(stop::getAsBoolean);
	}

	protected boolean supportsAggregation() {
		if(!aggregation || !itemGroups.isEmpty() || order != Order.NONE) {
			return false;
		}
		Map<Box, Boolean> distinct = new IdentityHashMap<>();
		for(BoxItem item : items) {
			if(item.getCount() <= 0 || item.getBox().getWeight() < 0 || item.getBox().getVolume() <= 0) {
				throw new IllegalArgumentException("Expected positive inventory and non-negative weights");
			}
			// An envelope cannot represent physical support areas, depth or identity.
			// Bypass the whole operation, including unconstrained items that might
			// otherwise be aggregated and placed on top of a constrained original.
			if(item.getBox().isMaxLoad() || item.getBox().isLoadIdenticalBoxOnly() || item.getGroup() != null
					|| item.getContainerPriority() != 0 || item.getExtractionOrder() != 0
					|| distinct.put(item.getBox(), Boolean.TRUE) != null) {
				return false;
			}
		}
		for(ContainerItem item : containers) {
			Container container = item.getContainer();
			if(item.hasControls() || item.hasInitialPoints() || !container.getStack().isEmpty() || container.getMotion() != null) {
				return false;
			}
		}
		return true;
	}

	protected VirtualBoxPlan prepare(PackagerInterruptSupplier stop) {
		List<ContainerItem> available = new ArrayList<>();
		for(ContainerItem item : containers) {
			if(item.getCount() > 0) {
				available.add(item);
			}
		}
		GridVirtualBoxLayoutGenerator grids = new GridVirtualBoxLayoutGenerator(getMinimumDimension());
		VirtualBoxLayoutCache cache = new VirtualBoxLayoutCache(items, available, maxContainerCount, grids, maxGridBoxes, maxLayouts);
		VirtualBoxPlan plan = new VirtualBoxPlan(items, cache, maxDelegateBoxes);
		for(int i = 0; i < items.size(); i++) {
			if(stop.getAsBoolean()) {
				break;
			}
			plan.add(i, stop);
		}
		return plan;
	}

	/** Leftover space shorter than every box dimension cannot be used by any item. */
	protected int getMinimumDimension() {
		int minimum = Integer.MAX_VALUE;
		for(BoxItem item : items) {
			for(BoxStackValue value : item.getBox().getStackValues()) {
				minimum = Math.min(minimum, Math.min(value.getDx(), Math.min(value.getDy(), value.getDz())));
			}
		}
		return minimum == Integer.MAX_VALUE ? 1 : minimum;
	}

	/**
	 * @param finalContainerCount container count which no refinement can improve, or -1 if unknown
	 */
	protected boolean shouldRefine(PackagerResult best, long finalContainerCount) {
		if(!best.isSuccess()) {
			return true;
		}
		if(best.size() > 1) {
			return best.size() != finalContainerCount;
		}
		if(best.getCost() >= 0) {
			for(ContainerItem item : containers) {
				if(item.hasCostCalculator() && item.getCostCalculator().getMinimumCost() < best.getCost()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Container count at which refinement cannot improve a result, or -1 if there is none.
	 * A result is compared by cost, then container count, then total container volume.
	 * Without costs, and with equal container volumes, a result using the volume and weight
	 * lower bound of containers cannot be improved.
	 */
	protected long getFinalContainerCount() {
		long maxVolume = 0;
		long maxWeight = 0;
		long containerVolume = -1;
		for(ContainerItem item : containers) {
			if(item.hasCostCalculator()) {
				return -1;
			}
			if(item.getCount() > 0) {
				Container container = item.getContainer();
				if(containerVolume != -1 && containerVolume != container.getVolume()) {
					return -1;
				}
				containerVolume = container.getVolume();
				maxVolume = Math.max(maxVolume, container.getMaxLoadVolume());
				maxWeight = Math.max(maxWeight, container.getMaxLoadWeight());
			}
		}
		long volume = 0;
		long weight = 0;
		for(BoxItem item : items) {
			volume += item.getBox().getVolume() * item.getCount();
			weight += (long) item.getBox().getWeight() * item.getCount();
		}
		long count = maxVolume <= 0 ? 1 : (volume + maxVolume - 1) / maxVolume;
		if(maxWeight > 0) {
			count = Math.max(count, (weight + maxWeight - 1) / maxWeight);
		}
		return count;
	}

	protected static boolean better(PackagerResult candidate, PackagerResult best) {
		if(!candidate.isSuccess()) {
			return false;
		}
		if(candidate.getCost() >= 0 && best.getCost() >= 0 && candidate.getCost() != best.getCost()) {
			return candidate.getCost() < best.getCost();
		}
		if(candidate.size() != best.size()) {
			return candidate.size() < best.size();
		}
		long a = 0, b = 0;
		for(Container container : candidate.getContainers()) {
			a += container.getVolume();
		}
		for(Container container : best.getContainers()) {
			b += container.getVolume();
		}
		return a < b;
	}
}
