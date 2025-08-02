package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Dimension;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractBoxItemIteratorBuilder<B extends AbstractBoxItemIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected Dimension size;
	protected List<BoxItem> boxItems;

	public B withSize(int dx, int dy, int dz) {
		this.size = new Dimension(dx, dy, dz);

		return (B)this;
	}

	public B withLoadSize(Dimension dimension) {
		this.size = dimension;

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

			if(box.getVolume() > size.getVolume()) {
				continue;
			}

			List<BoxStackValue> boundRotations = box.rotations(size);
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

	public abstract AbstractBoxItemPermutationRotationIterator build();

}
