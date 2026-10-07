package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 *      
 * @param <B> builder
 */


public abstract class AbstractBoxItemGroupIteratorBuilder<B extends AbstractBoxItemGroupIteratorBuilder<B>>  {

	protected int maxLoadWeight = -1;
	
	protected int dx = -1;
	protected int dy = -1;
	protected int dz = -1;
	protected long volume = -1L;

	protected List<BoxItemGroup> boxItemGroups;

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

	public B withBoxItemGroups(List<BoxItemGroup> stackableItems) {
		this.boxItemGroups = stackableItems;

		return (B)this;
	}

	/**
	 * @return whether all of the group's boxes can be loaded into the container together, by volume and weight,
	 *         and each box by its dimensions
	 */
	public boolean fitsInside(BoxItemGroup boxItemGroup) {
		if(boxItemGroup.getVolume() > volume || boxItemGroup.getWeight() > maxLoadWeight) {
			return false;
		}
		for(int i = 0; i < boxItemGroup.size(); i++) {
			Box box = boxItemGroup.get(i).getBox();
			if(!box.fitsInside(dx, dy, dz)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The groups and box items of an iterator, by index: copies of the groups which fit the container, with copies of
	 * their box items (sharing their boxes), and the rotations of each box item which fit (stack values of its box).
	 */
	protected record BoxItemGroupMatrix(BoxItemGroup[] groups, BoxItem[] boxItems, BoxStackValue[][] stackValues, List<BoxItemGroup> excluded) {
	}

	protected BoxItemGroupMatrix toMatrix() {
		List<BoxItemGroup> included = new ArrayList<>(boxItemGroups.size());
		List<BoxItemGroup> excluded = new ArrayList<>(boxItemGroups.size());

		int count = 0;
		for (BoxItemGroup group : boxItemGroups) {
			count += group.size();
		}
		BoxStackValue[][] stackValues = new BoxStackValue[count][];

		// box item and box item groups indexes are unique and static
		int offset = 0;
		for (int i = 0; i < boxItemGroups.size(); i++) {
			BoxItemGroup group = boxItemGroups.get(i);
			if(fitsInside(group)) {
				List<BoxItem> loadableItems = new ArrayList<>(group.size());
				for (int k = 0; k < group.size(); k++) {
					BoxItem item = group.get(k);
					Box box = item.getBox();

					stackValues[offset] = AbstractBoxItemPermutationRotationIterator.getRotations(box, dx, dy, dz);
					loadableItems.add(new BoxItem(box, item.getCount(), offset, item.getGlobalIndex()).withOrderingOf(item));

					offset++;
				}
				included.add(new BoxItemGroup(group.getId(), loadableItems, i));
			} else {
				excluded.add(group);

				offset += group.size();
			}
		}

		BoxItemGroup[] groupIndex = new BoxItemGroup[boxItemGroups.size()];
		BoxItem[] boxIndex = new BoxItem[offset];

		for (BoxItemGroup loadableItemGroup : included) {
			groupIndex[loadableItemGroup.getIndex()] = loadableItemGroup;
			for (int k = 0; k < loadableItemGroup.size(); k++) {
				BoxItem item = loadableItemGroup.get(k);
				boxIndex[item.getLocalIndex()] = item;
			}
		}
		return new BoxItemGroupMatrix(groupIndex, boxIndex, stackValues, excluded);
	}

	public abstract BoxItemGroupPermutationRotationIterator build();

}
