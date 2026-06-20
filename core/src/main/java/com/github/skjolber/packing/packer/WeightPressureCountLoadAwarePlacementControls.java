package com.github.skjolber.packing.packer;

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
 * Load aware placement controls which calculates weight and support area, accounts for max pressure and max box count. 
 * 
 */


public class WeightPressureCountLoadAwarePlacementControls extends AbstractLoadWeightComparatorPlacementControls {

	public WeightPressureCountLoadAwarePlacementControls(BoxItemSource boxItems, 
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			Order order, Comparator<Placement> placementComparator, Comparator<BoxItem> boxItemComparator, boolean fullSupport) {
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

					Placement placement = getPlacement(point3d, stackValue);
					
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
	

	private Placement getPlacement(Point point3d, BoxStackValue stackValue) {

		// if there is any box above which now needs to be supported, add weight
		long weight = calculateSupporteeWeight(stackValue, point3d);
		if(weight == -1L) {
			return null;
		}
		
		long supportedArea;
		if(point3d.getMinZ() > 0) {
			if(!populateSupporters(point3d, stackValue)) {
				return null;
			}
			
			supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, point3d.getMinX(), point3d.getMinY(), weight);
			if(supportedArea == -1L) {
				return null;
			}
			
			if(fullSupport && supportedArea != stackValue.getArea()) {
				return null;
			}
		} else {
			supportedArea = stackValue.getArea();
		}
		
		Placement placement = new Placement(stackValue, point3d);
		placement.setSupportedArea(supportedArea);
		return placement;
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
						Placement placement = getPlacement(point3d, stackValue, candidate, limitX, limitY, z, limitMaxX, limitMaxY);
						if(placement == null) {
							continue;
						}
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
	
	private Placement getPlacement(Point point3d, BoxStackValue stackValue, Placement candidate, int limitX, int limitY, int z, int limitMaxX, int limitMaxY) {
		if (candidate.getAbsoluteEndZ() != z) {
			return null;
		}
		
		if(candidate.getAbsoluteX() > limitX || candidate.getAbsoluteEndX() < limitMaxX) {
			return null;
		}
		
		if(candidate.getAbsoluteY() > limitY || candidate.getAbsoluteEndY() < limitMaxY) {
			return null;
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
			return null;
		}
		
		if(!populateSupporters(x, y, point3d.getMinZ(), x + stackValue.getDx() - 1, y + stackValue.getDy() - 1)) {
			return null;
		}
		
		long supportedArea = calculateSupportAndValidateSupporterLoad(stackValue, x, y, weight);
		if(supportedArea < 0) {
			return null;
		}

		if(supportedArea != stackValue.getArea()) {
			return null;
		}

		return createPlacement(stackValue, point3d.getIndex(), x, y, point3d.getMinZ());
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
			
			if(boxStackValue.isMaxLoadPressure()) {
				if(boxStackValue.getMaxLoadPressure() * area < effectiveWeight) {
					return -1;
				}
			}

			if(boxStackValue.isMaxLoadBoxCount()) {
				if(!isWithinSupporteeBoxCount(candidate, boxStackValue.getMaxLoadBoxCount(), pointSupportees, minX, minY, maxX, maxY)) {
					return -1;
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
	
	private boolean isWithinSupporteeBoxCount(Placement candidate, int count, List<Placement> pointSupportees, int minX, int minY, int maxX, int maxY) {
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
			
			if(!isWithinSupporteeBoxCount(placement, count, pointSupportees, minX, minY, maxX, maxY)) {
				return false;
			}
		}
		
		return true;
	}

	public long calculateSupporteeWeight(BoxStackValue boxStackValue, Point point) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		return calculateSupportAndValidateSupporteeLoad(boxStackValue, newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
	}

	protected boolean populateSupporters(Point point, BoxStackValue boxStackValue) {
		int newMinX = point.getMinX();
		int newMinY = point.getMinY();
		
		int newMaxX = newMinX + boxStackValue.getDx() - 1;
		int newMaxY = newMinY + boxStackValue.getDy() - 1;
		
		return populateSupporters(newMinX, newMinY, point.getMinZ(), newMaxX, newMaxY);
	}

	protected boolean populateSupporters(int newMinX, int newMinY, int minZ, int newMaxX, int newMaxY) {
		placementSupporters.clear();

		int z = minZ - 1;
		
		for (Placement candidate : pointSupporters) {
			if (candidate.getAbsoluteEndZ() != z) {
				continue;
			}
			
			if(!candidate.intersects2D(newMinX, newMaxX, newMinY, newMaxY)) {
				continue;
			}
			
			if(!canStackOneMore(candidate)) {
				return false;
			}
			
			placementSupporters.add(candidate);
		}
		
		return true;
	}

}
