/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // TradeForge brand palette
        brand: {
          50:  '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
          950: '#1e1b4b',
        },
        // Trading colors
        bid:  { DEFAULT: '#10b981', light: '#d1fae5', dark: '#065f46' },
        ask:  { DEFAULT: '#ef4444', light: '#fee2e2', dark: '#7f1d1d' },
        // UI surface colors (dark theme)
        surface: {
          DEFAULT:   '#0f172a',
          secondary: '#1e293b',
          tertiary:  '#334155',
          elevated:  '#1e293b',
          border:    '#334155',
        },
      },
      fontFamily: {
        sans: ['var(--font-inter)', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['var(--font-jetbrains)', 'JetBrains Mono', 'Fira Code', 'monospace'],
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'grid-pattern':
          'linear-gradient(rgba(99,102,241,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(99,102,241,0.04) 1px, transparent 1px)',
      },
      backgroundSize: {
        'grid': '40px 40px',
      },
      animation: {
        'fade-in':    'fadeIn 0.3s ease-in-out',
        'slide-up':   'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.2s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'glow':       'glow 2s ease-in-out infinite alternate',
        'ticker':     'ticker 30s linear infinite',
      },
      keyframes: {
        fadeIn:    { '0%': { opacity: '0' },                              '100%': { opacity: '1' } },
        slideUp:   { '0%': { opacity: '0', transform: 'translateY(10px)' }, '100%': { opacity: '1', transform: 'translateY(0)' } },
        slideDown: { '0%': { opacity: '0', transform: 'translateY(-8px)' }, '100%': { opacity: '1', transform: 'translateY(0)' } },
        glow:      { '0%': { boxShadow: '0 0 5px rgba(99,102,241,0.3)' },   '100%': { boxShadow: '0 0 20px rgba(99,102,241,0.6)' } },
        ticker:    { '0%': { transform: 'translateX(100%)' },               '100%': { transform: 'translateX(-100%)' } },
      },
      boxShadow: {
        'glow-brand': '0 0 20px rgba(99, 102, 241, 0.4)',
        'glow-bid':   '0 0 15px rgba(16, 185, 129, 0.3)',
        'glow-ask':   '0 0 15px rgba(239, 68, 68, 0.3)',
        'card':       '0 4px 6px -1px rgba(0,0,0,0.3), 0 2px 4px -2px rgba(0,0,0,0.2)',
      },
    },
  },
  plugins: [],
};
