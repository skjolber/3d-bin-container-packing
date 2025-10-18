package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public abstract class AbstractBruteForcePackager extends AbstractPackager<BruteForceIntermediatePackagerResult, AbstractBruteForcePackager.BruteForcePackagerResultBuilder> {

	private static final Logger LOGGER = Logger.getLogger(AbstractBruteForcePackager.class.getName());
	
	public AbstractBruteForcePackager(Comparator<BruteForceIntermediatePackagerResult> comparator) {
		super(comparator);
	}
	
	public class BruteForcePackagerResultBuilder extends AbstractPackagerResultBuilder<BruteForcePackagerResultBuilder> {
	
		private AbstractBruteForcePackager packager;
	
		public BruteForcePackagerResultBuilder withPackager(AbstractBruteForcePackager packager) {
			this.packager = packager;
			return this;
		}
	
		@Override
		protected void validate() {
			super.validate();
			
			for(ControlledContainerItem container : containers) {
				if(container.hasControls()) {
					throw new IllegalStateException("Controls not supported");
				}
			}
			
			if(order != Order.NONE) {
				throw new IllegalStateException("Order not supported for brute force packager");
			}
		}
		
		public PackagerResult build() {
			validate();
			
			long start = System.currentTimeMillis();
	
			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}
			
			booleanSupplierBuilder.withScheduledThreadPoolExecutor(packager.getScheduledThreadPoolExecutor());
	
			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			try {
				
				AbstractBruteForceBoxItemPackagerAdapter adapter;
				if(items != null && !items.isEmpty()) {
					adapter = createBoxItemAdapter(items, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = createBoxItemGroupAdapter(itemGroups, new ContainerItemsCalculator(containers), interrupt);
				}
				List<Container> packList = packAdapter(maxContainerCount, interrupt, adapter);
								
				long duration = System.currentTimeMillis() - start;
				if(packList == null) {
					return new PackagerResult(Collections.emptyList(), duration, true);
				}
				return new PackagerResult(packList, duration, false);
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true);
			} finally {
				interrupt.close();
			}
		}
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withPackager(this);
	}

	protected abstract AbstractBruteForceBoxItemPackagerAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups, ContainerItemsCalculator defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt);

	protected abstract AbstractBruteForceBoxItemPackagerAdapter createBoxItemAdapter(List<BoxItem> items, ContainerItemsCalculator defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt);

	static List<Placement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<Placement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new Placement());
		}
		return placements;
	}

	public BruteForceIntermediatePackagerResult pack(PointCalculator3DStack pointCalculator, List<Placement> stackPlacements, ControlledContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, boolean abortOnAnyBoxTooBig, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {

		Container holder = containerItem.getContainer().clone();
		
		Stack stack = holder.getStack();
		
		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), index, iterator);
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), index, iterator);

		// iterator over all permutations
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// iterate over all rotations
			bestPermutationResult.reset();

			do {
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

				List<Point> points = packStackPlacement(pointCalculator, stackPlacements, iterator, stack, holder, interrupt, minStackableAreaIndex, containerItem.getInitialPoints());
				if(points == null) {
					return null; // stack overflow
				}
				
				stack.clear();
				
				if(points.size() > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(points, iterator.getState(), stackPlacements);
					if(points.size() == iterator.length()) {
						// best possible result for this container
						return bestPermutationResult;
					}
				}

				// search for the next rotation which actually
				// has a chance of affecting the result.
				// i.e. if we have four boxes, and two boxes could be placed with the
				// current rotations, and the new rotation only changes the rotation of box 4,
				// then we know that attempting to stack again will not work
				// since box 1, 2 and 3 are still the same as in the previous permutation.

				int rotationIndex = iterator.nextRotation(points.size());

				if(rotationIndex == -1) {
					// no more rotations, continue to next permutation
					break;
				}
			} while (true);

			int permutationIndex = iterator.nextPermutation(bestPermutationResult.getSize());

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result
				
				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) == ARGUMENT_2_IS_BETTER) {
					// switch the two results for one another
					BruteForceIntermediatePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}
			
			// search for the next permutation which actually 
			// has a chance of affecting the result.

			// TODO can bounds on weight or volume be used to determine whether it is even possible to beat the best result?

			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		if(bestResult != null) {
			bestResult.markDirty();
		}

		return bestResult;
	}

	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, List<Placement> placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points) throws PackagerInterruptedException {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		int maxLoadWeight = container.getMaxLoadWeight();

		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));
		try {
			// note: currently implemented as a recursive algorithm
			return packStackPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight, 0, interrupt, minStackableAreaIndex, Collections.emptyList());
		} catch (StackOverflowError e) {
			// TODO throw packager exception
			
			LOGGER.warning("Stack overflow occoured for " + placements.size() + " boxes. Limit number of boxes or increase thread stack");
			return null;
		}
	}

	private List<Point> packStackPlacement(
			PointCalculator3DStack pointCalculatorStack, 
			List<Placement> placements, 
			BoxItemPermutationRotationIterator rotator, 
			Stack stack,
			int maxLoadWeight, 
			int placementIndex, 
			PackagerInterruptSupplier interrupt, 
			int minStackableAreaIndex,
			// optimize: pass best along so that we do not need to get points to known whether extracting the points is necessary
			List<Point> best
		) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		BoxStackValue stackValue = rotator.getStackValue(placementIndex);

		// TODO move this check and do not return null
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return null;
		}
		
		// TODO check remaining box size volumes against max point volume
		// if we are required to fit all boxes
		// and space is getting slim (i.e. 75% etc)

		Placement placement = placements.get(placementIndex);

		placement.setStackValue(stackValue);

		maxLoadWeight -= stackValue.getBox().getWeight();

		if(pointCalculatorStack.getStackIndex() > best.size()) {
			best = pointCalculatorStack.getPoints();
		}

		pointCalculatorStack.push();

		int currentPointsCount = pointCalculatorStack.size();

		for (int k = 0; k < currentPointsCount; k++) {
			Point point3d = pointCalculatorStack.get(k);

			if(!point3d.fits3D(stackValue)) {
				continue;
			}

			placement.setPoint(point3d);

			pointCalculatorStack.add(k, placement);

			if(placementIndex + 1 >= rotator.length()) {
				best = pointCalculatorStack.getPoints();

				break;
			}

			stack.add(placement);

			// should minimum area / volume be adjusted?
			int nextMinStackableAreaIndex;

			boolean minArea = placementIndex == minStackableAreaIndex;
			if(minArea) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);

				pointCalculatorStack.setMinimumAreaAndVolumeLimit(rotator.getStackValue(nextMinStackableAreaIndex).getArea(), rotator.getMinBoxVolume(placementIndex + 1));
			} else {
				pointCalculatorStack.setMinimumVolumeLimit(rotator.getMinBoxVolume(placementIndex + 1));

				nextMinStackableAreaIndex = minStackableAreaIndex;
			}
			
			// TODO abortOnAnyBoxTooBig: is there still space to pack the largest box item
			// or can we just give up here already?

			List<Point> points = packStackPlacement(
					pointCalculatorStack, 
					placements, 
					rotator, 
					stack, 
					maxLoadWeight, 
					placementIndex + 1, 
					interrupt, 
					nextMinStackableAreaIndex, 
					best);

			stack.remove(placement);

			if(points != null) {
				if(points.size() >= rotator.length()) {
					best = points;
					break;
				}

				if(best.size() < points.size()) {
					best = points;
				}
			}
			pointCalculatorStack.redo();
		}

		pointCalculatorStack.pop();

		return best;
	}

	protected boolean acceptAsFull(BruteForceIntermediatePackagerResult result, Container holder) {
		return result.getLoadVolume() == holder.getMaxLoadVolume();
	}
	
	@Override
	protected BruteForceIntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return BruteForceIntermediatePackagerResult.EMPTY;
	}
}
