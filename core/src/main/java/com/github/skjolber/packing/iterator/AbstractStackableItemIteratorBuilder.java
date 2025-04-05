package com.github.skjolber.packing.iterator;

import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractStackableItemIteratorBuilder<B extends AbstractStackableItemIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected Predicate<Box> filter;
	protected Dimension size;
	protected List<BoxItem> stackableItems;

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

	public B withStackableItems(List<BoxItem> stackableItems) {
		this.stackableItems = stackableItems;

		return (B)this;
	}

	protected IndexedStackableItem[] toMatrix() {
		IndexedStackableItem[] results = new IndexedStackableItem[stackableItems.size()];

		for (int i = 0; i < stackableItems.size(); i++) {
			BoxItem item = stackableItems.get(i);

			if(item.getCount() == 0) {
				continue;
			}

			Box stackable = item.getStackable();
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

			results[i] = new IndexedStackableItem(loadable, item.getCount(), i);
		}
		return results;
	}

	public abstract <I extends AbstractStackableItemPermutationRotationIterator> I build();

}
