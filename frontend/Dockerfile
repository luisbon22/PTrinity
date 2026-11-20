# --- Etapa 1: pruebas con cobertura y build de produccion ---
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG OMITIR_PRUEBAS=false
RUN if [ "$OMITIR_PRUEBAS" != "true" ]; then npm run test:coverage; fi && npm run build

# --- Etapa 2: Nginx sirve el front y enruta /api al backend (mismo origen, sin CORS) ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
