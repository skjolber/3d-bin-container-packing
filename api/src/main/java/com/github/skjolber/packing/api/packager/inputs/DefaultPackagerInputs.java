package com.github.skjolber.packing.api.packager.inputs;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.BoxItem;

public class DefaultPackagerInputs implements PackagerInputs {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		protected List<ContainerItem> containerItems;
		protected List<BoxItem> boxItems;		
		
		public Builder withContainerItems(List<ContainerItem> containerItems) {
			this.containerItems = containerItems;
			return this;
		}
		
		public Builder withStackableItems(List<BoxItem> boxItems) {
			this.boxItems = boxItems;
			return this;
		}

		public DefaultPackagerInputs build() {
			if(containerItems == null) {
				throw new IllegalStateException();
			}
			if(boxItems == null) {
				throw new IllegalStateException();
			}
			
			boolean[] coveredStackableItems = new boolean[boxItems.size()];
			ContainerItemInput[] indexedValues = new ContainerItemInput[boxItems.size()];
			
			List<ContainerItemInput> values = new ArrayList<>(containerItems.size());
			for (int j = 0; j < containerItems.size(); j++) {
				ContainerItem containerItem = containerItems.get(j);
				
				DefaultContainerItemInputs input = DefaultContainerItemInputs.newBuilder()
					.withContainerItem(containerItem)
					.withBoxItems(boxItems)
					.withCount(containerItem.getCount())
					.withIndex(j)
					.build();
				
				if(input != null) {
					values.add(input);
					
					indexedValues[j] = input;
					
					for(int i = 0; i < input.size(); i++) {
						BoxItem stackableItemInput = input.get(i);
						coveredStackableItems[stackableItemInput.getIndex()] = true;
					}
				}
			}
			
			if(values.isEmpty()) {
				return null;
			}
			
			for (boolean covered : coveredStackableItems) {
				if(!covered) {
					return null;
				}
			}
			
			return new DefaultPackagerInputs(values, indexedValues);
		}

	}
	
	protected final List<ContainerItemInput> values;
	protected final ContainerItemInput[] indexedValues;

	public DefaultPackagerInputs(List<ContainerItemInput> values, ContainerItemInput[] indexedValues) {
		this.values = values;
		this.indexedValues = indexedValues;
	}
	
	@Override
	public int getContainerItemInputSize() {
		return values.size();
	}

	@Override
	public ContainerItemInput getContainerItemInput(int index) {
		return values.get(index);
	}

	@Override
	public boolean removeContainerItem(ContainerItemInput input, int count) {
		ContainerItemInput stackableItemInput = indexedValues[input.getIndex()];
		
		if(stackableItemInput != null) {
			if(!stackableItemInput.decrementCount(count)) {
				indexedValues[input.getIndex()] = null;
	
				for (int i = 0; i < values.size(); i++) {
					ContainerItemInput c = values.get(i);
					
					if(c.getIndex() == input.getIndex()) {
						values.remove(i);
						break;
					}
				}
			}
		}
		return !values.isEmpty();
	}

	@Override
	public boolean removeBoxItem(BoxItem input, int count) {
		for (int i = 0; i < values.size(); i++) {
			ContainerItemInput containerItemInput = values.get(i);
			
			if(!containerItemInput.remove(input, count)) {
				indexedValues[containerItemInput.getIndex()] = null;
				
				values.remove(i);
				i--;
			}
		}
		return !values.isEmpty();
	}

}
