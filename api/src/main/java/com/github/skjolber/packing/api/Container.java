package com.github.skjolber.packing.api;

import java.util.List;

public class Container {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractContainerBuilder<Builder> {

		protected int emptyWeight = -1;
		protected Stack stack;

		public Builder withFixedStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withEmptyWeight(int emptyWeight) {
			this.emptyWeight = emptyWeight;

			return this;
		}

		public Container build() {
			if(dx == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(dy == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(dz == -1) {
				throw new IllegalStateException("Expected size");
			}
			if(maxLoadWeight == -1) {
				throw new IllegalStateException("Expected max weight");
			}
			if(loadDx == -1) {
				loadDx = dx;
			}
			if(loadDy == -1) {
				loadDy = dy;
			}
			if(loadDz == -1) {
				loadDz = dz;
			}
			if(surfaces == null || surfaces.isEmpty()) {
				surfaces = Surface.DEFAULT_SURFACE;
			}

			if(emptyWeight == -1) {
				throw new IllegalStateException("Expected empty weight");
			}
			if(stack == null) {
				stack = new Stack();
			}

			return new Container(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight, stack);
		}

	}

	protected final long volume;
	protected final long loadVolume;

	protected final int emptyWeight;
	/** i.e. best of the stack values */
	protected final long maxLoadVolume;
	/** i.e. best of the stack values */
	protected final int maxLoadWeight;

	protected final long maximumArea;
	
	protected final int dx; // x
	protected final int dy; // y
	protected final int dz; // z
	
	protected final int loadDx; // x
	protected final int loadDy; // y
	protected final int loadDz; // z

	protected final String id;
	protected final String description;

	protected final Stack stack;
	
	public Container(String id, String description, int dx, int dy, int dz, int emptyWeight, int loadDx, int loadDy, int loadDz, int maxLoadWeight, Stack stack) {
		this.id = id;
		this.description = description;

		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;
		
		this.loadVolume = (long)loadDx * (long)loadDy * (long)loadDz;
		
		this.emptyWeight = emptyWeight;

		this.maximumArea = ((long)loadDx)*loadDy;

		this.maxLoadVolume = maximumArea * (long)loadDz;
		this.maxLoadWeight = maxLoadWeight;
		
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.volume = (long)dx * (long)dy * (long)dz;
		
		this.stack = stack;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public int getWeight() {
		return emptyWeight + stack.getWeight();
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public int getEmptyWeight() {
		return emptyWeight;
	}

	public int getMaxWeight() {
		return emptyWeight + maxLoadWeight;
	}

	public int getLoadWeight() {
		return stack.getWeight();
	}

	public long getVolume() {
		return volume;
	}

	public long getMaximumArea() {
		return maximumArea;
	}

	public long getLoadVolume() {
		return stack.getVolume();
	}

	public boolean canLoad(Box box) {
		if(box.getVolume() > maxLoadVolume) {
			return false;
		}
		if(box.getWeight() > maxLoadWeight) {
			return false;
		}
		// at least one stack value must fit within the container load 
		for (BoxStackValue stackValue : box.getStackValues()) {
			if(canLoad(stackValue)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canLoad(BoxItem boxItem) {
		if(boxItem.getVolume() > maxLoadVolume) {
			return false;
		}
		if(boxItem.getWeight() > maxLoadWeight) {
			return false;
		}
		Box box = boxItem.getBox();
		for (BoxStackValue stackValue : box.getStackValues()) {
			if(canLoad(stackValue)) {
				return true;
			}
		}
		return false;
	}

	public boolean canLoad(BoxStackValue stackValue) {
		return stackValue.getDx() <= loadDx &&
				stackValue.getDy() <= loadDy &&
				stackValue.getDz() <= loadDz;
	}

	public boolean canLoad(BoxItemGroup group) {
		if(group.getVolume() > maxLoadVolume) {
			return false;
		}
		if(group.getWeight() > maxLoadWeight) {
			return false;
		}
		
		// all boxes must fit within the container load 
		for (BoxItem boxItem : group.getItems()) {
			Box box = boxItem.getBox();
			if(!canLoad(box)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canLoadAtLeastOneBox(List<BoxItem> boxes) {
		
		for (BoxItem boxItem : boxes) {
			Box box = boxItem.getBox();
			if(canLoad(box)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canLoadAtLeastOneGroup(List<BoxItemGroup> boxes) {
		
		for (BoxItemGroup group : boxes) {
			if(canLoad(group)) {
				return true;
			}
		}
		return false;
	}

	
	@Override
	public Container clone() {
		return new Container(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight, new Stack());
	}
	
	public int getLoadDx() {
		return loadDx;
	}
	
	public int getLoadDy() {
		return loadDy;
	}
	
	public int getLoadDz() {
		return loadDz;
	}

	public Stack getStack() {
		return stack;
	}
	
	public int getDx() {
		return dx;
	}
	
	public int getDy() {
		return dy;
	}
	
	public int getDz() {
		return dz;
	}

	public boolean fitsInside(BoxItemGroup boxItemGroup) {
		if(boxItemGroup.getVolume() <= getMaxLoadVolume() && boxItemGroup.getWeight() <= getMaxLoadWeight()) {			
			for(int i = 0; i < boxItemGroup.size(); i++) {
				
				Box box = boxItemGroup.get(i).getBox();
				for (BoxStackValue boxStackValue : box.getStackValues()) {
					if(boxStackValue.fitsInside3D(loadDx, loadDy, loadDz)) {
						return true;
					}
				}
			}		
		}
		return false;
	}
	
	public boolean fitsInside(BoxItem boxItem) {
		if(boxItem.getVolume() <= getMaxLoadVolume() && boxItem.getWeight() <= getMaxLoadWeight()) {
			Box box = boxItem.getBox();
			for (BoxStackValue boxStackValue : box.getStackValues()) {
				if(boxStackValue.fitsInside3D(loadDx, loadDy, loadDz)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean fitsInside(Box box) {
		if(box.getVolume() <= getMaxLoadVolume() && box.getWeight() <= getMaxLoadWeight()) {
			for (BoxStackValue boxStackValue : box.getStackValues()) {
				if(boxStackValue.fitsInside3D(loadDx, loadDy, loadDz)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String toString() {
		if(dx != loadDx || dy != loadDy || dz != loadDz) {
			return "Container[" + (id != null ? id : "") + "[" + dx + "x" + dy + "x" + dz + " (" + loadDx + "x" + loadDy + "x" + loadDz + ")]";
		}
		return "Container[" + (id != null ? id : "") + "[" + dx + "x" + dy + "x" + dz+ "]";
	}

	
}
