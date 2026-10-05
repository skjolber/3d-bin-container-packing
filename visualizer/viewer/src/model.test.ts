import { expect, test } from '@jest/globals';

import { Box, parsePackagings } from './model';

// written by DefaultPackagingResultVisualizerFactoryTest in visualizer/packaging
const sample = require('./fixtures/containers.json');

test('parses all results', () => {
  const packagings = parsePackagings(sample);
  expect(packagings.map(p => p.name)).toEqual(['sample', 'empty']);
  expect(packagings[1].success).toBe(false);
  expect(packagings[1].timeout).toBe(true);
  expect(packagings[1].containers).toHaveLength(0);
});

test('parses the sample written by the Java visualizer', () => {
  const packaging = parsePackagings(sample)[0];

  expect(packaging.containers).toHaveLength(1);
  const container = packaging.containers[0];
  expect(container.id).toBe('container');
  expect(container.name).toBe('sample');
  expect([container.dx, container.dy, container.dz]).toEqual([2, 1, 2]);
  expect([container.loadDx, container.loadDy, container.loadDz]).toEqual([2, 1, 2]);

  const placements = container.stack.placements;
  expect(placements.map(p => p.stackable.id)).toEqual(['A', 'B', 'C']);
  expect(placements.map(p => [p.x, p.y, p.z])).toEqual([[0, 0, 0], [1, 0, 0], [0, 0, 1]]);

  const a = placements[0].stackable as Box;
  expect(a).toBeInstanceOf(Box);
  expect(a.name).toBe('base');
  expect(a.weight).toBe(2);
  expect(a.maxLoadWeight).toBe(1);
  expect((placements[1].stackable as Box).maxLoadBoxCount).toBe(1);
  expect(new Set(placements.map(p => (p.stackable as Box).boxItemKey)).size).toBe(3);

  expect(packaging.success).toBe(true);
  expect(packaging.timeout).toBe(false);
  expect(packaging.duration).toBe(12);
  expect(packaging.cost).toBe(34);
  expect([container.emptyWeight, container.maxLoadWeight, container.loadWeight]).toEqual([0, 100, 9]);
  expect([container.maxLoadVolume, container.loadVolume]).toEqual([4, 4]);

  // A carries half of C (weight 4), more than its max load weight 1
  expect(packaging.valid).toBe(false);
  expect(packaging.validationReasons).toHaveLength(1);
  expect(packaging.validationReasons[0].type).toBe('ExcessiveLoadWeightReason');
  expect(placements[0].reasons).toEqual([packaging.validationReasons[0]]);
  expect(placements[1].reasons).toEqual([]);

  // container and stack, then one step per box
  expect(packaging.minStep).toBe(0);
  expect(packaging.maxStep).toBe(4);
  expect(placements.map(p => p.step)).toEqual([2, 3, 4]);
  expect(placements[0].points.length).toBeGreaterThan(0);
  expect(packaging.maxPointNumbers[2]).toBe(placements[0].points.length);
});
