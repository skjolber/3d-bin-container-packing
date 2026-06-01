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
    maxLoadWeight?: number;
    maxLoadPressure?: number;
    maxLoadBoxCount?: number;
  };
  color: string;
  isHovered: boolean;
}

export interface LoadInfo {
  /** Total weight pressing down on this box from above, distributed by overlap area. */
  loadWeight: number;
  /** Number of boxes whose bottom face directly rests on this box's top face. */
  directCount: number;
  /** Maximum stack depth above this box (0 = nothing on top). */
  stackDepth: number;
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
    results.set(bp, { loadWeight: 0, directCount: 0, stackDepth: 0 });
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

    if (totalCarried === 0) continue;

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
      }
    }
  }

  return results;
}