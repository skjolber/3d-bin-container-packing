package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultContainerStackValue;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.PlacementComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackValueComparator;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableComparator;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.PackResult;
import com.github.skjolber.packing.points2d.ExtremePoints2D;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public class LargestAreaFitFirstPackager extends AbstractPackager<LargestAreaFitFirstPackagerResultBuilder> {

	protected LargestAreaFitFirstPackagerConfigurationBuilderFactory<?> factory;
	
	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param configurationBuilder 
	 * @param checkpointsPerDeadlineCheck 
	 */
	public LargestAreaFitFirstPackager(List<Container> containers, LargestAreaFitFirstPackagerConfigurationBuilderFactory<?> factory) {
		this(containers, 1, factory);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public LargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, LargestAreaFitFirstPackagerConfigurationBuilderFactory<?> factory) {
		super(containers, checkpointsPerDeadlineCheck);
		
		this.factory = factory;
	}

	/**
	 *
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param containerProducts list of boxes to fit in a container.
	 * @param targetContainer the container to fit within
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> stackables, Container targetContainer,  BooleanSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);
		
		ContainerStackValue[] stackValues = targetContainer.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		StackConstraint constraint = containerStackValue.getConstraint();
		
		LevelStack stack = new LevelStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter( s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter( s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints2D<StackPlacement> extremePoints2D = new ExtremePoints2D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy());

		LargestAreaFitFirstPackagerConfiguration configuration = factory.newBuilder().withContainer(targetContainer).withExtremePoints(extremePoints2D).withStack(stack).build();
		
		StackableComparator firstComparator = configuration.getFirstComparator();
		StackValueComparator<Point2D> firstStackValueComparator = configuration.getFirstStackValueComparator();
		
		StackableComparator nextComparator = configuration.getNextComparator();
		StackValueComparator<Point2D> nextStackValueComparator = configuration.getNextStackValueComparator();

		while(!scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}
			
			int maxWeight = stack.getFreeWeightLoad();
			int maxHeight = stack.getFreeLoadDz();

			Point2D value = extremePoints2D.getValue(0);
			
			int firstIndex = -1;
			StackValue firstStackValue = null;
			Stackable firstBox = null;
			
			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(firstBox == null || firstComparator.compare(box, firstBox) > 0) {
					if(constraint == null || constraint.accepts(stack, box)) {
						for (StackValue stackValue : box.getStackValues()) {
							if(stackValue.getDz() > maxHeight) {
								continue;
							}
							if(stackValue.fitsInside2D(containerStackValue.getLoadDx(), containerStackValue.getLoadDy())) {
								if(firstStackValue == null || firstStackValueComparator.compare(value, firstStackValue, value, stackValue) > 0) {
									if(constraint == null || constraint.supports(stack, box, stackValue, 0, 0, 0)) {
										firstIndex = i;
										firstStackValue = stackValue;
										firstBox = box;
									}
								}
							}
						}
					}
				}
			}

			if(firstIndex == -1) {
				break;
			}
			Stackable stackable = scopedStackables.remove(firstIndex);
			remainingStackables.remove(stackable);
			
			DefaultContainerStackValue levelStackValue = stack.getContainerStackValue(firstStackValue.getDz());
			Stack levelStack = new DefaultStack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, firstStackValue, 0, 0, 0, -1, -1);

			levelStack.add(first);
			
			int levelHeight = levelStackValue.getDz();

			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints2D.add(0, first);
			
			while(!extremePoints2D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				
				long maxPointArea = extremePoints2D.getMaxArea();
				long maxPointVolume = maxPointArea * levelHeight;
				
				int bestPointIndex = -1;
				int bestIndex = -1;
				StackValue bestStackValue = null;
				Stackable bestStackable = null;
				
				List<Point2D> points = extremePoints2D.getValues();
				for (int i = 0; i < scopedStackables.size(); i++) {
					Stackable box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(maxPointArea < box.getMinimumArea()) {
						continue;
					}

					if(bestStackValue == null || nextComparator.compare(box, bestStackable) > 0) {
						for (StackValue stackValue : box.getStackValues()) {
							if(stackValue.getArea() > maxPointArea) {
								continue;
							}
							if(levelHeight < stackValue.getDz()) {
								continue;
							}
							
							// pick the point with the lowest area
							int bestStackValuePointIndex = -1;
							
							for(int k = 0; k < points.size(); k++) {
								Point2D point2d = points.get(k);
								if(point2d.getArea() < stackValue.getArea()) {
									continue;
								}
								
								if(point2d.fits2D(stackValue)) {
									if(bestStackValuePointIndex == -1 || nextStackValueComparator.compare(point2d, stackValue, points.get(bestStackValuePointIndex), bestStackValue) > 0) {
										bestPointIndex = k;
										bestIndex = i;
										bestStackValue = stackValue;
										bestStackable = stackable;
									}
								}
							}
						}
					}
				}
				
				if(bestIndex == -1) {
					break;
				}
				
				Stackable remove = scopedStackables.remove(bestIndex);
				Point2D point = extremePoints2D.getValue(bestPointIndex);
				
				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), 0, -1, -1);
				levelStack.add(stackPlacement);
				extremePoints2D.add(bestPointIndex, stackPlacement);

				maxRemainingLevelWeight -= remove.getWeight();
				
				remainingStackables.remove(remove);
			}
			
			extremePoints2D.reset();
		}
		
		return new LargestAreaFitFirstPackagerResult(remainingStackables, stack, new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack));
	}

	private class LAFFAdapter implements Adapter {

		private List<Stackable> boxes;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public LAFFAdapter(List<StackableItem> boxItems, List<Container> container, BooleanSupplier interrupt) {
			this.containers = container;

			List<Stackable> boxClones = new ArrayList<>(boxItems.size() * 2);

			for(StackableItem item : boxItems) {
				Stackable box = item.getStackable();
				boxClones.add(box);
				for(int i = 1; i < item.getCount(); i++) {
					boxClones.add(box.clone());
				}
			}

			this.boxes = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public PackResult attempt(int index) {
			return LargestAreaFitFirstPackager.this.pack(boxes, containers.get(index), interrupt);
		}

		@Override
		public Container accepted(PackResult result) {
			LargestAreaFitFirstPackagerResult laffResult = (LargestAreaFitFirstPackagerResult)result;
			
			return laffResult.getContainer();
		}

		@Override
		public boolean hasMore(PackResult result) {
			LargestAreaFitFirstPackagerResult laffResult = (LargestAreaFitFirstPackagerResult)result;
			return !laffResult.getRemainingBoxes().isEmpty();
		}
	}

	@Override
	protected Adapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new LAFFAdapter(boxes, containers, interrupt);
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
