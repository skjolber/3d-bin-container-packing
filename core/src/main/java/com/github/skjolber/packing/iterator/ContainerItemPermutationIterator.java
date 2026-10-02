package com.github.skjolber.packing.iterator;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Depth-first iterator over ordered container-item indexes. The caller pushes
 * the container indexes for each packing state, calls {@link #next()} to select
 * one, and pops the level when its indexes are exhausted. The caller determines
 * which indexes are available at each level.
 */
public class ContainerItemPermutationIterator {

	private final int[] permutation;
	private final List<Integer>[] containerIndexesByLevel;
	private final int[] nextContainerIndexOffsetByLevel;
	private int levelCount;
	// The top level is selected exactly when length == levelCount.
	private int length;

	@SuppressWarnings("unchecked")
	public ContainerItemPermutationIterator(int maxLength) {
		if(maxLength < 0) {
			throw new IllegalArgumentException("Negative maximum length");
		}
		this.permutation = new int[maxLength];
		this.containerIndexesByLevel = (List<Integer>[]) new List<?>[maxLength];
		this.nextContainerIndexOffsetByLevel = new int[maxLength];
	}

	/** Number of selected container items in the current prefix. */
	public int length() {
		return length;
	}

	/** Whether a level of container indexes is currently pushed. */
	public boolean hasLevel() {
		return levelCount > 0;
	}

	/** Container-item index at the specified position in the current prefix. */
	public int get(int index) {
		if(index < 0 || index >= length) {
			throw new IndexOutOfBoundsException(index);
		}
		return permutation[index];
	}

	/** A snapshot of the currently selected container-item indexes. */
	public int[] getPermutations() {
		return Arrays.copyOf(permutation, length);
	}

	/** Container indexes saved for the currently selected item. */
	public List<Integer> getContainerIndexes() {
		if(length != levelCount || length == 0) {
			throw new IllegalStateException("No current permutation");
		}
		return containerIndexesByLevel[length - 1];
	}

	/**
	 * Push container indexes for the root or for the current selected prefix.
	 * Indexes must be unique within the list. The list is copied because an
	 * adapter may reuse it after acceptance.
	 */
	public void push(List<Integer> containerIndexes) {
		if(levelCount == permutation.length) {
			throw new IllegalStateException("Maximum permutation length reached");
		}
		if(levelCount > 0 && length != levelCount) {
			throw new IllegalStateException("No selected parent for the next level");
		}
		containerIndexesByLevel[levelCount] = List.copyOf(Objects.requireNonNull(containerIndexes));
		nextContainerIndexOffsetByLevel[levelCount] = 0;
		levelCount++;
	}

	/** Discard the top level, including its selected item if any. */
	public void pop() {
		if(levelCount == 0) {
			throw new IllegalStateException("No level to pop");
		}
		int level = --levelCount;
		length = level;
		containerIndexesByLevel[level] = null;
	}

	/**
	 * Whether the top level has another container index. Does not change the
	 * current permutation; the caller must pop an exhausted level.
	 */
	public boolean hasNext() {
		return levelCount > 0 && nextContainerIndexOffsetByLevel[levelCount - 1] < containerIndexesByLevel[levelCount - 1].size();
	}

	/** Select and return the next container index at the top level. */
	public int next() {
		if(!hasNext()) {
			throw new NoSuchElementException("No more container indexes at this level");
		}
		int level = levelCount - 1;
		int containerIndex = containerIndexesByLevel[level].get(nextContainerIndexOffsetByLevel[level]++);
		if(containerIndex < 0) {
			throw new IllegalArgumentException("Negative container-item index");
		}
		permutation[level] = containerIndex;
		length = level + 1;
		return containerIndex;
	}
}
