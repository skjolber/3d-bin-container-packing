package com.github.skjolber.packing.packer.bruteforce;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Predicate;

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

	/**
	 * A search level. Points are immutable, and add(..) only flags its input list temporarily,
	 * so a new level reads its parent's list directly instead of copying it ("shared"). The
	 * level's own two buffers hold the output of its add(..), and a copy if the shared list
	 * must be modified otherwise (copy-on-write).
	 */
	protected static class StackItem {
		
		protected Point3DFlagList values = new Point3DFlagList();
		protected Point3DFlagList otherValues = new Point3DFlagList();
		/** current values are the parent level's list, which must not be modified */
		protected boolean shared;
		protected Point3DFlagList parentValues;
		protected Placement stackPlacement = new Placement(false);
		protected SimplePoint3D point;
		protected int placementCount;
		protected boolean batch;
		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	protected final StackItem[] stackItems;
	protected int stackIndex = 0;
	protected final SimplePoint3D[] bestPoints;
	protected final List<Point> bestPointList = new BestPointList();
	protected int bestStackIndex;
	private BruteForceSearchFrames searchFrames;

	public PointCalculator3DStack(int maxStackDepth) {
		super(true, maxStackDepth);
		this.stackItems = new StackItem[maxStackDepth];
		this.bestPoints = new SimplePoint3D[maxStackDepth];

		// deeper levels are created on first push: most searches use few levels per calculator
		stackItems[0] = new StackItem();

		values.copyInto(stackItems[0].values);

		loadCurrent();
	}

	@Override
	public boolean add(int index, Placement placement) {
		stackItems[this.stackIndex].point = values.get(index);

		return super.add(index, placement);
	}

	@Override
	protected boolean addBatch(int index, List<Placement> batch, long remainingMinimumArea, long remainingMinimumVolume) {
		stackItems[stackIndex].point = values.get(index);
		stackItems[stackIndex].batch = true;
		// Preserve space for subsequent ordinary search steps without changing
		// the single-placement insertion path.
		placements.ensureAdditionalCapacity(batch.size() + stackItems.length);
		return super.addBatch(index, batch, remainingMinimumArea, remainingMinimumVolume);
	}

	public Placement push() {
		StackItem currentStackItem = stackItems[stackIndex];
		// save current state
		currentStackItem.placementCount = placements.size();
		currentStackItem.minAreaLimit = minAreaLimit;
		currentStackItem.minVolumeLimit = minVolumeLimit;

		stackIndex++;

		StackItem nextStackItem = stackItems[stackIndex];
		if(nextStackItem == null) {
			nextStackItem = new StackItem();
			stackItems[stackIndex] = nextStackItem;
		}

		// share the current values instead of copying them
		nextStackItem.point = null;
		nextStackItem.batch = false;
		nextStackItem.parentValues = this.values;
		nextStackItem.shared = true;
		nextStackItem.otherValues.resetWithoutFlags();

		// set the current stack item as working variables
		this.values = nextStackItem.parentValues;
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
		
		placements.setSize(currentStackItem.placementCount);
		if(nextStackItem.batch) {
			// Batch minima belong to that arrangement. Keep ordinary insertion's
			// existing next-item filtering behavior unchanged.
			minAreaLimit = currentStackItem.minAreaLimit;
			minVolumeLimit = currentStackItem.minVolumeLimit;
			nextStackItem.batch = false;
		}

		if(!nextStackItem.shared) {
			// discard this level's values, and share the parent's again
			nextStackItem.values.resetWithoutFlags();
			nextStackItem.shared = true;
		}
		this.values = nextStackItem.parentValues;
		this.otherValues = nextStackItem.otherValues;
	}

	public void pop() {
		// no clear of current stack level necessary, everything is overwritten on push
		stackIndex--;
		loadCurrent();
	}

	private void loadCurrent() {
		StackItem stackItem = stackItems[stackIndex];

		this.values = stackItem.shared ? stackItem.parentValues : stackItem.values;
		this.otherValues = stackItem.otherValues;
		this.minAreaLimit = stackItem.minAreaLimit;
		this.minVolumeLimit = stackItem.minVolumeLimit;
		
		placements.setSize(stackItem.placementCount);
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

	/** Per-level state for the placement search, created on first use. */
	BruteForceSearchFrames getSearchFrames() {
		if(searchFrames == null) {
			searchFrames = new BruteForceSearchFrames(stackItems.length);
		}
		return searchFrames;
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
		stackItem.shared = false;
		stackItem.point = null;
		stackItem.placementCount = 0;

		stackItem.values.add(createContainerPoint());

		placements.setSize(0);
		bestStackIndex = 0;
		
		loadCurrent();
	}

	@Override
	protected void saveValues(Point3DFlagList values, Point3DFlagList otherValues) {
		// override because of the way the stack works, 
		StackItem stackItem = stackItems[stackIndex];
		if(stackItem.shared) {
			// the input belongs to the parent level: keep it, and use this level's spare buffer as next output
			Point3DFlagList spare = stackItem.values;
			spare.resetWithoutFlags();
			stackItem.values = otherValues;
			stackItem.otherValues = spare;
			stackItem.shared = false;
			this.values = otherValues;
			this.otherValues = spare;
			return;
		}
		super.saveValues(values, otherValues);

		stackItem.values = otherValues;
		stackItem.otherValues = values;
	}

	/** Copy-on-write: make the current values this level's own before modifying them. */
	protected void ensureOwnValues() {
		StackItem stackItem = stackItems[stackIndex];
		if(stackItem.shared) {
			values.copyInto(stackItem.values);
			values = stackItem.values;
			stackItem.shared = false;
		}
	}

	@Override
	public void setMinimumAreaAndVolumeLimit(long area, long volume) {
		if(minAreaLimit != area || minVolumeLimit != volume) {
			ensureOwnValues();
		}
		super.setMinimumAreaAndVolumeLimit(area, volume);
	}

	@Override
	public void setMinimumAreaLimit(long min) {
		if(minAreaLimit != min) {
			ensureOwnValues();
		}
		super.setMinimumAreaLimit(min);
	}

	@Override
	public void setMinimumVolumeLimit(long min) {
		if(minVolumeLimit != min) {
			ensureOwnValues();
		}
		super.setMinimumVolumeLimit(min);
	}

	@Override
	public void remove(int index) {
		ensureOwnValues();
		super.remove(index);
	}

	@Override
	public void remove(Predicate<Point> test) {
		ensureOwnValues();
		super.remove(test);
	}

	@Override
	public void clear() {
		ensureOwnValues();
		super.clear();
	}

}
