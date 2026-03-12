import { useState, useEffect } from "react";

// ── Design tokens from Avast ──
const T = {
  bg: "#0b1e24",
  card: "#133040",
  cardAlt: "#0f2a35",
  lime: "#c8e636",
  limeText: "#1a2e0a",
  yellow: "#f5d03a",
  orange: "#f5963a",
  red: "#f06040",
  teal: "#5de4c7",
  blue: "#4a9ede",
  textPrimary: "#e8e8ed",
  textSecondary: "#90a8b0",
  textMuted: "#506068",
  iconBg: "#1a3a48",
  radius: 16,
  radiusPill: 28,
};

// ── Animated ring ──
function Ring({ value, max = 100, size = 180, stroke = 8, color = T.teal, children }) {
  const r = (size - stroke * 2) / 2;
  const circ = 2 * Math.PI * r;
  const [v, setV] = useState(0);
  useEffect(() => { const t = setTimeout(() => setV(value), 150); return () => clearTimeout(t); }, [value]);
  const off = circ - (v / max) * circ;

  return (
    <div style={{ position: "relative", width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={T.iconBg} strokeWidth={stroke} />
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={color} strokeWidth={stroke}
          strokeLinecap="round" strokeDasharray={circ} strokeDashoffset={off}
          style={{ transition: "stroke-dashoffset 1.2s cubic-bezier(0.4,0,0.2,1)" }} />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
        {children}
      </div>
    </div>
  );
}

// ── Mini progress bar ──
function MiniBar({ value, max = 100, color = T.teal, height = 4 }) {
  return (
    <div style={{ width: "100%", height, borderRadius: height/2, background: T.iconBg, overflow: "hidden" }}>
      <div style={{
        width: `${(value/max)*100}%`, height: "100%",
        borderRadius: height/2, background: color,
        transition: "width 1s ease",
      }} />
    </div>
  );
}

// ── Icon wrapper (gray circle like Avast) ──
function IconCircle({ children, size = 44 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: size/2,
      background: T.iconBg,
      display: "flex", alignItems: "center", justifyContent: "center",
      flexShrink: 0,
    }}>{children}</div>
  );
}

// ── Grid card (2x2 feature grid) ──
function GridCard({ icon, title, subtitle, subtitleColor, onClick }) {
  return (
    <button onClick={onClick} style={{
      background: T.card, borderRadius: T.radius, border: "none",
      padding: "20px 12px", display: "flex", flexDirection: "column",
      alignItems: "center", gap: 10, cursor: "pointer",
      transition: "background 0.15s",
    }}
      onMouseEnter={e => e.currentTarget.style.background = "#163848"}
      onMouseLeave={e => e.currentTarget.style.background = T.card}
    >
      <IconCircle>{icon}</IconCircle>
      <span style={{ fontSize: 16, fontWeight: 600, color: T.textPrimary }}>{title}</span>
      {subtitle && (
        <span style={{ fontSize: 13, color: subtitleColor || T.textSecondary, marginTop: -4 }}>{subtitle}</span>
      )}
    </button>
  );
}

// ── List row ──
function ListRow({ icon, label, value, valueColor, chevron = true, onClick }) {
  return (
    <button onClick={onClick} style={{
      width: "100%", background: "none", border: "none",
      display: "flex", alignItems: "center", gap: 14,
      padding: "14px 0", cursor: chevron ? "pointer" : "default",
      borderBottom: `1px solid ${T.iconBg}`,
    }}>
      {icon && <div style={{ width: 20, display: "flex", justifyContent: "center" }}>{icon}</div>}
      <span style={{ flex: 1, textAlign: "left", fontSize: 15, color: T.textPrimary, fontWeight: 400 }}>{label}</span>
      <span style={{ fontSize: 15, fontWeight: 500, color: valueColor || T.textSecondary }}>{value}</span>
      {chevron && (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={T.textMuted} strokeWidth="2" strokeLinecap="round">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      )}
    </button>
  );
}

// ── Section header (uppercase like Avast) ──
function SectionHeader({ children, right }) {
  return (
    <div style={{
      display: "flex", justifyContent: "space-between", alignItems: "center",
      padding: "0 4px", marginBottom: 12,
    }}>
      <span style={{
        fontSize: 12, fontWeight: 600, letterSpacing: "0.08em",
        textTransform: "uppercase", color: T.textMuted,
      }}>{children}</span>
      {right}
    </div>
  );
}

// ── Status dot ──
function Dot({ color }) {
  return <span style={{ display: "inline-block", width: 8, height: 8, borderRadius: 4, background: color, flexShrink: 0 }} />;
}

// ════════════════════════════════════════
// MAIN APP
// ════════════════════════════════════════
export default function DevicePulseHome() {
  const [time, setTime] = useState(new Date());
  useEffect(() => { const i = setInterval(() => setTime(new Date()), 1000); return () => clearInterval(i); }, []);

  const healthScore = 85;
  const batteryPct = 50;

  return (
    <div style={{
      minHeight: "100vh",
      background: T.bg,
      fontFamily: "Roboto, 'SF Pro Display', -apple-system, sans-serif",
      color: T.textPrimary,
      maxWidth: 420,
      margin: "0 auto",
    }}>

      {/* ── Header ── */}
      <div style={{
        padding: "16px 16px 0",
        display: "flex", justifyContent: "space-between", alignItems: "center",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={T.textSecondary} strokeWidth="2" strokeLinecap="round">
            <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
          <span style={{ fontSize: 20, fontWeight: 700, letterSpacing: "-0.02em" }}>DevicePulse</span>
        </div>
        <button style={{
          width: 36, height: 36, borderRadius: 18,
          background: "none", border: `1.5px solid ${T.yellow}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          cursor: "pointer",
        }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.yellow} strokeWidth="2" strokeLinecap="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </button>
      </div>

      {/* ── Hero Card: Health Score ── */}
      <div style={{
        margin: "16px 16px 0",
        background: T.card,
        borderRadius: T.radius,
        padding: "28px 24px",
        display: "flex", flexDirection: "column", alignItems: "center",
      }}>
        <Ring value={healthScore} size={170} stroke={10} color={T.teal}>
          <span style={{ fontSize: 48, fontWeight: 700, letterSpacing: "-0.04em", lineHeight: 1 }}>{healthScore}</span>
          <span style={{ fontSize: 13, fontWeight: 500, color: T.textMuted, marginTop: 4, letterSpacing: "0.05em", textTransform: "uppercase" }}>Health Score</span>
        </Ring>

        <p style={{
          textAlign: "center", margin: "20px 0 0", fontSize: 15,
          color: T.textSecondary, lineHeight: 1.5,
        }}>
          Your device is in <span style={{ color: T.teal, fontWeight: 600 }}>good shape</span>.
          <br />Temperature is slightly elevated.
        </p>

        {/* Status breakdown inside hero card */}
        <div style={{
          width: "100%", marginTop: 20,
          display: "flex", flexDirection: "column", gap: 0,
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 0", borderTop: `1px solid ${T.iconBg}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Dot color={T.blue} />
              <span style={{ fontSize: 14, color: T.textSecondary }}>Battery</span>
            </div>
            <span style={{ fontSize: 14, fontWeight: 500, color: T.textPrimary }}>50% &middot; Charging</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 0", borderTop: `1px solid ${T.iconBg}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Dot color={T.orange} />
              <span style={{ fontSize: 14, color: T.textSecondary }}>Thermal</span>
            </div>
            <span style={{ fontSize: 14, fontWeight: 500, color: T.textPrimary }}>37.2°C &middot; Warm</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 0", borderTop: `1px solid ${T.iconBg}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Dot color={T.teal} />
              <span style={{ fontSize: 14, color: T.textSecondary }}>Network</span>
            </div>
            <span style={{ fontSize: 14, fontWeight: 500, color: T.textPrimary }}>5G &middot; Excellent</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 0", borderTop: `1px solid ${T.iconBg}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Dot color={T.teal} />
              <span style={{ fontSize: 14, color: T.textSecondary }}>Storage</span>
            </div>
            <span style={{ fontSize: 14, fontWeight: 500, color: T.textPrimary }}>202 GB free</span>
          </div>
        </div>
      </div>

      {/* ── Battery Card (prominent, Avast hero+action style) ── */}
      <div style={{
        margin: "8px 16px 0",
        background: T.card,
        borderRadius: T.radius,
        padding: "24px",
      }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase", color: T.textMuted, marginBottom: 8 }}>Battery</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 4 }}>
              <span style={{ fontSize: 44, fontWeight: 700, letterSpacing: "-0.03em", lineHeight: 1 }}>{batteryPct}</span>
              <span style={{ fontSize: 20, fontWeight: 400, color: T.textSecondary }}>%</span>
            </div>
            <div style={{ fontSize: 14, color: T.textSecondary, marginTop: 6 }}>Charging &middot; 18W &middot; ~52 min</div>
          </div>
          <Ring value={batteryPct} size={80} stroke={6} color={T.blue}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={T.blue} strokeWidth="2" strokeLinecap="round">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
            </svg>
          </Ring>
        </div>

        <div style={{ marginTop: 16 }}>
          <MiniBar value={50} color={T.blue} height={6} />
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12, fontSize: 13, color: T.textMuted }}>
          <span>Health: <span style={{ color: T.teal }}>Good</span></span>
          <span>Temp: <span style={{ color: T.orange }}>37.2°C</span></span>
        </div>
      </div>

      {/* ── Feature Grid (2x2 like Avast) ── */}
      <div style={{
        margin: "8px 16px 0",
        display: "grid",
        gridTemplateColumns: "1fr 1fr",
        gap: 8,
      }}>
        <GridCard
          title="Network"
          subtitle="5G &middot; Excellent"
          subtitleColor={T.teal}
          icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#708890" strokeWidth="2" strokeLinecap="round"><path d="M5 12.55a11 11 0 0 1 14.08 0"/><path d="M1.42 9a16 16 0 0 1 21.16 0"/><path d="M8.53 16.11a6 6 0 0 1 6.95 0"/><circle cx="12" cy="20" r="1"/></svg>}
        />
        <GridCard
          title="Thermal"
          subtitle="37.2°C &middot; Warm"
          subtitleColor={T.orange}
          icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#708890" strokeWidth="2" strokeLinecap="round"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>}
        />
        <GridCard
          title="Chargers"
          subtitle="Test & compare"
          icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#708890" strokeWidth="2" strokeLinecap="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>}
        />
        <GridCard
          title="Storage"
          subtitle="202 GB free"
          subtitleColor={T.teal}
          icon={<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#708890" strokeWidth="2" strokeLinecap="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>}
        />
      </div>

      {/* ── Quick Tools row ── */}
      <div style={{ margin: "24px 16px 0" }}>
        <SectionHeader>Quick Tools</SectionHeader>
        <div style={{
          background: T.card,
          borderRadius: T.radius,
          overflow: "hidden",
        }}>
          <ListRow
            label="Speed Test"
            value=""
            icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.textSecondary} strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>}
          />
          <ListRow
            label="System Info"
            value=""
            icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.textSecondary} strokeWidth="2" strokeLinecap="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>}
          />
          <ListRow
            label="App Usage"
            value={<span style={{ fontSize: 10, fontWeight: 600, color: T.yellow, background: `${T.yellow}20`, padding: "2px 8px", borderRadius: 8 }}>PRO</span>}
            valueColor={T.yellow}
            icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.textSecondary} strokeWidth="2" strokeLinecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>}
          />
        </div>
      </div>

      {/* ── Pro Banner (Avast CTA style) ── */}
      <div style={{
        margin: "24px 16px 0",
        background: T.card,
        borderRadius: T.radius,
        padding: "20px 24px",
        display: "flex", alignItems: "center", gap: 16,
        cursor: "pointer",
      }}>
        <div style={{
          width: 44, height: 44, borderRadius: 22,
          background: `${T.lime}18`,
          display: "flex", alignItems: "center", justifyContent: "center",
          flexShrink: 0,
        }}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={T.lime} strokeWidth="2" strokeLinecap="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 15, fontWeight: 600 }}>Unlock DevicePulse Pro</div>
          <div style={{ fontSize: 13, color: T.textSecondary, marginTop: 2 }}>History, charger testing, widgets & more</div>
        </div>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={T.textMuted} strokeWidth="2" strokeLinecap="round">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>

      {/* ── Settings shortcut ── */}
      <div style={{
        margin: "16px 16px 32px",
        display: "flex", justifyContent: "center",
      }}>
        <button style={{
          background: "none", border: "none",
          display: "flex", alignItems: "center", gap: 8,
          cursor: "pointer", padding: "10px 16px",
        }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.textMuted} strokeWidth="2" strokeLinecap="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
          <span style={{ fontSize: 14, fontWeight: 500, color: T.textMuted }}>Settings</span>
        </button>
      </div>
    </div>
  );
}
