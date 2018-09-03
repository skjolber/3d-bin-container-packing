package com.github.skjolberg.packing;

import java.util.ArrayList;

/**
 * A level within a container
 * 
 */

public class Level extends ArrayList<Placement>{
	
	private static final long serialVersionUID = 1L;

	public int getHeight() {
		int height = 0;
		
		for(final Placement placement : this) {
			final Box box = placement.getBox();
			if(box.getHeight() > height) {
				height = box.getHeight();
			}
		}
		
		return height;
	}
	
	/**
	 * 
	 * Check whether placement is valid, i.e. no overlaps.
	 * 
	 */
	
	public void validate() {
		for(int i = 0; i < size(); i++) {
			for(int j = 0; j < size(); j++) {
				if(j == i) {
					if(!get(i).intercets(get(j))) {
						throw new IllegalArgumentException();
					}
				} else {
					if(get(i).intercets(get(j))) {
						throw new IllegalArgumentException(i + " vs " + j);
					}
				}
			}
		}		
	}
	
}