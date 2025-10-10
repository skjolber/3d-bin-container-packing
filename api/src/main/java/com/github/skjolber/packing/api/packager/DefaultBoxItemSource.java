package com.github.skjolber.packing.api.packager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public class DefaultBoxItemSource implements BoxItemSource {

	protected List<BoxItem> values;
	
	public DefaultBoxItemSource(List<BoxItem> values) {
		this.values = new ArrayList<>(values);
		
		// update indexes
		for(int i = 0; i < values.size(); i++) {
			values.get(i).setIndex(i);
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

			// update indexes
			for(int i = index; i < values.size(); i++) {
				values.get(i).setIndex(i);
			}
		}
		return !values.isEmpty();
	}
	
	@Override
	public BoxItem remove(int index) {
		BoxItem remove = values.remove(index);
		
		// update indexes
		for(int i = index; i < values.size(); i++) {
			values.get(i).setIndex(i);
		}
		
		return remove;
	}

	public void setValues(List<BoxItem> values) {
		this.values = values;
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	public void removeEmpty() {
		int firstEmptyIndex = -1;
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				values.remove(i);
				
				if(firstEmptyIndex == -1) {
					firstEmptyIndex = i;
				}
				
				i--;
			}
		}
		
		if(firstEmptyIndex != -1) {
			// update indexes
			for(int i = firstEmptyIndex; i < values.size(); i++) {
				values.get(i).setIndex(i);
			}
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return values.listIterator();
	}

	@Override
	public BoxItemGroupSource getGroups() {
		return null;
	}

}
