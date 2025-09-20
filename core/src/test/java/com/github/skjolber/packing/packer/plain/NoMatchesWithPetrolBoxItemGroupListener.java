package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;
import com.github.skjolber.packing.api.packager.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.ManifestControls;
import com.github.skjolber.packing.api.packager.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;

public class NoMatchesWithPetrolBoxItemGroupListener implements ManifestControls {

	public static class Builder extends AbstractManifestControlsBuilder<Builder> {

		@Override
		public ManifestControls build() {
			return new NoMatchesWithPetrolBoxItemGroupListener(container, items, stack, points);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final ManifestControlsBuilderFactory newFactory() {
		return () -> NoMatchesWithPetrolBoxItemGroupListener.newBuilder();
	}
	
	protected final Container container;
	protected final BoxItemSource boxItems;
	protected final Stack stack;
	protected final PointSource points;

	protected boolean matches = false;
	protected boolean petrol = false;

	public NoMatchesWithPetrolBoxItemGroupListener(Container container, BoxItemSource boxItems, Stack stack, PointSource points) {
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
		BoxItemGroupSource groups = boxItems.getGroups();
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
