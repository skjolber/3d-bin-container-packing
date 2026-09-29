package com.github.skjolber.packing.virtualbox;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;

/**
 * Additional, size-dependent placement origins aligned to physical face edges.
 * Try the calculator's normal points as well. Each returned origin is distinct,
 * is not an already fitting normal origin and fits wholly in a current free-space
 * point. Four edge alignments per face/free-space pair are clamped to that free
 * space, so partially covered faces and boxes spanning several faces are allowed.
 * This is a finite candidate heuristic, not an enumeration of every coordinate.
 *
 * Iteration does not modify the calculator. Commit using {@link #getSourcePointIndex()}
 * and the candidate's absolute coordinates, not by looking up the generated point
 * in the calculator. Source indexes remain valid only while that state is retained.
 * Restore both calculator and surface stack before resuming after a search branch,
 * or reset the iterator after a permanent mutation. Load validity must be checked
 * against the complete physical graph, not the returned point's plane hint alone.
 */
public class VirtualBoxSurfacePointIterator implements Iterator<SimplePoint3D> {
	protected VirtualBoxSurfacePoints surfaces;
	protected DefaultPointCalculator3D calculator;
	protected BoxStackValue value;
	protected LongHashSet seen = new LongHashSet();
	protected int faceLimit;
	protected int faceIndex;
	protected int sourceIndex;
	protected int corner;
	protected long dy;
	protected long dz;
	protected boolean pending;
	protected int nextX, nextY, nextZ, nextSourceIndex, nextFaceIndex;
	protected int currentSourceIndex = -1;

	/** Borrow the worker-local surface stack; it must not be modified during iteration. */
	public VirtualBoxSurfacePointIterator(VirtualBoxSurfacePoints surfaces) {
		this.surfaces = surfaces;
	}

	/** Reuse scratch storage for another candidate orientation and current calculator state. */
	public void reset(DefaultPointCalculator3D calculator, BoxStackValue value) {
		// Linear coordinate keys are unique within a valid, long-volume container.
		VirtualBoxBounds.validateDimensions(calculator.getDx(), calculator.getDy(), calculator.getDz());
		this.calculator = calculator;
		this.value = value;
		this.dy = calculator.getDy();
		this.dz = calculator.getDz();
		faceLimit = surfaces.size;
		faceIndex = sourceIndex = corner = 0;
		currentSourceIndex = -1;
		pending = false;
		seen.clear();
		for(int i = 0; i < calculator.size(); i++) {
			SimplePoint3D point = calculator.get(i);
			if(point.fits3D(value)) {
				seen.add(key(point.getMinX(), point.getMinY(), point.getMinZ()));
			}
		}
	}

	protected long key(int x, int y, int z) {
		return ((long) x * dy + y) * dz + z;
	}

	@Override
	public boolean hasNext() {
		if(pending) {
			return true;
		}
		while(faceIndex < faceLimit) {
			if(sourceIndex == calculator.size()) {
				faceIndex++;
				sourceIndex = corner = 0;
				continue;
			}
			Placement face = surfaces.faces[faceIndex];
			SimplePoint3D source = calculator.get(sourceIndex);
			int z = surfaces.tops[faceIndex] ? face.getAbsoluteEndZ() + 1 : face.getAbsoluteZ() - value.getDz();
			if(corner == 4 || z < source.getMinZ() || (long) z + value.getDz() - 1 > source.getMaxZ() || !source.fits3D(value)) {
				sourceIndex++;
				corner = 0;
				continue;
			}
			int alignment = corner++;
			int x = (alignment & 1) == 0 ? face.getAbsoluteX() : face.getAbsoluteEndX() - value.getDx() + 1;
			int y = (alignment & 2) == 0 ? face.getAbsoluteY() : face.getAbsoluteEndY() - value.getDy() + 1;
			x = Math.max(source.getMinX(), Math.min(x, source.getMaxX() - value.getDx() + 1));
			y = Math.max(source.getMinY(), Math.min(y, source.getMaxY() - value.getDy() + 1));
			if(x > face.getAbsoluteEndX() || y > face.getAbsoluteEndY()
					|| x + value.getDx() - 1 < face.getAbsoluteX() || y + value.getDy() - 1 < face.getAbsoluteY()
					|| !seen.add(key(x, y, z))) {
				continue;
			}
			nextX = x;
			nextY = y;
			nextZ = z;
			nextSourceIndex = sourceIndex;
			nextFaceIndex = faceIndex;
			pending = true;
			return true;
		}
		return false;
	}

	@Override
	public SimplePoint3D next() {
		if(!hasNext()) {
			throw new NoSuchElementException();
		}
		pending = false;
		currentSourceIndex = nextSourceIndex;
		SimplePoint3D point = calculator.get(nextSourceIndex);
		if(nextX != point.getMinX()) {
			point = point.moveX(nextX);
		}
		if(nextY != point.getMinY()) {
			point = point.moveY(nextY);
		}
		Placement face = surfaces.faces[nextFaceIndex];
		if(surfaces.tops[nextFaceIndex] && nextX >= face.getAbsoluteX() && nextY >= face.getAbsoluteY()) {
			// Plane support calculations assume the origin lies inside the face.
			// A spanning candidate may start before it: keep only the original,
			// valid hint in that case; the load graph computes combined support.
			point = point.moveZ(nextZ, face);
		} else if(nextZ != point.getMinZ()) {
			point = point.moveZ(nextZ);
		}
		return point;
	}

	/** Index of the containing calculator point for the most recent next() result. */
	public int getSourcePointIndex() {
		return currentSourceIndex;
	}
}
