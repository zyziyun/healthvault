# HealthVault

拍照上传医院资料，AI 自动提取、分类、归档，并支持语义检索与问答。

## 架构

后端按域拆成两个服务，客户端分别直连，谁都不包谁。

- `apps/platform-api`  Java Spring Boot。登录注册、账号、档案管理、上传。事务型、要稳定的部分。
- `apps/ai-api`  Python FastAPI。提取、分类、embedding、RAG。AI 生态在 Python。
- `worker`  Celery worker，异步跑图片处理。
- `apps/web`  React 前端，调用两个服务。
- 两个服务不互相 RPC 包装。共享数据走 S3 加 Postgres，AI 侧通过 Redis 队列收到有新文件要处理的消息后自己去取。

## 本地启动

前置：Docker、JDK 21、Python 3.11。

```bash
cp .env.example .env          # 填好 .env，它不进 git
make up                       # 起 postgres+pgvector 和 redis
make platform                 # 另开一个终端，跑 Spring Boot，8080
make ai                       # 再开一个终端，跑 FastAPI，8000
```

验证：

- http://localhost:8080/           platform-api 返回 hello
- http://localhost:8080/actuator/health   健康检查
- http://localhost:8000/           ai-api 返回 hello
- http://localhost:8000/docs       FastAPI 自动生成的接口文档

## 测试

```bash
make test
```

## 目录

```
apps/platform-api   Java Spring Boot，Gradle
apps/ai-api         Python FastAPI，src layout + pyproject
apps/web            React 前端，占位
packages/core       两个服务共享的数据契约
worker              Celery worker，占位
infra               docker-compose、Dockerfile
.github/workflows   CI，两种语言分别跑
```
