package com.github.skjolber.packing.ep.points1d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.collections.api.block.function.primitive.BooleanFunction;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.PlacementList;

/**
 * 
 * Implementation of so-called extreme points in 1D.
 * 
 * This is a very simple "side by side" implementation in x, y or z direction.
 *
 */

public class DefaultPointCalculator1D implements PointCalculator {

	public static final Comparator<Point1D> COMPARATOR_X = new Comparator<Point1D>() {

		@Override
		public int compare(Point1D o1, Point1D o2) {
			return Integer.compare(o1.getMinX(), o2.getMinX());
		}
	};
	
	protected int containerMaxX;
	protected int containerMaxY;
	protected int containerMaxZ;

	protected Point1D value = null;
	protected PlacementList placements;

	protected Placement containerPlacement;

	protected long minAreaLimit = 0;

	protected Point1D initialPoint;
	
	protected BooleanFunction<Placement> adder;
	
	public DefaultPointCalculator1D(BoxItemSource boxItemSource, Dimension dimension) {
		int count = 0;
		for(int i = 0; i < boxItemSource.size(); i++) {
			count += boxItemSource.get(i).getCount();
		}
		
		this.placements = new PlacementList(count);
		this.adder = toAdder(dimension);
	}

	public DefaultPointCalculator1D(int capacity, Dimension dimension) {
		this.placements = new PlacementList(capacity);
		this.adder = toAdder(dimension);
	}	

	private BooleanFunction<Placement> toAdder(Dimension dimension) {
		switch(dimension) {
			case X: {
				return this::addX;
			}
			case Y: {
				return this::addY;
			}
			case Z: {
				return this::addZ;
			}
			default: {
				throw new RuntimeException();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void setSize(int dx, int dy, int dz) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		this.containerMaxZ = dz - 1;

		BoxStackValue stackValue = new BoxStackValue(dx, dy, dz, null, -1);
		
		this.containerPlacement = new Placement(stackValue, new Point1D(0, 0, 0, dx - 1, dy - 1, dz - 1));
	}

	private Point1D createContainerPoint() {
		Point1D point = new Point1D(0, 0, 0, containerMaxX, containerMaxY, containerMaxZ);
		point.setIndex(0);
		return point;
	}

	public boolean add(Point point, Placement placement) {
		if(point.getIndex() == -1) {
			return add(0, placement);
		} 
		return add(point.getIndex(), placement);
	}
	
	public boolean add(int index, Placement placement) {
		placements.add(placement);
		
		return adder.booleanValueOf(placement);
	}

	private boolean addX(Placement placement) {
		if(placement.getAbsoluteEndX() >= containerMaxX) {
			value = null;
			return false;
		}
		value = value.moveX(placement.getAbsoluteEndX() + 1);
		return true;
	}
	
	private boolean addY(Placement placement) {
		if(placement.getAbsoluteEndY() >= containerMaxY) {
			value = null;
			return false;
		}
		value = value.moveY(placement.getAbsoluteEndY() + 1);
		return true;
	}
	
	private boolean addZ(Placement placement) {
		if(placement.getAbsoluteEndZ() >= containerMaxZ) {
			value = null;
			return false;
		}
		value = value.moveZ(placement.getAbsoluteEndZ() + 1);
		return true;
	}

	@Override
	public String toString() {
		return "DefaultPointCalculator1D [" + containerMaxX + ": " + value + "]";
	}

	public List<Placement> getPlacements() {
		return placements.toList();
	}

	public Point1D get(int i) {
		return value;
	}

	public List<Point> getAll() {
		if(value != null) {
			List<Point> points = new ArrayList<>();
			points.add(value);
			return points;
		}
		return Collections.emptyList();
	}

	public int size() {
		return value == null ? 0 : 1;
	}

	public int getMinY() {
		if(value != null) {
			return value.getMinY();
		}
		return 0;
	}

	public int getMinX() {
		if(value != null) {
			return value.getMinX();
		}
		return 0;
	}

	public int getMinZ() {
		if(value != null) {
			return value.getMinZ();
		}
		return 0;
	}

	
	public boolean isEmpty() {
		return value == null;
	}

	public long getMaxArea() {
		if(value != null) {
			return value.getArea();
		}
		return -1L;
	}

	public void redo() {
		value = null;
		placements.clear();
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		redo();
	}

	public int findPoint(int x) {
		if(value != null) {
			if(value.getMinX() == x) {
				return 0;
			}
		}
		return -1;
	}

	public void setMinimumAreaLimit(long minArea) {
		if(minAreaLimit != minArea) {
			this.minAreaLimit = minArea;
			filterMinimums();
		}
	}

	private void filterMinimums() {
		if(value != null) {
			if(value.getArea() < minAreaLimit) {
				value = null;
			}
		}
	}

	public long getMinAreaLimit() {
		return minAreaLimit;
	}

	public void remove(int index) {
		value = null;
	}

	@Override
	public Iterator<Point> iterator() {
		return getAll().iterator();
	}
	@Override
	public void clearToSize(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		clear();
	}

	@Override
	public void clear() {
		value = createContainerPoint();
		placements.clear();
		minAreaLimit = 0;
	}

	public long calculateUsedVolume() {
		long used = 0;
		for(int i = 0; i < placements.size(); i++) {
			Placement stackPlacement = placements.get(i);
			used += stackPlacement.getStackValue().getBox().getVolume();
		}
		return used;
	}
	
	public long calculateUsedWeight() {
		long used = 0;
		for(int i = 0; i < placements.size(); i++) {
			Placement stackPlacement = placements.get(i);
			used += stackPlacement.getStackValue().getBox().getWeight();
		}
		return used;
	}
	
	public void setPoints(List<Point> points) {
		if(points.isEmpty()) {
			value = null;
		} else {
			if(points.size() != 1) {
				throw new IllegalArgumentException("Expected 0 or 1 points");
			}
			
			Point p = points.get(0);
			if(p.getMaxX() > containerMaxX) {
				throw new IllegalArgumentException();
			}
			if(p.getMaxY() > containerMaxY) {
				throw new IllegalArgumentException();
			}
			if(p.getMaxZ() > containerMaxZ) {
				throw new IllegalArgumentException();
			}
	
			// transform coordinates to internal representation, i.e. with support etc
			this.value = new Point1D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			this.value.setIndex(0);
		}
	}

	public void updateMinimums(BoxStackValue stackValue, BoxItemSource filteredBoxItems) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		if(minArea) {
			setMinimumAreaLimit(filteredBoxItems.getMinArea());
		}
	}
	
	public void updateMinimums(BoxStackValue stackValue, BoxItemGroupSource filteredBoxItemGroups) {
		boolean minArea = stackValue.getArea() == minAreaLimit;
		if(minArea) {
			setMinimumAreaLimit(filteredBoxItemGroups.getMinArea());
		}
	}

	@Override
	public void setMinimumAreaAndVolumeLimit(long area, long volume) {
		setMinimumAreaLimit(area);
	}

	@Override
	public void remove(Predicate<Point> test) {
		if(value != null) {
			if(!test.test(value)) {
				value = null;
			}
		}
	}
}
