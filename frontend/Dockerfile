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
# Default for local development. Override this in Railway/Production with:
# http://<service-name>.railway.internal:8080
ENV BACKEND_URL=http://backend:8080
ENV PORT=80
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
