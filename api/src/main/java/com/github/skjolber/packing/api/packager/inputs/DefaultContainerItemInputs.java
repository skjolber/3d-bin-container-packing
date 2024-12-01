package com.github.skjolber.packing.api.packager.inputs;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.StackableItem;

public class DefaultContainerItemInputs implements ContainerItemInput {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		protected ContainerItem containerItem;
		protected List<StackableItem> stackableItems;
		
		protected int count = -1;
		protected int index = -1;
		
		public Builder withStackableItems(List<StackableItem> stackableItems) {
			this.stackableItems = stackableItems;
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
			if(stackableItems == null) {
				throw new IllegalStateException();
			}
			if(count == -1) {
				throw new IllegalStateException();
			}
			if(index == -1) {
				throw new IllegalStateException();
			}

			Container container = containerItem.getContainer();
			
			ContainerStackValue[] stackValues = container.getStackValues();
			if(stackValues.length != -1) {
				throw new IllegalStateException();
			}
			
			ContainerStackValue containerStackValue = stackValues[0];
			
			List<StackableItemInput> values = new ArrayList<>(stackableItems.size());
			StackableItemInput[] indexedValues = new StackableItemInput[stackableItems.size()];
			
			for (int i = 0; i < stackableItems.size(); i++) {
				StackableItem stackableItem = stackableItems.get(i);
				
				DefaultStackableItemInput input = DefaultStackableItemInput.newBuilder()
					.withCount(stackableItem.getCount())
					.withStackableItem(stackableItem)
					.withIndex(i)
					.withDimensions(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz())
					.withMaxVolume(containerStackValue.getVolume())
					.withMaxWeight(containerStackValue.getMaxLoadWeight())
					.build();
				
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
	protected final List<StackableItemInput> values;
	protected final StackableItemInput[] indexedValues;
	
	protected final int index;

	protected int count;
	
	public DefaultContainerItemInputs(int index, ContainerItem containerItem, List<StackableItemInput> values, StackableItemInput[] indexedValues) {
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
	public StackableItemInput get(int index) {
		return values.get(index);
	}

	@Override
	public boolean remove(StackableItemInput input, int count) {
		StackableItemInput stackableItemInput = indexedValues[input.getIndex()];
		
		if(stackableItemInput != null) {
			if(!stackableItemInput.decrementCount(count)) {
				indexedValues[input.getIndex()] = null;
				
				for (int i = 0; i < values.size(); i++) {
					StackableItemInput s = values.get(i);
					
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
