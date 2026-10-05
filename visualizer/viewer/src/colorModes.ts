// Colour modes for the boxes. Kept free of three.js imports, so that it can be unit tested.

import { Box, StackPlacement } from "./model";

export enum ColorMode {
    /** A colour per box item (the default). */
    BOX_ITEM = "box item",
    /** A colour per box item group; grey without a group. */
    GROUP = "group",
    /** Red (unsupported) to green (fully supported, or on the floor). */
    SUPPORT = "support",
    /** Green (no load) to red (at or over the max load weight); grey without a max load weight. */
    LOAD = "load",
}

export const COLOR_MODES : Array<ColorMode> = [ColorMode.BOX_ITEM, ColorMode.GROUP, ColorMode.SUPPORT, ColorMode.LOAD];

export const NO_VALUE_COLOR = "#808080";

/** The part of the box's bottom which rests on supporters or the floor, from 0 to 1. */
export function getSupportedFraction(placement : StackPlacement) : number {
    if(placement.z === 0) {
        return 1;
    }
    const area = placement.stackable.dx * placement.stackable.dy;
    return area > 0 ? Math.min(1, placement.supportedArea / area) : 1;
}

/** The load weight relative to the max load weight, or undefined if the box has none. */
export function getLoadFraction(placement : StackPlacement) : number | undefined {
    const box = placement.stackable as Box;
    if(box.maxLoadWeight == null) {
        return undefined;
    }
    if(box.maxLoadWeight === 0) {
        return placement.loadWeight > 0 ? Infinity : 0;
    }
    return placement.loadWeight / box.maxLoadWeight;
}

/** From green (0) via yellow to red (1 or more), as a CSS colour. */
export function greenToRed(fraction : number) : string {
    const f = Math.max(0, Math.min(1, fraction));
    const hue = 120 * (1 - f);
    return `hsl(${Math.round(hue)}, 80%, 50%)`;
}

/**
 * @param groupColors colours by group id, filled on demand from randomColor
 * @return the CSS colour of the box in the mode, or undefined to keep its own (box item) colour
 */
export function getColor(mode : ColorMode, placement : StackPlacement, groupColors : Map<string, string>, randomColor : () => string) : string | undefined {
    switch (mode) {
        case ColorMode.GROUP: {
            const groupId = (placement.stackable as Box).groupId;
            if(groupId == null) {
                return NO_VALUE_COLOR;
            }
            var color = groupColors.get(groupId);
            if(!color) {
                color = randomColor();
                groupColors.set(groupId, color);
            }
            return color;
        }
        case ColorMode.SUPPORT:
            return greenToRed(1 - getSupportedFraction(placement));
        case ColorMode.LOAD: {
            const fraction = getLoadFraction(placement);
            return fraction === undefined ? NO_VALUE_COLOR : greenToRed(fraction);
        }
        default:
            return undefined;
    }
}
