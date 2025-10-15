package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

// simplified reset

public class CustomIntArrayList extends IntArrayList {
	
    @Override
    public void clear() {
        this.size = 0;
    }
    
    public IntArrayList insertionSortThis(IntComparator comparator) {
    	insertionSort(this.items, this.size(), comparator);
        return this;
    }

    public static void insertionSort(int[] arr, int size, IntComparator comparator) {
        int n = size;
        for (int i = 1; i < n; i++) { // Start from the second element
            int key = arr[i]; // Element to be inserted
            int j = i - 1; // Index of the last element in the sorted portion

            /* Move elements of arr[0..i-1], that are greater than key,
               to one position ahead of their current position */
            while (j >= 0 && comparator.compare(arr[j], key) > 0) {
                arr[j + 1] = arr[j];
                j = j - 1;
            }
            arr[j + 1] = key; // Place the key in its correct position
        }
    }
}
