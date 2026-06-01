/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // shadcn 兼容层
        border: 'var(--border)',
        input: 'var(--input)',
        ring: 'var(--ring)',
        background: 'var(--background)',
        foreground: 'var(--foreground)',
        primary: {
          DEFAULT: 'var(--primary)',
          foreground: 'var(--primary-foreground)',
        },
        secondary: {
          DEFAULT: 'var(--secondary)',
          foreground: 'var(--secondary-foreground)',
        },
        destructive: {
          DEFAULT: 'var(--destructive)',
          foreground: 'var(--destructive-foreground)',
        },
        muted: {
          DEFAULT: 'var(--muted)',
          foreground: 'var(--muted-foreground)',
        },
        accent: {
          DEFAULT: 'var(--accent)',
          foreground: 'var(--accent-foreground)',
        },
        card: {
          DEFAULT: 'var(--card)',
          foreground: 'var(--card-foreground)',
        },
        // 企业微信风格扩展
        wecom: {
          primary: 'var(--wecom-primary)',
          'primary-hover': 'var(--wecom-primary-hover)',
          'primary-active': 'var(--wecom-primary-active)',
          'primary-light': 'var(--wecom-primary-light)',
          success: 'var(--wecom-success)',
          'success-light': 'var(--wecom-success-light)',
          warning: 'var(--wecom-warning)',
          'warning-light': 'var(--wecom-warning-light)',
          error: 'var(--wecom-error)',
          'error-light': 'var(--wecom-error-light)',
          info: 'var(--wecom-info)',
          'info-light': 'var(--wecom-info-light)',
          text: {
            primary: 'var(--wecom-text-primary)',
            secondary: 'var(--wecom-text-secondary)',
            tertiary: 'var(--wecom-text-tertiary)',
          },
          bg: {
            page: 'var(--wecom-bg-page)',
            card: 'var(--wecom-bg-card)',
            hover: 'var(--wecom-bg-hover)',
          },
          border: 'var(--wecom-border)',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"PingFang SC"', '"Microsoft YaHei"', '"Helvetica Neue"', 'Arial', 'sans-serif'],
      },
      spacing: {
        // 企业微信间距系统
        'wecom-xs': 'var(--wecom-spacing-xs)',
        'wecom-sm': 'var(--wecom-spacing-sm)',
        'wecom-md': 'var(--wecom-spacing-md)',
        'wecom-lg': 'var(--wecom-spacing-lg)',
        'wecom-xl': 'var(--wecom-spacing-xl)',
        'wecom-xxl': 'var(--wecom-spacing-xxl)',
      },
      borderRadius: {
        // 企业微信圆角系统
        'wecom-sm': 'var(--wecom-radius-sm)',
        'wecom-md': 'var(--wecom-radius-md)',
        'wecom-lg': 'var(--wecom-radius-lg)',
        'wecom-xl': 'var(--wecom-radius-xl)',
      },
      boxShadow: {
        // 企业微信阴影系统
        'wecom-sm': 'var(--wecom-shadow-sm)',
        'wecom-md': 'var(--wecom-shadow-md)',
        'wecom-lg': 'var(--wecom-shadow-lg)',
        'wecom-xl': 'var(--wecom-shadow-xl)',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.25s ease-out forwards',
      },
      transitionDuration: {
        'wecom-fast': 'var(--wecom-transition-fast)',
        'wecom-base': 'var(--wecom-transition-base)',
        'wecom-slow': 'var(--wecom-transition-slow)',
      },
    },
  },
  plugins: [],
  corePlugins: {
    preflight: false,
  },
}
