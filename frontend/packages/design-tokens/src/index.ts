export const tokens = {
  color: {
    ink: '#f8f7f2',
    muted: '#9ca3af',
    surface: '#12151c',
    surfaceRaised: '#191d26',
    surfaceGlass: 'rgba(22, 26, 34, 0.78)',
    border: 'rgba(255, 255, 255, 0.08)',
    accent: '#f3ba2f',
    accentSoft: '#fef3c7',
    success: '#22c55e',
    danger: '#f97316',
    info: '#38bdf8'
  },
  radius: {
    card: '28px',
    pill: '999px'
  },
  shadow: {
    card: '0 24px 80px rgba(8, 12, 20, 0.48)'
  }
} as const

export type DesignTokens = typeof tokens
