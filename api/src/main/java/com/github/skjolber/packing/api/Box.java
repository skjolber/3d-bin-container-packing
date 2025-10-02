package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Box {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected Integer weight;

		protected String id;
		protected String description;

		protected Map<String, Object> properties;
		protected BoxItem boxItem;
		
		protected int dx = -1;
		protected int dy = -1;
		protected int dz = -1;
		
		protected Rotation rotation;

		public Builder withSize(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;

			return this;
		}

		public Builder withRotate2D() {
			return withRotation(Rotation.TWO_D);
		}

		public Builder withRotate3D() {
			return withRotation(Rotation.THREE_D);
		}

		public Builder withRotation(Rotation rotation) {
			this.rotation = rotation;

			return this;
		}

		public Builder withBoxItem(BoxItem boxItem) {
			this.boxItem = boxItem;
			return (Builder) this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return (Builder) this;
		}

		public Builder withProperty(String id, Object object) {
			if (properties == null) {
				properties = new HashMap<>();
			}
			properties.put(id, object);
			return (Builder) this;
		}

		public Builder withProperties(Map<String, Object> properties) {
			if (this.properties == null) {
				this.properties = new HashMap<>();
			}
			this.properties.putAll(properties);
			return (Builder) this;
		}

		public Builder withId(String id) {
			this.id = id;
			return (Builder) this;
		}

		protected <T> T[] getStackValues() {

			// z              y
			// |             / 
			// |            / 
			// |         /-------/|
			// |        /       / |
			// |       /       /  |    
			// |      /  top  /t  /
			// |     /   xy  /h  /  
			// |    /       /g z/ 
			// |   /       /i y/
			// |  |-------|r  /      
			// |  |  xz   |  /
			// |  | front | /
			// |  |-------|/      
			// | /      
			// |/       
			// |------------------ x
			//

			List<BoxStackValue> list = new ArrayList<>();

			// dx, dy, dz

			if (dx == dy && dx == dz) { // square 3d
				// all sides are equal

				// z          y
				// |         / 
				// |        / 
				// |     /-------/|
				// |    / xy    / | 
				// |   /       /  |
				// |  |-------|yz /      
				// |  |       |  /
				// |  |   xz  | /
				// |  |-------|/      
				// | /      
				// |/       
				// |------------------ x
				//

				if (rotation.is0() || rotation.is90()) {
					list.add(newStackValue(dx, dy, dz, rotation.getSides(), list.size()));
				}
			} else if (dx == dy) {


				// z               y
				// |              / 
				// |             / 
				// |     /-------/|
				// |    /  xy   / | 
				// |   /       /  |
				// |  |-------|   |      
				// |  |       |   |
				// |  |       | y |
				// |  |  xz   | z |
				// |  |       |   |
				// |  |       |   |
				// |  |       |  /
				// |  |       | /
				// |  |-------|/      
				// | /      
				// |/       
				// |------------------ x
				//

				// two square sides, the other 4 sides are equal (but can be rotated)
				// add xz/yz and xy

				if (rotation.isXY()) {
					list.add(newStackValue(dx, dx, dz, rotation.getXYSurfaces(), list.size()));
				}
				if (rotation.isXZ() || rotation.isYZ()) {

					boolean zero = rotation.isXZ0() || rotation.isYZ0();
					boolean ninety = rotation.isXZ90() || rotation.isYZ90();

					if (zero) {
						list.add(newStackValue(dx, dz, dx, rotation.getYZAndXZSurfaces0(), list.size()));
					}
					if (ninety) {
						list.add(newStackValue(dz, dx, dx, rotation.getYZAndXZSurfaces90(), list.size()));

					}

				}
			} else if (dz == dy) {

				// z           y
				// |          / 
				// |         / 
				// |     /--------------------/|
				// |    /        xy          / | 
				// |   /                    /  |
				// |  |--------------------| yz/      
				// |  |         xz         |  /
				// |  |                    | /
				// |  |--------------------|/      
				// | /      
				// |/       
				// |----------------------------------- x
				//

				// two square sides, the other 4 sides are equal (but can be rotated)
				// add xz/xy and yz

				if (rotation.isYZ()) {
					list.add(newStackValue(dy, dy, dx, rotation.getYZSurfaces(), list.size()));
				}
				if (rotation.isXY() || rotation.isXZ()) {

					boolean zero = rotation.isXY0() || rotation.isXZ0();
					boolean ninety = rotation.isXY90() || rotation.isXZ90();

					if (zero) {
						list.add(newStackValue(dx, dz, dz, rotation.getXYAndXZSurfaces0(), list.size()));
					}
					if (ninety) {
						list.add(newStackValue(dz, dx, dz, rotation.getXYAndXZSurfaces90(), list.size()));
					}
				}

			} else if (dx == dz) {

				//  
				// z               y
				// |              / 
				// |             / 
				// |         /-------/|
				// |        /       / |
				// |       /       /  |    
				// |      /  xy   /   /
				// |     /       /   /  
				// |    /       / z / 
				// |   /       / y /
				// |  |-------|   /      
				// |  |       |  /
				// |  |  xz   | /
				// |  |-------|/      
				// | /      
				// |/       
				// |------------------ x
				//
				// two square sides, the other 4 sides are equal (but can be rotated)
				// add xy/zy and xz

				if (rotation.isXZ()) {
					list.add(newStackValue(dx, dx, dy, rotation.getXZSurfaces(), list.size()));
				}
				if (rotation.isXY() || rotation.isYZ()) {
					boolean zero = rotation.isXY0() || rotation.isYZ0();
					boolean ninety = rotation.isXY90() || rotation.isYZ90();

					if (zero) {
						list.add(newStackValue(dx, dy, dx, rotation.getXYAndYZSurfaces0(), list.size()));
					}
					if (ninety) {
						list.add(newStackValue(dy, dx, dx, rotation.getXYAndYZSurfaces90(), list.size()));
					}
				}
			} else {
				// no equal length edges

				//
				//              dx
				// ---------------------------
				// |                         |
				// |                         | dy
				// |                         |
				// ---------------------------
				//
				//    dy
				// --------
				// |      |
				// |      |
				// |      |
				// |      |
				// |      |
				// |      | dz
				// |      |
				// |      |
				// |      |
				// |      |
				// |      |
				// --------
				//			
				//              dx
				// ---------------------------
				// |                         |
				// |                         |
				// |                         |
				// |                         | dz
				// |                         |
				// |                         |
				// --------------------------- 
				//
				//
				//    dy
				// ----------------
				// |              |
				// |              |
				// |              |
				// |              |
				// |              |
				// |              | dx
				// |              |
				// |              |
				// |              |
				// |              |
				// |              |
				// ----------------
				//			
				//			
				//    dy
				// --------
				// |      |
				// |      |
				// |      |
				// |      | dz
				// |      |
				// |      |
				// --------
				//
				//        dy
				// ----------------
				// |              |
				// |              | dz
				// |              |
				// ----------------
				//			


				if (rotation.isXY0()) {
					list.add(newStackValue(dx, dy, dz, rotation.getXY0Surfaces(), list.size()));
				}
				if (rotation.isXY90()) {
					list.add(newStackValue(dy, dx, dz, rotation.getXY90Surfaces(), list.size()));
				}

				if (rotation.isXZ0()) {
					list.add(newStackValue(dx, dz, dy, rotation.getXZ0Surfaces(), list.size()));
				}
				if (rotation.isXZ90()) {
					list.add(newStackValue(dz, dx, dy, rotation.getXZ90Surfaces(), list.size()));
				}

				if (rotation.isYZ0()) {
					list.add(newStackValue(dz, dy, dx, rotation.getYZ0Surfaces(), list.size()));
				}
				if (rotation.isYZ90()) {
					list.add(newStackValue(dy, dz, dx, rotation.getYZ90Surfaces(), list.size()));
				}
			}

			if (list.isEmpty()) {
				throw new IllegalStateException("Expected at least one stackable surface");
			}
			return list.toArray(newStackValueArray(list.size()));
		}

		public Builder withWeight(int weight) {
			this.weight = weight;

			return this;
		}

		public Box build() {
			if (dx == -1 || dy == -1 || dz == -1) {
				throw new IllegalStateException("No size");
			}
			if (weight == null) {
				throw new IllegalStateException("No weight");
			}

			if (rotation == null) {
				rotation = Rotation.TWO_D;
			}

			if (properties == null) {
				properties = Collections.emptyMap();
			}

			return new Box(id, description, (long)dy * (long)dx * (long)dz, weight, getStackValues(), properties, boxItem);
		}

		@SuppressWarnings("unchecked")
		protected <T> T[] newStackValueArray(int size) {
			return (T[]) new BoxStackValue[size];
		}

		protected BoxStackValue newStackValue(int dx, int dy, int dz, List<Surface> surfaces, int index) {
			return new BoxStackValue(dx, dy, dz, surfaces, index);
		}
	}

	protected final int weight;
	protected final BoxStackValue[] stackValues;
	protected final long volume;

	protected final BoxStackValue minimumArea;
	protected final BoxStackValue maximumArea;
	protected long minimumPressure;
	protected long maximumPressure;

	protected final String id;
	protected final String description;

	protected final Map<String, Object> properties;

	protected BoxItem boxItem;

	public Box(String id, String description, long volume, int weight, BoxStackValue[] stackValues,
			Map<String, Object> properties, BoxItem boxItem) {
		this.id = id;
		this.description = description;

		this.volume = volume;
		this.weight = weight;
		this.stackValues = stackValues;

		this.minimumArea = getMinimumArea(stackValues);
		this.maximumArea = getMinimumArea(stackValues);

		this.minimumPressure = (weight * 1000L) / maximumArea.getArea();
		this.maximumPressure = (weight * 1000L) / minimumArea.getArea();

		for (BoxStackValue boxStackValue : stackValues) {
			boxStackValue.setBox(this);
		}

		this.properties = properties;
		this.boxItem = boxItem;
	}

	public Box(Box box, List<BoxStackValue> stackValues) {
		this(box.id, box.description, box.volume, box.weight,
				stackValues.toArray(new BoxStackValue[stackValues.size()]), box.properties, box.boxItem);
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public int getWeight() {
		return weight;
	}

	public BoxStackValue[] getStackValues() {
		return stackValues;
	}

	public long getVolume() {
		return volume;
	}

	@Override
	public Box clone() {
		BoxStackValue[] stackValues = new BoxStackValue[this.stackValues.length];
		for (int i = 0; i < stackValues.length; i++) {
			stackValues[i] = this.stackValues[i].clone();
		}
		return new Box(id, description, volume, weight, stackValues, properties, boxItem);
	}

	public BoxStackValue getStackValue(int index) {
		return stackValues[index];
	}

	public long getMinimumArea() {
		return minimumArea.getArea();
	}

	public long getMaximumArea() {
		return maximumArea.getArea();
	}

	public String toString() {
		StringBuilder builder = new StringBuilder(256);
		builder.append("Box");
		if (id != null && !id.isEmpty()) {
			builder.append(' ');
			builder.append(id);
		}
		if (description != null && !description.isEmpty()) {
			builder.append(" (");
			builder.append(description);
			builder.append(")");
		}

		builder.append("[weight=");
		builder.append(weight);
		builder.append(", rotations=");
		builder.append(Arrays.toString(stackValues));
		builder.append(", volume=");
		builder.append(volume);
		builder.append(']');

		return builder.toString();
	}

	public long getMinimumPressure() {
		return minimumPressure;
	}

	public long getMaximumPressure() {
		return maximumPressure;
	}

	public boolean fitsInside(Container bound) {
		for (BoxStackValue stackValue : getStackValues()) {
			if (stackValue.fitsInside3D(bound)) {
				return true;
			}
		}

		return false;
	}
	
	public boolean fitsInside(int dx, int dy, int dz) {
		for (BoxStackValue stackValue : getStackValues()) {
			if (stackValue.fitsInside3D(dx, dy, dz)) {
				return true;
			}
		}

		return false;
	}

	public List<BoxStackValue> getStackValues(Container bound) {
		List<BoxStackValue> list = new ArrayList<>();

		for (BoxStackValue stackValue : getStackValues()) {
			if (stackValue.fitsInside3D(bound)) {
				list.add(stackValue);
			}
		}

		return list;
	}

	public List<BoxStackValue> rotations(Container bound) {
		return rotations(bound.getLoadDx(), bound.getLoadDy(), bound.getLoadDz());
	}

	public List<BoxStackValue> rotations(int dx, int dy, int dz) {
		// TODO optimize if max is above min bounds
		BoxStackValue[] rotations = getStackValues();
		for (int i = 0; i < rotations.length; i++) {
			BoxStackValue stackValue = rotations[i];
			if (stackValue.fitsInside3D(dx, dy, dz)) {
				List<BoxStackValue> fitsInside = new ArrayList<>(rotations.length);
				fitsInside.add(stackValue);

				i++;
				while (i < rotations.length) {
					if (rotations[i].fitsInside3D(dx, dy, dz)) {
						fitsInside.add(rotations[i]);
					}
					i++;
				}
				return fitsInside;
			}
		}
		return null;
	}

	public static BoxStackValue getMinimumArea(BoxStackValue[] rotations) {
		BoxStackValue minimumArea = null;
		for (BoxStackValue boxStackValue : rotations) {
			if (minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

	public static BoxStackValue getMinimumPressure(BoxStackValue[] rotations) {
		BoxStackValue minimumArea = null;
		for (BoxStackValue boxStackValue : rotations) {
			if (minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

	public static BoxStackValue getMaximumArea(BoxStackValue[] rotations) {
		BoxStackValue minimumArea = null;
		for (BoxStackValue boxStackValue : rotations) {
			if (minimumArea == null || boxStackValue.getArea() > minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key) {
		return (T) properties.get(key);
	}

	public void setBoxItem(BoxItem boxItem) {
		this.boxItem = boxItem;
	}

	public BoxItem getBoxItem() {
		return boxItem;
	}
}
