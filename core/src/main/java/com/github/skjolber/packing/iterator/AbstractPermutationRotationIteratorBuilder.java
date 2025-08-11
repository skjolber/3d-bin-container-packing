package com.github.skjolber.packing.iterator;

import java.util.List;

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

@Deprecated
public abstract class AbstractPermutationRotationIteratorBuilder<B extends AbstractPermutationRotationIteratorBuilder<B>> {

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

	public B withBoxItems(List<BoxItem> boxItems) {
		this.boxItems = boxItems;

		return (B)this;
	}

	protected PermutationBoxItemValue[] toMatrix() {
		PermutationBoxItemValue[] results = new PermutationBoxItemValue[boxItems.size()];

		for (int i = 0; i < boxItems.size(); i++) {
			BoxItem item = boxItems.get(i);

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

			results[i] = new PermutationBoxItemValue(i, item.getCount(), item, boundRotations);
		}
		return results;
	}

}
