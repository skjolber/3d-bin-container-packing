export async function http(
    request: RequestInfo
  ): Promise<any> {
    const response = await fetch(request);
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }
    const contentType = response.headers.get("content-type");
    if (!contentType || !contentType.includes("application/json")) {
      throw new Error(`Expected JSON but received: ${contentType}`);
    }
    const body = await response.json();
    return body;
  }

export interface BoxPlacementEntry {
  placement: { x: number; y: number; z: number };
  stackable: {
    dx: number; dy: number; dz: number;
    weight?: number;
    name?: string; id?: string;
    boxItemKey?: number;
    maxLoadWeight?: number;
    maxLoadPressure?: number;
    maxLoadBoxCount?: number;
    maxLoadIdenticalOnly?: boolean;
  };
  color: string;
  isHovered: boolean;
}

export interface LoadInfo {
  /** Total weight pressing down on this box from above, distributed by overlap area. */
  loadWeight: number;
  /** Load weight divided by the box's top area. */
  loadPressure: number;
  /** Number of boxes whose bottom face directly rests on this box's top face. */
  directCount: number;
  /** Maximum stack depth above this box (0 = nothing on top). */
  stackDepth: number;
  /** Unique boxes anywhere in the supportee graph above this box. */
  loadBoxCount: number;
  /** Loaded boxes belonging to the same BoxItem as this box. */
  identicalLoadBoxCount: number;
  /** Loaded boxes belonging to a different BoxItem. */
  nonIdenticalLoadBoxCount: number;
  /** Internal placement set used while propagating load information. */
  loadedBoxes: Set<BoxPlacementEntry>;
}

/**
 * Compare box-item identity from the visualizer JSON. New payloads carry an
 * exact per-result key; the remaining properties keep older payloads useful.
 */
export function isSameBoxItem(first: BoxPlacementEntry, second: BoxPlacementEntry): boolean {
  const firstBox = first.stackable;
  const secondBox = second.stackable;
  if (firstBox.boxItemKey != null && secondBox.boxItemKey != null) {
    return firstBox.boxItemKey === secondBox.boxItemKey;
  }
  if (firstBox.id != null && secondBox.id != null) {
    return firstBox.id === secondBox.id;
  }
  return firstBox.name === secondBox.name
    && firstBox.dx === secondBox.dx
    && firstBox.dy === secondBox.dy
    && firstBox.dz === secondBox.dz
    && firstBox.weight === secondBox.weight;
}

/**
 * Compute load metrics for every placement in the list.
 *
 * Algorithm: process placements top-to-bottom. For each box, distribute its
 * total carried weight (own weight + load from above) proportionally to every
 * supporter below it based on the 2-D footprint overlap area.
 */
export function computeLoads(placements: BoxPlacementEntry[]): Map<BoxPlacementEntry, LoadInfo> {
  const results = new Map<BoxPlacementEntry, LoadInfo>();
  for (const bp of placements) {
    results.set(bp, {
      loadWeight: 0,
      loadPressure: 0,
      directCount: 0,
      stackDepth: 0,
      loadBoxCount: 0,
      identicalLoadBoxCount: 0,
      nonIdenticalLoadBoxCount: 0,
      loadedBoxes: new Set<BoxPlacementEntry>()
    });
  }

  // Sort top-to-bottom: highest top-face first
  const sorted = [...placements].sort(
    (a, b) => (b.placement.z + b.stackable.dz) - (a.placement.z + a.stackable.dz)
  );

  function overlapArea(
    x1: number, y1: number, dx1: number, dy1: number,
    x2: number, y2: number, dx2: number, dy2: number
  ): number {
    const ox1 = Math.max(x1, x2);
    const ox2 = Math.min(x1 + dx1, x2 + dx2);
    const oy1 = Math.max(y1, y2);
    const oy2 = Math.min(y1 + dy1, y2 + dy2);
    return (ox2 > ox1 && oy2 > oy1) ? (ox2 - ox1) * (oy2 - oy1) : 0;
  }

  for (const topBp of sorted) {
    const { placement: tp, stackable: ts } = topBp;
    const info = results.get(topBp)!;
    const totalCarried = (ts.weight || 0) + info.loadWeight;
    const depth = info.stackDepth + 1; // levels including this box

    // Do not early-return on totalCarried === 0; directCount/stackDepth still matter.
    // Find all supporters: boxes whose top face touches this box's bottom face
    const supporters: Array<{ bp: BoxPlacementEntry; area: number }> = [];
    let totalArea = 0;

    for (const bp of placements) {
      if (bp === topBp) continue;
      const { placement: sp, stackable: ss } = bp;
      if (sp.z + ss.dz !== tp.z) continue;
      const area = overlapArea(tp.x, tp.y, ts.dx, ts.dy, sp.x, sp.y, ss.dx, ss.dy);
      if (area > 0) {
        supporters.push({ bp, area });
        totalArea += area;
      }
    }

    if (totalArea > 0) {
      for (const { bp: supBp, area } of supporters) {
        const supInfo = results.get(supBp)!;
        supInfo.loadWeight += (totalCarried * area) / totalArea;
        supInfo.directCount += 1;
        supInfo.stackDepth = Math.max(supInfo.stackDepth, depth);
        supInfo.loadedBoxes.add(topBp);
        info.loadedBoxes.forEach(loadedBox => {
          supInfo.loadedBoxes.add(loadedBox);
        });
      }
    }
  }

  for (const bp of placements) {
    const info = results.get(bp)!;
    const topArea = bp.stackable.dx * bp.stackable.dy;
    info.loadPressure = topArea > 0 ? info.loadWeight / topArea : 0;
    info.loadBoxCount = info.loadedBoxes.size;
    info.loadedBoxes.forEach(loadedBox => {
      if (isSameBoxItem(bp, loadedBox)) {
        info.identicalLoadBoxCount++;
      } else {
        info.nonIdenticalLoadBoxCount++;
      }
    });
  }

  return results;
}
