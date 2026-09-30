package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Per-operation virtual-box settings. Direct grids are preferred; small remaining
 * inventories can be assembled with bounding-box search. Selective refinement reuses operation-local layouts.
 * Ungrouped fallback is attempted when aggregation and refinement fail.
 */
public class VirtualBoxPackagerResultBuilder extends AbstractPackagerResultBuilder<VirtualBoxPackagerResultBuilder> {
	protected final Packager<? extends PackagerResultBuilder> delegate;
	protected final ScheduledThreadPoolExecutor scheduler;
	protected int maxLayouts = 8;
	protected int maxGridBoxes = 10_000;
	protected int maxSearchBoxes = 6;
	protected int maxSearches = 8;
	protected double dimensionDifference = 0.25;
	protected boolean bruteForce = true;
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

	/** Bound physical boxes per brute-force assembly; this is not the number of item types. */
	public VirtualBoxPackagerResultBuilder withMaxSearchBoxes(int count) {
		if(count < 2) {
			throw new IllegalArgumentException("Expected at least two boxes");
		}
		maxSearchBoxes = count;
		return this;
	}

	public VirtualBoxPackagerResultBuilder withMaxSearches(int count) {
		maxSearches = positive(count);
		return this;
	}

	public VirtualBoxPackagerResultBuilder withBruteForce(boolean enabled) {
		bruteForce = enabled;
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

	/** Maximum relative difference in sorted dimensions when clustering distinct items, from zero to one. */
	public VirtualBoxPackagerResultBuilder withMaximumDimensionDifference(double ratio) {
		if(!Double.isFinite(ratio) || ratio < 0 || ratio > 1) {
			throw new IllegalArgumentException("Expected a dimension difference from zero to one");
		}
		dimensionDifference = ratio;
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
		// One deadline supplier spans preprocessing, every layout search and both
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
		return packing.expand(result, items, start, requiresLoadValidation(), stop);
	}

	protected PackagerResultBuilder configured(PackagerInterruptSupplier stop) {
		List<ContainerItem> copies = new ArrayList<>();
		for(ContainerItem item : containers) {
			copies.add(new ContainerItem(item));
		}
		return delegate.newResultBuilder().withContainerItems(copies).withMaxContainerCount(maxContainerCount)
				.withOrder(order).withInterruptDeadline(-1).withInterrupt(stop::getAsBoolean);
	}

	protected boolean requiresLoadValidation() {
		for(BoxItem item : items) {
			if(item.getBox().isMaxLoad() || item.getBox().isLoadIdenticalBoxOnly()) {
				return true;
			}
		}
		return false;
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
			if(item.getGroup() != null
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
		List<BoxItem> remaining = new ArrayList<>();
		for(BoxItem item : items) {
			if(stop.getAsBoolean()) {
				break;
			}
			List<VirtualBoxLayout> layouts = List.of();
			if(item.getCount() > 1 && item.getCount() <= maxGridBoxes) {
				layouts = grids.generate(item, limits, maxLayouts, stop::getAsBoolean);
			}
			if(layouts.isEmpty()) {
				remaining.add(item);
			} else {
				result.add(VirtualBox.of(layouts));
			}
		}
		BruteForceVirtualBoxLayoutGenerator search = new BruteForceVirtualBoxLayoutGenerator(maxSearchBoxes, maxLayouts);
		boolean[] used = new boolean[remaining.size()];
		int searches = 0;
		for(int i = 0; i < remaining.size(); i++) {
			if(stop.getAsBoolean()) {
				break;
			}
			if(used[i]) {
				continue;
			}
			BoxItem first = remaining.get(i);
			List<BoxItem> subset = new ArrayList<>();
			subset.add(first);
			List<Integer> indexes = new ArrayList<>();
			indexes.add(i);
			int count = first.getCount();
			if(bruteForce && count <= maxSearchBoxes && searches < maxSearches) {
				for(int j = i + 1; j < remaining.size(); j++) {
					if(stop.getAsBoolean()) {
						break;
					}
					BoxItem candidate = remaining.get(j);
					if(!used[j] && candidate.getCount() <= maxSearchBoxes - count && similar(first, candidate)) {
						subset.add(candidate);
						indexes.add(j);
						count += candidate.getCount();
					}
				}
				if(count > 1) {
					searches++;
					List<VirtualBoxLayout> layouts = search.generate(subset, limits, stop);
					if(!layouts.isEmpty()) {
						result.add(VirtualBox.of(layouts));
						for(int index : indexes) {
							used[index] = true;
						}
						continue;
					}
				}
			}
			used[i] = true;
			result.add(first);
		}
		VirtualBoxLayoutCache cache = new VirtualBoxLayoutCache(items, limits, grids, bruteForce ? search : null,
				maxGridBoxes, maxLayouts, Math.max(0, maxSearches - searches));
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

	protected boolean similar(BoxItem a, BoxItem b) {
		var av = a.getBox().getStackValue(0);
		var bv = b.getBox().getStackValue(0);
		int[] left = {av.getDx(), av.getDy(), av.getDz()};
		int[] right = {bv.getDx(), bv.getDy(), bv.getDz()};
		Arrays.sort(left);
		Arrays.sort(right);
		for(int i = 0; i < 3; i++) {
			if(Math.abs((long) left[i] - right[i]) > dimensionDifference * Math.max(left[i], right[i])) {
				return false;
			}
		}
		return true;
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
