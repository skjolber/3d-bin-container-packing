package com.github.skjolber.packing.packer;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

public class AbstractPackagerAdapterTest {

	@Test
	public void testMaxWeightAndVolume() {
        Container container1 = Container.newBuilder()
                .withSize(10, 10, 10)
                .withEmptyWeight(0)
                .withMaxLoadWeight(1000000)
                .build();

        Container container2 = Container.newBuilder()
                .withSize(1, 1, 1)
                .withEmptyWeight(0)
                .withMaxLoadWeight(1)
                .build();

        List<ContainerItem> items = ContainerItem
                .newListBuilder()
                .withContainer(container1, 5)
                .withContainer(container2, 3)
                .build();

        ContainerItemsCalculator adapter = create(items);

		// volume
        for(int i = 0; i <= 5; i++) {
        	BigInteger max = adapter.calculateMaxVolume(i).value;
        	assertEquals(max, BigInteger.valueOf(i * 10 * 10 * 10));
        }

        for(int i = 0; i <= 3; i++) {
        	BigInteger max = adapter.calculateMaxVolume(5 + i).value;
        	assertEquals(max, BigInteger.valueOf(5 * 10 * 10 * 10 + i * 1));
        }
        
		// weight
        for(int i = 0; i <= 5; i++) {
        	BigInteger max = adapter.calculateMaxWeight(i).value;
        	assertEquals(max, BigInteger.valueOf(i * 1000000));
        }

        for(int i = 0; i <= 3; i++) {
        	BigInteger max = adapter.calculateMaxWeight(5 + i).value;
        	assertEquals(max, BigInteger.valueOf(5 * 1000000 + i * 1));
        }
	}
	
	@Test
	public void testMaxWeightAndVolumeLongOverflow() {
        Container container1 = Container.newBuilder()
                .withSize(23500, 13560, 2690)
                .withEmptyWeight(0)
                .withMaxLoadWeight(Integer.MAX_VALUE)
                .build();

        Container container2 = Container.newBuilder()
                .withSize(1, 1, 1)
                .withEmptyWeight(0)
                .withMaxLoadWeight(1)
                .build();

        List<ContainerItem> items = ContainerItem
                .newListBuilder()
                .withContainer(container1, Integer.MAX_VALUE)
                .withContainer(container2, 3)
                .build();
        
        ContainerItemsCalculator adapter = create(items);

		// volume overflows, max value is 9,223,372,036,854,775,807 (~19 digits) and 
		// max integer 2,147,483,647 (~10 digits) 
    	BigInteger maxVolume = adapter.calculateMaxVolume(Integer.MAX_VALUE).value;
    	BigInteger expectedVolume = BigInteger.valueOf(Integer.MAX_VALUE) // ~10
    			.multiply(BigInteger.valueOf(23500)) // ~4 
    			.multiply(BigInteger.valueOf(13560)) // ~4
    			.multiply(BigInteger.valueOf(2690)); // ~3
    	
    	assertEquals(maxVolume, expectedVolume);

		// weight should really not log overflow, but test max values here too
    	BigInteger maxWeight = adapter.calculateMaxWeight(Integer.MAX_VALUE).value;
    	BigInteger expectedWeight = BigInteger.valueOf(Integer.MAX_VALUE)
    			.multiply(BigInteger.valueOf(Integer.MAX_VALUE));
    	
    	assertEquals(maxWeight, expectedWeight);
	}
	
	private ContainerItemsCalculator create(List<ContainerItem> items) {
		List<ControlledContainerItem> containerItems = new ArrayList<>(items.size());
		for(ContainerItem containerItem : items) {
			containerItems.add(new ControlledContainerItem(containerItem));
		}
		
		return new ContainerItemsCalculator(containerItems);
	}
}
