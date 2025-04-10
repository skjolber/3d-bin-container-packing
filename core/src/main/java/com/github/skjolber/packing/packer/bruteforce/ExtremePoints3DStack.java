package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;

public class ExtremePoints3DStack extends ExtremePoints3D {

	protected static class StackItem {
		protected Point3DFlagList values = new Point3DFlagList();
		protected Point3DFlagList otherValues = new Point3DFlagList();
		protected ArrayList<StackPlacement> placements = new ArrayList<>();
		protected StackPlacement stackPlacement = new StackPlacement();
		protected Point point;
		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	protected List<StackItem> stackItems = new ArrayList<>();
	protected int stackIndex = 0;

	public ExtremePoints3DStack(int dx, int dy, int dz, int maxStackDepth) {
		super(dx, dy, dz, true);

		for (int i = 0; i < maxStackDepth; i++) {
			stackItems.add(new StackItem());
		}

		values.copyInto(stackItems.get(0).values);

		loadCurrent();
	}

	@Override
	public boolean add(int index, StackPlacement placement) {
		stackItems.get(this.stackIndex).point = values.get(index);

		return super.add(index, placement);
	}

	public StackPlacement push() {
		StackItem currentStackItem = stackItems.get(stackIndex);
		// save current state
		currentStackItem.minAreaLimit = minAreaLimit;
		currentStackItem.minVolumeLimit = minVolumeLimit;

		stackIndex++;

		StackItem nextStackItem = stackItems.get(stackIndex);

		// clone current state
		// make sure to overwrite everything, no clear is performed
		nextStackItem.point = null;
		nextStackItem.placements.clear();

		nextStackItem.placements.addAll(currentStackItem.placements);
		nextStackItem.values.copyFrom(currentStackItem.values);
		nextStackItem.otherValues.copyFrom(currentStackItem.otherValues);

		// set the current stack item as working variables
		this.values = nextStackItem.values;
		this.otherValues = nextStackItem.otherValues;
		this.placements = nextStackItem.placements;

		return nextStackItem.stackPlacement;
	}

	public int getStackIndex() {
		return stackIndex;
	}

	public void redo() {
		// i.e. copy values from the previous value into the current
		StackItem currentStackItem = stackItems.get(stackIndex - 1);

		StackItem nextStackItem = stackItems.get(stackIndex);
		nextStackItem.point = null;
		nextStackItem.placements.clear();

		nextStackItem.placements.addAll(currentStackItem.placements);

		nextStackItem.values.copyFrom(currentStackItem.values);
		nextStackItem.otherValues.copyFrom(currentStackItem.otherValues);
	}

	public void pop() {
		// no clear of current stack level necessary, everything is overwritten on push
		stackIndex--;
		loadCurrent();
	}

	private void loadCurrent() {
		StackItem stackItem = stackItems.get(stackIndex);

		this.values = stackItem.values;
		this.otherValues = stackItem.otherValues;
		this.placements = stackItem.placements;
		this.minAreaLimit = stackItem.minAreaLimit;
		this.minVolumeLimit = stackItem.minVolumeLimit;
	}

	public List<Point> getPoints() {
		// item 0 is always empty
		List<Point> list = new ArrayList<>(stackIndex + 1);
		for (int i = 1; i < stackIndex + 1; i++) {
			StackItem stackItem = stackItems.get(i);
			list.add(stackItem.point);
		}
		return list;
	}

	public List<StackPlacement> getStackPlacement() {
		List<StackPlacement> list = new ArrayList<>(stackIndex);
		for (StackItem stackItem : stackItems) {
			list.add(stackItem.stackPlacement);
		}
		return list;
	}

	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		stackIndex = 0;
		StackItem stackItem = stackItems.get(stackIndex);

		stackItem.placements.clear();
		stackItem.values.clear();
		stackItem.otherValues.clear();
		stackItem.point = null;

		stackItem.values.add(firstPoint);

		loadCurrent();
	}

	@Override
	protected void saveValues(Point3DFlagList values, Point3DFlagList otherValues) {
		// override because of the way the stack works, 
		super.saveValues(values, otherValues);

		StackItem stackItem = stackItems.get(stackIndex);

		stackItem.values = otherValues;
		stackItem.otherValues = values;
	}

}
