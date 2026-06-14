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
public class LoadAwarePlacementControls extends AbstractComparatorPlacementControls<Placement> {

	protected boolean fullSupport;

	protected List<Placement> pointSupportees = new ArrayList<>();
	protected List<Placement> pointSupporters = new ArrayList<>();
	protected List<Placement> placementSupportees = new ArrayList<>();
	protected List<Placement> placementSupporters = new ArrayList<>();

	protected long[] placementAreas = new long[pointCalculator.size()];

	public LoadAwarePlacementControls(BoxItemSource boxItems, 
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, Comparator<Placement> placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator);
		
		this.fullSupport = fullSupport;

		int count = 0;
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);
			count += boxItem.getCount();
		}
		
		placementAreas = new long[count];
		boxItems.size();
	}

	@Override
	public Placement getPlacement(int offset, int length) {
		Placement result = null;

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
				populatePointSupporters(point3d, stack.getPlacements());
				populatePointSupportees(point3d, stack.getPlacements());
				
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
					
					long weight = box.getWeight();
					
					// if there is any box above which now needs to be supported, add weight
					long supporteeWeight = calculateSupportAndValidateSupporteeWeight(placementAreas, pointSupportees, stackValue, point3d);
					
					populateSupporters(point3d, stackValue, pointSupporters);
					
					long supportedArea = calculateSupportAndValidateSupporterLoad(placementAreas, placementSupporters, stackValue, point3d.getMinX(), point3d.getMinY(), supporteeWeight + weight);
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

		if(result != null || !fullSupport) {
			return result;
		}
		
		// give it another shot by trying to place box within points (not just in the point origo)
		return getFullySupportedPlacement(offset, length);
	}

	protected Placement getFullySupportedPlacement(int offset, int length) {
		
		Placement result = null;
		// try placing boxes within points
		// pick the points where an underlying placement exists.
		
		for(int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();

			if(order == Order.NONE) {
				// is there any point in testing this box?
				//
				// a negative integer, zero, or a positive integer as the 
				// first argument is less than, equal to, or greater than the
			    // second.
				if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem) != AbstractPackager.ARGUMENT_2_IS_BETTER) {
					continue;
				}
			}
			
			PointSource points = pointControls.getPoints(boxItem);

			for (Point point3d : points) {
				populatePointSupporters(point3d, stack.getPlacements());
				populatePointSupportees(point3d, stack.getPlacements());

				for (BoxStackValue stackValue : box.getStackValues()) {
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					
					// check viable inner points in the same plane
					// use the corners of underlying placements
					int z = point3d.getMinZ() - 1;
					int minX = point3d.getMaxX() - stackValue.getDx();
					int minY = point3d.getMaxY() - stackValue.getDy();

					int minMaxX = point3d.getMinX() + stackValue.getDx();
					int minMaxY = point3d.getMinY() + stackValue.getDy();

					if(z <= 0 || minX <= 0 || minY <= 0) {
						continue;
					}
					
					for (Placement candidate : pointSupporters) {
						if (candidate.getAbsoluteEndZ() != z) {
							continue;
						}
						
						if(candidate.getAbsoluteX() > minX || candidate.getAbsoluteEndX() < minMaxX) {
							continue;
						}
						
						if(candidate.getAbsoluteY() > minY || candidate.getAbsoluteEndY() < minMaxY) {
							continue;
						}
						
						int x = candidate.getAbsoluteX();
						if(x < point3d.getMinX()) {
							x = point3d.getMinX();
						}
						int y = candidate.getAbsoluteY();
						if(y < point3d.getMinY()) {
							y = point3d.getMinY();
						}
						
						long weight = box.getWeight();
						
						long supporteeLoad = calculateSupportAndValidateSupporteeLoad(placementAreas, pointSupportees, stackValue, x, y, point3d.getMinZ(), point3d.getMinX() + stackValue.getDx() - 1, point3d.getMinY() + stackValue.getDy() - 1  );
						
						populateSupporters(point3d, stackValue, pointSupporters);
						
						long supportedArea = calculateSupportAndValidateSupporterLoad(placementAreas, placementSupporters, stackValue, x, y, supporteeLoad + weight);
						if(supportedArea < 0) {
							continue;
						}
						
						if(supportedArea != stackValue.getArea()) {
							continue;
						}

						Placement placement = createPlacement(stackValue, point3d.getIndex(), x, y, point3d.getMinZ());
						
						if(result != null && placementComparator.compare(result, placement) >= 0) {
							continue;
						}
						
						result = placement;
					}
				} 
			}
			
			if(order == Order.CRONOLOGICAL) {
				// even if null
				break;
			}
			if(order == Order.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}
		}
		return result;
	}

	protected Placement createPlacement(BoxStackValue stackValue, int index, int x, int y, int z) {
		Placement placement = new Placement(stackValue, index, x, y, z);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
	
	public static long calculateSupportAndValidateSupporteeLoad(long[] placementAreas2, List<Placement> placementSupportees, BoxStackValue boxStackValue, int minX, int minY, int minZ, int maxX, int maxY) {
		long weight = 0;
		
		int z = minZ + boxStackValue.getDz();
		
		for (Placement candidate : placementSupportees) {
			if (candidate.getAbsoluteZ() != z) {
				continue;
			}
			
			if(!candidate.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			
			long area = overlapArea(minX, minY, maxX, maxY, candidate);
			
			long candidateWeight = candidate.getWeight();
			for (PlacementLoad placementLoad : candidate.getSupportees()) {
				candidateWeight += placementLoad.getWeight();
			}

			weight += (candidateWeight * area) / (area + candidate.getSupportedArea());
		}
		
		return weight;
	}
	

	public static long calculateSupportAndValidateSupporteeWeight(long[] placementAreas, List<Placement> placementSupportees, BoxStackValue boxStackValue, Point point) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		return calculateSupportAndValidateSupporteeLoad(placementAreas, placementSupportees, boxStackValue, newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
	}

	protected void populateSupporters(Point point, BoxStackValue boxStackValue, List<Placement> pointSupports) {
		placementSupporters.clear();

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
			
			placementSupporters.add(candidate);
		}
	}

	protected void populatePointSupporters(Point point, List<Placement> pointSupports) {
		pointSupporters.clear();
		pointSupportees.clear();
		
		int z = point.getMinZ() - 1;
		
		for (Placement candidate : pointSupports) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!point.intersects(candidate)) {
				continue;
			}
			
			pointSupporters.add(candidate);
		}
	}

	protected void populatePointSupportees(Point point, List<Placement> pointSupports) {
		pointSupportees.clear();
		
		for (Placement candidate : pointSupports) {
			if (candidate.getAbsoluteZ() > point.getMinZ()) {
				continue;
			}
			
			if(!point.intersects(candidate)) {
				continue;
			}
			
			pointSupportees.add(candidate);
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
	
	protected static long calculateSupportAndValidateSupporterLoad(long[] placementAreas, List<Placement> placementSupporters, BoxStackValue stackValue, int absoluteX, int absoluteY, long weight) {
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

		for (int i = 0; i < n; i++) {
			long share = (weight * placementAreas[i]) / totalOverlapArea;
			if (!isWithinMaxLoadWeightAndPressure(placementSupporters.get(i), share, placementAreas[i])) {
				return -1;
			}
		}

		return totalOverlapArea;
	}
	
	protected static boolean isWithinMaxLoadWeightAndPressure(Placement placement, long weight, long area) {
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

	protected static long overlapArea(int minX, int minY, int maxX, int maxY, Placement candidate) {
		int overlapMinX = Math.max(minX, candidate.getAbsoluteX());
		int overlapMinY = Math.max(minY, candidate.getAbsoluteY());
		int overlapMaxX = Math.min(maxX, candidate.getAbsoluteEndX());
		int overlapMaxY = Math.min(maxY, candidate.getAbsoluteEndY());

		return (long)(overlapMaxX - overlapMinX + 1) * (long)(overlapMaxY - overlapMinY + 1);
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


}
