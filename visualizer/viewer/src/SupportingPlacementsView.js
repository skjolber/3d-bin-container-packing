import React, { useEffect, useRef } from 'react';

const MAX_DIM = 640;
const CANVAS_PADDING = 24;

/**
 * Compute canvas pixel dimensions that preserve the container's X-Y aspect ratio
 * while fitting within MAX_DIM.
 *
 * Container Y dimension → canvas width axis
 * Container X dimension → canvas height axis
 */
function canvasDimensions(container) {
    const totalY = (container.loadDy > 0 ? container.loadDy : container.dy) || 1;
    const totalX = (container.loadDx > 0 ? container.loadDx : container.dx) || 1;
    const aspect = totalY / totalX; // width-to-height
    const W = aspect >= 1 ? MAX_DIM : Math.round(MAX_DIM * aspect);
    const H = aspect >= 1 ? Math.round(MAX_DIM / aspect) : MAX_DIM;
    return { W, H, totalX, totalY };
}

/**
 * Return all placements from allBoxPlacements whose top face (placement.z + stackable.dz)
 * is flush with the hovered box's bottom face (hoveredZ) and whose X-Y footprint
 * overlaps with the hovered box's footprint.
 */
function findSupportingBoxes(hoveredSource, allBoxPlacements) {
    const hoveredZ = hoveredSource.z;
    const hoveredX = hoveredSource.x;
    const hoveredY = hoveredSource.y;
    const hoveredDx = hoveredSource.stackable.dx;
    const hoveredDy = hoveredSource.stackable.dy;
    const results = [];
    for (const bp of allBoxPlacements) {
        if (bp.isHovered) continue;
        const { placement, stackable } = bp;
        // Top face must touch the hovered box's bottom
        if (Math.abs(placement.z + stackable.dz - hoveredZ) > 0.5) continue;
        // X-axis footprint overlap
        if (placement.x + stackable.dx <= hoveredX || placement.x >= hoveredX + hoveredDx) continue;
        // Y-axis footprint overlap
        if (placement.y + stackable.dy <= hoveredY || placement.y >= hoveredY + hoveredDy) continue;
        results.push(bp);
    }
    return results;
}

/**
 * Draw a 2D top-down (looking down the algorithm Z / height axis) view onto a
 * canvas element.
 *
 * Canvas X-axis  = algorithm Y axis  (container width)
 * Canvas Y-axis  = algorithm X axis  (container depth)
 *
 * Only the hovered box and the boxes directly supporting it (one level down,
 * touching in the XY plane) are drawn. All other boxes are omitted.
 *
 * Each entry in allBoxPlacements has { placement, stackable, color, isHovered }.
 * color is the sRGB hex string matching the 3D view.
 */
/**
 * Compute a font size (px) so that the rendered label text fills roughly
 * `targetFraction` of the given pixel width, while also staying within
 * 50% of the pixel height.  Clamps between minSize and maxSize.
 */
function fitFontSize(ctx, label, pixelWidth, pixelHeight, targetFraction, minSize, maxSize) {
    ctx.font = `bold ${maxSize}px monospace`;
    const measured = ctx.measureText(label).width;
    if (measured <= 0) return minSize;
    const sizeByWidth = Math.floor(maxSize * (pixelWidth * targetFraction) / measured);
    // Cap at 50 % of the available height (font size ≈ cap height)
    const sizeByHeight = Math.floor(pixelHeight * 0.5);
    const size = Math.min(sizeByWidth, sizeByHeight);
    return Math.max(minSize, Math.min(maxSize, size));
}

/**
 * Given a box rectangle and an overlapping rectangle (both in canvas px coords),
 * return the largest axis-aligned sub-rectangle of the box that lies fully
 * outside the overlap.  Returns the full box rect when there is no overlap.
 */
function visibleSubRect(box, overlap) {
    const bx2 = box.cx + box.w;
    const by2 = box.cy + box.h;
    const ix1 = Math.max(box.cx, overlap.cx);
    const iy1 = Math.max(box.cy, overlap.cy);
    const ix2 = Math.min(bx2, overlap.cx + overlap.w);
    const iy2 = Math.min(by2, overlap.cy + overlap.h);

    if (ix2 <= ix1 || iy2 <= iy1) {
        // No real overlap – use the whole box
        return { cx: box.cx, cy: box.cy, w: box.w, h: box.h };
    }

    // Up to four axis-aligned strips remain visible; pick the largest by area.
    const candidates = [
        { cx: box.cx, cy: box.cy,  w: box.w,      h: iy1 - box.cy }, // top
        { cx: box.cx, cy: iy2,     w: box.w,      h: by2 - iy2    }, // bottom
        { cx: box.cx, cy: box.cy,  w: ix1 - box.cx, h: box.h      }, // left
        { cx: ix2,    cy: box.cy,  w: bx2 - ix2,    h: box.h      }, // right
    ].filter(s => s.w > 0 && s.h > 0);

    if (candidates.length === 0) {
        // Fully covered – fall back to original rect (label will be tiny)
        return { cx: box.cx, cy: box.cy, w: box.w, h: box.h };
    }

    return candidates.reduce((best, s) => (s.w * s.h > best.w * best.h ? s : best));
}

/**
 * Draw a label centered inside the given box rectangle with a dark outline so
 * it is always legible against any fill color.
 */
function drawLabel(ctx, label, cx, cy, w, h) {
    const fontSize = fitFontSize(ctx, label, w, h, 0.5, 8, 128);
    ctx.font = `bold ${fontSize}px monospace`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const tx = cx + w / 2;
    const ty = cy + h / 2;
    // Dark outline for contrast against any background
    ctx.globalAlpha = 1;
    ctx.strokeStyle = 'rgba(0,0,0,0.85)';
    ctx.lineWidth = Math.max(2, fontSize * 0.18);
    ctx.lineJoin = 'round';
    ctx.strokeText(label, tx, ty);
    ctx.fillStyle = '#ffffff';
    ctx.fillText(label, tx, ty);
    // Reset to safe defaults
    ctx.textAlign = 'start';
    ctx.textBaseline = 'alphabetic';
}

function drawTopDown(canvas, container, allBoxPlacements, hoveredSource) {
    const ctx = canvas.getContext('2d');
    const W = canvas.width;
    const H = canvas.height;
    ctx.clearRect(0, 0, W, H);

    // Background
    ctx.fillStyle = '#1a2632';
    ctx.fillRect(0, 0, W, H);

    const pad = CANVAS_PADDING;
    const drawW = W - 2 * pad;
    const drawH = H - 2 * pad;

    // Container dimensions (same logic as canvasDimensions, but read from canvas size)
    const totalY = (container.loadDy > 0 ? container.loadDy : container.dy) || 1;
    const totalX = (container.loadDx > 0 ? container.loadDx : container.dx) || 1;

    // algorithm Y → canvas X,  algorithm X → canvas Y
    const scaleX = drawW / totalY;
    const scaleY = drawH / totalX;

    const toCanvas = (algX, algY) => ({
        cx: pad + algY * scaleX,
        cy: pad + algX * scaleY,
    });

    // Container outline
    ctx.strokeStyle = '#42a5f5';
    ctx.lineWidth = 1;
    ctx.strokeRect(pad, pad, totalY * scaleX, totalX * scaleY);

    const supporting = hoveredSource ? findSupportingBoxes(hoveredSource, allBoxPlacements) : [];
    const supportingSet = new Set(supporting);

    // Collect box geometry for the two-pass render (fills → labels)
    const boxes = [];

    for (const bp of supportingSet) {
        const { placement, stackable } = bp;
        const { cx, cy } = toCanvas(placement.x, placement.y);
        const w = Math.max(1, stackable.dy * scaleX);
        const h = Math.max(1, stackable.dx * scaleY);
        boxes.push({ cx, cy, w, h, color: bp.color, lineWidth: 2, label: stackable.name || stackable.id || null, alpha: 0.85, isHovered: false });
    }

    let hoveredRect = null;
    if (hoveredSource) {
        const { cx, cy } = toCanvas(hoveredSource.x, hoveredSource.y);
        const w = Math.max(1, hoveredSource.stackable.dy * scaleX);
        const h = Math.max(1, hoveredSource.stackable.dx * scaleY);
        const hoverBp = allBoxPlacements.find(bp => bp.isHovered);
        const hoverColor = hoverBp ? hoverBp.color : '#FFC864';
        hoveredRect = { cx, cy, w, h };
        boxes.push({ cx, cy, w, h, color: hoverColor, lineWidth: 2.5, label: hoveredSource.stackable.name || hoveredSource.stackable.id || null, alpha: 0.9, isHovered: true });
    }

    // Pass 1 – fills and borders
    for (const b of boxes) {
        ctx.globalAlpha = b.alpha;
        ctx.fillStyle = b.color;
        ctx.fillRect(b.cx, b.cy, b.w, b.h);
        ctx.globalAlpha = 1;
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = b.lineWidth;
        ctx.strokeRect(b.cx, b.cy, b.w, b.h);
    }

    // Pass 2 – labels drawn last so they are never obscured by another box's fill.
    // For supporting (non-hovered) boxes that are partially overlaid by the hovered
    // box, size and position the label within the largest visible sub-rectangle.
    for (const b of boxes) {
        if (!b.label) continue;
        let lr = { cx: b.cx, cy: b.cy, w: b.w, h: b.h };
        if (!b.isHovered && hoveredRect) {
            lr = visibleSubRect({ cx: b.cx, cy: b.cy, w: b.w, h: b.h }, hoveredRect);
        }
        drawLabel(ctx, b.label, lr.cx, lr.cy, lr.w, lr.h);
    }

    // Axis labels
    ctx.globalAlpha = 1;
    ctx.fillStyle = '#888888';
    ctx.font = '14px monospace';
    ctx.fillText('Y →', W - pad - 22, pad - 6);
    ctx.save();
    ctx.translate(pad - 6, H - pad);
    ctx.rotate(-Math.PI / 2);
    ctx.fillText('X →', 0, 0);
    ctx.restore();
}

/**
 * Floating popup fixed to the lower-right corner showing which boxes directly
 * support the currently hovered box (i.e. boxes whose top face touches the
 * hovered box's bottom face and whose footprint overlaps).
 *
 * The canvas aspect ratio matches the container's X-Y footprint.
 *
 * Props:
 *   hoveredData  – { source: StackPlacement, container: Container,
 *                    allBoxPlacements: [{placement, stackable, color, isHovered}],
 *                    currentStep: number }
 */
function SupportingPlacementsView({ hoveredData }) {
    const canvasRef = useRef(null);

    useEffect(() => {
        if (!canvasRef.current || !hoveredData) return;
        const { source, container, allBoxPlacements } = hoveredData;
        if (!source || !container) return;

        drawTopDown(canvasRef.current, container, allBoxPlacements, source);
    }, [hoveredData]);

    if (!hoveredData) return null;

    const { source, container, allBoxPlacements } = hoveredData;
    if (!source || !container) return null;

    const { W: canvasW, H: canvasH } = canvasDimensions(container);

    return (
        <div
            style={{
                position: 'fixed',
                bottom: '16px',
                right: '16px',
                width: canvasW + 8,
                background: 'rgba(18, 26, 36, 0.97)',
                border: '1px solid #42a5f5',
                borderRadius: '6px',
                padding: '4px',
                zIndex: 1000,
                pointerEvents: 'none',
                boxShadow: '0 4px 24px rgba(0,0,0,0.75)',
            }}
        >
            {/* 2D canvas — aspect ratio matches container */}
            <canvas
                ref={canvasRef}
                width={canvasW}
                height={canvasH}
                style={{ display: 'block' }}
            />
        </div>
    );
}

export default SupportingPlacementsView;
