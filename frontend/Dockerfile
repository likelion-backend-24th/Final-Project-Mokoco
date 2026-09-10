# 1단계: 빌드 스테이지
FROM node:20-alpine AS builder
WORKDIR /app

# 빌드 시점에 환경 변수를 전달받기 위한 선언
ARG NEXT_PUBLIC_BACKEND_API_URL
ENV NEXT_PUBLIC_BACKEND_API_URL=$NEXT_PUBLIC_BACKEND_API_URL

COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2단계: 실행 스테이지
FROM node:18-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]