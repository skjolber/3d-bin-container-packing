package com.github.skjolber.packing.iterator;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;

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

	protected List<RemainingBoxItemGroup> boxItemGroups;

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

	public B withBoxItemGroups(List<RemainingBoxItemGroup> stackableItems) {
		this.boxItemGroups = stackableItems;

		return (B)this;
	}

	/**
	 * @return whether all of the group's boxes can be loaded into the container together, by volume and weight,
	 *         and each box by its dimensions
	 */
	public boolean fitsInside(RemainingBoxItemGroup boxItemGroup) {
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
	protected record BoxItemGroupMatrix(RemainingBoxItemGroup[] groups, RemainingBoxItem[] boxItems, BoxStackValue[][] stackValues, List<RemainingBoxItemGroup> excluded) {
	}

	protected BoxItemGroupMatrix toMatrix() {
		List<RemainingBoxItemGroup> included = new ArrayList<>(boxItemGroups.size());
		List<RemainingBoxItemGroup> excluded = new ArrayList<>(boxItemGroups.size());

		int count = 0;
		for (RemainingBoxItemGroup group : boxItemGroups) {
			count += group.size();
		}
		BoxStackValue[][] stackValues = new BoxStackValue[count][];

		// box item and box item groups indexes are unique and static
		int offset = 0;
		for (int i = 0; i < boxItemGroups.size(); i++) {
			RemainingBoxItemGroup group = boxItemGroups.get(i);
			if(fitsInside(group)) {
				List<RemainingBoxItem> loadableItems = new ArrayList<>(group.size());
				for (int k = 0; k < group.size(); k++) {
					RemainingBoxItem item = group.get(k);
					Box box = item.getBox();

					stackValues[offset] = AbstractBoxItemPermutationRotationIterator.getRotations(box, dx, dy, dz);
					loadableItems.add(new RemainingBoxItem(item.getBoxItem(), item.getCount(), offset, item.getGlobalIndex()));

					offset++;
				}
				included.add(new RemainingBoxItemGroup(group.getBoxItemGroup(), loadableItems, i));
			} else {
				excluded.add(group);

				offset += group.size();
			}
		}

		RemainingBoxItemGroup[] groupIndex = new RemainingBoxItemGroup[boxItemGroups.size()];
		RemainingBoxItem[] boxIndex = new RemainingBoxItem[offset];

		for (RemainingBoxItemGroup loadableItemGroup : included) {
			groupIndex[loadableItemGroup.getIndex()] = loadableItemGroup;
			for (int k = 0; k < loadableItemGroup.size(); k++) {
				RemainingBoxItem item = loadableItemGroup.get(k);
				boxIndex[item.getLocalIndex()] = item;
			}
		}
		return new BoxItemGroupMatrix(groupIndex, boxIndex, stackValues, excluded);
	}

	public abstract BoxItemGroupPermutationRotationIterator build();

}
