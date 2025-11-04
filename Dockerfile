# 阶段1: 构建前端项目
FROM node:18-alpine AS frontend-builder

# 设置npm镜像源加速下载
RUN npm config set registry https://registry.npmmirror.com

# 设置工作目录
WORKDIR /app/frontend

# 复制package.json和package-lock.json，利用缓存层
COPY frontend/package*.json ./

# 安装依赖，使用缓存
RUN --mount=type=cache,target=/root/.npm \
    npm ci

# 复制所有前端源码
COPY frontend/ .

# 构建前端项目
RUN npm run build

# 阶段2: 构建后端项目
FROM maven:3.9-eclipse-temurin-21 AS backend-builder

# 配置Maven镜像源加速下载（使用阿里云镜像）
COPY <<'EOF' /root/.m2/settings.xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

# 设置工作目录
WORKDIR /app/backend

# 先复制pom.xml，单独下载依赖，利用Docker缓存层
COPY backend/pom.xml ./

# 使用更高效的依赖下载命令
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:resolve dependency:resolve-plugins -B

# 复制所有后端源码
COPY backend/src ./src

# 构建后端项目，使用缓存
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests

# 阶段3: 最终镜像 - 整合前后端
FROM bellsoft/liberica-openjdk-rocky:21.0.7-cds

# 定义时区参数
ENV TZ=Asia/Shanghai
ENV LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8

# 设置时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo '$TZ' > /etc/timezone

# 安装 libvirt 客户端库、SSH 客户端和 Nginx
RUN microdnf install -y \
    libvirt-client \
    libvirt \
    openssh-clients \
    nginx \
    && microdnf clean all

# 定义应用名称和版本
ARG APP_NAME=vm-manager
ARG APP_VERSION=1.0.0

# 从后端构建阶段复制 JAR 文件
COPY --from=backend-builder /app/backend/target/*.jar /app.jar

# 从前端构建阶段复制静态文件到 Nginx 目录
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html

# 配置 Nginx
COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 80;
    server_name localhost;

    # 提供前端静态文件
    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files \$uri \$uri/ /index.html;
    }

    # 代理 API 请求到后端服务
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_buffering off;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# swagger
ENV SWAGGER_ENABLED=true

# libvirt连接URI - 默认值会被.env文件或docker-compose覆盖
ENV LIBVIRT_URI=qemu:///system

# 创建启动脚本 - 同时启动 Nginx 和后端服务
COPY <<'EOF' /run.sh
#!/bin/bash
# 确保环境变量正确加载
export SWAGGER_ENABLED=${SWAGGER_ENABLED:-false}
export LIBVIRT_URI=${LIBVIRT_URI:-qemu:///system}

# 输出环境变量以便调试
echo "Swagger启用: ${SWAGGER_ENABLED}"
echo "Libvirt连接URI: ${LIBVIRT_URI}"

# 启动 Nginx（默认后台运行）
nginx

# 启动后端服务（前台运行，作为容器的主进程）
java -jar /app.jar
EOF
RUN chmod 777 /run.sh

# 安装curl用于健康检查
RUN microdnf install -y curl && microdnf clean all

# 暴露端口（只暴露80端口，Nginx将代理所有请求）
EXPOSE 80

# 添加健康检查 - 分别检测前端index.html和后端actuator端点
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -s -o /dev/null -w "%{http_code}" http://localhost/index.html | grep -q "200\\|304" && \
    (curl -s -o /dev/null -w "%{http_code}" http://localhost/api/actuator/health || \
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health) || exit 1'

# 启动脚本
ENTRYPOINT ["/bin/sh", "/run.sh"]

