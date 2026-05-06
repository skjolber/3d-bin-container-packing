import React, { useEffect, useRef } from 'react';

const CANVAS_SIZE = 320;
const CANVAS_PADDING = 18;

// Colors cycled per point index
const POINT_COLORS = ['#64FF96', '#FF9664', '#9664FF', '#64FFFF', '#FF6496'];

/**
 * Return all placements whose top face (z + dz) sits flush with the point's
 * bottom face (point.z) and whose x-y footprint overlaps with the point.
 */
function findSupportingPlacements(point, allBoxPlacements) {
    const results = [];
    for (const bp of allBoxPlacements) {
        const { placement, stackable } = bp;

        // Top of this box must equal the bottom of the point (height axis = z)
        if (Math.abs(placement.z + stackable.dz - point.z) > 0.5) continue;

        // X-axis overlap
        if (placement.x + stackable.dx <= point.x || placement.x >= point.x + point.dx) continue;

        // Y-axis overlap
        if (placement.y + stackable.dy <= point.y || placement.y >= point.y + point.dy) continue;

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
 */
function drawTopDown(canvas, container, allBoxPlacements, hoveredSource, currentStep) {
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

    // Use load dimensions for the bounding box, fall back to full dimensions
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

    // All placed boxes (grey, only those visible at currentStep)
    for (const bp of allBoxPlacements) {
        if (bp.isHovered) continue;             // drawn last
        if (bp.stackable.step >= currentStep) continue; // not yet placed

        const { placement, stackable } = bp;
        const { cx, cy } = toCanvas(placement.x, placement.y);
        const w = Math.max(1, stackable.dy * scaleX);
        const h = Math.max(1, stackable.dx * scaleY);

        ctx.globalAlpha = 0.35;
        ctx.fillStyle = '#5080a0';
        ctx.fillRect(cx, cy, w, h);
        ctx.globalAlpha = 1;
        ctx.strokeStyle = '#4a7090';
        ctx.lineWidth = 0.5;
        ctx.strokeRect(cx, cy, w, h);
    }

    // For each point of the hovered placement: supporting placements + point outline
    const points = hoveredSource ? (hoveredSource.points || []) : [];

    for (let i = 0; i < points.length; i++) {
        const point = points[i];
        const color = POINT_COLORS[i % POINT_COLORS.length];
        const supporting = findSupportingPlacements(point, allBoxPlacements);

        // Highlight supporting placements
        for (const bp of supporting) {
            const { placement, stackable } = bp;
            const { cx, cy } = toCanvas(placement.x, placement.y);
            const w = Math.max(1, stackable.dy * scaleX);
            const h = Math.max(1, stackable.dx * scaleY);

            ctx.globalAlpha = 0.5;
            ctx.fillStyle = color;
            ctx.fillRect(cx, cy, w, h);
            ctx.globalAlpha = 1;
            ctx.strokeStyle = color;
            ctx.lineWidth = 1.5;
            ctx.strokeRect(cx, cy, w, h);
        }

        // Point footprint (dashed outline)
        const { cx, cy } = toCanvas(point.x, point.y);
        const pw = Math.max(1, point.dy * scaleX);
        const ph = Math.max(1, point.dx * scaleY);

        ctx.strokeStyle = color;
        ctx.lineWidth = 1.5;
        ctx.setLineDash([4, 3]);
        ctx.strokeRect(cx, cy, pw, ph);
        ctx.setLineDash([]);

        // Point index label
        ctx.fillStyle = color;
        ctx.font = 'bold 9px monospace';
        ctx.fillText(`P${i}`, cx + 2, cy + 10);
    }

    // Hovered box drawn on top (orange-yellow); hoveredSource is a StackPlacement
    if (hoveredSource) {
        const { cx, cy } = toCanvas(hoveredSource.x, hoveredSource.y);
        const w = Math.max(1, hoveredSource.stackable.dy * scaleX);
        const h = Math.max(1, hoveredSource.stackable.dx * scaleY);

        ctx.globalAlpha = 0.65;
        ctx.fillStyle = '#FFC864';
        ctx.fillRect(cx, cy, w, h);
        ctx.globalAlpha = 1;
        ctx.strokeStyle = '#FFC864';
        ctx.lineWidth = 2;
        ctx.strokeRect(cx, cy, w, h);

        if (hoveredSource.stackable.name) {
            ctx.fillStyle = '#FFC864';
            ctx.font = 'bold 9px monospace';
            ctx.fillText(hoveredSource.stackable.name, cx + 2, cy + 10);
        }
    }

    // Axis labels
    ctx.fillStyle = '#888888';
    ctx.font = '9px monospace';
    ctx.fillText('Y →', W - pad - 14, pad - 5);
    ctx.save();
    ctx.translate(pad - 5, H - pad);
    ctx.rotate(-Math.PI / 2);
    ctx.fillText('X →', 0, 0);
    ctx.restore();
}

/**
 * Floating popup window shown when the mouse hovers a box in the 3D scene.
 *
 * Props:
 *   hoveredData  – { source: StackPlacement, container: Container,
 *                    allBoxPlacements: [{placement, stackable, isHovered}],
 *                    currentStep: number }
 *   mouseX       – client X where the popup should appear
 *   mouseY       – client Y where the popup should appear
 */
function SupportingPlacementsView({ hoveredData, mouseX, mouseY }) {
    const canvasRef = useRef(null);

    useEffect(() => {
        if (!canvasRef.current || !hoveredData) return;
        const { source, container, allBoxPlacements, currentStep } = hoveredData;
        if (!source || !container) return;

        drawTopDown(canvasRef.current, container, allBoxPlacements, source, currentStep);
    }, [hoveredData]);

    if (!hoveredData) return null;

    const { source, allBoxPlacements, currentStep } = hoveredData;
    if (!source) return null;

    const points = source.points || [];

    // Pre-compute supporting placements for display below the canvas
    const pointInfo = points.map((p, i) => ({
        point: p,
        index: i,
        supporting: findSupportingPlacements(p, allBoxPlacements),
    }));

    const windowW = CANVAS_SIZE + 20;
    const popupLeft = Math.min(mouseX + 24, window.innerWidth - windowW - 10);
    const popupTop = Math.max(10, Math.min(mouseY - 24, window.innerHeight - 520));

    return (
        <div
            style={{
                position: 'fixed',
                left: popupLeft,
                top: popupTop,
                width: windowW,
                background: 'rgba(18, 26, 36, 0.97)',
                border: '1px solid #42a5f5',
                borderRadius: '6px',
                padding: '8px 10px 10px',
                zIndex: 1000,
                color: '#fff',
                fontFamily: 'monospace',
                fontSize: '11px',
                pointerEvents: 'none',
                boxShadow: '0 4px 24px rgba(0,0,0,0.75)',
            }}
        >
            {/* Window title bar */}
            <div
                style={{
                    color: '#42a5f5',
                    fontWeight: 'bold',
                    marginBottom: '6px',
                    borderBottom: '1px solid #2a3f55',
                    paddingBottom: '4px',
                    fontSize: '12px',
                }}
            >
                Supporting Placements — Top View
            </div>

            {/* 2D canvas */}
            <canvas
                ref={canvasRef}
                width={CANVAS_SIZE}
                height={CANVAS_SIZE}
                style={{ display: 'block' }}
            />

            {/* Legend */}
            <div style={{ marginTop: '5px', color: '#888', fontSize: '10px' }}>
                <span style={{ color: '#FFC864' }}>■</span> hovered box &nbsp;
                <span style={{ color: '#64FF96' }}>■</span> supporting placement &nbsp;
                <span style={{ color: '#5080a0' }}>■</span> other placed boxes
            </div>

            {/* Per-point supporting placement origins */}
            {pointInfo.length > 0 && (
                <div
                    style={{
                        marginTop: '8px',
                        maxHeight: '160px',
                        overflowY: 'auto',
                        borderTop: '1px solid #2a3f55',
                        paddingTop: '5px',
                    }}
                >
                    {pointInfo.map(({ point, index, supporting }) => (
                        <div key={index} style={{ marginBottom: '5px' }}>
                            <span
                                style={{
                                    color: POINT_COLORS[index % POINT_COLORS.length],
                                    fontWeight: 'bold',
                                }}
                            >
                                P{index}
                            </span>{' '}
                            <span style={{ color: '#cccccc' }}>
                                ({point.x}, {point.y}, {point.z})
                            </span>{' '}
                            <span style={{ color: '#888' }}>
                                — {supporting.length} support
                                {supporting.length !== 1 ? 's' : ''}:
                            </span>
                            {supporting.length === 0 && (
                                <span style={{ color: '#666', paddingLeft: '6px' }}>
                                    (container floor)
                                </span>
                            )}
                            {supporting.map((bp, j) => (
                                <div
                                    key={j}
                                    style={{
                                        paddingLeft: '14px',
                                        color: '#aaaacc',
                                        marginTop: '1px',
                                    }}
                                >
                                    origin ({bp.placement.x}, {bp.placement.y},{' '}
                                    {bp.placement.z})
                                    {bp.stackable.name
                                        ? ` "${bp.stackable.name}"`
                                        : ''}
                                </div>
                            ))}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default SupportingPlacementsView;
export { findSupportingPlacements };
