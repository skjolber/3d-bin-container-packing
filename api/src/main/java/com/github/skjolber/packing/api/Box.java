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

	public static class Builder extends AbstractPhysicsBuilder<Builder> {

		protected Integer weight;

		protected String id;
		protected String description;

		protected Map<String, Object> properties;
		
		public Builder withDescription(String description) {
			this.description = description;
			return (Builder)this;
		}
		
		public Builder withProperty(String id, Object object) {
			if(properties == null) {
				properties = new HashMap<>();
			}
			properties.put(id, object);
			return (Builder)this;
		}
		
		public Builder withProperties(Map<String, Object> properties) {
			if(this.properties == null) {
				this.properties = new HashMap<>();
			}
			this.properties.putAll(properties);
			return (Builder)this;
		}

		public Builder withId(String id) {
			this.id = id;
			return (Builder)this;
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

			int dx = size.getDx();
			int dy = size.getDy();
			int dz = size.getDz();

			// dx, dy, dz

			if(dx == dy && dx == dz) { // square 3d
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

				if(stackableSurface.is0() || stackableSurface.is90()) {
					list.add(newStackValue(dx, dy, dz, stackableSurface.getSides(), list.size()));
				}
			} else if(dx == dy) {

				// z               y
				// |              / 
				// |             / 
				// |     /---------/|
				// |    /   xy    / | 
				// |   /         /  |
				// |  |---------|   |      
				// |  |         |   |
				// |  |         | y |
				// |  |    xz   | z |
				// |  |         |   |
				// |  |         |   |
				// |  |         |  /
				// |  |         | /
				// |  |---------|/      
				// | /      
				// |/       
				// |------------------ x
				//

				// two square sides, the other 4 sides are equal (but can be rotated)
				// add xz/yz and xy

				if(stackableSurface.isXY()) {
					list.add(newStackValue(dx, dx, dz, stackableSurface.getXYSurfaces(), list.size()));
				}
				if(stackableSurface.isXZ() || stackableSurface.isYZ()) {

					boolean zero = stackableSurface.isXZ0() || stackableSurface.isYZ0();
					boolean ninety = stackableSurface.isXZ90() || stackableSurface.isYZ90();

					if(zero) {
						list.add(newStackValue(dx, dz, dx, stackableSurface.getYZAndXZSurfaces0(), list.size()));
					}
					if(ninety) {
						list.add(newStackValue(dz, dx, dx, stackableSurface.getYZAndXZSurfaces90(), list.size()));

					}

				}
			} else if(dz == dy) {

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

				if(stackableSurface.isYZ()) {
					list.add(newStackValue(dy, dy, dx, stackableSurface.getYZSurfaces(), list.size()));
				}
				if(stackableSurface.isXY() || stackableSurface.isXZ()) {

					boolean zero = stackableSurface.isXY0() || stackableSurface.isXZ0();
					boolean ninety = stackableSurface.isXY90() || stackableSurface.isXZ90();

					if(zero) {
						list.add(newStackValue(dx, dz, dz, stackableSurface.getXYAndXZSurfaces0(), list.size()));
					}
					if(ninety) {
						list.add(newStackValue(dz, dx, dz, stackableSurface.getXYAndXZSurfaces90(), list.size()));
					}
				}

			} else if(dx == dz) {

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

				if(stackableSurface.isXZ()) {
					list.add(newStackValue(dx, dx, dy, stackableSurface.getXZSurfaces(), list.size()));
				}
				if(stackableSurface.isXY() || stackableSurface.isYZ()) {
					boolean zero = stackableSurface.isXY0() || stackableSurface.isYZ0();
					boolean ninety = stackableSurface.isXY90() || stackableSurface.isYZ90();

					if(zero) {
						list.add(newStackValue(dx, dy, dx, stackableSurface.getXYAndYZSurfaces0(), list.size()));
					}
					if(ninety) {
						list.add(newStackValue(dy, dx, dx, stackableSurface.getXYAndYZSurfaces90(), list.size()));
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

				if(stackableSurface.isXY0()) {
					list.add(newStackValue(dx, dy, dz, stackableSurface.getXY0Surfaces(), list.size()));
				}
				if(stackableSurface.isXY90()) {
					list.add(newStackValue(dy, dx, dz,  stackableSurface.getXY90Surfaces(), list.size()));
				}

				if(stackableSurface.isXZ0()) {
					list.add(newStackValue(dx, dz, dy, stackableSurface.getXZ0Surfaces(), list.size()));
				}
				if(stackableSurface.isXZ90()) {
					list.add(newStackValue(dz, dx, dy, stackableSurface.getXZ90Surfaces(), list.size()));
				}

				if(stackableSurface.isYZ0()) {
					list.add(newStackValue(dz, dy, dx, stackableSurface.getYZ0Surfaces(), list.size()));
				}
				if(stackableSurface.isYZ90()) {
					list.add(newStackValue(dy, dz, dx, stackableSurface.getYZ90Surfaces(), list.size()));
				}
			}

			if(list.isEmpty()) {
				throw new IllegalStateException("Expected at least one stackable surface");
			}
			return list.toArray(newStackValueArray(list.size()));
		}		
		
		public Builder withWeight(int weight) {
			this.weight = weight;

			return this;
		}

		public Box build() {
			if(size == null) {
				throw new IllegalStateException("No size");
			}
			if(weight == null) {
				throw new IllegalStateException("No weight");
			}

			if(stackableSurface == null) {
				stackableSurface = StackableSurface.TWO_D;
			}
			
			if(properties == null) {
				properties = Collections.emptyMap();
			}

			return new Box(id, description, size.getVolume(), weight, getStackValues(), properties);
		}

		@SuppressWarnings("unchecked")
		protected <T> T[] newStackValueArray(int size) {
			return (T[])new BoxStackValue[size];
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
	
	public Box(String id, String description, long volume, int weight, BoxStackValue[] stackValues, Map<String, Object> properties) {
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
	}
	
	public Box(Box box, List<BoxStackValue> stackValues) {
		this(box.id, box.description, box.volume, box.weight, stackValues.toArray(new BoxStackValue[stackValues.size()]), box.properties);
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
		for(int i = 0; i < stackValues.length; i++) {
			stackValues[i] = this.stackValues[i].clone();
		}
		return new Box(id, description, volume, weight, stackValues, properties);
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
		if(id != null && !id.isEmpty()) {
			builder.append(' ');
			builder.append(id);
		}
		if(description != null && !description.isEmpty()) {
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
	
	public List<BoxStackValue> fitsInside(Dimension bound) {
		List<BoxStackValue> list = new ArrayList<>();

		for (BoxStackValue stackValue : getStackValues()) {
			if(stackValue.fitsInside3D(bound)) {
				list.add(stackValue);
			}
		}

		return list;
	}
	
	public List<BoxStackValue> rotations(Dimension bound) {
		return rotations(bound.getDx(), bound.getDy(), bound.getDz());
	}

	public List<BoxStackValue> rotations(int dx, int dy, int dz) {
		// TODO optimize if max is above min bounds 
		BoxStackValue[] rotations = getStackValues();
		for (int i = 0; i < rotations.length; i++) {
			BoxStackValue stackValue = rotations[i];
			if(stackValue.fitsInside3D(dx, dy, dz)) {
				List<BoxStackValue> fitsInside = new ArrayList<>(rotations.length);
				fitsInside.add(stackValue);

				i++;
				while (i < rotations.length) {
					if(rotations[i].fitsInside3D(dx, dy, dz)) {
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
			if(minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}
	
	public static BoxStackValue getMinimumPressure(BoxStackValue[] rotations) {
		BoxStackValue minimumArea = null;
		for (BoxStackValue boxStackValue : rotations) {
			if(minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

	public static BoxStackValue getMaximumArea(BoxStackValue[] rotations) {
		BoxStackValue minimumArea = null;
		for (BoxStackValue boxStackValue : rotations) {
			if(minimumArea == null || boxStackValue.getArea() > minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key) {
		return (T) properties.get(key);
	}

}
