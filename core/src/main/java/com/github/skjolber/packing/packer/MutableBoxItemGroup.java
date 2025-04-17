package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

public class MutableBoxItemGroup extends BoxItemGroup {

	private static final long serialVersionUID = 1L;
	
	private static List<BoxItem> toMutable(List<BoxItem> items) {
		List<BoxItem> list = new ArrayList<>(items.size());
		
		for (BoxItem boxItem : items) {
			MutableBoxItem mutableBoxItem = new MutableBoxItem(boxItem);
			mutableBoxItem.setIndex(list.size());
			list.add(mutableBoxItem);
		}
		
		return list;
	}
	
	public final BoxItemGroup source; 

	public MutableBoxItemGroup(BoxItemGroup group) {
		super(group.getId(), toMutable(group.getItems()));
		
		this.source = group;
	}
	
	public void reset() {
		for (BoxItem boxItem : items) {
			MutableBoxItem mutableBoxItem = (MutableBoxItem)boxItem;
			mutableBoxItem.reset();
		}
	}
	
	public BoxItemGroup getSource() {
		return source;
	}
	
	@Override
	public MutableBoxItem get(int i) {
		return (MutableBoxItem) super.get(i);
	}

	
}
