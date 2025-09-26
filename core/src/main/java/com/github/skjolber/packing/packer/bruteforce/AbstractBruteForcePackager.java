package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
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
					adapter = createBoxItemAdapter(items, priority, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = createBoxItemGroupAdapter(itemGroups, priority, new ContainerItemsCalculator(containers), interrupt);
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

	protected abstract AbstractBruteForceBoxItemPackagerAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups, BoxPriority priority,
			ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt);

	protected abstract AbstractBruteForceBoxItemPackagerAdapter createBoxItemAdapter(List<BoxItem> items, BoxPriority priority,
			ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt);

	static List<Placement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<Placement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new Placement());
		}
		return placements;
	}

	public BruteForceIntermediatePackagerResult pack(ExtremePoints3DStack extremePoints, List<Placement> stackPlacements, ContainerItem containerItem, int index,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {

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

				List<Point> points = packStackPlacement(extremePoints, stackPlacements, iterator, stack, holder, interrupt, minStackableAreaIndex);
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

	public List<Point> packStackPlacement(ExtremePoints3DStack extremePoints, List<Placement> placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex) throws PackagerInterruptedException {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		int maxLoadWeight = container.getMaxLoadWeight();

		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));

		try {
			// note: currently implemented as a recursive algorithm
			return packStackPlacement(extremePoints, placements, iterator, stack, maxLoadWeight, 0, interrupt, minStackableAreaIndex, Collections.emptyList());
		} catch (StackOverflowError e) {
			// TODO throw packager exception
			
			LOGGER.warning("Stack overflow occoured for " + placements.size() + " boxes. Limit number of boxes or increase thread stack");
			return null;
		}
	}

	private List<Point> packStackPlacement(
			ExtremePoints3DStack extremePointsStack, 
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

		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return null;
		}

		Placement placement = placements.get(placementIndex);

		placement.setStackValue(stackValue);

		maxLoadWeight -= stackValue.getBox().getWeight();

		if(extremePointsStack.getStackIndex() > best.size()) {
			best = extremePointsStack.getPoints();
		}

		extremePointsStack.push();

		int currentPointsCount = extremePointsStack.size();

		for (int k = 0; k < currentPointsCount; k++) {
			Point point3d = extremePointsStack.get(k);

			if(!point3d.fits3D(stackValue)) {
				continue;
			}

			placement.setPoint(point3d);

			extremePointsStack.add(k, placement);

			if(placementIndex + 1 >= rotator.length()) {
				best = extremePointsStack.getPoints();

				break;
			}

			stack.add(placement);

			// should minimum area / volume be adjusted?
			int nextMinStackableAreaIndex;

			boolean minArea = placementIndex == minStackableAreaIndex;
			if(minArea) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);

				extremePointsStack.setMinimumAreaAndVolumeLimit(rotator.getStackValue(nextMinStackableAreaIndex).getArea(), rotator.getMinBoxVolume(placementIndex + 1));
			} else {
				extremePointsStack.setMinimumVolumeLimit(rotator.getMinBoxVolume(placementIndex + 1));

				nextMinStackableAreaIndex = minStackableAreaIndex;
			}

			List<Point> points = packStackPlacement(
					extremePointsStack, 
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
			extremePointsStack.redo();
		}

		extremePointsStack.pop();

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
