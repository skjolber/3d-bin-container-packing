package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.BoxItemListenerBuilder;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class MaxFireHazardBoxItemPerContainerBoxItemListener implements BoxItemListener {

	public static class Builder extends BoxItemListenerBuilder<Builder> {

		private int maxCount = -1;
		
		public Builder withMaxCount(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}
		
		@Override
		public BoxItemListener build() {
			if(maxCount == -1) {
				throw new IllegalStateException("Expected max count");
			}
			return new MaxFireHazardBoxItemPerContainerBoxItemListener(container, input, stack, maxCount);
		}

	}
	
	protected final Container container;
	protected final FilteredBoxItems items;
	protected final Stack stack;
	protected final int maxCount;
	protected int count = 0;

	public MaxFireHazardBoxItemPerContainerBoxItemListener(Container container, FilteredBoxItems items, Stack stack, int maxCount) {
		this.container = container;
		this.items = items;
		this.stack = stack;
		this.maxCount = maxCount;
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
		
		if(isFireHazard(boxItem)) {
			count++;
			
			if(count >= maxCount) {

				for(int i = 0; i < items.size(); i++) {
					BoxItem candidate = items.get(i);
					
					if(isFireHazard(candidate)) {
						items.remove(i);
						i--;
					}
				}

			}
		}
	}

	private boolean isFireHazard(BoxItem item) {
		return item.getBox().getId().startsWith("fire-");
	}
}
