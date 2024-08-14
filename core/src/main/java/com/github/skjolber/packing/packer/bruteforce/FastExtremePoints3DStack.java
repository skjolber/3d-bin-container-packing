package com.github.skjolber.packing.packer.bruteforce;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;

public class FastExtremePoints3DStack extends ExtremePoints3D {

	private static class StackItem  {
		// value for extraction
		protected Point3D point;

		// adding a point might affect any index in the values array
		protected Point3DFlagList values = new Point3DFlagList();

		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	private int stackSize = 0;
	private List<StackItem> stackItems;

	public FastExtremePoints3DStack(int dx, int dy, int dz, int capacity) {
		super(dx, dy, dz, true);

		stackItems = new ArrayList<StackItem>(capacity);
		for (int i = 0; i < capacity; i++) {
			stackItems.add(new StackItem());
		}
	}

	@Override
	public boolean add(int index, StackPlacement placement) {
		// copy state before it is updated
		Point3D point3d = values.get(index);

		StackItem stackItem = stackItems.get(stackSize);
		stackItem.point = point3d;
		stackItem.minVolumeLimit = minVolumeLimit;
		stackItem.minAreaLimit = minAreaLimit;
		values.copyInto(stackItem.values);

		stackSize++;

		return super.add(index, placement);
	}

	public List<Point3D> getPoints() {
		List<Point3D> results = new ArrayList<Point3D>(stackSize);
		for (int i = 0; i < stackSize; i++) {
			results.add(stackItems.get(i).point);
		}
		return results;
	}

	@Override
	public void reset(int dx, int dy, int dz) {
		stackSize = 0;

		super.reset(dx, dy, dz);
	}

	public void setStackSize(int size) {
		if(stackSize != size) {
			stackSize = size;

			while (size < placements.size()) {
				placements.remove(placements.size() - 1);
			}

			reload();
		}
	}

	private void reload() {
		StackItem stackItem = stackItems.get(stackSize);
		stackItem.values.copyInto(values);
		minVolumeLimit = stackItem.minVolumeLimit;
		minAreaLimit = stackItem.minAreaLimit;
	}

}
