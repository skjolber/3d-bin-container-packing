package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Dimension;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

public abstract class AbstractBoxItemGroupIteratorBuilder<B extends AbstractBoxItemGroupIteratorBuilder<B>>  {

	protected int maxLoadWeight = -1;
	protected Predicate<Box> filter;
	protected Dimension size;
	protected List<BoxItemGroup> boxItemGroups;

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

	public B withBoxItemGroups(List<BoxItemGroup> stackableItems) {
		this.boxItemGroups = stackableItems;

		return (B)this;
	}

	protected List<BoxItemGroup> toMatrix() {
		List<BoxItemGroup> results = new ArrayList<>(boxItemGroups.size());

		int offset = 0;
		
		for (int i = 0; i < boxItemGroups.size(); i++) {
			
			BoxItemGroup group = boxItemGroups.get(i);
			
			List<BoxItem> loadableItems = new ArrayList<>(group.size());
			for (int k = 0; k < group.size(); k++) {
				BoxItem item = group.get(k);
	
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
	
				loadableItems.add(new BoxItem(loadable, item.getCount(), offset));
				
				offset++;
			}
			if(!loadableItems.isEmpty()) {
				results.add(new BoxItemGroup(group.getId(), loadableItems));
			}
		}
		return results;
	}

	public abstract BoxItemGroupPermutationRotationIterator build();

}
