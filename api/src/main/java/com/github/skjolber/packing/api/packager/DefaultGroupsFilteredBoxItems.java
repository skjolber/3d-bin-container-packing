package com.github.skjolber.packing.api.packager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultGroupsFilteredBoxItems implements FilteredBoxItems {

	protected List<BoxItemGroup> groups;
	protected List<BoxItem> values;
	protected int[] groupIndexes;

	
	public DefaultGroupsFilteredBoxItems(List<BoxItemGroup> groups) {
		setValues(new ArrayList<>(groups));
	}
	
	public DefaultGroupsFilteredBoxItems() {
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
			remove(index);
		}
		return !values.isEmpty();
	}
	
	@Override
	public BoxItem remove(int index) {		
		int groupIndex = groupIndexes[index];
		
		int startIndex = getGroupStartIndex(index, groupIndex);
		BoxItemGroup group = groups.remove(groupIndex);
		
		BoxItem item = values.remove(index);

		for(int i = 0; i < group.size() - 1; i++) {
			values.remove(startIndex);
		}
		
		for(int i = startIndex; i < groupIndexes.length; i++) {
			groupIndexes[i]--;
		}		
		
		return item;
	}

	private int getGroupStartIndex(int index, int groupIndex) {
		int startIndex = index;
		while(startIndex > 0) {
			if(groupIndexes[startIndex - 1] != groupIndex) {
				break;
			}
			startIndex--;
		}
		return startIndex;
	}

	public void setValues(List<BoxItemGroup> groups) {
		this.groups = groups;
		
		values = new ArrayList<>();
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			
			values.addAll(group.getItems());
		}
		
		groupIndexes = new int[values.size()];
		int index = 0;
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			
			for(int k = 0; k < group.size(); k++) {
				groupIndexes[index + k] = i;
			}
			index += groups.size();
		}
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}
	
	public void removeGroup(int groupIndex) {
		int startIndex = 0;
		for(int i = 0; i < groupIndex; i++) {
			startIndex += groups.get(groupIndex).size();
		}
		
		BoxItemGroup group = groups.remove(groupIndex);
		
		for(int i = 0; i < group.size(); i++) {
			values.remove(startIndex);
		}
		
		for(int i = startIndex; i < groupIndexes.length; i++) {
			groupIndexes[i]--;
		}		
	}

	@Override
	public void removeEmpty() {
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				int groupIndex = groupIndexes[i];

				int startIndex = getGroupStartIndex(i, groupIndex);

				remove(i);
				
				i = startIndex - 1;				
			}
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return values.listIterator();
	}
	
	public List<BoxItemGroup> getGroups() {
		return groups;
	}

}
