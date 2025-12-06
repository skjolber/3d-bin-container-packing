package com.github.skjolber.packing.ep.points1d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.PlacementList;

/**
 * 
 * Implementation of so-called extreme points in 2D.
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

	protected Point1D values = null;
	protected PlacementList placements;

	protected Placement containerPlacement;

	protected long minAreaLimit = 0;

	protected final boolean cloneOnConstrain;

	protected List<Point1D> initialPoints = Collections.emptyList();

	public DefaultPointCalculator1D(boolean immutablePoints, BoxItemSource boxItemSource) {
		this.cloneOnConstrain = immutablePoints;
		
		int count = 0;
		for(int i = 0; i < boxItemSource.size(); i++) {
			count += boxItemSource.get(i).getCount();
		}
		
		this.placements = new PlacementList(count);
	}

	public DefaultPointCalculator1D(boolean immutablePoints, int capacity) {
		this.cloneOnConstrain = immutablePoints;
		this.placements = new PlacementList(capacity);
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

		Point1D point = values;
		
		
		
		return true;
	}

	public int getDepth() {
		return containerMaxY + 1;
	}

	public int getWidth() {
		return containerMaxX + 1;
	}

	@Override
	public String toString() {
		return "DefaultPointCalculator1D [" + containerMaxX + ": " + values + "]";
	}

	public List<Placement> getPlacements() {
		return placements.toList();
	}

	public Point1D get(int i) {
		return values;
	}

	public List<Point> getAll() {
		if(values != null) {
			List<Point> points = new ArrayList<>();
			points.add(values);
			return points;
		}
		return Collections.emptyList();
	}

	public int size() {
		return values == null ? 0 : 1;
	}

	public int getMinY() {
		if(values != null) {
			return values.getMinY();
		}
		return 0;
	}

	public int getMinX() {
		if(values != null) {
			return values.getMinX();
		}
		return 0;
	}

	public boolean isEmpty() {
		return values == null;
	}

	public long getMaxArea() {
		if(values != null) {
			return values.getArea();
		}
		return -1L;
	}

	public void redo() {
		values = null;
		placements.clear();
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		redo();
	}

	public int findPoint(int x, int y) {
		if(values != null) {
			if(values.getMinX() == x && values.getMinY() == y) {
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
		if(values != null) {
			if(values.getArea() < minAreaLimit) {
				values = null;
			}
		}
	}

	public long getMinAreaLimit() {
		return minAreaLimit;
	}

	public void remove(int index) {
		values = null;
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
		values = createContainerPoint();
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
		if(!points.isEmpty()) {
			if(points.size() != 1) {
				throw new IllegalArgumentException();
			}
			
			Point p = points.get(0);
			if(p.getMaxX() > containerMaxX) {
				throw new IllegalArgumentException();
			}
			if(p.getMaxY() > containerMaxY) {
				throw new IllegalArgumentException();
			}

			// transform coordinates to internal representation, i.e. with support etc
			this.values = new Point1D(p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			this.values.setIndex(0);
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
		if(!test.test(values)) {
			values = null;
		}
	}
}
