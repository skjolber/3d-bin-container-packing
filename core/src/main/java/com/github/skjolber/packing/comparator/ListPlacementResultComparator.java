package com.github.skjolber.packing.comparator;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.packager.IntermediatePlacement;

public class ListPlacementResultComparator implements Comparator<IntermediatePlacement> {

	protected final Comparator<IntermediatePlacement>[] list;
	
	@SuppressWarnings("unchecked")
	public ListPlacementResultComparator(List<Comparator<IntermediatePlacement>> list) {
		this.list = list.toArray(new Comparator[list.size()]);
	}
	
	@Override
	public int compare(IntermediatePlacement o1, IntermediatePlacement o2) {
		for(int i = 0; i < list.length; i++) {
			int result = list[i].compare(o1, o2);
			if(result != 0) {
				return result;
			}
		}
		return 0;
	}
	
}