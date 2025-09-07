package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.StackableSurface;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractBoxItemGroupIteratorBuilder<B extends AbstractBoxItemGroupIteratorBuilder<B>>  {

	protected int maxLoadWeight = -1;
	
	protected int dx = -1;
	protected int dy = -1;
	protected int dz = -1;
	protected long volume = -1L;

	protected List<BoxItemGroup> boxItemGroups;

	public B withLoadSize(int dx, int dy, int dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		this.volume = (long)dx * (long)dy * (long)dz;

		return (B) this;
	}

	public B withMaxLoadWeight(int maxLoadWeight) {
		this.maxLoadWeight = maxLoadWeight;

		return (B)this;
	}

	public B withBoxItemGroups(List<BoxItemGroup> stackableItems) {
		this.boxItemGroups = stackableItems;

		return (B)this;
	}

	public boolean fitsInside(BoxItemGroup boxItemGroup) {
		if(boxItemGroup.getVolume() <= volume && boxItemGroup.getWeight() <= maxLoadWeight) {			
			for(int i = 0; i < boxItemGroup.size(); i++) {
				Box box = boxItemGroup.get(i).getBox();
				if(!box.fitsInside(dx, dy, dz)) {
					return false;
				}
			}		
		}
		return true;
	}

	public abstract BoxItemGroupPermutationRotationIterator build();

}
