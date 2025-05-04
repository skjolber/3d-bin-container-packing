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

public class NoMatchesWithPetrolBoxItemListener implements BoxItemControls {

	public static class Builder extends AbstractBoxItemControlsBuilder<Builder> {

		@Override
		public BoxItemControls build() {
			return new NoMatchesWithPetrolBoxItemListener(container, items, points, stack);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static BoxItemControlsBuilderFactory newFactory() {
		return () -> NoMatchesWithPetrolBoxItemListener.newBuilder();
	}
	
	protected final Container container;
	protected final FilteredBoxItems items;
	protected final Stack stack;
	protected final FilteredPoints points;

	protected boolean matches = false;
	protected boolean petrol = false;

	public NoMatchesWithPetrolBoxItemListener(Container container, FilteredBoxItems items, FilteredPoints filteredPoints, Stack stack) {
		this.container = container;
		this.items = items;
		this.points = filteredPoints;
		this.stack = stack;
	}

	@Override
	public void accepted(BoxItem group) {
		// do nothing
		
		if(!matches) {
			matches = isMatches(group);
			
			if(matches) {
				// remove groups which contain petrol
				remove(this::isPetrol);
			}
		}
		if(!petrol) {
			petrol = isPetrol(group);
			
			if(petrol) {
				// remove groups which contain matches
				
				remove(this::isMatches);
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

	private boolean isMatches(BoxItem item) {
		return item.getBox().getId().startsWith("matches-");
	}

	@Override
	public FilteredBoxItems getFilteredBoxItems() {
		return items;
	}

	@Override
	public FilteredPoints getPoints(BoxItem boxItem) {
		return points;
	}

}
