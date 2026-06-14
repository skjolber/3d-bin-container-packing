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
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * 
 * Load aware placement controls which calculates weight and support area.
 * 
 */

public class WeightLoadAwarePlacementControls extends AbstractComparatorPlacementControls<Placement> {

	protected boolean fullSupport;

	protected List<Placement> pointSupportees = new ArrayList<>();
	protected List<Placement> pointSupporters = new ArrayList<>();
	protected List<Placement> placementSupportees = new ArrayList<>();
	protected List<Placement> placementSupporters = new ArrayList<>();

	protected long[] placementAreas = new long[pointCalculator.size()];
	protected long[] reliefWeights = new long[pointCalculator.size()];

	public WeightLoadAwarePlacementControls(BoxItemSource boxItems, 
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
				if(point3d.getMinZ() > 0) {
					populatePointSupporters(point3d, stack.getPlacements());
				}
				populatePointSupportees(point3d, stack.getPlacements());
				
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					
					if (!point3d.fits3D(stackValue)) {
						continue;
					}

					// if there is any box above which now needs to be supported, add weight
					long weight = calculateSupporteeWeight(pointSupportees, stackValue, point3d);
					if(weight == -1L) {
						continue;
					}
					
					long supportedArea;
					if(point3d.getMinZ() > 0) {
						populateSupporters(point3d, stackValue, pointSupporters);
						
						supportedArea = calculateSupportAndValidateSupporterLoad(placementSupporters, stackValue, point3d.getMinX(), point3d.getMinY(), weight);
						if(supportedArea == -1L) {
							continue;
						}
						
						if(fullSupport && supportedArea != stackValue.getArea()) {
							continue;
						}
					} else {
						supportedArea = stackValue.getArea();
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
			result.setIndex(stack.size());
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
											
						// if there is any box above which now needs to be supported, add weight
						long weight = calculateSupportAndValidateSupporteeLoad(pointSupportees, stackValue, x, y, point3d.getMinZ(), x + stackValue.getDx() - 1, y + stackValue.getDy() - 1  );
						if(weight == -1L) {
							continue;
						}
						
						populateSupporters(point3d, stackValue, pointSupporters);
						
						long supportedArea = calculateSupportAndValidateSupporterLoad(placementSupporters, stackValue, x, y, weight);
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
		
		if(result != null) {
			result.setIndex(stack.size());
		}
		
		return result;
	}

	protected Placement createPlacement(BoxStackValue stackValue, int index, int x, int y, int z) {
		Placement placement = new Placement(stackValue, index, x, y, z);
		placement.setSupportedArea(stackValue.getArea());
		return placement;
	}
	
	public long calculateSupportAndValidateSupporteeLoad(List<Placement> pointSupportees, BoxStackValue boxStackValue, int minX, int minY, int minZ, int maxX, int maxY) {
		long weight = 0;
		
		int z = minZ + boxStackValue.getDz();
		
		int stackSize = stack.size();
		for(int i = 0; i < stackSize; i++) {
			reliefWeights[i] = 0;
		}
		
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
			
			long effectiveWeight = (candidateWeight * area) / (area + candidate.getSupportedArea());
			
			if(boxStackValue.isMaxLoadPressure()) {
				if(boxStackValue.getMaxLoadPressure() * area < effectiveWeight) {
					return -1;
				}
			}

			if(boxStackValue.isMaxLoadBoxCount()) {
				if(!!isWithinBoxCount(candidate, boxStackValue.getMaxLoadBoxCount(), pointSupportees, minX, minY, maxX, maxY)) {
					continue;
				}
			}

			calculateRelifWeight(candidate, effectiveWeight);
			
			weight += effectiveWeight;
		}
		
		if(boxStackValue.isMaxLoadWeight() && weight > boxStackValue.getMaxLoadWeight()) {
			return -1;
		}
		
		return weight +  boxStackValue.getBox().getWeight();
	}
	
	private boolean isWithinBoxCount(Placement candidate, int count, List<Placement> pointSupportees, int minX, int minY, int maxX, int maxY) {
		BoxStackValue supporteeStackValue = candidate.getStackValue();
		if(supporteeStackValue.isMaxLoadBoxCount() && supporteeStackValue.getMaxLoadBoxCount() > count) {
			return true;
		}
		
		if(count <= 0) {
			return false;
		}
		
		count--;
		
		for (Placement placement : pointSupportees) {
			if(!placement.intersects2D(minX, maxX, minY, maxY)) {
				continue;
			}
			
			if(!isWithinBoxCount(placement, count, pointSupportees, minX, minY, maxX, maxY)) {
				return false;
			}
		}
		
		return true;
	}

	public void calculateRelifWeight(Placement placement, long reliefWeight) {
		long supportedArea = placement.getSupportedArea();
		for (PlacementLoad placementLoad : placement.getSupporters()) {
			Placement supporter = placementLoad.getPlacement();
			
			long r = (reliefWeight * placementLoad.getArea()) / supportedArea;
			
			this.reliefWeights[supporter.getIndex()] += r;
			
			calculateRelifWeight(supporter, r);
		}
	}

	public long calculateSupporteeWeight(List<Placement> pointSupportees, BoxStackValue boxStackValue, Point point) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		return calculateSupportAndValidateSupporteeLoad(pointSupportees, boxStackValue, newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
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
	
	protected long calculateSupportAndValidateSupporterLoad(List<Placement> placementSupporters, BoxStackValue stackValue, int absoluteX, int absoluteY, long weight) {
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
	
	protected boolean isWithinMaxLoadWeightAndPressure(Placement placement, long weight, long area) {
		
		long effectiveWeight = weight - reliefWeights[placement.getIndex()];
		
		BoxStackValue candidateSupporter = placement.getStackValue();
		if(candidateSupporter.isMaxLoadPressure()) {
			if(effectiveWeight > area * candidateSupporter.getMaxLoadPressure()) {
				return false;
			}
		}
		
		if(candidateSupporter.isMaxLoadWeight()) {
			if(effectiveWeight > candidateSupporter.getMaxLoadWeight()) {
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

	protected static long overlapArea(int minX, int minY, int maxX, int maxY, Placement candidate) {
		int overlapMinX = Math.max(minX, candidate.getAbsoluteX());
		int overlapMinY = Math.max(minY, candidate.getAbsoluteY());
		int overlapMaxX = Math.min(maxX, candidate.getAbsoluteEndX());
		int overlapMaxY = Math.min(maxY, candidate.getAbsoluteEndY());

		return (long)(overlapMaxX - overlapMinX + 1) * (long)(overlapMaxY - overlapMinY + 1);
	}


}
