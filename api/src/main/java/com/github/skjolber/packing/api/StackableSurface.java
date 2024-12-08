package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StackableSurface {

	public static final StackableSurface THREE_D = newBuilder().withBottom().withLeft().withRear().build();
	public static final StackableSurface TWO_D = newBuilder().withBottom().build();

	public static Builder newBuilder() {
		return new Builder();
	}

	private static class SurfaceRotate {
		private Surface surface;
		private boolean rotated; // 90 degrees

		public SurfaceRotate(Surface surface, boolean rotated) {
			super();
			this.surface = surface;
			this.rotated = rotated;
		}

		public boolean isRotated() {
			return rotated;
		}

		public Surface getSurface() {
			return surface;
		}
	}

	public static class Builder {

		protected List<SurfaceRotate> sides = new ArrayList<>();

		public Builder withFront() {
			return withSide(new SurfaceRotate(Surface.FRONT, false)).withSide(new SurfaceRotate(Surface.FRONT, true));
		}

		public Builder withFrontAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.FRONT, false));
		}

		public Builder withFrontAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.FRONT, true));
		}

		public Builder withRear() {
			return withSide(new SurfaceRotate(Surface.REAR, false)).withSide(new SurfaceRotate(Surface.REAR, true));
		}

		public Builder withRearAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.REAR, false));
		}

		public Builder withBackAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.REAR, true));
		}

		public Builder withLeft() {
			return withSide(new SurfaceRotate(Surface.LEFT, false)).withSide(new SurfaceRotate(Surface.LEFT, true));
		}

		public Builder withLeftAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.LEFT, false));
		}

		public Builder withLeftAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.LEFT, true));
		}

		public Builder withRight() {
			return withSide(new SurfaceRotate(Surface.RIGHT, false)).withSide(new SurfaceRotate(Surface.RIGHT, true));
		}

		public Builder withRightAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.RIGHT, false));
		}

		public Builder withRightAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.RIGHT, false));
		}

		public Builder withTop() {
			return withSide(new SurfaceRotate(Surface.TOP, false)).withSide(new SurfaceRotate(Surface.TOP, true));
		}

		public Builder withTopAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.TOP, false));
		}

		public Builder withTopAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.TOP, true));
		}

		public Builder withBottom() {
			return withSide(new SurfaceRotate(Surface.BOTTOM, false)).withSide(new SurfaceRotate(Surface.BOTTOM, true));
		}

		public Builder withBottomAtZeroDegrees() {
			return withSide(new SurfaceRotate(Surface.BOTTOM, false));
		}

		public Builder withBottomAtNinetyDegrees() {
			return withSide(new SurfaceRotate(Surface.BOTTOM, true));
		}

		public Builder withSide(Surface side, boolean rotate) {
			sides.add(new SurfaceRotate(side, rotate));
			return this;
		}

		protected Builder withSide(SurfaceRotate side) {
			sides.add(side);
			return this;
		}

		public StackableSurface build() {
			return new StackableSurface(sides);
		}
	}

	protected List<Surface> surfaces;

	protected Surface top0; // xy
	protected Surface top90;
	protected Surface bottom0; // xy
	protected Surface bottom90;
	protected Surface left0; // yz
	protected Surface left90;
	protected Surface right0; // yz
	protected Surface right90;
	protected Surface front0; // xz
	protected Surface front90;
	protected Surface rear0; // xz
	protected Surface rear90;

	public StackableSurface(List<SurfaceRotate> sides) {
		super();
		this.surfaces = sides.stream().map(s -> s.getSurface()).collect(Collectors.toList());
		for (SurfaceRotate surfaceRotate : sides) {

			switch (surfaceRotate.getSurface().label) {
			case TOP: {
				if(surfaceRotate.isRotated()) {
					top90 = surfaceRotate.getSurface();
				} else {
					top0 = surfaceRotate.getSurface();
				}
				break;
			}
			case BOTTOM: {
				if(surfaceRotate.isRotated()) {
					bottom90 = surfaceRotate.getSurface();
				} else {
					bottom0 = surfaceRotate.getSurface();
				}
				break;
			}
			case LEFT: {
				if(surfaceRotate.isRotated()) {
					left90 = surfaceRotate.getSurface();
				} else {
					left0 = surfaceRotate.getSurface();
				}
				break;
			}
			case RIGHT: {
				if(surfaceRotate.isRotated()) {
					right90 = surfaceRotate.getSurface();
				} else {
					right0 = surfaceRotate.getSurface();
				}
				break;
			}
			case FRONT: {
				if(surfaceRotate.isRotated()) {
					front90 = surfaceRotate.getSurface();
				} else {
					front0 = surfaceRotate.getSurface();
				}
				break;
			}
			case REAR: {
				if(surfaceRotate.isRotated()) {
					rear90 = surfaceRotate.getSurface();
				} else {
					rear0 = surfaceRotate.getSurface();
				}

				break;
			}
			default: {
				throw new IllegalStateException();
			}
			}
		}
	}

	public boolean isXY() {
		return isXY0() || isXY90();
	}

	public boolean isXY0() {
		return top0 != null || bottom0 != null;
	}

	public boolean isXY90() {
		return top90 != null || bottom90 != null;
	}

	public boolean isXZ0() {
		return front0 != null || rear0 != null;
	}

	public boolean isXZ90() {
		return front90 != null || rear90 != null;
	}

	public boolean isYZ0() {
		return left0 != null || right0 != null;
	}

	public boolean isYZ90() {
		return left90 != null || right90 != null;
	}

	public boolean is90() {
		return isYZ90() || isXY90() || isXZ90();
	}

	public boolean is0() {
		return isYZ0() || isXY0() || isXZ0();
	}

	public Surface getTop0() {
		return top0;
	}

	public Surface getTop90() {
		return top90;
	}

	public Surface getBottom0() {
		return bottom0;
	}

	public Surface getBottom90() {
		return bottom90;
	}

	public Surface getLeft0() {
		return left0;
	}

	public Surface getLeft90() {
		return left90;
	}

	public Surface getRight0() {
		return right0;
	}

	public Surface getRight90() {
		return right90;
	}

	public Surface getFront0() {
		return front0;
	}

	public Surface getFront90() {
		return front90;
	}

	public Surface getRear0() {
		return rear0;
	}

	public Surface getRear90() {
		return rear90;
	}

	public List<Surface> getSides() {
		return surfaces;
	}

	public List<Surface> getXYSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXY(surfaces);

		return surfaces;
	}

	public List<Surface> getXY0Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXY0(surfaces);

		return surfaces;
	}

	public List<Surface> getXY90Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXY90(surfaces);

		return surfaces;
	}

	private void addXY(List<Surface> surfaces) {
		addXY0(surfaces);
		addXY90(surfaces);
	}

	private void addXY0(List<Surface> surfaces) {
		if(top0 != null) {
			surfaces.add(top0);
		}
		if(bottom0 != null) {
			surfaces.add(bottom0);
		}
	}

	private void addXY90(List<Surface> surfaces) {
		if(top90 != null) {
			surfaces.add(top90);
		}
		if(bottom90 != null) {
			surfaces.add(bottom90);
		}
	}

	public List<Surface> getXZSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXZ(surfaces);

		return surfaces;
	}

	public List<Surface> getXZ0Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXZ0(surfaces);

		return surfaces;
	}

	public List<Surface> getXZ90Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXZ90(surfaces);

		return surfaces;
	}

	private void addXZ(List<Surface> surfaces) {
		addXZ0(surfaces);
		addXZ90(surfaces);
	}

	private void addXZ0(List<Surface> surfaces) {
		if(front0 != null) {
			surfaces.add(front0);
		}
		if(rear0 != null) {
			surfaces.add(rear0);
		}
	}

	private void addXZ90(List<Surface> surfaces) {
		if(front90 != null) {
			surfaces.add(front90);
		}
		if(rear90 != null) {
			surfaces.add(rear90);
		}
	}

	public List<Surface> getYZAndXZSurfaces0() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ0(surfaces);
		addXZ0(surfaces);

		return surfaces;
	}

	public List<Surface> getYZAndXZSurfaces90() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ90(surfaces);
		addXZ90(surfaces);

		return surfaces;
	}

	public List<Surface> getYZAndXZSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ(surfaces);
		addXZ(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndXZSurfaces0() {
		List<Surface> surfaces = new ArrayList<>();

		addXY0(surfaces);
		addXZ0(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndXZSurfaces90() {
		List<Surface> surfaces = new ArrayList<>();

		addXY90(surfaces);
		addXZ90(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndXZSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXY(surfaces);
		addXZ(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndYZSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addXY(surfaces);
		addYZ(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndYZSurfaces0() {
		List<Surface> surfaces = new ArrayList<>();

		addXY0(surfaces);
		addYZ0(surfaces);

		return surfaces;
	}

	public List<Surface> getXYAndYZSurfaces90() {
		List<Surface> surfaces = new ArrayList<>();

		addXY90(surfaces);
		addYZ90(surfaces);

		return surfaces;
	}

	public List<Surface> getYZSurfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ(surfaces);

		return surfaces;
	}

	public List<Surface> getYZ0Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ0(surfaces);

		return surfaces;
	}

	public List<Surface> getYZ90Surfaces() {
		List<Surface> surfaces = new ArrayList<>();

		addYZ90(surfaces);

		return surfaces;
	}

	private void addYZ0(List<Surface> surfaces) {
		if(left0 != null) {
			surfaces.add(left0);
		}
		if(right0 != null) {
			surfaces.add(right0);
		}
	}

	private void addYZ90(List<Surface> surfaces) {
		if(left90 != null) {
			surfaces.add(left90);
		}
		if(right90 != null) {
			surfaces.add(right90);
		}
	}

	private void addYZ(List<Surface> surfaces) {
		addYZ0(surfaces);
		addYZ90(surfaces);
	}

	public boolean isXZ() {
		return isXZ0() || isXZ90();
	}

	public boolean isYZ() {
		return isYZ0() || isYZ90();
	}

}
