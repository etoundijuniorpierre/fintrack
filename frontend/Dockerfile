# Stage 1: Development
FROM node:22-alpine AS development

WORKDIR /app

COPY package*.json ./
RUN npm install --force

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]


# Stage 2: Build pour la Production
FROM node:22-alpine AS builder

WORKDIR /app

# Copier le lock file pour un install déterministe avec toutes les optional deps
COPY package-lock.json ./
COPY package.json ./

# npm ci respecte le lock et installe les optional dependencies (esbuild, rolldown bindings)
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build


# Stage 3: Production avec Nginx
FROM nginx:1.24-alpine AS production

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 -O /dev/null http://127.0.0.1/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
