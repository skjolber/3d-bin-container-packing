package com.github.skjolber.packing.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooHighReason;
import com.github.skjolber.packing.validator.reasons.BoxItemCountTooLowReason;
import com.github.skjolber.packing.validator.reasons.TooFewBoxItemIdsReason;
import com.github.skjolber.packing.validator.reasons.TooManyBoxItemIdsReason;

public class BoxCountValidator {

	// check that each group is just within one container
	public boolean validateBoxItemGroupsCounts(List<BoxItemGroup> groups, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, BoxItemGroup> boxToGroup = new HashMap<>();
		
		for (BoxItemGroup boxItemGroup : groups) {
			
			for (BoxItem item: boxItemGroup.getItems()) {
				boxToGroup.put(item.getBox().getId(), boxItemGroup);
			}
		}
		
		for (Container container : result.getContainers()) {
			Stack stack = container.getStack();

			Map<String, BoxItemGroup> groupsInContainer = new HashMap<>(); // box id to group

			Map<String, Integer> resultCount = new HashMap<>();
			for (Placement placement : stack.getPlacements()) {
				String id = placement.getBox().getId();
				
				Integer integer = resultCount.get(id);
				if(integer == null) {
					resultCount.put(id, 1);
				} else {
					resultCount.put(id, integer + 1);
				}
				
				BoxItemGroup boxItemGroup = boxToGroup.get(id);
				if(boxItemGroup == null) {
					reasons.add(new TooManyBoxItemIdsReason(id + " not found"));
					return false;
				}

				groupsInContainer.put(boxItemGroup.getId(), boxItemGroup);
			}

			for (Entry<String, BoxItemGroup> entry : groupsInContainer.entrySet()) {
				BoxItemGroup boxItemGroup = entry.getValue();
				
				for (BoxItem boxItem : boxItemGroup.getItems()) {
					Integer count = resultCount.remove(boxItem.getBox().getId());

					if(count == null) {
						reasons.add(new TooFewBoxItemIdsReason(boxItem.getBox().getId() + " not found"));
						return false;
					}

					if(count < boxItem.getCount()) {
						reasons.add(new BoxItemCountTooLowReason("Box id " + boxItem.getBox().getId() + " expected count " + boxItem.getCount() + ", found " + count));
						return false;
					} else if(count > boxItem.getCount()) {
						reasons.add(new BoxItemCountTooHighReason("Box id " + boxItem.getBox().getId() + " expected count " + boxItem.getCount() + ", found " + count));
						return false;
					}
				}
			}
			
			// is there any boxes which did not belong to a group?
			if(!resultCount.isEmpty()) {
				reasons.add(new TooManyBoxItemIdsReason("Unexpectedly found " + resultCount.keySet()));
				return false;
			}
		}
				
		return true;
	}

	public boolean validate(List<BoxItem> items, PackagerResult result, List<ValidatorResultReason> reasons) {
		
		Map<String, Integer> resultCount = new HashMap<>();
		for (Container container : result.getContainers()) {
			Stack stack = container.getStack();
			for (Placement placement : stack.getPlacements()) {
				Box box = placement.getBox();
				
				String id = box.getId();

				Integer integer = resultCount.get(id);
				if(integer == null) {
					resultCount.put(id, 1);
				} else {
					resultCount.put(id, integer + 1);
				}
			}
		}
		
		for (BoxItem boxItem : items) {
			String id = boxItem.getBox().getId();
			
			Integer count = resultCount.get(id);
			
			if(count == null) {
				reasons.add(new TooFewBoxItemIdsReason(id + ": Not found"));
				return false;
			}

			if(count < boxItem.getCount()) {
				reasons.add(new BoxItemCountTooLowReason(id + ": Expected " + boxItem.getCount() + ", found " + count));
				return false;
			} else if(count > boxItem.getCount()) {
				reasons.add(new BoxItemCountTooHighReason(id + ": Expected " + boxItem.getCount() + ", found " + count));
				return false;
			}
		}
		
		return true;
	}

}
