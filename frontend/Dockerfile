# Build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template
# Default for local development
ENV VITE_API_BASE_URL=""
ENV BACKEND_URL=http://backend:8080
ENV PORT=80
EXPOSE 80

# Use a shell script to replace the placeholder in index.html with the environment variable at runtime
CMD ["sh", "-c", "sed -i \"s|__VITE_API_BASE_URL__|${VITE_API_BASE_URL}|g\" /usr/share/nginx/html/index.html && (envsubst '${PORT} ${BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf) && nginx -g 'daemon off;'"]
