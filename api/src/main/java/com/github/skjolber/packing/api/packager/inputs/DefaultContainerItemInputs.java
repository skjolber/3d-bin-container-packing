package com.github.skjolber.packing.api.packager.inputs;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;

public class DefaultContainerItemInputs implements ContainerItemInput {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		protected ContainerItem containerItem;
		protected List<BoxItem> boxItems;
		
		protected int count = -1;
		protected int index = -1;
		
		public Builder withBoxItems(List<BoxItem> stackableItems) {
			this.boxItems = stackableItems;
			return this;
		}
		
		public Builder withContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
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
		
		public DefaultContainerItemInputs build() {
			if(containerItem == null) {
				throw new IllegalStateException();
			}
			if(boxItems == null) {
				throw new IllegalStateException();
			}
			if(count == -1) {
				throw new IllegalStateException();
			}
			if(index == -1) {
				throw new IllegalStateException();
			}

			List<BoxItem> values = new ArrayList<>(boxItems.size());
			BoxItem[] indexedValues = new BoxItem[boxItems.size()];
			
			for (int i = 0; i < boxItems.size(); i++) {
				BoxItem input = boxItems.get(i);

				BoxItem clone = input.clone();
				clone.setIndex(i);
				
				if(input != null) {
					values.add(input);
					
					indexedValues[i] = input;
				}
			}
			
			if(values.isEmpty()) {
				return null;
			}
			
			return new DefaultContainerItemInputs(index, containerItem, values, indexedValues);
		}
	}
	
	protected final ContainerItem containerItem;
	protected final List<BoxItem> values;
	protected final BoxItem[] indexedValues;
	
	protected final int index;

	protected int count;
	
	public DefaultContainerItemInputs(int index, ContainerItem containerItem, List<BoxItem> values, BoxItem[] indexedValues) {
		this.index = index;
		this.values = values;
		this.containerItem = containerItem;
		this.indexedValues = indexedValues;
		this.count = containerItem.getCount();
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public BoxItem get(int index) {
		return values.get(index);
	}

	@Override
	public boolean remove(BoxItem input, int count) {
		BoxItem stackableItemInput = indexedValues[input.getIndex()];
		
		if(stackableItemInput != null) {
			if(!stackableItemInput.decrement(count)) {
				indexedValues[input.getIndex()] = null;
				
				for (int i = 0; i < values.size(); i++) {
					BoxItem s = values.get(i);
					
					if(s.getIndex() == index) {
						values.remove(i);
						break;
					}
				}
			}
		}
		return !values.isEmpty();
	}

	@Override
	public ContainerItem getContainerItem() {
		return containerItem;
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
