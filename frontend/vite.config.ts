import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'

const ANTD_DATA_COMPONENTS = new Set([
  'auto-complete',
  'calendar',
  'cascader',
  'checkbox',
  'color-picker',
  'date-picker',
  'form',
  'input',
  'input-number',
  'mentions',
  'radio',
  'rate',
  'select',
  'slider',
  'switch',
  'time-picker',
  'transfer',
  'tree',
  'tree-select',
  'upload',
])

const ANTD_TABLE_COMPONENTS = new Set(['pagination', 'table'])

const ANTD_ACTION_COMPONENTS = new Set(['button'])

const ANTD_LAYOUT_COMPONENTS = new Set([
  'affix',
  'anchor',
  'back-top',
  'breadcrumb',
  'col',
  'flex',
  'float-button',
  'grid',
  'layout',
  'menu',
  'masonry',
  'row',
  'segmented',
  'space',
  'splitter',
  'steps',
  'tabs',
])

const ANTD_OVERLAY_COMPONENTS = new Set([
  'drawer',
  'dropdown',
  'message',
  'modal',
  'notification',
  'popconfirm',
  'popover',
  'tooltip',
  'tour',
])

const ANTD_DISPLAY_COMPONENTS = new Set([
  'alert',
  'avatar',
  'badge',
  'card',
  'carousel',
  'collapse',
  'descriptions',
  'divider',
  'empty',
  'image',
  'list',
  'progress',
  'qr-code',
  'qrcode',
  'result',
  'skeleton',
  'spin',
  'statistic',
  'tag',
  'timeline',
  'typography',
  'watermark',
])

// Repartit Ant Design par usage afin que les routes lazy ne chargent pas tous les composants.
const getAntdChunk = (id: string): string | undefined => {
  const normalizedId = id.replaceAll('\\', '/')
  if (
    normalizedId.includes('/node_modules/@rc-component/') ||
    normalizedId.includes('/node_modules/rc-')
  ) {
    return 'vendor-antd-rc'
  }
  if (
    normalizedId.includes('/node_modules/@ant-design/') ||
    normalizedId.includes('/node_modules/antd/')
  ) {
    const component = normalizedId.match(/\/node_modules\/antd\/es\/([^/]+)/)?.[1]
    if (component && ANTD_TABLE_COMPONENTS.has(component)) {
      return 'vendor-antd-table'
    }
    if (component && ANTD_ACTION_COMPONENTS.has(component)) {
      return 'vendor-antd-actions'
    }
    if (component && ANTD_DATA_COMPONENTS.has(component)) {
      return 'vendor-antd-data'
    }
    if (component && ANTD_OVERLAY_COMPONENTS.has(component)) {
      return 'vendor-antd-overlay'
    }
    if (component && ANTD_DISPLAY_COMPONENTS.has(component)) {
      return 'vendor-antd-display'
    }
    if (component && ANTD_LAYOUT_COMPONENTS.has(component)) {
      return 'vendor-antd-layout'
    }
    return 'vendor-antd-core'
  }
  return undefined
}

// Configure le frontend et charge les params d'env
export default defineConfig(({ mode }) => {
  const envDir = fileURLToPath(new URL('../', import.meta.url))
  const env = { ...loadEnv(mode, envDir, ''), ...process.env }
  const configuredPort = Number(env.DEV_SERVER_PORT ?? 5173)
  const devServerPort = Number.isInteger(configuredPort) && configuredPort > 0
    ? configuredPort
    : 5173
  const allowedHosts = (env.DEV_ALLOWED_HOSTS ?? 'fintrack.finstar.local,fintrack.local')
    .split(',')
    .map((host) => host.trim())
    .filter(Boolean)
  const usePolling = env.VITE_USE_POLLING === 'true'
  const watchInterval = Number(env.VITE_WATCH_INTERVAL ?? 1000)

  return {
    envDir,
    plugins: [react()],
    cacheDir: 'node_modules/.vite',
    define: {
      global: 'window',
    },
    server: {
      host: true,
      port: devServerPort,
      allowedHosts,
      hmr: {
        clientPort: devServerPort,
      },
      proxy: {
        '/api/v1/userService': {
          target: env.VITE_USER_SERVICE_URL,
          changeOrigin: true,
        },
        '/api/v1/incidentService': {
          target: env.VITE_INCIDENT_SERVICE_URL,
          changeOrigin: true,
        },
        '/api/v1/documentService': {
          target: env.VITE_DOCUMENT_SERVICE_URL,
          changeOrigin: true,
        },
        '/api/v1/notificationService': {
          target: env.VITE_NOTIFICATION_SERVICE_URL,
          changeOrigin: true,
        },
        '/api/v1/reportingService': {
          target: env.VITE_REPORTING_SERVICE_URL,
          changeOrigin: true,
        },
        '/api/v1/auditService': {
          target: env.VITE_AUDIT_SERVICE_URL,
          changeOrigin: true,
        },
      },
      watch: usePolling
        ? {
            usePolling: true,
            interval: Number.isFinite(watchInterval) ? watchInterval : 1000,
          }
        : undefined,
    },
    optimizeDeps: {
      include: [
        'react',
        'react-dom/client',
        'react-router-dom',
        '@tanstack/react-query',
        'antd',
        '@ant-design/icons',
        'axios',
        'i18next',
        'react-i18next',
        'zustand',
        'recharts',
      ],
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      minify: 'oxc',
      rollupOptions: {
        output: {
          manualChunks: (id) => {
            if (id.includes('node_modules/react/') || id.includes('node_modules/react-dom/')) {
              return 'vendor-react'
            }

            if (id.includes('react-router') || id.includes('@tanstack/react-query')) {
              return 'vendor-router-query'
            }

            if (id.includes('@ant-design/icons')) {
              return 'vendor-antd-icons'
            }

            if (id.includes('recharts')) {
              return 'vendor-charts'
            }

            if (id.includes('@stomp') || id.includes('sockjs-client')) {
              return 'vendor-realtime'
            }

            if (id.includes('i18next') || id.includes('react-i18next')) {
              return 'vendor-i18n'
            }

            if (id.includes('axios')) {
              return 'vendor-http'
            }

            // Visionneuses de pieces jointes : chargees seulement quand on ouvre un
            // document. Sans regle dediee elles tombent dans vendor-misc, un chunk
            // statiquement atteignable — l'import dynamique ne les differait plus.
            if (
              id.includes('node_modules/xlsx') ||
              id.includes('docx-preview') ||
              id.includes('read-excel-file')
            ) {
              return 'vendor-document-viewers'
            }

            const antdChunk = getAntdChunk(id)
            if (antdChunk) {
              return antdChunk
            }

            if (id.includes('node_modules')) {
              return 'vendor-misc'
            }
          },
        },
      },
    },
  }
})
