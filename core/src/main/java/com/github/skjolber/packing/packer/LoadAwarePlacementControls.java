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
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
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
public class LoadAwarePlacementControls extends AbstractComparatorPlacementControls<Placement> {

	protected boolean fullSupport;
	protected boolean calculateSupport;

	public LoadAwarePlacementControls(BoxItemSource boxItems, 
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, Comparator<Placement> placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport, boolean calculateSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		
		this.fullSupport = fullSupport;
		this.calculateSupport = calculateSupport;
	}

	@Override
	public Placement getPlacement(int offset, int length) {
		Placement result = null;

		List<Placement> pointSupports = new ArrayList<>();
		List<Placement> placementSupporters = new ArrayList<>();

		long maxPointArea = pointCalculator.getMaxArea();

		for (int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			Box box = boxItem.getBox();

			if (order == Order.NONE) {
				if (result != null && boxItemComparator != null && boxItemComparator.compare(result.getBoxItem(), boxItem) >= 0) {
					continue;
				}
			}

			PointSource points = pointControls.getPoints(boxItem);

			for (Point point3d : points) {
				for (BoxStackValue stackValue : box.getStackValues()) {
					if (stackValue.getArea() > maxPointArea) {
						continue;
					}

					if (!point3d.fits3D(stackValue)) {
						continue;
					}
					
					if(point3d.getMinZ() == 0) {
						Placement placementResult = new Placement(stackValue, point3d);
						
						if (result != null && placementComparator != null && placementComparator.compare(result, placementResult) >= 0) {
							continue;
						}
	
						result = placementResult;
					}
					
					// so while the below placements must hold the new placement, a below placement might indirectly get a smaller weight
					// share of the load if the new placement is inserted below it, which can have ripple effects throughout the stack

					if(stackValue.isMaxLoadBoxCount()) {
						if(stackValue.isMaxLoadBoxCountIdenticalOnly()) {
							// find any feasible inner points
							
							populateSupportersFullyWithinPoint(point3d, pointSupports, stack.getPlacements());
							for (Placement candidate : pointSupports) {
								if (candidate.getBox() != box) {
									continue;
								}

								// check max count
								if (!canStackOneMore(candidate)) {
									continue;
								}
								
								// so the only the current box will be stacked on the candidate box, which
								// simplifies things a bit
								
								if(!isWithinMaxLoadWeightAndPressure(candidate, box.getWeight(), stackValue.getArea())) {
									continue;
								}
								
								Placement placement = new Placement(stackValue, point3d.getIndex(), candidate.getAbsoluteX(), candidate.getAbsoluteY(), candidate.getAbsoluteEndZ() + 1);
								placement.setSupportedArea(stackValue.getArea());
								
								if(result != null && placementComparator.compare(result, placement) >= 0) {
									continue;
								}
								
								result = placement;
							}
						} else {
							populatePlacementSupporters(point3d, stackValue, placementSupporters, stack.getPlacements());
							
							long supportedArea = (fullSupport || calculateSupport) ? calculateSupport(placementSupporters, stackValue, point3d.getMinX(), point3d.getMinY()) : 0;
							if(fullSupport && supportedArea != stackValue.getArea()) {
								continue;
							}

							if(!isWithinMaxLoadBoxCount(placementSupporters)) {
								continue;
							}
							
							if(!isWithinMaxLoadWeightAndPressure(placementSupporters, stackValue, point3d.getMinX(), point3d.getMaxY())) {
								continue;
							}
							
							Placement placement = new Placement(stackValue, point3d);
							placement.setSupportedArea(supportedArea);
							
							if(result != null && placementComparator.compare(result, placement) >= 0) {
								continue;
							}
							
							result = placement;
						}
					} else {
						populatePlacementSupporters(point3d, stackValue, placementSupporters, stack.getPlacements());
						
						long supportedArea = (fullSupport || calculateSupport) ? calculateSupport(placementSupporters, stackValue, point3d.getMinX(), point3d.getMinY()) : 0;						
						if(fullSupport && supportedArea != stackValue.getArea()) {
							continue;
						}
						
						if(!isWithinMaxLoadWeightAndPressure(placementSupporters, stackValue, point3d.getMinX(), point3d.getMaxY())) {
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
			}

			if (order == Order.CRONOLOGICAL) {
				break;
			}
			if (order == Order.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}
		}

		if (result != null && result.getAbsoluteZ() > 0) {
			applyLoad(result);
		}
		return result;
	}

	public static boolean canStackOneMore(Placement candidate) {
		return canStackLevels(candidate, 1);
	}

	public static boolean canStackLevels(Placement candidate, int levels) {
		BoxStackValue stackValue = candidate.getStackValue();
		if(stackValue.isMaxLoadBoxCount()) {
			if(stackValue.getMaxLoadBoxCount() <= levels) {
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

	public static void populatePlacementSupporters(Point point, BoxStackValue boxStackValue, List<Placement> result, List<Placement> pointSupports) {
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
			
			if(candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				result.add(candidate);
			}
		}
	}
	
	public void populateSupportersFullyWithinPoint(Point point, List<Placement> result, List<Placement> placements) {
		result.clear();

		int z = point.getMinZ() - 1;
		
		for (Placement candidate : placements) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			// the whole placement must fit in xy-plane
			if(!point.fitsInXYPlane(candidate)) {
				continue;
			}
			result.add(candidate);
		}
	}
	
	/**
	 * Checks whether placing this stackValue at this point would violate
	 * load constraints on any of the supporters below.
	 */
	
	private boolean isWithinMaxLoadWeightAndPressure(List<Placement> placementSupporters, BoxStackValue stackValue, int absoluteX, int absoluteY) {
		
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;
		
		long totalOverlapArea = 0;
		for (Placement placement : placementSupporters) {
			totalOverlapArea += overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placement);
		}
		
		long weight = stackValue.getBox().getWeight();
		
		for (Placement candidate : placementSupporters) {
			long area = overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, candidate);
			long share = (weight * area) / totalOverlapArea;

			if(!isWithinMaxLoadWeightAndPressure(candidate, share, area)) {
				return false;
			}
		}
		
		return true;
	}

	private long calculateSupport(List<Placement> placementSupporters, BoxStackValue stackValue, int absoluteX, int absoluteY) {
		
		int newMaxX = absoluteX + stackValue.getDx() - 1;
		int newMaxY = absoluteY + stackValue.getDy() - 1;
		
		long totalOverlapArea = 0;
		for (Placement placement : placementSupporters) {
			totalOverlapArea += overlapArea(absoluteX, absoluteY, newMaxX, newMaxY, placement);
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
		
		long totalArea = 0;
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			totalArea += placementLoad.getArea();
		}
		
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			long weightShare = (candidateSupporter.getBox().getWeight() * placementLoad.getArea()) / totalArea;
			
			if(!isWithinMaxLoadWeightAndPressure(placementLoad.getPlacement(), weightShare, placementLoad.getArea())) {
				return false;
			}		
		}
		
		return true;
	}

	/**
	 * Distributes load from the placed box to its supporters and handles the case
	 * where the placement is inserted below existing placements (requiring load recalculation).
	 */
	protected void applyLoad(Placement placement) {
		List<Placement> existing = stack.getPlacements();

		// Check if there are existing placements resting on top of this new one
		// (i.e., this box was placed in a gap below existing boxes)
		// this can have ripple effects throughout the stack
		
		List<PlacementLoad> supporteesAbove = findSupportees(placement, existing);
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
		List<PlacementLoad> supportersBelow = findSupporters(placement, existing);
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
	protected List<PlacementLoad> findSupporters(Placement placement, List<Placement> existing) {
		List<PlacementLoad> result = new ArrayList<>();
		int newMinX = placement.getAbsoluteX();
		int newMinY = placement.getAbsoluteY();
		int newMaxX = placement.getAbsoluteEndX();
		int newMaxY = placement.getAbsoluteEndY();
		int z = placement.getAbsoluteZ() - 1;

		for (int i = 0; i < existing.size(); i++) {
			Placement candidate = existing.get(i);
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			if(!candidate.intersects2D(newMinX, newMinY, newMaxX, newMaxY)) {
				continue;
			}
			long area = overlapArea(newMinX, newMinY, newMaxX, newMaxY, candidate);
			if (area > 0) {
				result.add(new PlacementLoad(candidate, area));
			}
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
			if(!candidate.intersects2D(pMinX, pMaxX, pMinY, pMaxY)) {
				continue;
			}
			long area = overlapArea(pMinX, pMinY, pMaxX, pMaxY, candidate);
			if (area > 0) {
				result.add(new PlacementLoad(candidate, area));
			}
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
