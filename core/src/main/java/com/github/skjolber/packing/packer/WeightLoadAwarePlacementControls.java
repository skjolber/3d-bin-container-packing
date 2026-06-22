package com.github.skjolber.packing.packer;

import java.util.Comparator;

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
import com.github.skjolber.packing.comparator.PlacementComparator;

/**
 * 
 * Load aware placement controls which calculates weight and support area.
 * 
 */

public class WeightLoadAwarePlacementControls extends AbstractLoadWeightComparatorPlacementControls {

	public WeightLoadAwarePlacementControls(BoxItemSource boxItems, 
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, PlacementComparator placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
		super(boxItems, pointControls, pointCalculator, container, stack, order, placementComparator, boxItemComparator, fullSupport);
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
					populatePointSupporters(point3d);
				} else {
					pointSupporters.clear();
				}
				populatePointSupportees(point3d);
				
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					
					if (!point3d.fits3D(stackValue)) {
						continue;
					}

					// if there is any box above which now needs to be supported, add weight
					long weight = calculateSupporteeWeight(stackValue, point3d);
					if(weight == -1L) {
						continue;
					}
					
					long supportedArea;
					if(point3d.getMinZ() > 0) {
						populateSupporters(point3d, stackValue);
						
						supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, point3d.getMinX(), point3d.getMinY(), weight);
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

		if(result != null) {
			result.setIndex(stack.size());
			return result;
		}

		if(!fullSupport) {
			return null;
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
				populatePointSupporters(point3d);
				populatePointSupportees(point3d);

				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > point3d.getArea()) {
						continue;
					}
					
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					
					// check viable inner points in the same plane
					// use the corners of underlying placements
					int z = point3d.getMinZ() - 1;
					int limitX = point3d.getMaxX() - stackValue.getDx();
					int limitY = point3d.getMaxY() - stackValue.getDy();

					int limitMaxX = point3d.getMinX() + stackValue.getDx();
					int limitMaxY = point3d.getMinY() + stackValue.getDy();

					if(z <= 0 || limitX <= 0 || limitY <= 0) {
						continue;
					}
					
					for (Placement candidate : pointSupporters) {
						if (candidate.getAbsoluteEndZ() != z) {
							continue;
						}
						
						if(candidate.getAbsoluteX() > limitX || candidate.getAbsoluteEndX() < limitMaxX) {
							continue;
						}
						
						if(candidate.getAbsoluteY() > limitY || candidate.getAbsoluteEndY() < limitMaxY) {
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
						long weight = calculateSupportAndValidateSupporteeLoad(stackValue, x, y, point3d.getMinZ(), x + stackValue.getDx() - 1, y + stackValue.getDy() - 1);
						if(weight == -1L) {
							continue;
						}
						
						populateSupporters(x, y, point3d.getMinZ(), x + stackValue.getDx() - 1, y + stackValue.getDy() - 1);
						
						long supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, x, y, weight);
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

	public long calculateSupportAndValidateSupporteeLoad(BoxStackValue boxStackValue, int minX, int minY, int minZ, int maxX, int maxY) {
		long weight = 0;
		
		int z = minZ + boxStackValue.getDz();
		
		int stackSize = stack.size();
		for(int i = 0; i < stackSize; i++) {
			reliefWeights[i] = 0;
		}
		
		for (Placement candidate : pointSupportees) {
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
			
			calculateRelifWeight(candidate, effectiveWeight);
			
			weight += effectiveWeight;
		}
		
		if(boxStackValue.isMaxLoadWeight() && weight > boxStackValue.getMaxLoadWeight()) {
			return -1;
		}
		
		return weight +  boxStackValue.getBox().getWeight();
	}

	public long calculateSupporteeWeight(BoxStackValue boxStackValue, Point point) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		return calculateSupportAndValidateSupporteeLoad(boxStackValue, newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
	}

	protected void populateSupporters(Point point, BoxStackValue boxStackValue) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		populateSupporters(newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
	}

	protected void populateSupporters(int newMinX, int newMinY, int minZ, int newMaxX, int newMaxY) {
		placementSupporters.clear();
		
		int z = minZ - 1;
		
		for (Placement candidate : pointSupporters) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}
			
			placementSupporters.add(candidate);
		}
	}

}
