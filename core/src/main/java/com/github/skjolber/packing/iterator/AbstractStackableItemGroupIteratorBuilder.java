package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.StackableItemGroup;
import com.github.skjolber.packing.api.packager.BoundedStackable;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractStackableItemGroupIteratorBuilder<B extends AbstractStackableItemGroupIteratorBuilder<B>> {

	protected int maxLoadWeight = -1;
	protected Predicate<Stackable> filter;
	protected Dimension size;
	protected List<StackableItemGroup> stackableItemGroups;

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

	public B withStackableItemGroups(List<StackableItemGroup> stackableItems) {
		this.stackableItemGroups = stackableItems;

		return (B)this;
	}

	protected List<StackableItemGroup> toMatrix() {
		List<StackableItemGroup> results = new ArrayList<>(stackableItemGroups.size());

		int offset = 0;
		
		for (int i = 0; i < stackableItemGroups.size(); i++) {
			
			StackableItemGroup group = stackableItemGroups.get(i);
			
			List<StackableItem> loadableItems = new ArrayList<>(group.size());
			for (int k = 0; k < group.size(); k++) {
				StackableItem item = group.get(k);
	
				if(item.getCount() == 0) {
					continue;
				}
	
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
							
				BoundedStackable loadable = new BoundedStackable(stackable, boundRotations);
	
				loadableItems.add(new IndexedStackableItem(loadable, item.getCount(), offset));
				
				offset++;
			}
			if(!loadableItems.isEmpty()) {
				results.add(new StackableItemGroup(group.getId(), loadableItems));
			}
		}
		return results;
	}

	public abstract StackableItemPermutationRotationIterator build();

}
