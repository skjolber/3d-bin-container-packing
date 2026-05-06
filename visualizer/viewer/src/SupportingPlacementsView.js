import React, { useEffect, useRef } from 'react';

const MAX_DIM = 320;
const CANVAS_PADDING = 18;

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

    // Draw only the direct supporting boxes
    for (const bp of supportingSet) {
        const { placement, stackable } = bp;
        const { cx, cy } = toCanvas(placement.x, placement.y);
        const w = Math.max(1, stackable.dy * scaleX);
        const h = Math.max(1, stackable.dx * scaleY);

        ctx.globalAlpha = 0.85;
        ctx.fillStyle = bp.color;
        ctx.fillRect(cx, cy, w, h);
        ctx.globalAlpha = 1;
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.strokeRect(cx, cy, w, h);

        if (stackable.name || stackable.id) {
            const label = stackable.name || stackable.id;
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 8px monospace';
            ctx.fillText(label, cx + 2, cy + 9);
        }
    }

    // Hovered box drawn on top with its own (emissive-blended) color + bright white border
    if (hoveredSource) {
        const { cx, cy } = toCanvas(hoveredSource.x, hoveredSource.y);
        const w = Math.max(1, hoveredSource.stackable.dy * scaleX);
        const h = Math.max(1, hoveredSource.stackable.dx * scaleY);
        const hoverBp = allBoxPlacements.find(bp => bp.isHovered);
        const hoverColor = hoverBp ? hoverBp.color : '#FFC864';

        ctx.globalAlpha = 0.9;
        ctx.fillStyle = hoverColor;
        ctx.fillRect(cx, cy, w, h);
        ctx.globalAlpha = 1;
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2.5;
        ctx.strokeRect(cx, cy, w, h);

        if (hoveredSource.stackable.name || hoveredSource.stackable.id) {
            const label = hoveredSource.stackable.name || hoveredSource.stackable.id;
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 8px monospace';
            ctx.fillText(label, cx + 2, cy + 9);
        }
    }

    // Axis labels
    ctx.globalAlpha = 1;
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
    const supporting = findSupportingBoxes(source, allBoxPlacements);

    return (
        <div
            style={{
                position: 'fixed',
                bottom: '16px',
                right: '16px',
                width: canvasW + 20,
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
                Supporting Boxes — Top View
            </div>

            {/* 2D canvas — aspect ratio matches container */}
            <canvas
                ref={canvasRef}
                width={canvasW}
                height={canvasH}
                style={{ display: 'block' }}
            />

            {/* Legend */}
            <div style={{ marginTop: '5px', color: '#888', fontSize: '10px' }}>
                <span style={{ color: '#ffffff' }}>□</span> hovered box &nbsp;
                <span style={{ color: '#ffffff', opacity: 0.85 }}>■</span> supporting box
            </div>

            {/* Supporting box list */}
            {supporting.length > 0 && (
                <div
                    style={{
                        marginTop: '8px',
                        maxHeight: '120px',
                        overflowY: 'auto',
                        borderTop: '1px solid #2a3f55',
                        paddingTop: '5px',
                    }}
                >
                    {supporting.map((bp, i) => (
                        <div
                            key={i}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                marginBottom: '3px',
                            }}
                        >
                            <span
                                style={{
                                    display: 'inline-block',
                                    width: '10px',
                                    height: '10px',
                                    background: bp.color,
                                    border: '1px solid #fff',
                                    flexShrink: 0,
                                }}
                            />
                            <span style={{ color: '#cccccc' }}>
                                {bp.stackable.name || bp.stackable.id || '(unnamed)'}
                            </span>
                        </div>
                    ))}
                </div>
            )}
            {supporting.length === 0 && (
                <div style={{ marginTop: '6px', color: '#666', fontSize: '10px' }}>
                    Resting on container floor
                </div>
            )}
        </div>
    );
}

export default SupportingPlacementsView;
