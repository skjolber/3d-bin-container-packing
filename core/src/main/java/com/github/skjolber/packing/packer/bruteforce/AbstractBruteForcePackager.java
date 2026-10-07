package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.InsertionSequencer;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.PackagerInput;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;
import com.github.skjolber.packing.packer.util.WeightPressureCountIdenticalLoadAwarePlacementUtility;
import com.github.skjolber.packing.packer.util.WeightPressureCountLoadAwarePlacementUtility;
import com.github.skjolber.packing.packer.util.WeightLoadAwarePlacementUtility;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * For one container, {@link #pack(PointCalculator3DStack, Placement[], int, ContainerItem, int, BoxItemPermutationRotationIterator, PackagerInterruptSupplier, BruteForcePointIteratorFilter, IntermediatePackagerResult)}
 * searches every permutation (order) of the boxes, and for each permutation every placement of the boxes in that
 * order: each box in each rotation at each free (extreme) point (see {@link #searchOrder}). The same search packs a box
 * item order ({@link Order#CHRONOLOGICAL}: the one permutation) and, when boxes may be skipped
 * ({@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}), also tries skipping each box.
 * <br>
 * <br>
 * Without skipping, boxes are placed in permutation order, so a result is a prefix of a permutation: the longest prefix
 * which could be placed. This allows skipping work which cannot change the outcome:
 * <ul>
 * <li>if the first {@code n} boxes were placed, reordering box {@code n + 1} or later cannot place more boxes,
 * so the iterator skips ahead to a change at index {@code n} or lower,</li>
 * <li>a prefix cannot be longer than the boxes which fit by volume and weight ({@link #getMaxPackableCount(BoxItemPermutationRotationIterator, long, long)}),
 * so the search stops once that many are placed, and</li>
 * <li>when the comparator prefers higher load volume, permutations (and containers) whose maximum load volume does not exceed
 * the best result so far are skipped.</li>
 * </ul>
 * Note: The search is exponential in the number of boxes. It is not intended for more than about 10 boxes per container.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public abstract class AbstractBruteForcePackager extends AbstractPackager<AbstractBruteForcePackager.BruteForcePackagerResultBuilder> {

	/** Picks the order of box item groups with equal container priority and extraction order, see {@link #sortGroups(List)} */
	protected Comparator<BoxItemGroup> boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();

	protected void setBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	/** Place boxes only where they rest completely on the floor or on the boxes below (see {@link FullSupportCandidates}) */
	protected boolean requireFullSupport;

	protected void setRequireFullSupport(boolean requireFullSupport) {
		this.requireFullSupport = requireFullSupport;
	}

	/** Skip the group orders which cannot give a better result (see {@link AbstractBruteForceBoxItemGroupSession#attemptGroupOrders}); tests turn this off */
	boolean skipGroupOrders = true;

	public AbstractBruteForcePackager(Comparator<IntermediatePackagerResult> comparator) {
		super(comparator);
	}
	
	public static class BruteForcePackagerResultBuilder extends AbstractPackagerResultBuilder<BruteForcePackagerResultBuilder> {
	
		private AbstractBruteForcePackager packager;
	
		public BruteForcePackagerResultBuilder withPackager(AbstractBruteForcePackager packager) {
			this.packager = packager;
			return this;
		}

		@Override
		public PackagerResult build() {
			return packager.pack(validate(packager), deadline, interrupt);
		}
	}

	@Override
	public String getUnsupportedReason(PackagerInput input) {
		String reason = super.getUnsupportedReason(input);
		if(reason != null) {
			return reason;
		}
		for(ContainerItem container : input.getContainerItems()) {
			if(container.hasControls()) {
				return "Controls not supported";
			}
		}
		return null;
	}

	protected static boolean hasContainerPriorities(PackagerInput input) {
		if(input.hasBoxItems()) {
			List<BoxItem> boxItems = input.getBoxItems();
			for (int i = 1; i < boxItems.size(); i++) {
				if(boxItems.get(i).getContainerPriority() != boxItems.get(0).getContainerPriority()) {
					return true;
				}
			}
			return false;
		}
		List<BoxItemGroup> groups = input.getBoxItemGroups();
		for (int i = 1; i < groups.size(); i++) {
			if(groups.get(i).getContainerPriority() != groups.get(0).getContainerPriority()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		boolean sort = input.getOrder() == Order.NONE && hasContainerPriorities(input);
		AbstractBruteForceBoxItemSession session;
		if(input.hasBoxItems()) {
			AbstractPackagerSession.initializeGlobalIndexes(input.getBoxItems());
			List<BoxItem> boxItems = input.getBoxItems();
			if(sort) {
				// the boxes of a lower container priority first; the iterators permute within each priority
				boxItems = new ArrayList<>(boxItems);
				for (int i = 1; i < boxItems.size(); i++) {
					BoxItem boxItem = boxItems.get(i);
					int j = i - 1;
					while(j >= 0 && boxItems.get(j).getContainerPriority() > boxItem.getContainerPriority()) {
						boxItems.set(j + 1, boxItems.get(j));
						j--;
					}
					boxItems.set(j + 1, boxItem);
				}
			}
			session = createBoxItemSession(boxItems, input.getContainerItems(), input.getMaxContainerCount(), interrupt);
		} else {
			AbstractPackagerSession.initializeGlobalIndexesForGroups(input.getBoxItemGroups());
			List<BoxItemGroup> groups = input.getBoxItemGroups();
			if(input.getOrder() == Order.NONE) {
				// groups are packed in order: the order in which the plain packager picks them
				groups = sortGroups(groups);
			}
			session = createBoxItemGroupSession(groups, input.getContainerItems(), input.getMaxContainerCount(), interrupt);
		}
		session.setOrder(input.getOrder());
		// with full support, boxes rest on the boxes placed before them: a permutation and its reverse do not pack equally well
		session.setReverseSymmetric(!requireFullSupport && isReverseSymmetric(input));
		if(session instanceof AbstractBruteForceBoxItemGroupSession groupSession) {
			groupSession.skipGroupOrders = skipGroupOrders;
		}
		return session;
	}

	/**
	 * Sort box item groups in the order the plain packager picks them (see {@code AnyOrderBoxItemGroupIterator}): the
	 * lowest container priority first, then the groups which are extracted last, then the best by the group comparator
	 * (by default the largest). Groups which are equal keep their order.
	 *
	 * @return the sorted groups (a new list)
	 */
	protected List<BoxItemGroup> sortGroups(List<BoxItemGroup> groups) {
		List<BoxItemGroup> sorted = new ArrayList<>(groups);
		for (int i = 1; i < sorted.size(); i++) {
			BoxItemGroup group = sorted.get(i);
			int j = i - 1;
			while(j >= 0 && isPickedAfter(sorted.get(j), group)) {
				sorted.set(j + 1, sorted.get(j));
				j--;
			}
			sorted.set(j + 1, group);
		}
		return sorted;
	}

	/**
	 * @return true if the plain packager picks group b before group a
	 */
	private boolean isPickedAfter(BoxItemGroup a, BoxItemGroup b) {
		if(a.getContainerPriority() != b.getContainerPriority()) {
			return a.getContainerPriority() > b.getContainerPriority();
		}
		if(a.getExtractionOrder() != b.getExtractionOrder()) {
			// the groups which are extracted last are placed first
			return a.getExtractionOrder() < b.getExtractionOrder();
		}
		return boxItemGroupComparator.compare(a, b) < 0;
	}

	/**
	 * Whether a permutation and its reverse can be expected to pack equally well, so that skipping reverse permutations
	 * only skips equivalent arrangements. Not when the insertion order matters: box extraction orders, container access,
	 * obstacles or initial points, or box load limits (the order decides which box is below).
	 */
	protected static boolean isReverseSymmetric(PackagerInput input) {
		if(hasLoadLimits(input)) {
			return false;
		}
		for (ContainerItem containerItem : input.getContainerItems()) {
			Container container = containerItem.getContainer();
			if(container.getAccess() != ContainerAccess.ANY || !container.getObstacles().isEmpty() || containerItem.hasInitialPoints()) {
				return false;
			}
		}
		List<BoxItem> boxItems;
		if(input.hasBoxItems()) {
			boxItems = input.getBoxItems();
		} else {
			boxItems = new ArrayList<>();
			for (BoxItemGroup group : input.getBoxItemGroups()) {
				boxItems.addAll(group.getItems());
			}
		}
		for (int i = 1; i < boxItems.size(); i++) {
			if(boxItems.get(i).getExtractionOrder() != boxItems.get(0).getExtractionOrder()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withPackager(this);
	}

	protected abstract AbstractBruteForceBoxItemSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt);

	protected abstract AbstractBruteForceBoxItemSession createBoxItemSession(List<BoxItem> items, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt);

	static Placement[] getPlacements(int size, boolean load) {
		// each box will at most have a single placement with a space (and its remainder).
		Placement[] placements = new Placement[size];

		for (int i = 0; i < size; i++) {
			placements[i] = new Placement(load);
		}
		return placements;
	}

	static Placement[] getPlacements(int size) {
		return getPlacements(size, false);
	}

	static int removeFirstPlacements(Placement[] placements, int size, int count) {
		int remaining = count - size;
		if(remaining > 0) {
			System.arraycopy(placements, size, placements, 0, remaining);
		}
		return remaining;
	}

	/**
	 * Returns an upper bound for the number of items in the current permutation
	 * which can be packed without exceeding the container's volume or weight.
	 *
	 * <p>
	 * The brute-force search packs a prefix of each permutation. Therefore,
	 * once the next item would exceed either capacity, no placement branch or
	 * rotation of that permutation can produce a longer result.
	 * </p>
	 */
	protected static int getMaxPackableCount(BoxItemPermutationRotationIterator iterator, long maxLoadVolume, long maxLoadWeight) {
		long loadVolume = 0L;
		long loadWeight = 0L;
		for (int i = 0; i < iterator.length(); i++) {
			BoxStackValue stackValue = iterator.getStackValue(i);
			long volume = stackValue.getBox().getVolume();
			long weight = stackValue.getBox().getWeight();
			if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
				return i;
			}
			loadVolume += volume;
			loadWeight += weight;
		}
		return iterator.length();
	}

	/**
	 * @return the load volume a result must exceed to be selected over {@code best}, or 0
	 */
	protected long getMinLoadVolume(IntermediatePackagerResult best) {
		if(best == null || !prefersHigherLoadVolume || best.isEmpty()) {
			return 0L;
		}
		return best.getLoadVolume();
	}

	/**
	 * @return an upper bound for the load volume of any result for the container
	 */
	protected static long getMaxLoadVolume(BoxItemPermutationRotationIterator iterator, Container holder, boolean allItemsFit) {
		long volume = getLoadVolume(iterator, iterator.length());
		if(allItemsFit) {
			return volume;
		}
		return Math.min(volume, holder.getMaxLoadVolume());
	}

	/**
	 * @return the volume of the first {@code count} boxes of the current permutation
	 */
	protected static long getLoadVolume(BoxItemPermutationRotationIterator iterator, int count) {
		long volume = 0L;
		for (int i = 0; i < count; i++) {
			volume += iterator.getStackValue(i).getBox().getVolume();
		}
		return volume;
	}

	protected static boolean canPackAll(BoxItemPermutationRotationIterator iterator, long maxLoadVolume, long maxLoadWeight) {
		long loadVolume = 0L;
		long loadWeight = 0L;
		for (int i = 0; i < iterator.length(); i++) {
			BoxStackValue stackValue = iterator.getStackValue(i);
			long volume = stackValue.getBox().getVolume();
			long weight = stackValue.getBox().getWeight();
			if(volume > maxLoadVolume - loadVolume || weight > maxLoadWeight - loadWeight) {
				return false;
			}
			loadVolume += volume;
			loadWeight += weight;
		}
		return true;
	}

	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, index, iterator, interrupt, pointFilter, null);
	}

	/**
	 * Pack the boxes of the iterator into one container: the best placement over all permutations and rotations.
	 * Within a permutation, the result which places the most boxes wins (it is a longer prefix, so it has more volume and weight);
	 * across permutations, results are compared with the packager's comparator.
	 *
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 * @return the best result, or an empty result; the result refers to reused state (stack placements, points) until materialized
	 */
	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, IntermediatePackagerResult best)
			throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, index, iterator, interrupt, pointFilter, best, Integer.MAX_VALUE);
	}

	/**
	 * @param limit the number of leading boxes of the permutations which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 */
	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, IntermediatePackagerResult best,
			int limit) throws PackagerInterruptedException {

		Container holder = containerItem.getContainer().copy(iterator.length());
		
		Stack stack = holder.getStack();
		
		// with box load limits, the search tracks the loads of the placed boxes
		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);

		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, utility != null);
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, utility != null);

		if(limit == 0) {
			return bestResult;
		}
		// if all boxes fit by volume and weight, every permutation may place all of them;
		// otherwise each permutation is limited to the prefix which fits (see getMaxPackableCount(..))
		boolean allItemsFit = limit >= iterator.length() && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}
		// box item groups: the boxes of earlier groups; the permutations keep the groups' positions
		int[] insertAfterCounts = getInsertAfterCounts(iterator);
		int[] prefixRotations = new int[iterator.length()];
		boolean checkExtraction = hasExtractionOrders(iterator);
		// permutations
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			bestPermutationResult.reset();
			int maxPackableCount = allItemsFit ? iterator.length() : Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
			if(!allItemsFit && prefersHigherLoadVolume && getLoadVolume(iterator, maxPackableCount) < Math.max(minLoadVolume, bestResult.getLoadVolume())) {
				// no rotation of this permutation can load more than the best result,
				// and neither can permutations which only reorder boxes after the packable prefix
				if(iterator.nextPermutation(maxPackableCount) == -1) {
					break;
				}
				continue;
			}

			// search the permutation as a box order: the rotations and positions of the longest prefix
			pointCalculator.resetBest();
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
			if(containerItem.getInitialPoints() != null) {
				pointCalculator.setPoints(containerItem.getInitialPoints());
				pointCalculator.clear();
			}
			if(utility != null) {
				utility.initialize(iterator.length());
			}
			searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, utility, pointFilter, requireFullSupport, checkExtraction, insertAfterCounts, maxPackableCount,
					null, Integer.MAX_VALUE, null, prefixRotations);
			stack.clear();
			List<Point> points = pointCalculator.getBestPoints();
			if(!points.isEmpty()) {
				bestPermutationResult.setStateFromReusablePoints(points, getState(iterator, prefixRotations, points.size()), stackPlacements, stackPlacementCount);
				if(points.size() == iterator.length()) {
					// best possible result for this container
					return bestPermutationResult;
				}
			}

			// the first bestPermutationResult.getSize() boxes were placed, but the next could not be:
			// skip permutations which keep the same boxes up to and including that index
			int permutationIndex = iterator.nextPermutation(bestPermutationResult.getSize());

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result
				
				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) < 0) {
					// switch the two results for one another
					BruteForceIntermediatePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}
			
			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		bestResult.markDirty();

		return bestResult;
	}

	/**
	 * Pack the boxes in the iterator's order (a box item order) into one container: the longest prefix of the order
	 * which can be placed (see {@link #searchOrder}), where each box must be insertable after the boxes before it.
	 *
	 * @param best the best result so far, or null; see {@link #pack(PointCalculator3DStack, Placement[], int, ContainerItem, int, BoxItemPermutationRotationIterator, PackagerInterruptSupplier, BruteForcePointIteratorFilter, IntermediatePackagerResult)}
	 * @param limit the number of leading boxes which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 * @return the result, or an empty result; the result refers to reused state (stack placements, points) until materialized
	 */
	public BruteForceIntermediatePackagerResult packInOrder(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, IntermediatePackagerResult best,
			int limit) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, utility != null);
		if(limit == 0) {
			return result;
		}
		// the prefix of the order which fits by volume and weight
		int maxPackableCount = Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getLoadVolume(iterator, maxPackableCount) < minLoadVolume) {
			// cannot load more than the best result
			return result;
		}

		int[] permutations = iterator.getPermutations();
		int[] rotations = new int[permutations.length];
		List<Point> points = packStackPlacementInOrder(pointCalculator, stackPlacements, iterator, permutations, rotations, stack, holder, interrupt,
				containerItem.getInitialPoints(), utility, pointFilter, maxPackableCount);
		stack.clear();
		if(!points.isEmpty()) {
			result.setStateFromReusablePoints(points, new PermutationRotationState(rotations, permutations), stackPlacements, stackPlacementCount);
		}
		result.markDirty();
		return result;
	}

	/**
	 * @param rotations the rotations of the boxes of the best arrangement, set by the search
	 * @return the points of the best arrangement
	 */
	protected List<Point> packStackPlacementInOrder(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator,
			int[] permutations, int[] rotations, Stack stack, Container container, PackagerInterruptSupplier interrupt, List<Point> points,
			LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
		pointCalculator.resetBest();
		if(placements.length == 0 || maxPackableCount == 0) {
			return Collections.emptyList();
		}
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(iterator.length());
		}
		searchOrder(pointCalculator, placements, iterator, stack, container, interrupt, utility, pointFilter, requireFullSupport, hasExtractionOrders(iterator), null, maxPackableCount,
				null, Integer.MAX_VALUE, null, rotations);
		return pointCalculator.getBestPoints();
	}

	/**
	 * @return for each index of the permutation, the smallest area of the boxes from the index on, in any rotation
	 */
	protected static long[] getMinAreas(BoxItem[] boxItems, int[] permutations) {
		long[] minAreas = new long[permutations.length];
		long minArea = Long.MAX_VALUE;
		for (int i = permutations.length - 1; i >= 0; i--) {
			for (BoxStackValue stackValue : boxItems[permutations[i]].getBox().getStackValues()) {
				if(stackValue.getArea() < minArea) {
					minArea = stackValue.getArea();
				}
			}
			minAreas[i] = minArea;
		}
		return minAreas;
	}

	/**
	 * Keep the arrangement if the longest so far, with the rotations of its boxes.
	 *
	 * @param count the number of boxes placed
	 */
	private static void updateBest(PointCalculator3DStack pointCalculator, int[] rotationIndexes, int[] rotations, int count) {
		int bestStackIndex = pointCalculator.getBestStackIndex();
		pointCalculator.updateBest();
		if(pointCalculator.getBestStackIndex() > bestStackIndex) {
			System.arraycopy(rotationIndexes, 0, rotations, 0, count);
		}
	}

	/**
	 * Pack the boxes in the iterator's order (a box item order, {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}) into one
	 * container: any box may be skipped (it waits for a later container), and the boxes which are placed are in the order,
	 * each insertable after the boxes before it. The search (see {@link #searchOrder}) tries each rotation of each box at
	 * each point, and skipping it; the best arrangement is chosen by the result comparator.
	 *
	 * @param skipEnds for each box, the box to continue with when skipping it, or -1 if it cannot be skipped (box item
	 *        groups are skipped whole), or null if each box can be skipped
	 * @param maxContainerPriority the highest container priority which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getMaxContainerPriority(BoxItemPermutationRotationIterator)})
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 * @return the result, or an empty result; the result may hold any of the boxes (see
	 *         {@link BruteForceIntermediatePackagerResult#isAnyRemaining()})
	 */
	public BruteForceIntermediatePackagerResult packInOrderSkipping(PointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, BruteForcePointIteratorFilter pointFilter, int[] skipEnds,
			int maxContainerPriority, IntermediatePackagerResult best) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), index, iterator, utility != null);
		result.setAnyRemaining(true);
		int length = iterator.length();
		if(length == 0 || stackPlacements.length == 0) {
			return result;
		}

		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.getInitialPoints() != null) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(length);
		}
		SkippingBest arrangements = newSkippingBest(result, best, stackPlacements, stackPlacementCount);
		searchOrder(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, utility, pointFilter, requireFullSupport, hasExtractionOrders(iterator), null, length,
				skipEnds, maxContainerPriority, arrangements, null);
		stack.clear();
		BruteForceIntermediatePackagerResult packed = arrangements.best;
		packed.markDirty();
		return packed;
	}

	/**
	 * @param empty an empty result for the container, for the best arrangement
	 * @param best the best result so far (of other attempts), or null
	 */
	protected SkippingBest newSkippingBest(BruteForceIntermediatePackagerResult empty, IntermediatePackagerResult best, Placement[] stackPlacements, int stackPlacementCount) {
		BruteForceIntermediatePackagerResult candidate = new BruteForceIntermediatePackagerResult(empty.getContainerItem(), new Stack(empty.getIterator().length()), empty.getContainerItemIndex(),
				empty.getIterator(), empty.isCalculateLoads());
		candidate.setAnyRemaining(true);
		return new SkippingBest(intermediatePackagerResultComparator, prefersHigherLoadVolume, getMinLoadVolume(best), stackPlacements, stackPlacementCount, empty, candidate);
	}

	/**
	 * The best arrangement found by the searches which do not skip boxes: the arrangements are the prefixes of the box
	 * order, and the longest is the best.
	 */
	protected static class PrefixBest {

		/** the number of boxes of the best arrangement */
		protected int count;
		/** the points of the placed boxes */
		protected List<Point> points = Collections.emptyList();
		/** the rotation of each placement */
		protected final int[] rotations;

		protected PrefixBest(int length) {
			this.rotations = new int[length];
		}

		protected void reset() {
			count = 0;
			points = Collections.emptyList();
		}

		protected void offer(List<Point> points, int[] rotations, int count) {
			this.points = points;
			this.count = count;
			System.arraycopy(rotations, 0, this.rotations, 0, count);
		}
	}

	/**
	 * The best arrangement found by the searches which skip boxes (see {@link #searchOrder}): the best by the
	 * result comparator, as for the other searches.
	 */
	protected static class SkippingBest {

		private final Comparator<IntermediatePackagerResult> comparator;
		/** results with less load volume always compare worse: only search where the load volume can be enough */
		private final boolean volumeBound;
		/** the load volume of the best result so far (of other attempts) */
		private final long minLoadVolume;
		private final Placement[] placements;
		private final int placementCount;

		/** the best arrangement, or an empty result */
		protected BruteForceIntermediatePackagerResult best;
		private BruteForceIntermediatePackagerResult candidate;
		/** the number of boxes of the best arrangement */
		protected int count;

		protected SkippingBest(Comparator<IntermediatePackagerResult> comparator, boolean volumeBound, long minLoadVolume, Placement[] placements, int placementCount,
				BruteForceIntermediatePackagerResult best, BruteForceIntermediatePackagerResult candidate) {
			this.comparator = comparator;
			this.volumeBound = volumeBound;
			this.minLoadVolume = minLoadVolume;
			this.placements = placements;
			this.placementCount = placementCount;
			this.best = best;
			this.candidate = candidate;
		}

		/**
		 * @param loadVolume the most load volume an arrangement can have
		 * @return false if such an arrangement cannot be better than the best
		 */
		protected boolean canBeBetter(long loadVolume) {
			return !volumeBound || (loadVolume >= minLoadVolume && loadVolume >= best.getLoadVolume());
		}

		/**
		 * Keep an arrangement if it is better than the best.
		 *
		 * @param points the points of the placed boxes
		 * @param permutations the box (index in the iterator's box items) of each placement
		 * @param rotations the rotation of each placement
		 * @param count the number of placed boxes
		 */
		protected void offer(List<Point> points, int[] permutations, int[] rotations, int count) {
			candidate.setState(points, new PermutationRotationState(rotations, permutations, count), placements, placementCount);
			if(comparator.compare(best, candidate) < 0) {
				BruteForceIntermediatePackagerResult swap = best;
				best = candidate;
				candidate = swap;
				this.count = count;
			}
		}
	}

	/** Enter a level of {@link #searchOrder}: how it was reached, and the state when entering it */
	protected static void enter(int level, int parent, int placedCount, long placedVolume, int freeLoadWeight, int maxContainerPriority,
			int[] parents, int[] placedCounts, long[] placedVolumes, int[] freeLoadWeights, int[] maxContainerPriorities) {
		parents[level] = parent;
		placedCounts[level] = placedCount;
		placedVolumes[level] = placedVolume;
		freeLoadWeights[level] = freeLoadWeight;
		maxContainerPriorities[level] = maxContainerPriority;
	}

	/**
	 * @return the load placement utility for the box load limits of the iterator's boxes, or null if they have none
	 */
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		boolean maxLoadWeight = false;
		boolean maxLoadPressure = false;
		boolean maxLoadBoxCount = false;
		boolean loadIdenticalBox = false;
		for(int i = 0; i < iterator.length(); i++) {
			Box box = iterator.getStackValue(i).getBox();
			maxLoadWeight |= box.isMaxLoadWeight();
			maxLoadPressure |= box.isMaxLoadPressure();
			maxLoadBoxCount |= box.isMaxLoadBoxCount();
			loadIdenticalBox |= box.isLoadIdenticalBoxOnly();
		}

		if(!maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount && !loadIdenticalBox) {
			return null;
		}
		if(maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount && !loadIdenticalBox) {
			return new WeightLoadAwarePlacementUtility(stack);
		}
		if(!loadIdenticalBox) {
			return new WeightPressureCountLoadAwarePlacementUtility(stack);
		}
		return new WeightPressureCountIdenticalLoadAwarePlacementUtility(stack);
	}

	/**
	 * Pack the boxes of the iterator's permutation, in that order, into a container: the longest prefix which can be
	 * placed (see {@link #searchOrder}).
	 *
	 * @param points the container's initial free points, or null
	 * @return the points of the placed boxes (a copy)
	 */
	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, List<Point> points, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter)
			throws PackagerInterruptedException {
		pointCalculator.resetBest();
		int maxPackableCount = placements.length == 0 ? 0 : getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
		if(maxPackableCount == 0) {
			return Collections.emptyList();
		}
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		if(utility != null) {
			utility.initialize(iterator.length());
		}
		searchOrder(pointCalculator, placements, iterator, stack, container, interrupt, utility, pointFilter, requireFullSupport, hasExtractionOrders(iterator), getInsertAfterCounts(iterator),
				maxPackableCount, null, Integer.MAX_VALUE, null, new int[iterator.length()]);
		return preservePoints(pointCalculator.getBestPoints());
	}

	protected static List<Point> preservePoints(List<Point> points) {
		if(points.isEmpty()) {
			return Collections.emptyList();
		}
		ArrayList<Point> result = new ArrayList<>(points.size());
		for(int i = 0; i < points.size(); i++) {
			result.add(points.get(i));
		}
		return result;
	}

	/**
	 * Depth-first search for the longest prefix of the permutation which can be placed, with the current rotations.
	 * <br>
	 * <br>
	 * Level {@code i} of the search places box {@code i} of the permutation. Each level tries the box at each candidate
	 * point in turn; after a placement, the next level tries the next box at the free points which remain. When a level
	 * runs out of points, the search goes back to the previous level, which removes its placement and tries its next point:
	 *
	 * <pre>
	 *  level 0 (box 0)    level 1 (box 1)    level 2 (box 2)
	 *  point a ─────────┬─ point c ────────── point e, f, ...
	 *                   └─ point d ────────── ...
	 *  point b ─────────── point c ────────── ...
	 * </pre>
	 *
	 * The search is a loop over two steps rather than recursion, so the number of boxes is not limited by the thread's stack:
	 * <ul>
	 * <li><b>descend</b> into a level: record the boxes placed so far as the best arrangement if it is the longest yet,
	 * save the point calculator's state ({@link PointCalculator3DStack#push()}) and start at the level's first candidate point.</li>
	 * <li><b>ascend</b> back into a level from the next level: remove the level's placement from the stack (and its load links),
	 * and restore the free points to before the placement ({@link PointCalculator3DStack#redo()}), then continue with the next candidate point.</li>
	 * </ul>
	 * After either step, the box is placed at the next candidate point ({@link PointCalculator3DStack#add(int, Placement)}
	 * calculates the new free points) and the search descends to the next level. A level without more candidate points
	 * discards its saved state ({@link PointCalculator3DStack#pop()}) and ascends.
	 * <br>
	 * <br>
	 * The search ends when level 0 has no more points, or as soon as {@code maxPackableCount} boxes are placed; no arrangement
	 * can be longer than that. A level is skipped (the search ascends without trying any point) if its box is heavier than
	 * the remaining load weight, since then no box from that index on can be part of the prefix.
	 * <br>
	 * <br>
	 * The point calculator also discards free points which are too small for every remaining box: the minimum area is that of the
	 * smallest remaining box ({@code minStackableAreaIndex}), updated when that box has been placed, and the minimum volume is
	 * {@link BoxItemPermutationRotationIterator#getMinBoxVolume(int)}.
	 * <br>
	 * <br>
	 * The result is the longest arrangement found, as points per box, in {@link PointCalculator3DStack#getBestPoints()}
	 * ({@link PointCalculator3DStack#getBestStackIndex()} boxes). The state of each level is kept in {@link BruteForceSearchFrames}.
	 *
	 * @param placements one reusable placement per level
	 * @param stack the placed boxes, for load constraints; boxes are added on descend and removed on ascend
	 * @param maxLoadWeight the container's max load weight
	 * @param minStackableAreaIndex index of the box with the smallest area, from level 0
	 * @param maxPackableCount the maximum number of boxes which fit by volume and weight
	 * @param utility load constraints, or null if none
	 * @param pointFilter candidate points, or null for all fitting points
	 * @throws PackagerInterruptedException if interrupted
	 */
	/**
	 * @return true if a box can be inserted at the point after the boxes already in the container (obstacles):
	 *         none of them rests on it, or is in its path (see {@link ContainerAccess})
	 */
	protected static boolean isInsertable(Point point, BoxStackValue stackValue, List<Placement> obstacles, ContainerAccess access) {
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		int endZ = z + stackValue.getDz() - 1;
		for (int i = 0; i < obstacles.size(); i++) {
			if(obstacles.get(i).mustFollow(x, y, z, endX, endY, endZ, access)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The boxes which a box must be insertable after. Box item groups are inserted one at a time (see
	 * {@link InsertionSequencer}): the iterators place the groups in order, each at a fixed range of levels, so a box
	 * must not have to be inserted before the boxes of the levels before its group. With a box item order, see
	 * {@link #getInsertAfterAllCounts(int)}.
	 *
	 * @return for each level, the number of boxes placed before it which it must be insertable after, or an empty
	 *         array if none
	 */
	private static final int[] NO_COUNTS = new int[0];

	/**
	 * With a box item order, the boxes are inserted in their order: a box must be insertable after all the boxes before it.
	 *
	 * @param length the number of boxes
	 * @return for each level, the number of boxes placed before it
	 */
	protected static int[] getInsertAfterAllCounts(int length) {
		int[] counts = new int[length];
		for (int i = 0; i < length; i++) {
			counts[i] = i;
		}
		return counts;
	}

	protected static int[] getInsertAfterCounts(BoxItemPermutationRotationIterator iterator) {
		int length = iterator.length();
		if(length < 2) {
			return NO_COUNTS;
		}

		BoxItem[] boxItems = iterator.getBoxItems();
		for (int i = 0; i < boxItems.length; i++) {
			if(boxItems[i] != null) {
				if(boxItems[i].getGroup() == null) {
					// not packing groups
					return NO_COUNTS;
				}
				break;
			}
		}
		int[] permutations = iterator.getPermutations();
		int[] starts = new int[length];
		Object previous = boxItems[permutations[0]].getGroupKey();
		for (int i = 1; i < length; i++) {
			Object group = boxItems[permutations[i]].getGroupKey();
			starts[i] = group.equals(previous) ? starts[i - 1] : i;
			previous = group;
		}
		return starts[length - 1] == 0 ? NO_COUNTS : starts;
	}

	/**
	 * @param count the number of placed boxes which belong to earlier groups
	 * @return true if a box at the point can be inserted after the first boxes of the stack: none of them rests on it,
	 *         or is in its path
	 */
	protected static boolean isInsertableAfter(Point point, BoxStackValue stackValue, Stack stack, int count, ContainerAccess access) {
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		int endZ = z + stackValue.getDz() - 1;
		List<Placement> placements = stack.getPlacements();
		for (int i = 0; i < count; i++) {
			if(placements.get(i).mustFollow(x, y, z, endX, endY, endZ, access)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if the boxes of the iterator have different extraction orders
	 */
	protected static boolean hasExtractionOrders(BoxItemPermutationRotationIterator iterator) {
		for (int i = 1; i < iterator.length(); i++) {
			if(iterator.getStackValue(i).getBox().getBoxItem().getExtractionOrder() != iterator.getStackValue(0).getBox().getBoxItem().getExtractionOrder()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if a box at the point does not prevent extracting the placed boxes in their extraction order, and
	 *         is not prevented by them: a box extracted earlier must not have a box extracted later resting on it, or
	 *         in its path (see {@link ContainerAccess})
	 */
	protected static boolean isExtractable(Point point, BoxStackValue stackValue, Stack stack, ContainerAccess access) {
		int order = stackValue.getBox().getBoxItem().getExtractionOrder();
		int x = point.getMinX();
		int y = point.getMinY();
		int z = point.getMinZ();
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		int endZ = z + stackValue.getDz() - 1;
		List<Placement> placements = stack.getPlacements();
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			int placementOrder = placement.getBoxItem().getExtractionOrder();
			if(order < placementOrder) {
				if(placement.mustFollow(x, y, z, endX, endY, endZ, access)) {
					return false;
				}
			} else if(order > placementOrder) {
				if(placement.mustPrecede(x, y, z, endX, endY, endZ, access)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Depth-first search over the boxes of the iterator's permutation, in order: each box is tried in each of its
	 * rotations at each candidate point (all points, or the point filter's), and, when skipping, skipped. The same search
	 * packs a box item order ({@link Order#CHRONOLOGICAL}, and with skipping {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING})
	 * and each permutation of the boxes ({@link Order#NONE}).
	 * <br>
	 * <br>
	 * Without skipping, an arrangement ends at the first box which cannot be placed: the arrangements are the prefixes of
	 * the order, and the longest is the best (see {@link PointCalculator3DStack#getBestPoints()}). With skipping, the
	 * best arrangement is chosen by the result comparator.
	 * <br>
	 * <br>
	 * A position is valid where the box fits a free point, is insertable after the boxes placed before it (see
	 * {@code insertAfterCounts}), is not blocked by obstacles, keeps the extraction order and, with load limits, is
	 * carried by the boxes below; with full support required, the positions are where the box is fully supported (see
	 * {@link FullSupportCandidates}). Levels are boxes, while the point calculator stack has a level for each placed box.
	 *
	 * @param insertAfterCounts null if each box must be insertable after all the boxes placed before it (a box item
	 *        order), empty if not, otherwise for each level the number of boxes placed before it which it must be
	 *        insertable after (box item groups)
	 * @param maxPackableCount without skipping, the number of leading boxes which may be placed
	 * @param skipEnds with skipping: for each level, the level to continue with when skipping it, -1 if it cannot be
	 *        skipped, or null for the next level
	 * @param maxContainerPriority the highest container priority which may be placed
	 * @param skipping the best arrangement, when skipping, otherwise null
	 * @param rotations without skipping, the rotations of the boxes of the best arrangement, set by the search
	 */
	protected void searchOrder(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
			PackagerInterruptSupplier interrupt, LoadPlacementUtility utility, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction,
			int[] insertAfterCounts, int maxPackableCount, int[] skipEnds, int maxContainerPriority, SkippingBest skipping, int[] rotations) throws PackagerInterruptedException {
		BruteForceSearchFrames frames = pointCalculator.getSearchFrames();
		int[] nextPointIndexes = frames.nextPointIndexes;
		int[] pointCounts = frames.pointCounts;
		IntIterator[] pointIterators = frames.pointIterators;
		int[] rotationIndexes = frames.rotationIndexes;
		int[] freeLoadWeights = frames.freeLoadWeights;
		boolean[] unplaced = frames.unplaced;
		int[] parents = frames.parents;
		int[] placedCounts = frames.placedCounts;
		long[] placedVolumes = frames.placedVolumes;
		int[] maxContainerPriorities = frames.maxContainerPriorities;
		int[] placedPermutations = frames.placedPermutations;
		int[] placedRotations = frames.placedRotations;
		long[] remainingVolumes = frames.remainingVolumes;
		long[] minAreas = frames.minAreas;
		Box[] boxes = frames.boxes;

		int length = skipping != null ? iterator.length() : maxPackableCount;
		List<Placement> obstacles = container.getObstacles();
		ContainerAccess access = container.getAccess();
		boolean checkObstacles = obstacles != null && !obstacles.isEmpty();
		long maxLoadVolume = container.getMaxLoadVolume();

		// the box of each level, the smallest area of the boxes from each level on (in any rotation), and with skipping
		// (as a bound) their volume
		long minArea = Long.MAX_VALUE;
		for (int i = length - 1; i >= 0; i--) {
			Box box = iterator.getStackValue(i).getBox();
			boxes[i] = box;
			if(box.getMinimumArea() < minArea) {
				minArea = box.getMinimumArea();
			}
			minAreas[i] = minArea;
		}
		if(skipping != null) {
			long remainingVolume = 0L;
			remainingVolumes[length] = 0L;
			for (int i = length - 1; i >= 0; i--) {
				remainingVolume += boxes[i].getVolume();
				remainingVolumes[i] = remainingVolume;
			}
		}
		if(length == 0) {
			return;
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[0], iterator.getMinBoxVolume(0));

		int level = 0;
		parents[0] = -1;
		placedCounts[0] = 0;
		placedVolumes[0] = 0L;
		freeLoadWeights[0] = container.getMaxLoadWeight();
		maxContainerPriorities[0] = maxContainerPriority;
		boolean descend = true;
		while(true) {
			if(descend) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int placedCount = placedCounts[level];
				long placedVolume = placedVolumes[level];
				if(skipping == null) {
					// the boxes placed so far: keep them if the longest arrangement so far
					updateBest(pointCalculator, placedRotations, rotations, placedCount);
				}
				if(level == length) {
					if(skipping != null && placedCount > 0 && skipping.canBeBetter(placedVolume)) {
						// every box is placed or skipped: keep the arrangement if the best so far
						skipping.offer(pointCalculator.getPoints(), placedPermutations, placedRotations, placedCount);
					}
					level = parents[level];
					descend = false;
					continue;
				}
				if(skipping != null && !skipping.canBeBetter(placedVolume + remainingVolumes[level])) {
					// cannot beat the best arrangement
					level = parents[level];
					descend = false;
					if(level == -1) {
						return;
					}
					continue;
				}
				Box box = boxes[level];
				// without skipping, the boxes up to the max packable count fit by volume and weight
				unplaced[level] = skipping != null && !(box.getBoxItem().getContainerPriority() <= maxContainerPriorities[level]
						&& box.getWeight() <= freeLoadWeights[level]
						&& placedVolume + box.getVolume() <= maxLoadVolume);
				if(!unplaced[level]) {
					// save the free points, so that each candidate of this level starts from them (see redo())
					pointCalculator.push();
					BoxStackValue stackValue = box.getStackValues()[0];
					rotationIndexes[level] = 0;
					placements[placedCount].setStackValue(stackValue);
					startCandidates(pointCalculator, pointFilter, fullSupport, frames, level, stack, stackValue);
				}
			} else if(!unplaced[level]) {
				// back from the next level: remove this level's placement
				Placement placement = placements[placedCounts[level]];
				if(utility != null) {
					placement.removeSupporteesAbove();
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				if(skipping != null ? skipping.count == length : pointCalculator.getBestStackIndex() >= length) {
					// the best arrangement places all boxes: unwind without trying more candidates
					pointCalculator.pop();
					level = parents[level];
					if(level == -1) {
						return;
					}
					continue;
				}
				// restore the free points to before this level's placement
				pointCalculator.redo();
			} else {
				// back from skipping this level's box
				level = parents[level];
				if(level == -1) {
					return;
				}
				continue;
			}

			int placedCount = placedCounts[level];
			if(!unplaced[level]) {
				// find the next candidate for this level's box: a rotation and a position where the box can be inserted
				// after the boxes placed before it and, with load constraints, is carried by the boxes below
				Placement placement = placements[placedCount];
				BoxStackValue[] stackValues = boxes[level].getStackValues();
				BoxStackValue stackValue = placement.getStackValue();
				int insertAfterCount = insertAfterCounts == null ? placedCount : insertAfterCounts.length == 0 ? 0 : insertAfterCounts[level];
				FullSupportCandidates candidates = fullSupport ? frames.getFullSupportCandidates(level) : null;
				int pointIndex = -1;
				SimplePoint3D point = null;
				long supportedArea = 0L;
				while(true) {
					int candidate = nextCandidate(pointCalculator, pointFilter, candidates, frames, level, stackValue);
					if(candidate == -1) {
						// no more positions for this rotation: the next rotation
						int rotationIndex = rotationIndexes[level] + 1;
						if(rotationIndex == stackValues.length) {
							break;
						}
						rotationIndexes[level] = rotationIndex;
						stackValue = stackValues[rotationIndex];
						placement.setStackValue(stackValue);
						startCandidates(pointCalculator, pointFilter, fullSupport, frames, level, stack, stackValue);
						continue;
					}
					SimplePoint3D candidatePoint = candidates != null ? candidates.getPoint(candidate) : pointCalculator.get(candidate);
					if(insertAfterCount > 0 && !isInsertableAfter(candidatePoint, stackValue, stack, insertAfterCount, access)) {
						continue;
					}
					if(checkObstacles && !isInsertable(candidatePoint, stackValue, obstacles, access)) {
						continue;
					}
					if(checkExtraction && !isExtractable(candidatePoint, stackValue, stack, access)) {
						continue;
					}
					if(utility != null) {
						// -1 if the boxes below cannot carry the box at this position
						utility.populatePointSupporters(candidatePoint);
						utility.populatePointSupportees(candidatePoint, stackValue.getDz(), stackValue.getDz());
						supportedArea = utility.getSupportedAreaAtPoint(candidatePoint, stackValue, false);
						if(supportedArea == -1L) {
							continue;
						}
					}
					pointIndex = candidates != null ? candidates.getPointIndex(candidate) : candidate;
					point = candidatePoint;
					break;
				}
				if(pointIndex != -1) {
					// place the box and continue with the next level
					placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
					if(utility != null) {
						// results link the loads of the placements they build their stack from (see
						// BruteForceIntermediatePackagerResult#calculateStack), and searches reuse the placements
						placement.clearLoad();
						placement.setIndex(stack.size());
						placement.setSupportedArea(supportedArea);
					}
					pointCalculator.add(pointIndex, placement, point);
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					int nextLevel = level + 1;
					if(nextLevel < length) {
						if(minAreas[nextLevel] != minAreas[level]) {
							pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[nextLevel], iterator.getMinBoxVolume(nextLevel));
						} else {
							pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
						}
					}
					placedPermutations[placedCount] = boxes[level].getBoxItem().getLocalIndex();
					placedRotations[placedCount] = rotationIndexes[level];
					enter(nextLevel, level, placedCount + 1, placedVolumes[level] + stackValue.getBox().getVolume(), freeLoadWeights[level] - stackValue.getBox().getWeight(),
							maxContainerPriorities[level], parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
					level = nextLevel;
					descend = true;
					continue;
				}
				// no more candidates: undo this level's push
				pointCalculator.pop();
				unplaced[level] = true;
			}

			if(skipping == null) {
				// without skipping, the arrangement ends at this box
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}

			// skip this level's box: it waits for a later container, and so do the boxes of a higher container priority
			int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
			if(skipEnd == -1) {
				// cannot skip here (inside a box item group)
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}
			int skippedContainerPriority = Math.min(maxContainerPriorities[level], boxes[level].getBoxItem().getContainerPriority());
			enter(skipEnd, level, placedCount, placedVolumes[level], freeLoadWeights[level], skippedContainerPriority,
					parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
			level = skipEnd;
			descend = true;
		}
	}

	/** Start the candidate positions of a level's box in a rotation (see {@link #nextCandidate}) */
	private static void startCandidates(PointCalculator3DStack pointCalculator, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, BruteForceSearchFrames frames,
			int level, Stack stack, BoxStackValue stackValue) {
		if(fullSupport) {
			frames.getFullSupportCandidates(level).populate(pointCalculator, getPoints(pointFilter, pointCalculator, stackValue), stack.getPlacements(), stackValue);
			frames.nextPointIndexes[level] = 0;
		} else if(pointFilter == null) {
			frames.pointCounts[level] = pointCalculator.size();
			frames.nextPointIndexes[level] = 0;
		} else {
			frames.pointIterators[level] = pointFilter.getPoints(pointCalculator, stackValue);
		}
	}

	/**
	 * @return the next candidate position of a level's box: a point index (where the box fits), or with full support an
	 *         index in the candidates; or -1 if none
	 */
	private static int nextCandidate(PointCalculator3DStack pointCalculator, BruteForcePointIteratorFilter pointFilter, FullSupportCandidates candidates,
			BruteForceSearchFrames frames, int level, BoxStackValue stackValue) {
		if(candidates != null) {
			int next = frames.nextPointIndexes[level];
			if(next < candidates.size()) {
				frames.nextPointIndexes[level] = next + 1;
				return next;
			}
			return -1;
		}
		if(pointFilter == null) {
			int k = frames.nextPointIndexes[level];
			int count = frames.pointCounts[level];
			while(k < count && !pointCalculator.get(k).fits3D(stackValue)) {
				k++;
			}
			if(k < count) {
				frames.nextPointIndexes[level] = k + 1;
				return k;
			}
			return -1;
		}
		IntIterator pointIterator = frames.pointIterators[level];
		return pointIterator.hasNext() ? pointIterator.next() : -1;
	}

	/**
	 * @param rotations the rotations of the placed boxes, in the iterator's order
	 * @return the iterator's permutation, with the rotations of the placed boxes
	 */
	protected static PermutationRotationState getState(BoxItemPermutationRotationIterator iterator, int[] rotations, int count) {
		PermutationRotationState state = iterator.getState();
		System.arraycopy(rotations, 0, state.getRotations(), 0, count);
		return state;
	}

	/** @return the point filter's points for the box, or null for all points */
	protected static IntIterator getPoints(BruteForcePointIteratorFilter pointFilter, PointCalculator3DStack pointCalculator, BoxStackValue stackValue) {
		return pointFilter == null ? null : pointFilter.getPoints(pointCalculator, stackValue);
	}

	protected boolean acceptAsFull(BruteForceIntermediatePackagerResult result, Container holder) {
		return result.getLoadVolume() == holder.getMaxLoadVolume();
	}
	
	@Override
	protected BruteForceIntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return BruteForceIntermediatePackagerResult.EMPTY;
	}
}
