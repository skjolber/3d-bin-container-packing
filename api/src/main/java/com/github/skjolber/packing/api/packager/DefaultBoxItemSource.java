package com.github.skjolber.packing.api.packager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;

public class DefaultBoxItemSource implements BoxItemSource {

	protected List<BoxItem> values;

	// minimum area and volume of the box items, recalculated after items are added or removed
	private boolean minimumsValid;
	private long minArea;
	private long minVolume;
	
	public DefaultBoxItemSource(List<BoxItem> values) {
		this.values = new ArrayList<>(values);
		
		// update indexes
		for(int i = 0; i < values.size(); i++) {
			values.get(i).setLocalIndex(i);
		}
	}
	
	public DefaultBoxItemSource() {
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public BoxItem get(int index) {
		return values.get(index);
	}

	@Override
	public boolean decrement(int index, int count) {
		BoxItem boxItem = values.get(index);
		if(!boxItem.decrement(count)) {
			values.remove(index);
			removed(boxItem);

			// update indexes
			for(int i = index; i < values.size(); i++) {
				values.get(i).setLocalIndex(i);
			}
		}
		return !values.isEmpty();
	}
	
	@Override
	public BoxItem remove(int index) {
		BoxItem remove = values.remove(index);
		removed(remove);
		
		// update indexes
		for(int i = index; i < values.size(); i++) {
			values.get(i).setLocalIndex(i);
		}
		
		return remove;
	}

	private void removed(BoxItem boxItem) {
		// the minimums of the remaining items are unchanged unless the removed item attained one of them
		if(minimumsValid) {
			Box box = boxItem.getBox();
			if(box.getMinimumArea() <= minArea || box.getVolume() <= minVolume) {
				minimumsValid = false;
			}
		}
	}

	public void setValues(List<BoxItem> values) {
		this.values = values;
		minimumsValid = false;
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	public void removeEmpty() {
		int firstEmptyIndex = -1;
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				values.remove(i);
				minimumsValid = false;
				
				if(firstEmptyIndex == -1) {
					firstEmptyIndex = i;
				}
				
				i--;
			}
		}
		
		if(firstEmptyIndex != -1) {
			// update indexes
			for(int i = firstEmptyIndex; i < values.size(); i++) {
				values.get(i).setLocalIndex(i);
			}
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return values.listIterator();
	}

	@Override
	public BoxItemGroupSource getGroups() {
		// no groups
		return null;
	}

	public void add(BoxItem boxItem) {
		values.add(boxItem);
		minimumsValid = false;
	}


	@Override
	public long getMinArea() {
		if(!minimumsValid) {
			calculateMinimums();
		}
		return minArea;
	}

	@Override
	public long getMinVolume() {
		if(!minimumsValid) {
			calculateMinimums();
		}
		return minVolume;
	}

	private void calculateMinimums() {
		long minArea = Integer.MAX_VALUE;
		long minVolume = Integer.MAX_VALUE;
		for (int i = 0; i < values.size(); i++) {
			Box box = values.get(i).getBox();
			if(box.getMinimumArea() < minArea) {
				minArea = box.getMinimumArea();
			}
			if(box.getVolume() < minVolume) {
				minVolume = box.getVolume();
			}
		}
		this.minArea = minArea;
		this.minVolume = minVolume;
		this.minimumsValid = true;
	}
}
