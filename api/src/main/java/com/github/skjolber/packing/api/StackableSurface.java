package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Surface.Label;

public class StackableSurface {

	public static final StackableSurface THREE_D = newBuilder().withBottom().withLeft().withBack().build();
	public static final StackableSurface TWO_D = newBuilder().withBottom().build();

	public static Builder newBuilder() {
		return new Builder();
	}
	
	private static class SideRotate {
		private Surface surface;
		private boolean rotated; // 90 degrees
		
		public SideRotate(Surface surface, boolean rotated) {
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
		
		protected List<SideRotate> sides = new ArrayList<>();

		public Builder withFrontAnyRotation() {
			return withSide(new SideRotate(new Surface(Label.FRONT), false)).withSide(new SideRotate(new Surface(Label.FRONT), true));
		}

		public Builder withFrontAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.FRONT), false));
		}

		public Builder withFrontAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.FRONT), true));
		}
		
		public Builder withBack() {
			return withSide(new SideRotate(new Surface(Label.REAR), false)).withSide(new SideRotate(new Surface(Label.REAR), true));
		}

		public Builder withBackAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.REAR), false));
		}

		public Builder withBackAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.REAR), true));
		}

		public Builder withLeft() {
			return withSide(new SideRotate(new Surface(Label.LEFT), false)).withSide(new SideRotate(new Surface(Label.LEFT), true));
		}

		public Builder withLeftAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.LEFT), false));
		}

		public Builder withLeftAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.LEFT), true));
		}

		public Builder withRight() {
			return withSide(new SideRotate(new Surface(Label.RIGHT), false)).withSide(new SideRotate(new Surface(Label.RIGHT), true));
		}

		public Builder withRightAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.RIGHT), false));
		}

		public Builder withRightAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.RIGHT), false));
		}

		public Builder withTop() {
			return withSide(new SideRotate(new Surface(Label.TOP), false)).withSide(new SideRotate(new Surface(Label.TOP), true));
		}

		public Builder withTopAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.TOP), false));
		}

		public Builder withTopAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.TOP), true));
		}

		public Builder withBottom() {
			return withSide(new SideRotate(new Surface(Label.BOTTOM), false)).withSide(new SideRotate(new Surface(Label.BOTTOM), true));
		}

		public Builder withBottomAtZeroDegrees() {
			return withSide(new SideRotate(new Surface(Label.BOTTOM), false));
		}

		public Builder withBottomAtNinetyDegrees() {
			return withSide(new SideRotate(new Surface(Label.BOTTOM), true));
		}

		public Builder withSide(Surface side, boolean rotate) {
			sides.add(new SideRotate(side, rotate));
			return this;
		}
		
		protected Builder withSide(SideRotate side) {
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
	
	public StackableSurface(List<SideRotate> sides) {
		super();
		this.surfaces = sides.stream().map(s -> s.getSurface()).collect(Collectors.toList());
		for(SideRotate sideRotate : sides) {
			
			switch(sideRotate.getSurface().label) {
			case TOP: {
				if(sideRotate.isRotated()) {
					top90 = sideRotate.getSurface();
				} else {
					top0 = sideRotate.getSurface();
				}
				break;
			}
			case BOTTOM: {
				if(sideRotate.isRotated()) {
					bottom90 = sideRotate.getSurface();
				} else {
					bottom0 = sideRotate.getSurface();
				}
				break;
			}
			case LEFT: {
				if(sideRotate.isRotated()) {
					left90 = sideRotate.getSurface();
				} else {
					left0 = sideRotate.getSurface();
				}
				break;
			}
			case RIGHT: {
				if(sideRotate.isRotated()) {
					right90 = sideRotate.getSurface();
				} else {
					right0 = sideRotate.getSurface();
				}
				break;
			}
			case FRONT: {
				if(sideRotate.isRotated()) {
					front90 = sideRotate.getSurface();
				} else {
					front0 = sideRotate.getSurface();
				}
				break;
			}
			case REAR: {
				if(sideRotate.isRotated()) {
					rear90 = sideRotate.getSurface();
				} else {
					rear0 = sideRotate.getSurface();
				}
				
				break;
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
