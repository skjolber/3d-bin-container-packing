package com.github.skjolber.packing.virtualbox;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Read-only internal contacts and exposed horizontal faces of a filled layout.
 * All indexes refer to its placement list, not BoxItem indexes. Contacts describe
 * geometry, not remaining load capacity. Returned arrays are borrowed and must
 * not be modified. Mutable load links belong to worker placements, never here.
 */
public class VirtualBoxLayoutSupport {
	protected int[] supporters;
	protected int[] supportees;
	protected long[] contactAreas;
	protected int[] topFaces;
	protected int[] bottomFaces;

	/** Retain arrays directly; callers must not modify them after construction. */
	protected VirtualBoxLayoutSupport(int[] supporters, int[] supportees, long[] contactAreas, int[] topFaces, int[] bottomFaces) {
		this.supporters = supporters;
		this.supportees = supportees;
		this.contactAreas = contactAreas;
		this.topFaces = topFaces;
		this.bottomFaces = bottomFaces;
	}

	protected static VirtualBoxLayoutSupport create(VirtualBoxLayout layout) {
		IntArrayList supporters = new IntArrayList();
		IntArrayList supportees = new IntArrayList();
		LongArrayList areas = new LongArrayList();
		IntArrayList topFaces = new IntArrayList();
		IntArrayList bottomFaces = new IntArrayList();
		var placements = layout.getPlacements();
		for(int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			if(placement.getAbsoluteZ() == 0) {
				bottomFaces.add(i);
			}
			if(placement.getAbsoluteEndZ() == layout.getBoundingBox().dz() - 1) {
				topFaces.add(i);
			}
		}
		long[] order = layout.placementOrder;
		for(int i = 0; i < order.length; i++) {
			int leftIndex = (int) order[i];
			Placement left = placements.get(leftIndex);
			// A Z sweep includes the adjacent plane; X/Y sweeps require overlap.
			int end = layout.maximum(left) + (layout.sweepAxis == 2 ? 1 : 0);
			for(int j = i + 1; j < order.length && (order[j] >>> 32) <= end; j++) {
				int rightIndex = (int) order[j];
				Placement right = placements.get(rightIndex);
				int lower, upper;
				if(left.getAbsoluteEndZ() + 1 == right.getAbsoluteZ()) {
					lower = leftIndex;
					upper = rightIndex;
				} else if(right.getAbsoluteEndZ() + 1 == left.getAbsoluteZ()) {
					lower = rightIndex;
					upper = leftIndex;
				} else {
					continue;
				}
				long area = LoadPlacementUtility.overlapArea(left.getAbsoluteX(), left.getAbsoluteY(), left.getAbsoluteEndX(), left.getAbsoluteEndY(), right);
				if(area != 0) {
					supporters.add(lower);
					supportees.add(upper);
					areas.add(area);
				}
			}
		}
		return new VirtualBoxLayoutSupport(supporters.toArray(), supportees.toArray(), areas.toArray(), topFaces.toArray(), bottomFaces.toArray());
	}

	public int[] getSupporters() { return supporters; }
	public int[] getSupportees() { return supportees; }
	public long[] getContactAreas() { return contactAreas; }
	public int[] getTopFaces() { return topFaces; }
	public int[] getBottomFaces() { return bottomFaces; }
}
