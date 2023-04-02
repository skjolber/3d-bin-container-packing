package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;

public class ExtremePoints3DStack extends ExtremePoints3D<StackPlacement> {

	protected static class StackItem {
		protected Point3DFlagList<StackPlacement> values = new Point3DFlagList<>();
		protected Point3DFlagList<StackPlacement> otherValues = new Point3DFlagList<>();
		protected ArrayList<StackPlacement> placements = new ArrayList<>();
		protected StackPlacement stackPlacement = new StackPlacement();
		protected Point3D<StackPlacement> point;
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
		StackItem stackItem = stackItems.get(stackIndex);

		stackIndex++;

		StackItem nextStackItem = stackItems.get(stackIndex);
		nextStackItem.placements.addAll(stackItem.placements);
		stackItem.values.copyInto(nextStackItem.values);

		stackItem.minAreaLimit = minAreaLimit;
		stackItem.minVolumeLimit = minVolumeLimit;

		loadCurrent();

		return nextStackItem.stackPlacement;
	}
	
	public int getStackIndex() {
		return stackIndex;
	}

	public void redo() {
		// clear current level
		placements.clear();

		StackItem stackItem = stackItems.get(stackIndex);
		stackItem.point = null;

		// copy from previous level
		StackItem previousStackItem = stackItems.get(stackIndex - 1);
		minAreaLimit = previousStackItem.minAreaLimit;
		minVolumeLimit = previousStackItem.minVolumeLimit;

		placements.addAll(previousStackItem.placements);

		previousStackItem.values.copyInto(values);
	}

	public void pop() {
		StackItem nextStackItem = stackItems.get(stackIndex);
		nextStackItem.placements.clear();
		nextStackItem.values.clear();

		this.minAreaLimit = 0;
		this.minVolumeLimit = 0;

		nextStackItem.point = null;

		stackIndex--;
		loadCurrent();
	}

	private void loadCurrent() {
		StackItem stackItem = stackItems.get(stackIndex);

		this.values = stackItem.values;

		xxComparator.setValues(values);
		yyComparator.setValues(values);
		zzComparator.setValues(values);

		this.otherValues = stackItem.otherValues;
		this.placements = stackItem.placements;
		this.minAreaLimit = stackItem.minAreaLimit;
		this.minVolumeLimit = stackItem.minVolumeLimit;

	}

	public List<Point3D<StackPlacement>> getPoints() {
		// item 0 is always empty
		List<Point3D<StackPlacement>> list = new ArrayList<>(stackIndex + 1);
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

		for (int i = 0; i < stackItems.size(); i++) {
			StackItem stackItem = stackItems.get(i);
			stackItem.point = null;

			stackItem.placements.clear();
			stackItem.values.clear();
		}

		stackIndex = 0;

		loadCurrent();

		values.add(firstPoint);
	}

}
