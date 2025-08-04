package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.packager.AbstractBoxItemControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class NoMatchesWithPetrolBoxItemGroupListener implements BoxItemControls {

	public static class Builder extends AbstractBoxItemControlsBuilder<Builder> {

		@Override
		public BoxItemControls build() {
			return new NoMatchesWithPetrolBoxItemGroupListener(container, items, stack, points);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final BoxItemControlsBuilderFactory newFactory() {
		return () -> NoMatchesWithPetrolBoxItemGroupListener.newBuilder();
	}
	
	protected final Container container;
	protected final FilteredBoxItems boxItems;
	protected final Stack stack;
	protected final FilteredPoints points;

	protected boolean matches = false;
	protected boolean petrol = false;

	public NoMatchesWithPetrolBoxItemGroupListener(Container container, FilteredBoxItems boxItems, Stack stack, FilteredPoints points) {
		this.container = container;
		this.boxItems = boxItems;
		this.stack = stack;
		this.points = points;
	}

	@Override
	public void attemptSuccess(BoxItemGroup group) {
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

	private void remove(Predicate<BoxItemGroup> test) {
		FilteredBoxItemGroups groups = boxItems.getGroups();
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup boxItemGroup = groups.get(i);
			
			if(test.test(boxItemGroup)) {
				groups.remove(i);
				i--;
			}
		}
	}

	private boolean isPetrol(BoxItemGroup boxItemGroup) {
		for(int i = 0; i < boxItemGroup.size(); i++) {
			BoxItem item = boxItemGroup.get(i);
			if(isPetrol(item)) {
				return true;
			}
		}
		return false;
	}

	private boolean isPetrol(BoxItem item) {
		return item.getBox().getId().startsWith("petrol-");
	}

	
	private boolean isMatches(BoxItemGroup boxItemGroup) {
		for(int i = 0; i < boxItemGroup.size(); i++) {
			BoxItem item = boxItemGroup.get(i);
			if(isMatches(item)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMatches(BoxItem item) {
		return item.getBox().getId().startsWith("matches-");
	}
	
	@Override
	public void accepted(BoxItem boxItem) {
	}

}
