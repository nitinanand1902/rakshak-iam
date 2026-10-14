import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // OIDC handshake — Spring Security handles these
      '/login/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // SAML handshake — Spring Security SAML2
      '/saml2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
