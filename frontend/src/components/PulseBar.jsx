import React from 'react'

// Signature element: an ECG-style pulse line stands in for the progress bar.
// A flat line with a single beat blip reads instantly as "vitals" in a health
// context — the waveform itself is the progress indicator, not decoration.
const WAVE = "M0,20 L40,20 L48,20 L54,4 L60,36 L66,20 L120,20 L160,20 L168,20 L174,4 L180,36 L186,20 L240,20 L280,20 L288,20 L294,4 L300,36 L306,20 L360,20"

export default function PulseBar({ percent = 0, status = 'NOT_STARTED', compact = false }) {
  const clamped = Math.max(0, Math.min(100, percent))
  const toneClass =
    status === 'COMPLETED' ? 'pulse--complete' :
    status === 'BLOCKED' ? 'pulse--blocked' :
    status === 'IN_PROGRESS' ? 'pulse--active' : 'pulse--idle'

  return (
    <div className={`pulse ${toneClass} ${compact ? 'pulse--compact' : ''}`}>
      <svg viewBox="0 0 360 40" preserveAspectRatio="none" className="pulse__svg">
        <path d={WAVE} className="pulse__track" />
        <clipPath id={`clip-${Math.round(clamped)}-${status}`}>
          <rect x="0" y="0" width={`${clamped}%`} height="40" />
        </clipPath>
        <path d={WAVE} className="pulse__fill" clipPath={`url(#clip-${Math.round(clamped)}-${status})`} />
      </svg>
      <span className="pulse__label">{clamped}%</span>
    </div>
  )
}
