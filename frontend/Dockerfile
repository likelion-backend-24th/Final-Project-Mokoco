# 실행 스테이지 (EC2에서는 빌드하지 않고 이미 완성된 결과물만 실행)
FROM node:18-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

# GitHub Actions가 미리 빌드해 둔 결과물들을 EC2로 복사해서 그대로 사용
COPY public ./public
COPY .next/standalone ./
COPY .next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]