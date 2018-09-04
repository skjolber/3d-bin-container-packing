package com.github.skjolberg.packing;

final class PermutationRotation {

    private final int count;
    private final Box[] boxes;

    PermutationRotation(int count, Box[] boxes) {
        this.count = count;
        this.boxes = boxes;
    }

    Box[] getBoxes() {
        return boxes;
    }

    int getCount() {
        return count;
    }
}
