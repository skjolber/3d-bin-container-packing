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
		protected Point3DFlagList values;

		protected int placementCount;
		protected long minVolumeLimit;
		protected long minAreaLimit;
	}

	private int stackSize = 0;
	private final StackItem[] stackItems;
	/** frame which takes the input list of the next add(..) as its snapshot, instead of a copy */
	private StackItem snapshot;

	public FastPointCalculator3DStack(int capacity) {
		super(true, capacity);

		// frames are created on first use: most searches use few frames per calculator
		stackItems = new StackItem[capacity];
	}

	@Override
	public boolean add(int index, Placement placement) {
		// copy state before it is updated
		SimplePoint3D point3d = values.get(index);

		StackItem stackItem = frame();
		stackItem.point = point3d;
		stackItem.placementCount = placements.size();
		stackItem.minVolumeLimit = minVolumeLimit;
		stackItem.minAreaLimit = minAreaLimit;
		// add(..) writes a new output list and only flags its input temporarily:
		// keep the input list itself as the snapshot (see saveValues)
		snapshot = stackItem;

		stackSize++;

		return super.add(index, placement);
	}

	/**
	 * As {@link #add(int, Placement)}, for a placement within the free point rather than at its origin (see
	 * {@link FullSupportCandidates}): the arrangement keeps the placement's position.
	 *
	 * @param position the placement's position
	 */
	public boolean add(int index, Placement placement, SimplePoint3D position) {
		boolean result = add(index, placement);
		stackItems[stackSize - 1].point = position;
		return result;
	}

	private FullSupportCandidates fullSupportCandidates;

	/** @return the positions where a box is fully supported, when full support is required (reused) */
	FullSupportCandidates getFullSupportCandidates() {
		if(fullSupportCandidates == null) {
			fullSupportCandidates = new FullSupportCandidates();
		}
		return fullSupportCandidates;
	}

	@Override
	protected boolean addBatch(int index, List<Placement> batch, long remainingMinimumArea, long remainingMinimumVolume) {
		snapshot = null;
		StackItem frame = frame();
		frame.point = values.get(index);
		frame.placementCount = placements.size();
		frame.minAreaLimit = minAreaLimit;
		frame.minVolumeLimit = minVolumeLimit;
		// a batch makes several insertions, so take an explicit copy before the first
		if(frame.values == null) {
			frame.values = new Point3DFlagList();
		}
		values.copyInto(frame.values);
		placements.ensureAdditionalCapacity(batch.size() + stackItems.length);
		try {
			boolean result = super.addBatch(index, batch, remainingMinimumArea, remainingMinimumVolume);
			stackSize++;
			return result;
		} catch(RuntimeException e) {
			// This subclass already owns a checkpoint: restore it on invalid input.
			placements.setSize(frame.placementCount);
			reload();
			throw e;
		}
	}

	@Override
	protected void saveValues(Point3DFlagList values, Point3DFlagList otherValues) {
		StackItem frame = snapshot;
		if(frame == null) {
			super.saveValues(values, otherValues);
			return;
		}
		snapshot = null;
		// the frame keeps the input; its previous snapshot becomes the next output buffer
		Point3DFlagList spare = frame.values;
		if(spare == null) {
			spare = new Point3DFlagList(values.getCapacity());
		} else {
			spare.resetWithoutFlags();
		}
		frame.values = values;
		this.values = otherValues;
		this.otherValues = spare;
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
		snapshot = null;

		super.clearToSize(dx, dy, dz);
	}

	private StackItem frame() {
		StackItem frame = stackItems[stackSize];
		if(frame == null) {
			frame = new StackItem();
			stackItems[stackSize] = frame;
		}
		return frame;
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
