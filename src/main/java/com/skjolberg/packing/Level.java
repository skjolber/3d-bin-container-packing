package com.skjolberg.packing;

import java.util.ArrayList;

/**
 * A level within a container
 * 
 */

public class Level extends ArrayList<Box>{
	
	public int getHeight() {
		int height = 0;
		
		for(Box box : this) {
			if(box.getHeight() > height) {
				height = box.getHeight();
			}
		}
		
		return height;
	}
	
	
}