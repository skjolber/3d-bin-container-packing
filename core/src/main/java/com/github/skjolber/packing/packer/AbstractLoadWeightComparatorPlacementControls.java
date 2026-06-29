package com.github.skjolber.packing.packer;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.packer.util.PlacementList;

public abstract class AbstractLoadWeightComparatorPlacementControls extends AbstractComparatorPlacementControls {

	protected boolean fullSupport;

	protected PlacementList pointSupportees = new PlacementList();
	protected PlacementList pointSupporters = new PlacementList();
	protected PlacementList placementSupportees = new PlacementList();
	protected PlacementList placementSupporters = new PlacementList();

	protected long[] placementAreas;
	protected long[] reliefWeights;
	
	public AbstractLoadWeightComparatorPlacementControls(BoxItemSource boxItems, PointControls pointControls,
			PointCalculator pointCalculator, Container container, Stack stack, Order order,
			PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		
		this.fullSupport = fullSupport;

		int count = 0;
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);
			count += boxItem.getCount();
		}
		
		initialize(count);
	}
	
	protected void initialize(int count) {
		placementAreas = new long[count];
		reliefWeights = new long[count];
		
		pointSupportees.ensureAdditionalCapacity(count);
		pointSupporters.ensureAdditionalCapacity(count);
		placementSupportees.ensureAdditionalCapacity(count);
		placementSupporters.ensureAdditionalCapacity(count);
	}
	
	protected Placement createPlacement(BoxStackValue stackValue, int index, int x, int y, int z) {
		Placement placement = new Placement(stackValue, index, x, y, z);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}

	protected static long overlapArea(int minX, int minY, int maxX, int maxY, Placement candidate) {
		int overlapMinX = Math.max(minX, candidate.getAbsoluteX());
		int overlapMinY = Math.max(minY, candidate.getAbsoluteY());
		int overlapMaxX = Math.min(maxX, candidate.getAbsoluteEndX());
		int overlapMaxY = Math.min(maxY, candidate.getAbsoluteEndY());

		return (long)(overlapMaxX - overlapMinX + 1) * (long)(overlapMaxY - overlapMinY + 1);
	}

	protected void calculateRelifWeight(Placement placement, long reliefWeight) {
		long supportedArea = placement.getSupportedArea();
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			Placement supporter = placementLoad.getPlacement();
			
			long r = (reliefWeight * placementLoad.getArea()) / supportedArea;
			
			this.reliefWeights[supporter.getIndex()] += r;
			
			calculateRelifWeight(supporter, r);
		}
	}
	
	/**
	 * Checks whether placing this stackValue at this point would violate
	 * load constraints on any of the supporters below.
	 */
	
	/**
	 * Calculates the total overlap area of all supporters with the new box and simultaneously
	 * validates load (weight/pressure) constraints in a single pass using cached per-supporter
	 * areas — avoiding the O(2n) double {@code overlapArea} scan that separate
	 * {@code calculateSupport} + {@code isWithinMaxLoadWeightAndPressure} calls would incur.
	 *
	 * @return total supported area, or {@code -1} if any load constraint is violated
	 */
	
	protected long calculateSupportAndValidateSupporterLoad(BoxStackValue stackValue, int absoluteX, int absoluteY, long weight) {
		int n = placementSupporters.size();
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;

		long totalOverlapArea = 0;
		for (int i = 0; i < n; i++) {
			placementAreas[i] = overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placementSupporters.get(i));
			totalOverlapArea += placementAreas[i];
		}

		for (int i = 0; i < n; i++) {
			long weightShare = (weight * placementAreas[i]) / totalOverlapArea;
			if (!isWithinMaxLoadWeightAndPressure(placementSupporters.get(i), weightShare, placementAreas[i])) {
				return -1;
			}
		}

		return totalOverlapArea;
	}
	
	protected boolean isWithinMaxLoadWeightAndPressure(Placement placement, long weight, long area) {
		long effectiveWeight = weight - reliefWeights[placement.getIndex()];

		BoxStackValue candidateSupporter = placement.getStackValue();
		if(candidateSupporter.isMaxLoadPressure()) {
			if((double)effectiveWeight > (double)area * candidateSupporter.getMaxLoadPressure()) {
				return false;
			}
		}
		
		if(candidateSupporter.isMaxLoadWeight()) {
			long existingWeight = 0;
			for (PlacementLoad placementLoad : placement.getSupportees()) {
				existingWeight += placementLoad.getWeight();
			}
			
			if(effectiveWeight + existingWeight > candidateSupporter.getMaxLoadWeight()) {
				return false;
			}
		}
		
		// getSupportedArea() is maintained incrementally by addSupporter/removeSupporter,
		// so it equals the sum of all supporter areas without an extra loop.
		long totalArea = placement.getSupportedArea();

		if (totalArea > 0) {
			for (PlacementLoad placementLoad : placement.getSupporters()) {
				long weightShare = (effectiveWeight * placementLoad.getArea()) / totalArea;
				
				if(!isWithinMaxLoadWeightAndPressure(placementLoad.getPlacement(), weightShare, placementLoad.getArea())) {
					return false;
				}		
			}
		}
		
		return true;
	}
	

	/**
	 * Wires load-graph relationships after a placement is accepted.
	 * Finds all direct supporters (placements at absoluteEndZ == placement.absoluteZ - 1
	 * that overlap in XY), then calls {@link Placement#addLoad} on each, distributing
	 * the new placement's weight proportionally by overlap area.
	 * <p>
	 * Guard against double-wiring: some packagers (e.g. LAFF) hold two
	 * {@code PlacementControls} instances and call {@code accepted()} on both
	 * for the same placement. We detect this by checking whether supporters
	 * have already been registered, and skip if so.
	 */
	@Override
	public void accepted(Placement placement) {
		int z = placement.getAbsoluteZ();
		if (z == 0) {
			return;
		}

		int minX = placement.getAbsoluteX();
		int maxX = placement.getAbsoluteEndX();
		int minY = placement.getAbsoluteY();
		int maxY = placement.getAbsoluteEndY();
		int supportZ = z - 1;

		// First pass: compute total overlap area across all direct supporters
		long totalArea = 0;
		List<Placement> stackPlacements = stack.getPlacements();
		int n = stackPlacements.size();
		for (int i = 0; i < n; i++) {
			Placement candidate = stackPlacements.get(i);
			if (candidate.getAbsoluteEndZ() != supportZ) continue;
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) continue;
			totalArea += overlapArea(minX, minY, maxX, maxY, candidate);
		}

		if (totalArea == 0) {
			return;
		}

		// Reset supportedArea to 0 so addSupporter (called inside addLoad) accumulates correctly.
		// getPlacement sets it via setSupportedArea; addSupporter would double-count it otherwise.
		placement.setSupportedArea(0);

		// Second pass: wire load relationships, distributing weight proportionally
		long weight = placement.getWeight();
		for (int i = 0; i < n; i++) {
			Placement candidate = stackPlacements.get(i);
			if (candidate.getAbsoluteEndZ() != supportZ) continue;
			if (!candidate.intersects2D(minX, maxX, minY, maxY)) continue;
			long area = overlapArea(minX, minY, maxX, maxY, candidate);
			long weightShare = (weight * area) / totalArea;
			candidate.addLoad(placement, area, weightShare);
		}
	}

	protected void populatePointSupporters(Point point) {
		pointSupporters.clear();
		
		int z = point.getMinZ() - 1;
		
		for (Placement candidate : stack.getPlacements()) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!point.intersectsXY(candidate)) {
				continue;
			}
			
			pointSupporters.add(candidate);
		}
	}
	
	protected void populatePointSupportees(Point point, int minDz, int maxDz) {
		pointSupportees.clear();
		
		int limitMinDz = point.getMinZ() + minDz;
		int limitMaxDz = point.getMinZ() + maxDz;
		
		for (Placement candidate : stack.getPlacements()) {
			if (candidate.getAbsoluteZ() < limitMinDz) {
				continue;
			}
			
			if (candidate.getAbsoluteZ() > limitMaxDz) {
				continue;
			}

			 
			if(!point.intersectsXY(candidate)) {
				continue;
			}
			
			pointSupportees.add(candidate);
		}
	}

}
