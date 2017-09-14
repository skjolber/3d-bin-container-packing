package com.github.skjolberg.packing;

import java.util.ArrayList;

/**
 * A level within a container
 * 
 */

public class Level extends ArrayList<Placement>{
	
	public int getHeight() {
		int height = 0;
		
		for(Placement placement : this) {
			Box box = placement.getBox();
			if(box.getHeight() > height) {
				height = box.getHeight();
			}
		}
		
		return height;
	}
	
	
}