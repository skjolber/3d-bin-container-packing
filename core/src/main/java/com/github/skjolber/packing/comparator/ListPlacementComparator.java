package com.github.skjolber.packing.comparator;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement;

public class ListPlacementComparator implements Comparator<Placement> {

	protected final Comparator<Placement>[] list;
	
	@SuppressWarnings("unchecked")
	public ListPlacementComparator(List<Comparator<Placement>> list) {
		this.list = list.toArray(new Comparator[list.size()]);
	}
	
	@Override
	public int compare(Placement o1, Placement o2) {
		for(int i = 0; i < list.length; i++) {
			int result = list[i].compare(o1, o2);
			if(result != 0) {
				return result;
			}
		}
		return 0;
	}
	
}