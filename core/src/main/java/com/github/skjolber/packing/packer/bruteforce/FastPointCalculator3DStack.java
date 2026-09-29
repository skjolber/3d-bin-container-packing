package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

public class FastPointCalculator3DStack extends DefaultPointCalculator3D {

	private static class StackItem  {
		// value for extraction
		protected SimplePoint3D point;

		// adding a point might affect any index in the values array
		protected Point3DFlagList values = new Point3DFlagList();

		protected int placementCount;
		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	private int stackSize = 0;
	private final StackItem[] stackItems;

	public FastPointCalculator3DStack(int capacity) {
		super(true, capacity);

		stackItems = new StackItem[capacity];
		for (int i = 0; i < capacity; i++) {
			stackItems[i] = new StackItem();
		}
	}

	@Override
	public boolean add(int index, Placement placement) {
		// copy state before it is updated
		SimplePoint3D point3d = values.get(index);

		StackItem stackItem = stackItems[stackSize];
		stackItem.point = point3d;
		stackItem.placementCount = placements.size();
		stackItem.minVolumeLimit = minVolumeLimit;
		stackItem.minAreaLimit = minAreaLimit;
		values.copyInto(stackItem.values);

		stackSize++;

		return super.add(index, placement);
	}

	@Override
	public boolean add(int index, List<Placement> batch) {
		StackItem frame = stackItems[stackSize];
		frame.point = values.get(index);
		frame.placementCount = placements.size();
		frame.minAreaLimit = minAreaLimit;
		frame.minVolumeLimit = minVolumeLimit;
		values.copyInto(frame.values);
		placements.ensureAdditionalCapacity(batch.size() + stackItems.length);
		try {
			boolean result = super.add(index, batch);
			stackSize++;
			return result;
		} catch(RuntimeException e) {
			// This subclass already owns a checkpoint: restore it on invalid input.
			placements.setSize(frame.placementCount);
			reload();
			throw e;
		}
	}

	public List<Point> getPoints() {
		List<Point> results = new ArrayList<Point>(stackSize);
		for (int i = 0; i < stackSize; i++) {
			results.add(stackItems[i].point);
		}
		return results;
	}

	@Override
	public void clearToSize(int dx, int dy, int dz) {
		stackSize = 0;

		super.clearToSize(dx, dy, dz);
	}

	public void setStackSize(int size) {
		if(stackSize != size) {
			stackSize = size;

			placements.setSize(stackItems[size].placementCount);

			reload();
		}
	}

	private void reload() {
		StackItem stackItem = stackItems[stackSize];
		stackItem.values.copyInto(values);
		minVolumeLimit = stackItem.minVolumeLimit;
		minAreaLimit = stackItem.minAreaLimit;
	}

}
