package com.github.skjolber.packing.iterator;

import java.util.Comparator;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.point.PointCalculator;

public class AnyOrderBoxItemGroupIterator implements BoxItemGroupIterator {
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		protected BoxItemGroupSource filteredBoxItemGroups;
		protected Container container;
		protected PointCalculator pointCalculator;
		protected Comparator<BoxItemGroup> comparator;
		
		public Builder withComparator(Comparator<BoxItemGroup> comparator) {
			this.comparator = comparator;
			return this;
		}
		
		public Builder withContainer(Container container) {
			this.container = container;
			return this;
		}
		
		public Builder withPointCalculator(PointCalculator pointCalculator) {
			this.pointCalculator = pointCalculator;
			return this;
		}
		
		public Builder withFilteredBoxItemGroups(BoxItemGroupSource filteredBoxItemGroups) {
			this.filteredBoxItemGroups = filteredBoxItemGroups;
			return this;
		}
		
		public AnyOrderBoxItemGroupIterator build() {
			if(comparator == null) {
				throw new IllegalStateException();
			}
			if(container == null) {
				throw new IllegalStateException();
			}
			if(pointCalculator == null) {
				throw new IllegalStateException();
			}
			if(filteredBoxItemGroups == null) {
				throw new IllegalStateException();
			}
			return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, pointCalculator, comparator);
		}
	}
	
	protected final BoxItemGroupSource filteredBoxItemGroups;
	protected final Container container;
	protected final PointCalculator pointCalculator;
	protected final Comparator<BoxItemGroup> comparator;
	
	protected int next = -1;
	protected boolean dirty = true;
	
	public AnyOrderBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, Container container,
			PointCalculator pointCalculator, Comparator<BoxItemGroup> comparator) {
		this.filteredBoxItemGroups = filteredBoxItemGroups;
		this.container = container;
		this.pointCalculator = pointCalculator;
		this.comparator = comparator;
	}

	@Override
	public boolean hasNext() {
		if(dirty) {
			next = getBestItemGroup();
			dirty = false;
		}
		
		return next != -1;
	}

	@Override
	public int next() {
		if(dirty) {
			next = getBestItemGroup();
		} else {
			dirty = true;
		}
		return next;
	}
	

	protected int getBestItemGroup() {
		BoxItemGroup bestBoxItemGroup = null;
		int bestIndex = -1;
		
		// the groups of the lowest container priority come first, and of those the groups which are extracted last
		int priority = Integer.MAX_VALUE;
		int extractionOrder = Integer.MIN_VALUE;
		for (int l = 0; l < filteredBoxItemGroups.size(); l++) {
			BoxItemGroup group = filteredBoxItemGroups.get(l);
			if(group.getContainerPriority() < priority) {
				priority = group.getContainerPriority();
				extractionOrder = group.getExtractionOrder();
			} else if(group.getContainerPriority() == priority) {
				extractionOrder = Math.max(extractionOrder, group.getExtractionOrder());
			}
		}

		// find next best group
		for (int l = 0; l < filteredBoxItemGroups.size(); l++) {
			BoxItemGroup group = filteredBoxItemGroups.get(l);
			if(group.getContainerPriority() != priority || group.getExtractionOrder() != extractionOrder) {
				continue;
			}
			if(bestBoxItemGroup == null || comparator.compare(bestBoxItemGroup, group) < 0) {
				bestBoxItemGroup = group;
				bestIndex = l;
			}
		}
		return bestIndex;
	}

}
