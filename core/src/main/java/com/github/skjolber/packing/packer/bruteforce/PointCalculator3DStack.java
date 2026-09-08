package com.github.skjolber.packing.packer.bruteforce;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class PointCalculator3DStack extends DefaultPointCalculator3D {

	private class BestPointList extends AbstractList<Point> implements RandomAccess {

		@Override
		public Point get(int index) {
			if(index < 0 || index >= bestStackIndex) {
				throw new IndexOutOfBoundsException(index);
			}
			return bestPoints[index];
		}

		@Override
		public int size() {
			return bestStackIndex;
		}
	}

	protected static class StackItem {
		
		protected Point3DFlagList values = new Point3DFlagList();
		protected Point3DFlagList otherValues = new Point3DFlagList();
		protected Placement stackPlacement = new Placement();
		protected SimplePoint3D point;
		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	protected final StackItem[] stackItems;
	protected int stackIndex = 0;
	protected final SimplePoint3D[] bestPoints;
	protected final List<Point> bestPointList = new BestPointList();
	protected int bestStackIndex;

	public PointCalculator3DStack(int maxStackDepth) {
		super(true, maxStackDepth);
		this.stackItems = new StackItem[maxStackDepth];
		this.bestPoints = new SimplePoint3D[maxStackDepth];

		for (int i = 0; i < maxStackDepth; i++) {
			stackItems[i] = new StackItem();
		}

		values.copyInto(stackItems[0].values);

		loadCurrent();
	}

	@Override
	public boolean add(int index, Placement placement) {
		stackItems[this.stackIndex].point = values.get(index);

		return super.add(index, placement);
	}

	public Placement push() {
		StackItem currentStackItem = stackItems[stackIndex];
		// save current state
		currentStackItem.minAreaLimit = minAreaLimit;
		currentStackItem.minVolumeLimit = minVolumeLimit;

		stackIndex++;

		StackItem nextStackItem = stackItems[stackIndex];

		// clone current state
		// make sure to overwrite everything, no clear is performed
		nextStackItem.point = null;
		nextStackItem.values.copyFrom(currentStackItem.values);
		nextStackItem.otherValues.copyFrom(currentStackItem.otherValues);

		// set the current stack item as working variables
		this.values = nextStackItem.values;
		this.otherValues = nextStackItem.otherValues;

		return nextStackItem.stackPlacement;
	}

	public int getStackIndex() {
		return stackIndex;
	}

	public void redo() {
		// i.e. copy values from the previous value into the current
		StackItem currentStackItem = stackItems[stackIndex - 1];

		StackItem nextStackItem = stackItems[stackIndex];
		nextStackItem.point = null;
		
		placements.setSize(stackIndex);

		nextStackItem.values.copyFrom(currentStackItem.values);
		nextStackItem.otherValues.copyFrom(currentStackItem.otherValues);
	}

	public void pop() {
		// no clear of current stack level necessary, everything is overwritten on push
		stackIndex--;
		loadCurrent();
	}

	private void loadCurrent() {
		StackItem stackItem = stackItems[stackIndex];

		this.values = stackItem.values;
		this.otherValues = stackItem.otherValues;
		this.minAreaLimit = stackItem.minAreaLimit;
		this.minVolumeLimit = stackItem.minVolumeLimit;
		
		placements.setSize(stackIndex);
	}

	public List<Point> getPoints() {
		// item 0 is always empty
		List<Point> list = new ArrayList<>(stackIndex + 1);
		for (int i = 1; i < stackIndex + 1; i++) {
			StackItem stackItem = stackItems[i];
			list.add(stackItem.point);
		}
		return list;
	}

	protected void resetBest() {
		bestStackIndex = 0;
	}

	protected void updateBest() {
		if(stackIndex > bestStackIndex) {
			for(int i = 0; i < stackIndex; i++) {
				bestPoints[i] = stackItems[i + 1].point;
			}
			bestStackIndex = stackIndex;
		}
	}

	protected int getBestStackIndex() {
		return bestStackIndex;
	}

	protected List<Point> getBestPoints() {
		return bestPointList;
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		stackIndex = 0;
		StackItem stackItem = stackItems[stackIndex];

		stackItem.values.clear();
		stackItem.otherValues.clear();
		stackItem.point = null;

		stackItem.values.add(createContainerPoint());

		placements.setSize(0);
		bestStackIndex = 0;
		
		loadCurrent();
	}

	@Override
	protected void saveValues(Point3DFlagList values, Point3DFlagList otherValues) {
		// override because of the way the stack works, 
		super.saveValues(values, otherValues);

		StackItem stackItem = stackItems[stackIndex];

		stackItem.values = otherValues;
		stackItem.otherValues = values;
	}

}
