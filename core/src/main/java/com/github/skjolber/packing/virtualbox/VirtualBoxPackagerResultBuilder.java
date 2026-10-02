package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
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
 * Per-operation virtual-box settings. Identical boxes form direct rectangular grids.
 * Selective refinement reuses operation-local layouts without permutation searches.
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

	/** Bound the number of alternative layouts (delegate stack values) per virtual box. */
	public VirtualBoxPackagerResultBuilder withMaxLayouts(int count) {
		maxLayouts = positive(count);
		return this;
	}

	/** Bound the physical size of a directly constructed grid. Larger items remain ungrouped. */
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
			for(int i = 0; i < maxRefinements && shouldRefine(best) && !stop.getAsBoolean(); i++) {
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
			return new PackagerResult(best.getContainers(), VirtualBoxPacking.elapsed(start), best.isTimeout() || stop.getAsBoolean(), best.getCost());
		} finally {
			if(interrupt != null) {
				interrupt.close();
			}
		}
	}

	protected PackagerResult attempt(VirtualBoxPacking packing, PackagerInterruptSupplier stop, long start) {
		// Filled envelopes are ordinary count-one delegate items. Keep expansion
		// outside the delegate's search/point loops, including the no-load path.
		PackagerResult result = configured(stop).withBoxItems(packing.getItems()).build();
		return packing.expand(result, items, start);
	}

	protected PackagerResultBuilder configured(PackagerInterruptSupplier stop) {
		List<ContainerItem> copies = new ArrayList<>();
		for(ContainerItem item : containers) {
			copies.add(new ContainerItem(item));
		}
		return delegate.newResultBuilder().withContainerItems(copies).withMaxContainerCount(maxContainerCount)
				.withOrder(order).withInterruptDeadline(-1).withInterrupt(stop::getAsBoolean);
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
		VirtualBoxPacking result = new VirtualBoxPacking();
		List<Container> limits = new ArrayList<>();
		for(ContainerItem item : containers) {
			if(item.getCount() > 0) {
				limits.add(item.getContainer());
			}
		}
		GridVirtualBoxLayoutGenerator grids = new GridVirtualBoxLayoutGenerator();
		for(BoxItem item : items) {
			if(stop.getAsBoolean()) {
				break;
			}
			List<VirtualBoxLayout> layouts = List.of();
			if(item.getCount() > 1 && item.getCount() <= maxGridBoxes) {
				layouts = grids.generate(item, limits, maxLayouts, stop::getAsBoolean);
			}
			if(layouts.isEmpty()) {
				result.add(item);
			} else {
				result.add(VirtualBox.of(layouts));
			}
		}
		VirtualBoxLayoutCache cache = new VirtualBoxLayoutCache(items, limits, grids, maxGridBoxes, maxLayouts);
		return new VirtualBoxPlan(items, result, cache, maxDelegateBoxes);
	}

	protected boolean shouldRefine(PackagerResult best) {
		if(!best.isSuccess() || best.size() > 1) {
			return true;
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
