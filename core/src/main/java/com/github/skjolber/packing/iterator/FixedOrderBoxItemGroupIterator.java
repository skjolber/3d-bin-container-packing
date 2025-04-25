package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;

public class FixedOrderBoxItemGroupIterator implements BoxItemGroupIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		protected FilteredBoxItemGroups filteredBoxItemGroups;
		protected Container container;
		protected ExtremePoints extremePoints;
		
		public Builder withContainer(Container container) {
			this.container = container;
			return this;
		}
		
		public Builder withExtremePoints(ExtremePoints extremePoints) {
			this.extremePoints = extremePoints;
			return this;
		}
		
		public Builder withFilteredBoxItemGroups(FilteredBoxItemGroups filteredBoxItemGroups) {
			this.filteredBoxItemGroups = filteredBoxItemGroups;
			return this;
		}
		
		public FixedOrderBoxItemGroupIterator build() {
			if(container == null) {
				throw new IllegalStateException();
			}
			if(extremePoints == null) {
				throw new IllegalStateException();
			}
			if(filteredBoxItemGroups == null) {
				throw new IllegalStateException();
			}
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
		}
	}
	
	protected final FilteredBoxItemGroups filteredBoxItemGroups;
	protected final Container container;
	protected final ExtremePoints extremePoints;
	
	protected int next = -1;
	protected boolean dirty = true;
	
	public FixedOrderBoxItemGroupIterator(FilteredBoxItemGroups filteredBoxItemGroups, Container container,
			ExtremePoints extremePoints) {
		this.filteredBoxItemGroups = filteredBoxItemGroups;
		this.container = container;
		this.extremePoints = extremePoints;
	}

	@Override
	public boolean hasNext() {
		if(dirty) {
			next = getFirstBoxItemGroup();
			dirty = false;
		}
		
		return next != -1;
	}

	@Override
	public int next() {
		if(dirty) {
			next = getFirstBoxItemGroup();
		} else {
			dirty = true;
		}
		return next;
	}
	
	protected int getFirstBoxItemGroup() {
		long maxPointVolume = extremePoints.getMaxVolume();
		long maxPointArea = extremePoints.getMaxArea();

		long maxTotalPointVolume = container.getMaxLoadVolume() -  extremePoints.getUsedVolume();		
		long maxTotalWeight = container.getMaxLoadWeight() -  extremePoints.getUsedWeight();

		BoxItemGroup bestBoxItemGroup = filteredBoxItemGroups.get(0);

		if(bestBoxItemGroup.getVolume() > maxTotalPointVolume) {
			return -1;
		}
		if(bestBoxItemGroup.getWeight() > maxTotalWeight) {
			return -1;
		}
		for (int i = 0; i < bestBoxItemGroup.size(); i++) {
			BoxItem boxItem = bestBoxItemGroup.get(i);
			
			Box box = boxItem.getBox();
			if(box.getVolume() > maxPointVolume) {
				return -1;
			}

			if(box.getMinimumArea() > maxPointArea) {
				return -1;
			}
		}
		return 0;
	}

}
