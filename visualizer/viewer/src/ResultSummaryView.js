import React from 'react';

const MAX_REASONS = 10;

function percent(value, max) {
    return max > 0 ? Math.round(value * 1000 / max) / 10 + ' %' : '–';
}

/**
 * Panel in the upper-left corner with the packager result (success, timeout, duration, cost)
 * and the volume and weight used per container.
 *
 * Props:
 *   packaging – the parsed Packaging (see model.ts), or null before the first load
 *   colorMode – the current colour mode (see colorModes.ts)
 */
const LEGENDS = {
    'box item': 'a colour per box item',
    'group': 'a colour per group, grey without',
    'support': 'green: fully supported, red: unsupported',
    'load': 'green: no load, red: at or over max load weight, grey: no limit',
};

function ResultSummaryView({ packaging, colorMode }) {
    if (!packaging) return null;

    const { containers, success, timeout, duration, cost, valid, validationReasons } = packaging;

    let loadVolume = 0;
    let maxLoadVolume = 0;
    let boxes = 0;
    for (const c of containers) {
        loadVolume += c.loadVolume;
        maxLoadVolume += c.maxLoadVolume;
        boxes += c.stack.placements.length;
    }

    const rowStyle = { display: 'flex', justifyContent: 'space-between', gap: '12px' };
    const labelStyle = { color: '#aaa' };

    return (
        <div
            style={{
                position: 'absolute',
                zIndex: 2,
                top: 0,
                left: 0,
                padding: '8px',
                minWidth: '220px',
                background: 'rgba(0, 0, 0, 0.5)',
                color: '#fff',
                fontFamily: 'monospace',
                fontSize: '12px',
                textAlign: 'left'
            }}
        >
            {success !== undefined && (
                <div style={{ fontWeight: 'bold', color: success ? '#81c784' : '#ef5350' }}>
                    {success ? 'Packed' : 'Not packed'}{timeout ? ' (timeout)' : ''}
                </div>
            )}
            <div style={{ fontWeight: 'bold', color: valid ? '#81c784' : '#ef5350' }}>
                {valid ? 'Valid' : `Invalid (${validationReasons.length} reason${validationReasons.length === 1 ? '' : 's'})`}
            </div>
            {validationReasons.slice(0, MAX_REASONS).map((reason, i) => (
                <div key={i} style={{ color: '#ef5350', maxWidth: '360px' }}>{reason.type}: {reason.message}</div>
            ))}
            {validationReasons.length > MAX_REASONS && <div style={{ color: '#ef5350' }}>… and {validationReasons.length - MAX_REASONS} more (see the log)</div>}
            <div style={rowStyle}><span style={labelStyle}>Colours (C)</span><span>{colorMode}</span></div>
            <div style={{ color: '#aaa', maxWidth: '360px' }}>{LEGENDS[colorMode]}</div>
            <div style={rowStyle}><span style={labelStyle}>Containers</span><span>{containers.length}</span></div>
            <div style={rowStyle}><span style={labelStyle}>Boxes</span><span>{boxes}</span></div>
            <div style={rowStyle}><span style={labelStyle}>Volume used</span><span>{percent(loadVolume, maxLoadVolume)}</span></div>
            {duration !== undefined && <div style={rowStyle}><span style={labelStyle}>Duration</span><span>{duration} ms</span></div>}
            {cost !== undefined && cost >= 0 && <div style={rowStyle}><span style={labelStyle}>Cost</span><span>{cost}</span></div>}
            {containers.map((c, i) => (
                <div key={i} style={{ marginTop: '6px' }}>
                    <div style={{ color: '#42a5f5', fontWeight: 'bold' }}>{c.id || c.name || 'Container ' + i}</div>
                    <div style={rowStyle}><span style={labelStyle}>Boxes</span><span>{c.stack.placements.length}</span></div>
                    <div style={rowStyle}><span style={labelStyle}>Volume</span><span>{percent(c.loadVolume, c.maxLoadVolume)}</span></div>
                    <div style={rowStyle}><span style={labelStyle}>Weight</span><span>{c.loadWeight} / {c.maxLoadWeight} ({percent(c.loadWeight, c.maxLoadWeight)})</span></div>
                </div>
            ))}
        </div>
    );
}

export default ResultSummaryView;
