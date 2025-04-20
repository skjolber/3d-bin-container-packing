package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemGroupListenerBuilder;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;

public class NoMatchesWithPetrolBoxItemGroupListener implements BoxItemGroupListener {

	public static class Builder extends BoxItemGroupListenerBuilder<Builder> {

		@Override
		public BoxItemGroupListener build() {
			return new NoMatchesWithPetrolBoxItemGroupListener(container, groups, stack);
		}

	}
	
	protected final Container container;
	protected final FilteredBoxItemGroups groups;
	protected final Stack stack;
	
	protected boolean matches = false;
	protected boolean petrol = false;

	public NoMatchesWithPetrolBoxItemGroupListener(Container container, FilteredBoxItemGroups groups, Stack stack) {
		this.container = container;
		this.groups = groups;
		this.stack = stack;
	}

	@Override
	public void accepted(BoxItemGroup group) {
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
}
