package com.github.skjolber.packing.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;

public class BoxItemOrderValidator {

	public boolean validate(List<BoxItem> items, Order order, List<Container> containers) {
		if(order == Order.CRONOLOGICAL) {
			int boxItemIndex = 0;
			int boxIndex = 0;
			
			search:
			for (Container container : containers) {
				for (Placement placement : container.getStack().getPlacements()) {
					Box box = placement.getBox();

					BoxItem boxItem = items.get(boxItemIndex);
					if(!box.getId().equals(boxItem.getBox().getId())) {
						return false;
					}

					boxIndex++;
					if(boxIndex >= boxItem.getCount()) {
						boxItemIndex++;
						if(boxItemIndex >= items.size()) {
							break search;
						}

						boxIndex = 0;
					}
				}
			}
			
		} else if(order == Order.CRONOLOGICAL_ALLOW_SKIPPING) {
			Map<String, Integer> map = new HashMap<>();
			for(int i = 0; i < items.size(); i++) {
				BoxItem boxItem = items.get(i);
				map.put(boxItem.getBox().getId(), map.size());
			}
			
			for (Container container : containers) {
				int index = -1;
				for (Placement placement : container.getStack().getPlacements()) {
					int currentIndex = map.get(placement.getBox().getId());
					if(currentIndex < index) {
						return false;
					}
				}
			}
		}
		return true;
	}
	

	public boolean validate(List<BoxItemGroup> itemGroups, Order order, PackagerResult result) {
		if(order == Order.CRONOLOGICAL) {
			
			List<Container> containers = result.getContainers();
			
			int boxItemGroupIndex = 0;
			int boxItemIndex = 0;
			int boxIndex = 0;
			
			search:
			for (Container container : containers) {
				for (Placement placement : container.getStack().getPlacements()) {
					
					BoxItemGroup boxItemGroup = itemGroups.get(boxItemGroupIndex);
					
					Box box = placement.getBox();

					BoxItem boxItem = boxItemGroup.get(boxItemIndex);
					if(!box.getId().equals(boxItem.getBox().getId())) {
						return false;
					}

					boxIndex++;
					if(boxIndex >= boxItem.getCount()) {
						boxItemIndex++;
						if(boxItemIndex >= boxItemGroup.size()) {
							boxItemGroupIndex++;

							if(boxItemGroupIndex >= itemGroups.size()) {
								break search;
							}
							
							boxItemIndex = 0;
						}

						boxIndex = 0;
					}
				}
			}
			
		} else if(order == Order.CRONOLOGICAL_ALLOW_SKIPPING) {
			List<Container> containers = result.getContainers();
			
			Map<String, Integer> map = new HashMap<>();
			
			for (BoxItemGroup boxItemGroup : itemGroups) {
				int size = map.size();
				for (BoxItem boxItem : boxItemGroup.getItems()) {
					map.put(boxItem.getBox().getId(), size);
				}
			}
			
			for (Container container : containers) {
				int index = -1;
				for (Placement placement : container.getStack().getPlacements()) {
					int currentIndex = map.get(placement.getBox().getId());
					if(currentIndex < index) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
