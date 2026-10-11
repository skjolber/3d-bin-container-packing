import { expect, test } from '@jest/globals';

import { ColorMode, getColor, getExtractionColor, getLoadFraction, getSupportedFraction, greenToRed, NO_VALUE_COLOR } from './colorModes';
import { parsePackagings } from './model';

const sample = require('./fixtures/containers.json');

test('support: floor boxes and boxes resting on others with their whole bottom are fully supported', () => {
  const placements = parsePackagings(sample)[0].containers[0].stack.placements;
  expect(placements.map(getSupportedFraction)).toEqual([1, 1, 1]);
  expect(getColor(ColorMode.SUPPORT, placements[2], new Map(), () => '#000')).toBe(greenToRed(0));
});

test('load: relative to the max load weight, grey without one', () => {
  const placements = parsePackagings(sample)[0].containers[0].stack.placements;
  // A carries half of C (weight 4) with a max load weight of 1
  expect(getLoadFraction(placements[0])).toBe(2);
  expect(getColor(ColorMode.LOAD, placements[0], new Map(), () => '#000')).toBe(greenToRed(1));
  expect(getColor(ColorMode.LOAD, placements[2], new Map(), () => '#000')).toBe(NO_VALUE_COLOR);
});

test('group: a colour per group id, grey without a group', () => {
  const placements = parsePackagings(sample)[0].containers[0].stack.placements;
  expect(getColor(ColorMode.GROUP, placements[0], new Map(), () => '#123456')).toBe(NO_VALUE_COLOR);

  const colors = new Map<string, string>();
  (placements[0].stackable as any).groupId = 'g';
  (placements[1].stackable as any).groupId = 'g';
  let next = 0;
  const random = () => ['#111111', '#222222'][next++];
  expect(getColor(ColorMode.GROUP, placements[0], colors, random)).toBe('#111111');
  expect(getColor(ColorMode.GROUP, placements[1], colors, random)).toBe('#111111');
});

test('extraction: a colour per extraction order, grey without one', () => {
  const placements = parsePackagings(sample)[0].containers[0].stack.placements;
  expect(getColor(ColorMode.EXTRACTION, placements[0], new Map(), () => '#000')).toBe(NO_VALUE_COLOR);

  (placements[0].stackable as any).extractionOrder = 1;
  (placements[1].stackable as any).extractionOrder = 2;
  (placements[2].stackable as any).extractionOrder = 1;
  const first = getColor(ColorMode.EXTRACTION, placements[0], new Map(), () => '#000');
  expect(first).toBe(getExtractionColor(1));
  expect(getColor(ColorMode.EXTRACTION, placements[2], new Map(), () => '#000')).toBe(first);
  expect(getColor(ColorMode.EXTRACTION, placements[1], new Map(), () => '#000')).not.toBe(first);
});

test('box item mode keeps the own colour', () => {
  const placements = parsePackagings(sample)[0].containers[0].stack.placements;
  expect(getColor(ColorMode.BOX_ITEM, placements[0], new Map(), () => '#000')).toBeUndefined();
});
