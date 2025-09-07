package com.github.skjolber.packing.iterator;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@Deprecated
public abstract class AbstractPermutationRotationIteratorBuilder<B extends AbstractPermutationRotationIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected int dx = -1;
	protected int dy = -1;
	protected int dz = -1;
	protected long volume = -1L;
	protected List<BoxItem> boxItems;

	public B withLoadSize(Container container) {
		return withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
	}

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

			if(box.getVolume() > volume) {
				continue;
			}

			List<BoxStackValue> boundRotations = box.rotations(dx, dy, dz);
			if(boundRotations == null || boundRotations.isEmpty()) {
				continue;
			}

			results[i] = new PermutationBoxItemValue(i, item.getCount(), item, boundRotations);
		}
		return results;
	}

}
