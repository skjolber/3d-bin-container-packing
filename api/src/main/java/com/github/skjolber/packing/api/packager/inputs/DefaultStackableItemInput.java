package com.github.skjolber.packing.api.packager.inputs;

import java.util.List;

import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.BoxItem;

/**
 * 
 * Stackable item which fit within certain bounds, i.e. load dimensions of a container.
 * 
 */

public class DefaultStackableItemInput implements StackableItemInput {

	private static final long serialVersionUID = 1L;
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		protected BoxItem stackableItem;
		
		protected int count = -1;
		protected int index = -1;
		
		protected int dx = -1;
		protected int dy = -1;
		protected int dz = -1;
		
		protected int maxWeight = -1;
		protected long maxVolume = -1L;
		
		public Builder withStackableItem(BoxItem stackableItem) {
			this.stackableItem = stackableItem;
			return this;
		}

		public Builder withCount(int count) {
			this.count = count;
			return this;
		}

		public Builder withIndex(int index) {
			this.index = index;
			return this;
		}
		
		public Builder withDimensions(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			return this;
		}

		public Builder withDx(int dx) {
			this.dx = dx;
			return this;
		}

		public Builder withDy(int dy) {
			this.dy = dy;
			return this;
		}

		public Builder withDz(int dz) {
			this.dz = dz;
			return this;
		}

		public Builder withMaxWeight(int maxWeight) {
			this.maxWeight = maxWeight;
			return this;
		}

		public Builder withMaxVolume(long maxVolume) {
			this.maxVolume = maxVolume;
			return this;
		}

		public DefaultStackableItemInput build() {
			if(stackableItem == null) {
				throw new IllegalStateException();
			}
			if(count == -1) {
				throw new IllegalStateException();
			}
			if(index == -1) {
				throw new IllegalStateException();
			}
			if(dx == -1) {
				throw new IllegalStateException();
			}
			if(dy == -1) {
				throw new IllegalStateException();
			}
			if(dz == -1) {
				throw new IllegalStateException();
			}
			if(maxWeight == -1) {
				throw new IllegalStateException();
			}
			if(maxVolume == -1L) {
				throw new IllegalStateException();
			}

			Stackable stackable = stackableItem.getStackable();
			
			if(stackable.getWeight() > maxWeight) {
				return null;
			}

			if(stackable.getVolume() > maxVolume) {
				return null;
			}
			
			List<StackValue> boundRotations = stackable.rotations(dx, dy, dz);
			if(boundRotations == null || boundRotations.isEmpty()) {
				return null;
			}

			StackValue[] stackValues = boundRotations.toArray(new StackValue[boundRotations.size()]);
			
			return new DefaultStackableItemInput(index, stackableItem, stackValues);
		}

	}

	protected final BoxItem stackableItem;
	protected final StackValue[] values;
	
	protected final long minimumArea;
	protected final long maximumArea;
	
	protected int count;
	protected final int index;

	public DefaultStackableItemInput(int index, BoxItem stackableItem, StackValue[] values) {
		this.index = index;
		this.values = values;
		this.stackableItem = stackableItem;
		this.count = stackableItem.getCount();
		
		this.minimumArea = Stackable.getMinimumArea(values);
		this.maximumArea = Stackable.getMinimumArea(values);
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public int getSize() {
		return values.length;
	}

	@Override
	public StackValue get(int index) {
		return values[index];
	}

	@Override
	public BoxItem getStackableItem() {
		return stackableItem;
	}
	
	@Override
	public int getCount() {
		return count;
	}

	@Override
	public boolean decrementCount(int amount) {
		count -= amount;
		return count > 0;
	}
	
}
