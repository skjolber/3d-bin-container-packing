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
 *      
 * @param <B> builder
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

	/**
	 * The box items of an iterator, by index: copies of the box items which fit the container (sharing their boxes), and
	 * the rotations of each which fit (stack values of its box).
	 */
	protected record BoxItemMatrix(BoxItem[] boxItems, BoxStackValue[][] stackValues, List<BoxItem> excluded) {
	}

	protected BoxItemMatrix toMatrix() {
		return toMatrix(boxItems, dx, dy, dz, volume, maxLoadWeight);
	}

	protected static BoxItemMatrix toMatrix(List<BoxItem> boxItems, int dx, int dy, int dz, long volume, int maxLoadWeight) {
		BoxItem[] included = new BoxItem[boxItems.size()];
		BoxStackValue[][] stackValues = new BoxStackValue[boxItems.size()][];
		List<BoxItem> excluded = new ArrayList<>(boxItems.size());

		// box item and box item groups indexes are unique and static
		for (int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);

			Box box = boxItem.getBox();
			if(box.getWeight() > maxLoadWeight) {
				excluded.add(boxItem);
				continue;
			}

			if(box.getVolume() > volume) {
				excluded.add(boxItem);
				continue;
			}

			BoxStackValue[] rotations = AbstractBoxItemPermutationRotationIterator.getRotations(box, dx, dy, dz);
			if(rotations == null) {
				excluded.add(boxItem);
				continue;
			}
			stackValues[i] = rotations;
			included[i] = new BoxItem(box, boxItem.getCount(), i, boxItem.getGlobalIndex()).withOrderingOf(boxItem);
		}
		return new BoxItemMatrix(included, stackValues, excluded);
	}

	public abstract BoxItemPermutationRotationIterator build();

}
