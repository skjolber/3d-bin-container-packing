package com.github.skjolber.packing.iterator;

import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractPermutationRotationIteratorBuilder<B extends AbstractPermutationRotationIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected Predicate<Stackable> filter;
	protected Dimension size;
	protected List<StackableItem> stackableItems;

	public B withSize(int dx, int dy, int dz) {
		this.size = new Dimension(dx, dy, dz);

		return (B)this;
	}

	public B withLoadSize(Dimension dimension) {
		this.size = dimension;

		return (B)this;
	}

	public B withFilter(Predicate<Stackable> filter) {
		this.filter = filter;

		return (B)this;
	}

	public B withMaxLoadWeight(int maxLoadWeight) {
		this.maxLoadWeight = maxLoadWeight;

		return (B)this;
	}

	public B withStackableItems(List<StackableItem> stackableItems) {
		this.stackableItems = stackableItems;

		return (B)this;
	}

	protected PermutationStackableValue[] toMatrix() {
		PermutationStackableValue[] results = new PermutationStackableValue[stackableItems.size()];

		for (int i = 0; i < stackableItems.size(); i++) {
			StackableItem item = stackableItems.get(i);

			Stackable stackable = item.getStackable();
			if(stackable.getWeight() > maxLoadWeight) {
				continue;
			}

			if(stackable.getVolume() > size.getVolume()) {
				continue;
			}

			List<StackValue> boundRotations = stackable.rotations(size);
			if(boundRotations == null || boundRotations.isEmpty()) {
				continue;
			}

			if(filter != null && !filter.test(stackable)) {
				continue;
			}

			results[i] = new PermutationStackableValue(i, item.getCount(), stackable, boundRotations);
		}
		return results;
	}


}
