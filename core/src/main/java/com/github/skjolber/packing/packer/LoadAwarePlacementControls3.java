package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * A {@link PlacementControls} implementation that enforces load constraints
 * (weight, pressure, box count) during placement selection. It also handles
 * inter-point placement to align identical boxes vertically when the
 * identical-box-count constraint is active.
 * <p>
 * When a placement is selected, this class:
 * <ol>
 *   <li>Validates that the placement would not exceed load constraints on any supporter below.</li>
 *   <li>If the identical-only constraint is active, prefers points that align directly above
 *       existing placements of the same box type.</li>
 *   <li>Distributes load proportionally and propagates it through the support graph.</li>
 *   <li>If the new placement is inserted below existing placements (filling a gap), recalculates
 *       load shares for all implicated placements above.</li>
 * </ol>
 */
public class LoadAwarePlacementControls3 extends AbstractComparatorPlacementControls<Placement> {

	protected boolean fullSupport;

	/** Reusable buffer for per-supporter overlap areas; grows as needed. */
	private long[] placementAreas = new long[8];

	public LoadAwarePlacementControls3(BoxItemSource boxItems, 
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, Comparator<Placement> placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		
		this.fullSupport = fullSupport;
	}

	@Override
	public Placement getPlacement(int offset, int length) {
		Placement result = null;

		List<Placement> pointSupporters = new ArrayList<>();
		List<Placement> pointSupportees = new ArrayList<>();
		List<Placement> placementSupporters = new ArrayList<>();
		List<Placement> placementSupportees = new ArrayList<>();

		for (int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			Box box = boxItem.getBox();
			
			if (order == Order.NONE) {
				if (result != null && boxItemComparator != null && boxItemComparator.compare(result.getBoxItem(), boxItem) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
					continue;
				}
			}

			PointSource points = pointControls.getPoints(boxItem);

			for (Point point3d : points) {
				populatePointSupporters(point3d, pointSupporters, stack.getPlacements());
				
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					
					if (!point3d.fits3D(stackValue)) {
						continue;
					}
					
					if(point3d.getMinZ() == 0) {
						Placement placementResult = new Placement(stackValue, point3d);
						
						if (result != null && placementComparator != null && placementComparator.compare(result, placementResult) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
							continue;
						}
	
						result = placementResult;
					}
					
					if(!isWithinMaxLoadBoxCount(placementSupporters)) {
						continue;
					}
					
					long supportedArea = calculateSupportAndValidateLoad(placementSupporters, stackValue, point3d.getMinX(), point3d.getMinY());
					if(supportedArea < 0) {
						continue;
					}
					if(fullSupport && supportedArea != stackValue.getArea()) {
						continue;
					}
					
					Placement placement = new Placement(stackValue, point3d);
					placement.setSupportedArea(supportedArea);

					if(result != null && placementComparator.compare(result, placement) >= 0) {
						continue;
					}
					
					result = placement;
				}
			}

			if (order == Order.CRONOLOGICAL) {
				break;
			}
			if (order == Order.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}
		}
		
		if(result != null) {
			if(result.getAbsoluteZ() > 0) {
				applyLoad(result);
			}
			return result;
		}
		
		return result;
	}

	public static boolean canStackOneMore(Placement candidate) {
		return canStackLevels(candidate, 1);
	}

	public static boolean canStackLevels(Placement candidate, int levels) {
		BoxStackValue stackValue = candidate.getStackValue();
		if(stackValue.isMaxLoadBoxCount()) {
			if(stackValue.getMaxLoadBoxCount() < levels) {
				return false;
			}
		}
		
		levels++;
		for (PlacementLoad placementLoad : candidate.getSupporters()) {
			if(!canStackLevels(placementLoad.getPlacement(), levels)) {
				return false;
			}
		}
		
		return true;
	}

	public static boolean isWithinMaxLoadBoxCount(List<Placement> supporters) {
		for (Placement candidate : supporters) {
			if(!canStackOneMore(candidate)) {
				return false;
			}
		}
		return true;
	}

	public void populatePointSupportees(Point point, List<Placement> result, List<Placement> pointSupports) {
		result.clear();
		
		for (Placement candidate : pointSupports) {
			if (point.getMinZ() < candidate.getAbsoluteZ() && candidate.getAbsoluteZ() <= point.getMaxZ()) {
				continue;
			}
			
			if(!point.intersects(candidate)) {
				continue;
			}
			
			result.add(candidate);
		}
	}
	
	public static boolean populatePointSupporters(Point point, List<Placement> result, List<Placement> pointSupports) {
		result.clear();
		
		int z = point.getMinZ() - 1;
		
		for (Placement candidate : pointSupports) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!point.intersects(candidate)) {
				continue;
			}
			
			result.add(candidate);
		}
		return true;
	}
	
	public static boolean populatePointSupporters2(Point point, BoxStackValue boxStackValue, List<Placement> result, List<Placement> pointSupports) {
		result.clear();

		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		int z = point.getMinZ() - 1;
		
		for (Placement candidate : pointSupports) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}
			
			BoxStackValue stackValue = candidate.getStackValue();
			if(stackValue.isLoadIdenticalBoxOnly()) {
				// this candidate cannot be a supporter if it cannot have another box stacked on top of it
				return false;
			}

			result.add(candidate);
		}
		return true;
	}
	
	
	public static boolean populatePlacementSupportees(Point point, BoxStackValue boxStackValue, List<Placement> result, List<Placement> pointSupports) {
		result.clear();

		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		int z = point.getMinZ() + boxStackValue.getDz();
		
		for (Placement candidate : pointSupports) {
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}

			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}

			if(candidate.getStackValue().isLoadIdenticalBoxOnly()) {
				// this candidate cannot be a supportee if it cannot have another box stacked on top of it
				return false;
			}

			result.add(candidate);
		}
		return true;
	}
	
	public boolean hasSupporteesWithNonIdenticalBox(Placement placement, BoxStackValue stackValue, List<Placement> placements) {
		// must not have any supportees above, otherwise the candidate would not be the only box stacked on top
		int aboveZ = placement.getAbsoluteEndZ() + stackValue.getDz() + 1;
		
		
		int newMinX = placement.getAbsoluteX();
		int newMinY = placement.getAbsoluteY();
		
		int newMaxX = newMinX + stackValue.getDx() - 1;
		int newMaxY = newMinY + stackValue.getDy() - 1;
		
		for (Placement candidate : placements) {
			if (candidate.getAbsoluteEndZ() != aboveZ) {
				continue;
			}
			
			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}
			
			if(candidate.getStackValue().getBox() == stackValue.getBox()) {
				// corner case, will this ever happen?
				continue;
			}
			return true;
		}
		return false;
	}
	
	public void filterPlacementsSupportersWithIdenticalBox(Box box, BoxStackValue stackValue, Point point, List<Placement> result, List<Placement> placements) {
		result.clear();

		// already know the stackValue dz fits in the point
		int maxX = point.getMaxX() - stackValue.getDx();
		int maxY = point.getMaxY() - stackValue.getDy();
		
		for (Placement candidate : placements) {
			if(candidate.getStackValue().getBox() != box) {
				continue;
			}
			
			if(candidate.getAbsoluteX() > maxX || candidate.getAbsoluteY() > maxY) {
				// the whole box is not within the point
				continue;
			}

			// check max count
			if (!canStackOneMore(candidate)) {
				continue;
			}

			result.add(candidate);
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
	private long calculateSupportAndValidateLoad(List<Placement> placementSupporters, BoxStackValue stackValue, int absoluteX, int absoluteY) {
		int n = placementSupporters.size();
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;

		if (placementAreas.length < n) {
			placementAreas = new long[n];
		}

		long totalOverlapArea = 0;
		for (int i = 0; i < n; i++) {
			placementAreas[i] = overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placementSupporters.get(i));
			totalOverlapArea += placementAreas[i];
		}

		long weight = stackValue.getBox().getWeight();
		for (int i = 0; i < n; i++) {
			long share = (weight * placementAreas[i]) / totalOverlapArea;
			if (!isWithinMaxLoadWeightAndPressure(placementSupporters.get(i), share, placementAreas[i])) {
				return -1;
			}
		}

		return totalOverlapArea;
	}
	
	private boolean isWithinMaxLoadWeightAndPressure(Placement placement, long weight, long area) {
		BoxStackValue candidateSupporter = placement.getStackValue();
		if(candidateSupporter.isMaxLoadPressure()) {
			if(weight > area * candidateSupporter.getMaxLoadPressure()) {
				return false;
			}
		}
		
		if(candidateSupporter.isMaxLoadWeight()) {
			if(weight > candidateSupporter.getMaxLoadWeight()) {
				return false;
			}
		}
		
		// getSupportedArea() is maintained incrementally by addSupporter/removeSupporter,
		// so it equals the sum of all supporter areas without an extra loop.
		long totalArea = placement.getSupportedArea();

		if (totalArea > 0) {
			for (PlacementLoad placementLoad : placement.getSupporters()) {
				long weightShare = (candidateSupporter.getBox().getWeight() * placementLoad.getArea()) / totalArea;
				
				if(!isWithinMaxLoadWeightAndPressure(placementLoad.getPlacement(), weightShare, placementLoad.getArea())) {
					return false;
				}		
			}
		}
		
		return true;
	}

	/**
	 * Distributes load from the placed box to its supporters and handles the case
	 * where the placement is inserted below existing placements (requiring load recalculation).
	 */
	protected void applyLoad(Placement placement) {
		// setSupportedArea was pre-computed for comparator use during candidate selection.
		// Reset here so addSupporter accumulates the correct total from a clean 0.
		placement.setSupportedArea(0);

		List<Placement> placements = stack.getPlacements();

		// Check if there are existing placements resting on top of this new one
		// (i.e., this box was placed in a gap below existing boxes)
		// this can have ripple effects throughout the stack
		
		List<PlacementLoad> supporteesAbove = findSupportees(placement, placements);
		if (!supporteesAbove.isEmpty()) {
			long totalArea = 0;
			for (int i = 0; i < supporteesAbove.size(); i++) {
				totalArea += supporteesAbove.get(i).getArea();
			}
			for (int i = 0; i < supporteesAbove.size(); i++) {
				PlacementLoad s = supporteesAbove.get(i);
				Placement above = s.getPlacement();
				long share = (above.getWeight() * s.getArea()) / totalArea;
				placement.addLoad(above, s.getArea(), share);
			}
		}
		
		// Distribute load to supporters below
		List<PlacementLoad> supportersBelow = findSupporters(placement, placements);
		if (!supportersBelow.isEmpty()) {
			long totalArea = 0;
			for (int i = 0; i < supportersBelow.size(); i++) {
				totalArea += supportersBelow.get(i).getArea();
			}
			long weight = placement.getWeight();
			for (int i = 0; i < supportersBelow.size(); i++) {
				PlacementLoad s = supportersBelow.get(i);
				long share = (weight * s.getArea()) / totalArea;
				s.getPlacement().addLoad(placement, s.getArea(), share);
			}
		}

	}

	/**
	 * Finds placements below that support the given placement.
	 */
	protected List<PlacementLoad> findSupporters(Placement placement, List<Placement> placements) {
		List<PlacementLoad> result = new ArrayList<>();
		int newMinX = placement.getAbsoluteX();
		int newMinY = placement.getAbsoluteY();
		int newMaxX = placement.getAbsoluteEndX();
		int newMaxY = placement.getAbsoluteEndY();
		int z = placement.getAbsoluteZ() - 1;

		for (int i = 0; i < placements.size(); i++) {
			Placement candidate = placements.get(i);
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}
			long area = overlapArea(newMinX, newMinY, newMaxX, newMaxY, candidate);
			result.add(new PlacementLoad(candidate, area));
		}
		return result;
	}

	/**
	 * Finds placements above that rest on the given placement (for gap-fill scenarios).
	 */
	protected List<PlacementLoad> findSupportees(Placement placement, List<Placement> existing) {
		List<PlacementLoad> result = new ArrayList<>();
		int z = placement.getAbsoluteEndZ() + 1;
		int pMinX = placement.getAbsoluteX();
		int pMinY = placement.getAbsoluteY();
		int pMaxX = placement.getAbsoluteEndX();
		int pMaxY = placement.getAbsoluteEndY();

		for (int i = 0; i < existing.size(); i++) {
			Placement candidate = existing.get(i);
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}
			if(!candidate.intersects2D(placement)) {
				continue;
			}
			long area = overlapArea(pMinX, pMinY, pMaxX, pMaxY, candidate);
			result.add(new PlacementLoad(candidate, area));
		}
		return result;
	}

	protected long overlapArea(int minX, int minY, int maxX, int maxY, Placement candidate) {
		int overlapMinX = Math.max(minX, candidate.getAbsoluteX());
		int overlapMinY = Math.max(minY, candidate.getAbsoluteY());
		int overlapMaxX = Math.min(maxX, candidate.getAbsoluteEndX());
		int overlapMaxY = Math.min(maxY, candidate.getAbsoluteEndY());

		return (long)(overlapMaxX - overlapMinX + 1) * (long)(overlapMaxY - overlapMinY + 1);
	}

	protected boolean intersects3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Placement p) {
		return !(p.getAbsoluteEndX() < minX || p.getAbsoluteX() > maxX ||
				p.getAbsoluteEndY() < minY || p.getAbsoluteY() > maxY ||
				p.getAbsoluteEndZ() < minZ || p.getAbsoluteZ() > maxZ);
	}

	protected boolean hasIdenticalOnlyConstraint(Box box) {
		for (BoxStackValue sv : box.getStackValues()) {
			if (sv.isMaxLoadBoxCount() && sv.isMaxLoadBoxCountIdenticalOnly()) {
				return true;
			}
		}
		return false;
	}
}
