FROM node:22 AS build
ARG APP
WORKDIR /src
COPY frontend/ .
WORKDIR /src/${APP}
RUN npm ci && npx vite build

FROM nginx:1.27-alpine
ARG APP
COPY --from=build /src/${APP}/dist /usr/share/nginx/html
COPY deploy/web/nginx.conf.template /etc/nginx/templates/default.conf.template
