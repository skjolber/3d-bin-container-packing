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
 *   packagings, resultIndex, onSelectResult – all results, the shown one, and a callback to show another
 */
const LEGENDS = {
    'box item': 'a colour per box item',
    'group': 'a colour per group, grey without',
    'support': 'green: fully supported, red: unsupported',
    'load': 'green: no load, red: at or over max load weight, grey: no limit',
};

function totals(packaging) {
    let loadVolume = 0;
    let maxLoadVolume = 0;
    let boxes = 0;
    for (const c of packaging.containers) {
        loadVolume += c.loadVolume;
        maxLoadVolume += c.maxLoadVolume;
        boxes += c.stack.placements.length;
    }
    return { loadVolume, maxLoadVolume, boxes };
}

/**
 * Table comparing the results, when there are several: click a row (or press R) to show it.
 */
function ComparisonTable({ packagings, resultIndex, onSelectResult }) {
    const cell = { padding: '1px 6px', textAlign: 'right' };
    return (
        <table style={{ borderCollapse: 'collapse', marginBottom: '6px' }}>
            <thead>
                <tr style={{ color: '#aaa' }}>
                    <th style={{ ...cell, textAlign: 'left' }}>result (R)</th>
                    <th style={cell}>containers</th>
                    <th style={cell}>boxes</th>
                    <th style={cell}>volume</th>
                    <th style={cell}>ms</th>
                    <th style={cell}>cost</th>
                </tr>
            </thead>
            <tbody>
                {packagings.map((p, i) => {
                    const t = totals(p);
                    const status = p.success === false ? '#ef5350' : (p.valid ? '#81c784' : '#ffb74d');
                    return (
                        <tr key={i} onClick={() => onSelectResult(i)}
                            style={{ cursor: 'pointer', background: i === resultIndex ? 'rgba(66, 165, 245, 0.35)' : 'transparent' }}>
                            <td style={{ ...cell, textAlign: 'left', color: status }}>{p.name || 'result ' + i}</td>
                            <td style={cell}>{p.containers.length}</td>
                            <td style={cell}>{t.boxes}</td>
                            <td style={cell}>{percent(t.loadVolume, t.maxLoadVolume)}</td>
                            <td style={cell}>{p.duration ?? '–'}</td>
                            <td style={cell}>{p.cost !== undefined && p.cost >= 0 ? p.cost : '–'}</td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
}

function ResultSummaryView({ packaging, packagings, resultIndex, colorMode, onSelectResult }) {
    if (!packaging) return null;

    const { containers, success, timeout, duration, cost, valid, validationReasons } = packaging;
    const { loadVolume, maxLoadVolume, boxes } = totals(packaging);

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
            {packagings && packagings.length > 1 && (
                <ComparisonTable packagings={packagings} resultIndex={resultIndex} onSelectResult={onSelectResult} />
            )}
            {packaging.name && <div style={{ color: '#42a5f5', fontWeight: 'bold' }}>{packaging.name}</div>}
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
                    {c.access !== 'ANY' && <div style={rowStyle}><span style={labelStyle}>Access</span><span>{c.access === 'FRONT' ? 'door at x end (orange)' : 'from the top (orange)'}</span></div>}
                    <div style={rowStyle}><span style={labelStyle}>Volume</span><span>{percent(c.loadVolume, c.maxLoadVolume)}</span></div>
                    <div style={rowStyle}><span style={labelStyle}>Weight</span><span>{c.loadWeight} / {c.maxLoadWeight} ({percent(c.loadWeight, c.maxLoadWeight)})</span></div>
                </div>
            ))}
        </div>
    );
}

export default ResultSummaryView;
