package com.github.skjolber.packing.iterator;

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
	protected Predicate<Box> filter;
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

	public B withFilter(Predicate<Box> filter) {
		this.filter = filter;

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

			Box stackable = item.getBox();
			if(stackable.getWeight() > maxLoadWeight) {
				continue;
			}

			if(stackable.getVolume() > size.getVolume()) {
				continue;
			}

			List<BoxStackValue> boundRotations = stackable.rotations(size);
			if(boundRotations == null || boundRotations.isEmpty()) {
				continue;
			}

			if(filter != null && !filter.test(stackable)) {
				continue;
			}
						
			Box loadable = new Box(stackable, boundRotations);

			results[i] = new BoxItem(loadable, item.getCount(), i);
		}
		return results;
	}

	public abstract <I extends AbstractBoxItemPermutationRotationIterator> I build();

}
