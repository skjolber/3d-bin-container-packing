package com.github.skjolber.packing;

import java.util.ArrayList;

/**
 * A level within a container
 *
 */
public class Level extends ArrayList<Placement>{

	private static final long serialVersionUID = 1L;

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

	public int getWeight() {
		int weight = 0;

		for(Placement placement : this) {
			weight += placement.getBox().getWeight();
		}

		return weight;
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
					if(!get(i).intersects(get(j))) {
						throw new IllegalArgumentException();
					}
				} else {
					if(get(i).intersects(get(j))) {
						throw new IllegalArgumentException(i + " vs " + j + ": " + get(i) + " vs " + get(j));
					}
				}
			}
		}
	}


}
