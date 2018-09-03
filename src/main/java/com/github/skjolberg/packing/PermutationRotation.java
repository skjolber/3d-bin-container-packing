package com.github.skjolberg.packing;

public class PermutationRotation {

    private final int count;

    private final Box[] boxes;

    public PermutationRotation(final int count, final Box[] boxes) {
        this.count = count;
        this.boxes = boxes;
    }

    public Box[] getBoxes() {
        return boxes;
    }

    public int getCount() {
        return count;
    }

}
