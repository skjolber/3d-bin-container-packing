package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class PackagerBoxItems {

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
			int startIndex = getFirstBoxItemIndexForGroup(groupIndex);
			
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
		
	};
	
	public class InnerFilteredBoxItems implements FilteredBoxItems {

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
			
			BoxItem item = values.get(index);

			filteredBoxItemGroups.remove(groupIndex);
			
			return item;
		}
		
		public boolean isEmpty() {
			return values.isEmpty();
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
	
	protected List<BoxItem> values;
	protected List<BoxItemGroup> groups;
	protected int[] boxToGroupIndexes;
	
	protected InnerFilteredBoxItemGroup filteredBoxItemGroups = new InnerFilteredBoxItemGroup();
	protected InnerFilteredBoxItems filteredBoxItems = new InnerFilteredBoxItems();
	
	public PackagerBoxItems(List<BoxItemGroup> groups) {
		setValues(new ArrayList<>(groups));
	}
	
	public PackagerBoxItems() {
	}

	public int getFirstBoxItemIndexForGroup(int memberBoxItemIndex, int groupIndex) {
		int startIndex = memberBoxItemIndex;
		while(startIndex > 0) {
			if(boxToGroupIndexes[startIndex - 1] != groupIndex) {
				break;
			}
			startIndex--;
		}
		return startIndex;
	}
	
	public int getFirstBoxItemIndexForGroup(int groupIndex) {
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
	
	public void decrement(int index) {
		// potentially remove box item from group, 
		// but do not remove group
		BoxItem boxItem = values.get(index);
		boxItem.decrement();
		
		if(boxItem.isEmpty()) {
			BoxItemGroup boxItemGroup = groups.get(boxToGroupIndexes[index]);
			boxItemGroup.removeEmpty();
			
			values.remove(index);
			System.arraycopy(boxToGroupIndexes, index + 1, boxToGroupIndexes, index, values.size() - index);
		}
	}

	public void removeEmpty(boolean group) {
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).isEmpty()) {
				
				int groupIndex = boxToGroupIndexes[i];
				if(group) {
					int startIndex = getFirstBoxItemIndexForGroup(i, groupIndex);
	
					filteredBoxItems.remove(i);
					
					i = startIndex - 1;				
				} else {
					values.remove(i);
					System.arraycopy(boxToGroupIndexes, i + 1, boxToGroupIndexes, i, values.size() - i - 1);
					i--;
					
					groups.get(groupIndex).removeEmpty();
				}
			}
		}
	}

	public InnerFilteredBoxItems getFilteredBoxItems() {
		return filteredBoxItems;
	}
	
	public InnerFilteredBoxItemGroup getFilteredBoxItemGroups() {
		return filteredBoxItemGroups;
	}

	public boolean contains(BoxItemGroup boxItemGroup) {
		return groups.contains(boxItemGroup);
	}

	public void remove(BoxItemGroup boxItemGroup) {
		int index = groups.indexOf(boxItemGroup);
		
		filteredBoxItemGroups.remove(index);
	}
}
