import {PlacementModel} from './placement.model';

export class PackModel {
  placement: PlacementModel[];
  weight: number;
  width: number;
  length: number;
  height: number;
  volume: number;
  name: string;

  constructor(placement: PlacementModel[], weight: number, width: number, length: number, height: number, volume: number, name: string) {
    this.placement = placement;
    this.weight = weight;
    this.width = width;
    this.length = length;
    this.height = height;
    this.volume = volume;
    this.name = name;
  }
}
