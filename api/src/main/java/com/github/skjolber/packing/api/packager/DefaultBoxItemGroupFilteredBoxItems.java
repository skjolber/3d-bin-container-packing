package com.github.skjolber.packing.api.packager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class DefaultBoxItemGroupFilteredBoxItems implements FilteredBoxItems {

	public class InnerFilteredBoxItemGroup implements FilteredBoxItemGroups {

		@Override
		public int size() {
			return groups.size();
		}

		@Override
		public BoxItemGroup get(int index) {
			return groups.get(index);
		}

		@Override
		public BoxItemGroup remove(int groupIndex) {
			// also removes from parent filtered box items
			int startIndex = 0;
			for(int i = 0; i < groupIndex; i++) {
				startIndex += groups.get(i).size();
			}
			
			BoxItemGroup group = groups.remove(groupIndex);
			
			for(int i = 0; i < group.size(); i++) {
				values.remove(startIndex);
			}
			
			for(int i = startIndex; i < boxToGroupIndexes.length; i++) {
				boxToGroupIndexes[i]--;
			}
			return group;
		}

		public boolean isEmpty() {
			return groups.isEmpty();
		}
		
		public void removeEmpty() {
			for(int i = 0; i < groups.size(); i++) {
				if(groups.get(i).isEmpty()) {
					remove(i);
					i--;
				}
			}
		}

		@Override
		public Iterator<BoxItemGroup> iterator() {
			return groups.listIterator();
		}

		public boolean contains(BoxItemGroup boxItemGroup) {
			return groups.contains(boxItemGroup);
		}

		public void remove(BoxItemGroup boxItemGroup) {
			groups.remove(boxItemGroup);
		}
		
	};
	
	protected List<BoxItem> values;
	protected List<BoxItemGroup> groups;
	protected int[] boxToGroupIndexes;
	
	protected InnerFilteredBoxItemGroup filteredBoxItemGroups = new InnerFilteredBoxItemGroup();
	
	public DefaultBoxItemGroupFilteredBoxItems(List<BoxItemGroup> groups) {
		setValues(new ArrayList<>(groups));
	}
	
	public DefaultBoxItemGroupFilteredBoxItems() {
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
		// also remove all boxes in the same group, and the box group itself.
		int groupIndex = boxToGroupIndexes[index];
		
		int startIndex = getGroupStartIndex(index, groupIndex);
		BoxItemGroup group = groups.remove(groupIndex);
		
		BoxItem item = values.remove(index);

		for(int i = 0; i < group.size() - 1; i++) {
			values.remove(startIndex);
		}
		
		for(int i = startIndex; i < boxToGroupIndexes.length; i++) {
			boxToGroupIndexes[i]--;
		}		
		
		return item;
	}

	public int getGroupStartIndex(int index, int groupIndex) {
		int startIndex = index;
		while(startIndex > 0) {
			if(boxToGroupIndexes[startIndex - 1] != groupIndex) {
				break;
			}
			startIndex--;
		}
		return startIndex;
	}
	
	public int getGroupStartIndex(int groupIndex) {
		int index = 0;
		for(int i = 0; i < groupIndex; i++) {
			index += groups.get(index).size();
		}
		return index;
	}

	public void setValues(List<BoxItemGroup> groups) {
		this.groups = groups;
		
		values = new ArrayList<>();
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			
			values.addAll(group.getItems());
		}
		
		boxToGroupIndexes = new int[values.size()];
		int index = 0;
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			
			for(int k = 0; k < group.size(); k++) {
				boxToGroupIndexes[index + k] = i;
			}
			index += group.size();
		}
	}
	
	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	public void removeEmpty(boolean group) {
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				
				if(group) {
					int groupIndex = boxToGroupIndexes[i];
	
					int startIndex = getGroupStartIndex(i, groupIndex);
	
					remove(i);
					
					i = startIndex - 1;				
				} else {
					values.remove(i);
					i--;
				}
			}
		}
	}

	@Override
	public Iterator<BoxItem> iterator() {
		return values.listIterator();
	}

	@Override
	public InnerFilteredBoxItemGroup getGroups() {
		return filteredBoxItemGroups;
	}


}
