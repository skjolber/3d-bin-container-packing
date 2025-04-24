package com.github.skjolber.packing.packer;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public class ListPlacementResultComparator implements Comparator<IntermediatePlacementResult> {

	protected final Comparator<IntermediatePlacementResult>[] list;
	
	@SuppressWarnings("unchecked")
	public ListPlacementResultComparator(List<Comparator<IntermediatePlacementResult>> list) {
		this.list = list.toArray(new Comparator[list.size()]);
	}
	
	@Override
	public int compare(IntermediatePlacementResult o1, IntermediatePlacementResult o2) {
		for(int i = 0; i < list.length; i++) {
			int result = list[i].compare(o1, o2);
			if(result != 0) {
				return result;
			}
		}
		return 0;
	}
	
}