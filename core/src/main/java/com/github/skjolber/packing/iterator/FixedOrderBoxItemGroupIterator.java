package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.point.ExtremePoints;

public class FixedOrderBoxItemGroupIterator implements BoxItemGroupIterator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		protected BoxItemGroupSource filteredBoxItemGroups;
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
		
		public Builder withFilteredBoxItemGroups(BoxItemGroupSource filteredBoxItemGroups) {
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
	
	protected final BoxItemGroupSource filteredBoxItemGroups;
	protected final Container container;
	protected final ExtremePoints extremePoints;
	
	protected int next = -1;
	protected boolean dirty = true;
	
	public FixedOrderBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, Container container,
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
		return 0;
	}

}
