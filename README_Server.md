# 一、服务器信息

- **操作系统**：Ubuntu Server（LTS，具体版本未指定）
- **公网 IP**：
- **登录用户**：root
- **部署目录**：`/opt/myapp`
- **防火墙**：UFW（已放行 22 / 80 / 443）

---

# 二、整体架构

```
浏览器
  ↓ HTTP (80)
Nginx (Docker)
  ↓ /api/* 反向代理
Spring Boot 后端 (Docker, 8888, context-path=/job)
  ↓
MySQL 8.0 (Docker)
  ↓
Redis (Docker)
```

- **前后端分离**
- **公网仅暴露 Nginx 80 端口**
- **服务间通过 Docker bridge 网络通信**

---

# 三、Docker / docker-compose

## 1. 使用方式
- 使用 `docker-compose` 统一管理服务
- 常用命令：
  - 启动：`docker-compose up -d`
  - 停止：`docker-compose down`
  - 重建：`docker-compose up -d --build`
  - 重启单服务：`docker-compose restart <service>`

## 2. 运行中的容器

| 服务名 | 镜像 | 端口 | 说明 |
|---|---|---|---|
| nginx | nginx:latest | 80 | 前端静态资源 + 反向代理 |
| backend | myapp_backend | 8888 | Spring Boot 后端 |
| mysql | mysql:8.0 | 3306 | 数据库（仅容器内访问） |
| redis | redis | 6379 | 缓存（仅容器内访问） |

---

# 四、前端信息（Vue）

- **构建方式**：`npm run build`
- **部署位置**：`/opt/myapp/frontend/dist` → 挂载到 Nginx `/usr/share/nginx/html`
- **访问路径**：``
- **API 请求方式**：
  - 使用相对路径：`/api/...`
  - 由 Nginx 转发到后端

---

# 五、Nginx 配置要点

- **静态资源**：
  - `root /usr/share/nginx/html`
  - `try_files $uri $uri/ /index.html`
- **反向代理**：
  - `/api/*` → `http://backend:8888/job/*`
- **MIME 修复**：
  - 已引入 `mime.types`，CSS/JS 正常加载

---

# 六、后端信息（Spring Boot）

- **框架**：Spring Boot 2.3.7.RELEASE
- **Java 版本**：Java 8（`eclipse-temurin:8-jre`）
- **监听端口**：8888
- **Context Path**：`/job`
- **启动方式**：Docker 容器内 `java -jar app.jar`

---

# 七、数据库（MySQL）

- **版本**：MySQL 8.0（Docker 官方镜像）
- **数据库名**：appdb
- **连接方式**：容器内通过服务名 `mysql`
- **JDBC URL 关键参数**：
  - `allowPublicKeyRetrieval=true`
  - `useSSL=false`
  - `serverTimezone=UTC`
- **问题记录**：
  - 曾出现 `Public Key Retrieval is not allowed`
  - 已通过 JDBC URL 参数解决

---

# 八、Redis

- **版本**：redis 官方镜像
- **端口**：6379（仅容器内）
- **用途**：缓存 / 会话等

---

# 九、已解决的关键问题记录

1. **CSS 不生效**
   - 原因：Nginx 未配置 MIME 类型
   - 解决：引入 `mime.types`

2. **前后端 502 Bad Gateway**
   - 原因：
     - 后端端口为 8888，但 Nginx 指向 8080
     - 后端存在 `/job` context-path
   - 解决：修正 Nginx `proxy_pass`

3. **MySQL JDBC 连接失败**
   - 错误：`Public Key Retrieval is not allowed`
   - 解决：JDBC URL 增加 `allowPublicKeyRetrieval=true`

---

# 十、当前系统状态总结

- ✅ 前端页面可正常访问
- ✅ CSS / JS 样式正常
- ✅ 前端 ↔ 后端 API 通信正常
- ✅ MySQL / Redis 连接正常
- ✅ Docker 化部署完成，可随服务器重启恢复

---

# 十一、当前项目性质

- **用途**：个人练手项目
- **目标**：
  - 掌握前后端分离部署
  - Docker / Nginx / 云服务器实战
  - 基础安全防护（未追求企业级）
