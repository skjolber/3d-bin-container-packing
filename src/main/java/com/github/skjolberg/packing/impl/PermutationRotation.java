package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Box;

public final class PermutationRotation {

    private final int count;
    private final Box[] boxes;

    PermutationRotation(int count, Box[] boxes) {
        this.count = count;
        this.boxes = boxes;
    }

    Box[] getBoxes() {
        return boxes;
    }

    public int getCount() {
        return count;
    }
}
