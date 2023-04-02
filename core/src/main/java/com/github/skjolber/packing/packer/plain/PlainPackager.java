package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResult;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractPlainPackager<Point3D<StackPlacement>> {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		public PlainPackager build() {
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new PlainPackager(checkpointsPerDeadlineCheck, packResultComparator);
		}
	}

	public PlainPackager(int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		super(checkpointsPerDeadlineCheck, packResultComparator);
	}

	public DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);

		ContainerStackValue[] stackValues = targetContainer.getStackValues();

		ContainerStackValue containerStackValue = stackValues[0];

		StackConstraint constraint = containerStackValue.getConstraint();

		DefaultStack stack = new DefaultStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter(s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));

		int maxRemainingWeight = containerStackValue.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && maxRemainingWeight > 0 && !scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			long maxPointVolume = extremePoints3D.getMaxVolume();
			long maxPointArea = extremePoints3D.getMaxArea();

			int bestPointIndex = -1;
			int bestIndex = -1;

			StackValue bestStackValue = null;
			Stackable bestStackable = null;

			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getVolume() > maxPointVolume) {
					continue;
				}
				if(box.getWeight() > maxRemainingWeight) {
					continue;
				}

				if(bestStackable != null && !isBetter(bestStackable, box)) {
					continue;
				}

				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}

				for (StackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
					if(stackValue.getVolume() > maxPointVolume) {
						continue;
					}

					int currentPointsCount = extremePoints3D.getValueCount();
					for (int k = 0; k < currentPointsCount; k++) {
						Point3D<StackPlacement> point3d = extremePoints3D.getValue(k);

						if(!point3d.fits3D(stackValue)) {
							continue;
						}

						if(bestIndex != -1) {
							if(!isBetter(bestStackable, extremePoints3D.getValue(bestPointIndex), bestStackValue, box, point3d, stackValue)) {
								continue;
							}

						}
						if(constraint != null && !constraint.supports(stack, box, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
							continue;
						}
						bestPointIndex = k;
						bestIndex = i;
						bestStackValue = stackValue;
						bestStackable = box;
					}
				}
			}

			if(bestIndex == -1) {
				break;
			}

			scopedStackables.remove(bestIndex);
			remainingStackables.remove(bestStackable);

			Point3D<StackPlacement> point = extremePoints3D.getValue(bestPointIndex);

			StackPlacement stackPlacement = new StackPlacement(bestStackable, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ(), -1, -1);
			stack.add(stackPlacement);
			extremePoints3D.add(bestPointIndex, stackPlacement);

			if(!scopedStackables.isEmpty()) {
				boolean minArea = bestStackValue.getArea() == extremePoints3D.getMinAreaLimit();
				boolean minVolume = extremePoints3D.getMinVolumeLimit() == bestStackable.getVolume();
				if(minArea && minVolume) {
					extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));
				} else if(minArea) {
					extremePoints3D.setMinimumAreaLimit(getMinStackableArea(scopedStackables));
				} else if(minVolume) {
					extremePoints3D.setMinimumVolumeLimit(getMinStackableVolume(scopedStackables));
				}
			}

			maxRemainingWeight -= bestStackable.getWeight();
		}

		return new DefaultPackResult(new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack),
				stack, remainingStackables.isEmpty(), index);
	}

	protected boolean isBetter(Stackable referenceStackable, Point3D<StackPlacement> referencePoint, StackValue referenceStackValue, Stackable candidateBox, Point3D<StackPlacement> candidatePoint,
			StackValue candidateStackValue) {
		// ********************************************
		// * Prefer lowest point
		// ********************************************

		// compare supported area
		long referenceSupport = referencePoint.calculateXYSupport(referenceStackValue.getDx(), referenceStackValue.getDy());
		long candidateSupport = candidatePoint.calculateXYSupport(candidateStackValue.getDx(), candidateStackValue.getDy());

		if(candidateSupport == referenceSupport) {

			if(candidatePoint.getMinZ() == referencePoint.getMinZ()) {

				if(candidateStackValue.getArea() == referenceStackValue.getArea()) {

					// compare sideways support	
					referenceSupport = referencePoint.calculateXZSupport(referenceStackValue.getDx(), referenceStackValue.getDz()) +
							referencePoint.calculateYZSupport(referenceStackValue.getDy(), referenceStackValue.getDz());

					candidateSupport = candidatePoint.calculateXZSupport(candidateStackValue.getDx(), candidateStackValue.getDz()) +
							candidatePoint.calculateYZSupport(candidateStackValue.getDy(), candidateStackValue.getDz());

					if(candidateSupport == referenceSupport) {
						// if everything is equal, the point with the tightest fit 
						return candidatePoint.getArea() < referencePoint.getArea();
					}
					return candidateSupport > referenceSupport;
				}
				return candidateStackValue.getArea() > referenceStackValue.getArea();

			}
			return candidatePoint.getMinZ() < referencePoint.getMinZ();
		}
		return candidateSupport > referenceSupport;
	}

	protected boolean isBetter(Stackable referenceStackable, Stackable potentiallyBetterStackable) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************

		if(referenceStackable.getVolume() == potentiallyBetterStackable.getVolume()) {
			return referenceStackable.getWeight() < potentiallyBetterStackable.getWeight();
		}
		return referenceStackable.getVolume() < potentiallyBetterStackable.getVolume();
	}

	@Override
	public PlainPackagerResultBuilder newResultBuilder() {
		return new PlainPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
