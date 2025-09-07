package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractBoxItemIteratorBuilder<B extends AbstractBoxItemIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected int dx = -1;
	protected int dy = -1;
	protected int dz = -1;
	protected long volume = -1L;
	
	protected List<BoxItem> boxItems;

	public B withLoadSize(int dx, int dy, int dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		this.volume = (long)dx * (long)dy * (long)dz;

		return (B)this;
	}

	public B withMaxLoadWeight(int maxLoadWeight) {
		this.maxLoadWeight = maxLoadWeight;

		return (B)this;
	}

	public B withBoxItems(List<BoxItem> stackableItems) {
		this.boxItems = stackableItems;

		return (B)this;
	}

	protected BoxItem[] toMatrix() {
		BoxItem[] results = new BoxItem[boxItems.size()];

		for (int i = 0; i < boxItems.size(); i++) {
			BoxItem item = boxItems.get(i);

			if(item.getCount() == 0) {
				continue;
			}

			Box box = item.getBox();
			if(box.getWeight() > maxLoadWeight) {
				continue;
			}

			if(box.getVolume() > volume) {
				continue;
			}

			List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
			if(boundRotations == null || boundRotations.isEmpty()) {
				continue;
			}
			
			List<BoxStackValue> cloned = new ArrayList<>(boundRotations.size());
			for(BoxStackValue v : boundRotations) {
				cloned.add(v.clone());
			}
			Box clonedBox = new Box(box, cloned);

			results[i] = new BoxItem(clonedBox, item.getCount(), i);
		}
		return results;
	}

	public abstract BoxItemPermutationRotationIterator build();

}
