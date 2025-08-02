package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.packager.AbstractBoxItemControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class NoLightersWithPetrolBoxItemListener implements BoxItemControls {

	public static class Builder extends AbstractBoxItemControlsBuilder<Builder> {

		@Override
		public BoxItemControls build() {
			return new NoLightersWithPetrolBoxItemListener(container, items, points, stack);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static BoxItemControlsBuilderFactory newFactory() {
		return () -> NoLightersWithPetrolBoxItemListener.newBuilder();
	}
	
	protected final Container container;
	protected final FilteredBoxItems items;
	protected final Stack stack;

	protected boolean matches = false;
	protected boolean petrol = false;

	public NoLightersWithPetrolBoxItemListener(Container container, FilteredBoxItems items, FilteredPoints filteredPoints, Stack stack) {
		this.container = container;
		this.items = items;
		this.stack = stack;
	}

	@Override
	public void accepted(BoxItem group) {
		// do nothing
		
		if(!matches) {
			matches = isLighter(group);
			
			if(matches) {
				// remove groups which contain petrol
				remove(this::isPetrol);
			}
		}
		if(!petrol) {
			petrol = isPetrol(group);
			
			if(petrol) {
				// remove groups which contain matches
				
				remove(this::isLighter);
			}
		}
	}

	private void remove(Predicate<BoxItem> test) {
		for(int i = 0; i < items.size(); i++) {
			BoxItem item = items.get(i);
			
			if(test.test(item)) {
				items.remove(i);
				i--;
			}
		}
	}

	private boolean isPetrol(BoxItem item) {
		return item.getBox().getId().startsWith("petrol-");
	}

	private boolean isLighter(BoxItem item) {
		return item.getBox().getId().startsWith("lighter-");
	}

}
