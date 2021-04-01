package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;


/**
 * {@linkplain Stackable} builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public class AbstractStackableBuilder<B extends AbstractStackableBuilder<B>> {

	protected static final int DEFAULT_PRESSURE_REFERENCE = 1000;

	public static class Rotation {
		
		protected int count;
		
		protected int maxSupportedCount;
		protected int maxSupportedWeight;

		protected int dx; // width
		protected int dy; // depth
		protected int dz; // height

		public Rotation(int count, int dx, int dy, int dz, int maxSupportedWeight, int maxSupportedCount) {
			super();
			this.count = count;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.maxSupportedWeight = maxSupportedWeight;
			this.maxSupportedCount = maxSupportedCount;
		}
	}
	
	protected List<Rotation> rotations = new ArrayList<>();
	
	protected int pressureReference = DEFAULT_PRESSURE_REFERENCE;
	protected String name;
	
	public B withName(String name) {
		this.name = name;
		return (B)this;
	}
	
	public B withPressureReference(int pressureReference) {
		this.pressureReference = pressureReference;
		
		return (B)this;
	}

	public B withRotate(int count, int dx, int dy, int dz, int maxSupportedCount, int maxSupportedWeight) {
		rotations.add(new Rotation(count, dx, dy, dz, maxSupportedWeight, maxSupportedCount));
		
		return (B)this;

	}

	public B withRotateXY(int count, int dx, int dy, int dz, int maxSupportedCount, int maxSupportedWeight) {
		rotations.add(new Rotation(count, dx, dy, dz, maxSupportedWeight, maxSupportedCount));
		
		if(dx != dy) {
			rotations.add(new Rotation(count, dy, dx, dz, maxSupportedWeight, maxSupportedCount));
		}
		
		return (B)this;
	}
	
	public B withRotateXYZ(int count, int dx, int dy, int dz, int maxSupportedCount, int maxSupportedWeight) {
		rotations.add(new Rotation(count, dx, dy, dz, maxSupportedWeight, maxSupportedCount));
		
		if(!(dx == dy && dx == dz)) {

			//
			//              dx
			// ---------------------------
			// |                         |
			// |                         | dy
			// |                         |
			// ---------------------------
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
			
			if(dx != dy) {
				rotations.add(new Rotation(count, dy, dx, dz, maxSupportedWeight, maxSupportedCount));
			}
			
			rotations.add(new Rotation(count, dx, dz, dy, maxSupportedWeight, maxSupportedCount));
			if(dx != dz) {
				rotations.add(new Rotation(count, dz, dx, dy, maxSupportedWeight, maxSupportedCount));
			}
			
			rotations.add(new Rotation(count, dy, dz, dx, maxSupportedWeight, maxSupportedCount));
			if(dy != dz) {
				rotations.add(new Rotation(count, dz, dy, dx, maxSupportedWeight, maxSupportedCount));
			}
		}			
		
		return (B)this;
	}

}
