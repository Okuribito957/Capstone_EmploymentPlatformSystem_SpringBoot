# 备忘
- 项目需要启动redis
- job-web工程的npm依赖于taobao镜像，国外环境需要删除`package-lock.json`文件后重新`npm install`加载依赖

# 命令
- 停止所有服务
```
docker-compose down
```
- 启动所有服务
```
docker-compose up -d
```
- 启动并重新构建
```
docker-compose up -d --build
```
- 重启某个服务
```
docker-compose restart backend
```
- 后端日志
```
docker-compose logs -f backend
```
- 本地后台打包
```
mvn clean package -DskipTests
```
- 本地前台打包
```
npm run build
```
# 目录结构
```
/opt/myapp/
│   
│   
│
└───backend
│   │   app.jar
│   │   Dockerfile
│   │
│   └───config
│       │   application.yml
│   
└───frontend
│   │   dist
│
│
│
└───mysql
│
└───nginx
    │   nginx.conf
```
