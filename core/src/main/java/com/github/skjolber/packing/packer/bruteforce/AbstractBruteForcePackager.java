package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.points3d.Point3D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. 
 * This implementation tries all permutations, rotations and points.
 * <br><br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public abstract class AbstractBruteForcePackager extends AbstractPackager<BruteForcePackagerResult, BruteForcePackagerResultBuilder> {
	
	public AbstractBruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
	
	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new StackPlacement());
		}
		return placements;
	}

	public BruteForcePackagerResult pack(ExtremePoints3DStack extremePoints, List<StackPlacement> stackPlacements, Container targetContainer, PermutationRotationIterator rotator, BooleanSupplier interrupt) {
		Container holder = new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), targetContainer.getStackValues(), new DefaultStack());
		
		BruteForcePackagerResult result = new BruteForcePackagerResult(holder, rotator);

		// iterator over all permutations
		do {
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			do {
				List<Point3D> points = packStackPlacement(extremePoints, stackPlacements, rotator, holder, interrupt);
				if (points == null) {
					return null; // timeout
				}
				if (points.size() == rotator.length()) {
					// best possible result for this container
					result.setState(points, rotator.getState(), stackPlacements.subList(0, points.size()), points.size() == stackPlacements.size());
					return result;
				} else if (points.size() > 0) {
					// continue search, but see if this is the best fit so far
					if (points.size() > result.getSize()) {
						result.setState(points, rotator.getState(), stackPlacements.subList(0, points.size()), points.size() == stackPlacements.size());
					}
				}

				holder.getStack().clear();

				int diff = rotator.nextRotation();
				if(diff == -1) {
					// no more rotations, continue to next permutation
					break;
				}
			} while (true);
		} while (rotator.nextPermutation() != -1);
		
		return result;
	}

	protected List<Point3D> packStackPlacement(ExtremePoints3DStack extremePoints, List<StackPlacement> placements, PermutationRotationIterator iterator, Container container) {
		return packStackPlacement(extremePoints, placements, iterator, container, BooleanSupplierBuilder.NOOP);
	}

	public List<Point3D> packStackPlacement(ExtremePoints3DStack extremePoints, List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, BooleanSupplier interrupt) {
		if (placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		ContainerStackValue[] stackValues = container.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];

		int maxLoadWeight = containerStackValue.getMaxLoadWeight();
		
		extremePoints.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		
		return packStackPlacement(extremePoints, placements, rotator, container, maxLoadWeight, 0, interrupt, containerStackValue.getConstraint());
	}
	
	private List<Point3D> packStackPlacement(ExtremePoints3DStack extremePointsStack, List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, int maxLoadWeight, int placementIndex, BooleanSupplier interrupt, StackConstraint constraint) {
		if (interrupt.getAsBoolean()) {
			// fit2d below might have returned due to deadline
			return null;
		}
		
		PermutationRotation permutationRotation = rotator.get(placementIndex);
		
		Stackable stackable = permutationRotation.getStackable();
		if (stackable.getWeight() > maxLoadWeight) {
			return null;
		}
		
		Stack stack = container.getStack();
		if(constraint != null && !constraint.accepts(stack, stackable)) {
			return extremePointsStack.getPoints();
		}

		StackPlacement placement = placements.get(placementIndex);
		StackValue stackValue = permutationRotation.getValue();

		placement.setStackable(stackable);
		placement.setStackValue(stackValue);

		maxLoadWeight -= stackable.getWeight();

		List<Point3D> best = extremePointsStack.getPoints();

		extremePointsStack.push();

		List<Point3D> currentPoints = extremePointsStack.getValues();
		for(int k = 0; k < currentPoints.size(); k++) {
			Point3D point3d = currentPoints.get(k);
			if(!point3d.fits3D(stackValue)) {
				continue;
			}
			if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
				continue;
			}

			placement.setX(point3d.getMinX());
			placement.setY(point3d.getMinY());
			placement.setZ(point3d.getMinZ());

			extremePointsStack.add(k, placement);

			if(placementIndex + 1 >= rotator.length()) {
				best = extremePointsStack.getPoints();

				break;
			}

			stack.add(placement);

			List<Point3D> points = packStackPlacement(extremePointsStack, placements, rotator, container, maxLoadWeight, placementIndex + 1, interrupt, constraint);
			
			stack.remove(placement);

			if(points != null) {
				if(points.size() >= rotator.length()) {
					best = points;
					break;
				}
				
				if(best == null || best.size() < points.size()) {
					best = points;
				}
			}
			extremePointsStack.redo();
		}
		
		extremePointsStack.pop();
		
		return best;
	}


}
