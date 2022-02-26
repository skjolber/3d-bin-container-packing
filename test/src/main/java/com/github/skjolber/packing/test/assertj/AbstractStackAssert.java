package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;

public class AbstractStackAssert<SELF extends AbstractStackAssert<SELF, ACTUAL>, ACTUAL extends Stack>
extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractStackAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF placementsDoNotIntersect() {
		isNotNull();
		
		List<StackPlacement> entries = actual.getPlacements();
		
		for(StackPlacement stackPlacement1 : entries) {
			for(StackPlacement stackPlacement2 : entries) {
				if(stackPlacement1 != stackPlacement2) {
					if(stackPlacement1.intersects(stackPlacement2)) {
						failWithMessage(stackPlacement1 + " intersects " + stackPlacement2 + " for stack " + actual.getClass().getName());
					}
				}
			}
		}
		return myself;
	}


}
