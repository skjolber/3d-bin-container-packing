export class PlacementModel {
  name: string;
  x: number;
  y: number;
  z: number;
  width: number;
  length: number;
  height: number;

  constructor(name: string, x: number, y: number, z: number, width: number, length: number, height: number) {
    this.name = name;
    this.x = x;
    this.y = y;
    this.z = z;
    this.width = width;
    this.length = length;
    this.height = height;
  }
}
