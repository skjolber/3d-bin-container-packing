import { expect, test } from '@jest/globals';

import { BoxPlacementEntry, computeLoads, isSameBoxItem } from './utils';

function box(
  id: string,
  boxItemKey: number | undefined,
  z: number,
  weight: number
): BoxPlacementEntry {
  return {
    placement: { x: 0, y: 0, z },
    stackable: { id, boxItemKey, dx: 2, dy: 2, dz: 1, weight },
    color: '#ffffff',
    isHovered: false
  };
}

test('computes load pressure and identical boxes throughout the load graph', () => {
  const base = box('same-label', 1, 0, 1);
  const middle = box('same-label', 1, 1, 2);
  const top = box('same-label', 2, 2, 3);

  const loads = computeLoads([base, middle, top]);
  const baseLoad = loads.get(base)!;

  expect(baseLoad.loadWeight).toBeCloseTo(5);
  expect(baseLoad.loadPressure).toBeCloseTo(1.25);
  expect(baseLoad.directCount).toBe(1);
  expect(baseLoad.stackDepth).toBe(2);
  expect(baseLoad.loadBoxCount).toBe(2);
  expect(baseLoad.identicalLoadBoxCount).toBe(1);
  expect(baseLoad.nonIdenticalLoadBoxCount).toBe(1);
});

test('uses the serialized box-item key instead of a matching display id', () => {
  const first = box('same-label', 1, 0, 1);
  const second = box('same-label', 2, 1, 1);

  expect(isSameBoxItem(first, second)).toBe(false);
});
