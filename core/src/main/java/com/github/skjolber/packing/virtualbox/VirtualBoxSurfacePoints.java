package com.github.skjolber.packing.virtualbox;

import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Placement;

/**
 * Worker-local stack of physical top/bottom faces. One frame represents one
 * logical placement, possibly containing many children. Faces covered by later
 * placements need not be removed: candidate iteration checks current free space.
 * This does not change the calculator's geometric pruning or combine load limits.
 */
public class VirtualBoxSurfacePoints {
	protected Placement[] faces = new Placement[0];
	protected boolean[] tops = new boolean[0];
	protected int[] frameEnds = new int[0];
	protected int size;
	protected int stackSize;

	public VirtualBoxSurfacePoints() {
	}

	/**
	 * Register a translated, accepted layout in its original child order. Retains
	 * placement references directly; do not modify them until this frame is undone.
	 * The layout must be a filled rectangle. Its internal faces are not registered.
	 */
	public void push(VirtualBoxLayout layout, List<Placement> children) {
		if(children.size() != layout.getPlacements().size()) {
			throw new IllegalArgumentException("Expected one placement per layout child");
		}
		VirtualBoxLayoutSupport support = layout.getLoadSupport();
		ensureCapacity(support.getTopFaces().length + support.getBottomFaces().length);
		for(int index : support.getTopFaces()) {
			faces[size] = children.get(index);
			tops[size++] = true;
		}
		for(int index : support.getBottomFaces()) {
			faces[size] = children.get(index);
			tops[size++] = false;
		}
		frameEnds[stackSize++] = size;
	}

	/** Register an ordinary physical placement in a mixed virtual/ordinary operation. */
	public void push(Placement placement) {
		ensureCapacity(2);
		faces[size] = placement;
		tops[size++] = true;
		faces[size] = placement;
		tops[size++] = false;
		frameEnds[stackSize++] = size;
	}

	protected void ensureCapacity(int additionalFaces) {
		if(size + additionalFaces > faces.length) {
			int capacity = Math.max(size + additionalFaces, faces.length * 2 + 16);
			faces = Arrays.copyOf(faces, capacity);
			tops = Arrays.copyOf(tops, capacity);
		}
		if(stackSize == frameEnds.length) {
			frameEnds = Arrays.copyOf(frameEnds, stackSize * 2 + 16);
		}
	}

	public void pop() {
		setStackSize(stackSize - 1);
	}

	/** Undo complete logical placements alongside point-calculator/load-graph rollback. */
	public void setStackSize(int count) {
		if(count < 0 || count > stackSize) {
			throw new IllegalArgumentException("Expected an existing surface stack depth");
		}
		int nextSize = count == 0 ? 0 : frameEnds[count - 1];
		Arrays.fill(faces, nextSize, size, null);
		size = nextSize;
		stackSize = count;
	}

	public int getStackSize() { return stackSize; }
	public int size() { return size; }

	public void clear() {
		setStackSize(0);
	}

	/** Allocate once per worker/search depth, then reuse via the iterator's reset method. */
	public VirtualBoxSurfacePointIterator newIterator() {
		return new VirtualBoxSurfacePointIterator(this);
	}
}
