package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.AbstractBoxItemListenerBuilder;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.BoxItemListenerBuilder;
import com.github.skjolber.packing.api.packager.BoxItemListenerBuilderFactory;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class NoMatchesWithPetrolBoxItemListener implements BoxItemListener {

	public static class Builder extends AbstractBoxItemListenerBuilder<Builder> {

		@Override
		public BoxItemListener build() {
			return new NoMatchesWithPetrolBoxItemListener(container, items, stack);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static BoxItemListenerBuilderFactory newFactory() {
		return () -> NoMatchesWithPetrolBoxItemListener.newBuilder();
	}
	
	protected final Container container;
	protected final FilteredBoxItems items;
	protected final Stack stack;
	
	protected boolean matches = false;
	protected boolean petrol = false;

	public NoMatchesWithPetrolBoxItemListener(Container container, FilteredBoxItems items, Stack stack) {
		this.container = container;
		this.items = items;
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

}
