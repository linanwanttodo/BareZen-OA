# 02-Docker环境编排

## 1. 概述

BareZen_Base 后端依赖 MySQL 8.0 和 Redis 7.0 两个中间件。为了统一开发环境、避免"在我电脑上能跑"的问题，我们使用 Docker Compose 编排这两个服务，实现一键启动。

通过 Docker 启动中间件有以下好处：
- 无需手动安装 MySQL 和 Redis
- 所有开发人员使用相同的版本和配置
- 数据通过 Volume 持久化，重启不会丢失
- 环境隔离，不与宿主机已有服务冲突

---

## 2. 前置要求

确保 Docker 已安装并正常运行：

```bash
# 检查 Docker 版本
docker --version
# 预期输出：Docker version 24.x.x ...

# 检查 Docker 服务是否运行
docker ps
# 如果报错 "Cannot connect to the Docker daemon"，需启动 Docker 服务：
sudo systemctl start docker    # Linux
# macOS/Windows 需启动 Docker Desktop 应用

# 检查 Docker Compose 版本
docker-compose --version
# 或新版本命令
docker compose version
```

---

## 3. docker-compose.yml 文件详解

### 3.1 文件完整内容

文件路径：`backend/docker-compose.yml`

```yaml
# Docker Compose 文件格式版本
# 3.8 是当前广泛支持的稳定版本
version: '3.8'

services:
  # ============================================
  # MySQL 8.0 数据库服务
  # ============================================
  mysql:
    # 使用 MySQL 8.0 官方镜像
    image: mysql:8.0
    # 容器名称，方便通过 docker 命令直接操作
    container_name: zen-mysql
    # 环境变量配置
    environment:
      # root 用户密码（生产环境应使用更强的密码）
      MYSQL_ROOT_PASSWORD: root123456
      # 容器启动时自动创建的数据库名
      MYSQL_DATABASE: barezen_base
      # 时区设置为亚洲/上海
      TZ: Asia/Shanghai
    # 端口映射：宿主机端口:容器端口
    ports:
      - "3306:3306"
    # 数据卷挂载：将容器内数据持久化到宿主机目录
    # 防止容器删除后数据丢失
    volumes:
      - ./data/mysql:/var/lib/mysql
    # 启动命令参数
    command:
      # 使用旧版密码认证方式（兼容旧客户端）
      --default-authentication-plugin=mysql_native_password
      # 设置字符集为 utf8mb4（支持 emoji 等4字节字符）
      --character-set-server=utf8mb4
      # 设置排序规则
      --collation-server=utf8mb4_unicode_ci
    # 加入 zen-network 网络
    networks:
      - zen-network

  # ============================================
  # Redis 7.0 缓存/NoSQL 服务
  # ============================================
  redis:
    # 使用 Redis 7.0 官方镜像
    image: redis:7.0
    container_name: zen-redis
    ports:
      - "6379:6379"
    # 数据持久化目录
    volumes:
      - ./data/redis:/data
    networks:
      - zen-network

# ============================================
# 自定义网络配置
# ============================================
networks:
  # 创建一个名为 zen-network 的桥接网络
  # 同一网络中的服务可以通过容器名称互相访问
  # 例如：Java 应用中可以通过 "zen-mysql" 访问 MySQL
  zen-network:
    driver: bridge
```

### 3.2 逐段说明

#### version
```yaml
version: '3.8'
```
指定 Compose 文件格式的版本。`3.8` 支持大多数 Docker Engine 19.03.0+ 的新特性，包括资源限制、健康检查等。

#### MySQL 服务

```yaml
image: mysql:8.0
```
从 Docker Hub 拉取 MySQL 8.0 官方镜像。首次启动时会自动下载，后续启动直接使用本地缓存。

```yaml
environment:
  MYSQL_ROOT_PASSWORD: root123456
  MYSQL_DATABASE: barezen_base
```
环境变量是 MySQL 镜像约定的配置方式：
- `MYSQL_ROOT_PASSWORD`：**必填**，root 用户密码
- `MYSQL_DATABASE`：可选，容器首次启动时自动创建的数据库
- `TZ`：设置容器时区

```yaml
ports:
  - "3306:3306"
```
端口映射格式为 `宿主机端口:容器端口`。启动后，宿主机通过 `localhost:3306` 即可连接 MySQL。

```yaml
volumes:
  - ./data/mysql:/var/lib/mysql
```
数据卷挂载将容器内的 MySQL 数据目录映射到宿主机的相对路径 `./data/mysql`。这样即使执行 `docker-compose down` 删除容器，数据库数据仍然保留在宿主机上。

```yaml
command:
  --default-authentication-plugin=mysql_native_password
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_unicode_ci
```
MySQL 8.0 默认使用 `caching_sha2_password` 认证插件，某些旧版客户端可能不支持。这里强制使用 `mysql_native_password` 以保证兼容性。后两行设置数据库字符集和排序规则，确保支持中文和表情符号。

#### Redis 服务

```yaml
image: redis:7.0
volumes:
  - ./data/redis:/data
```
Redis 同样通过 Volume 持久化数据。Redis 的数据文件 `dump.rdb` 和 `appendonly.aof` 保存在 `/data` 目录中。

#### 自定义网络

```yaml
networks:
  zen-network:
    driver: bridge
```
创建独立的 Docker 桥接网络，MySQL 和 Redis 都在此网络中。Java 应用如果也加入此网络，可以通过容器名 `zen-mysql` 直接访问数据库，而不需要通过宿主机端口转发，网络性能更好。

---

## 4. 启动与停止

### 4.1 启动中间件环境

```bash
# 进入 backend 目录
cd backend

# 方式一：使用 Make 命令（推荐）
make env-up

# 方式二：直接使用 docker-compose
docker-compose up -d

# 说明：
# up    ：创建并启动所有服务
# -d    ：detached 模式，后台运行，不占用终端
```

首次启动会拉取镜像，可能需要几分钟。后续启动通常在几秒内完成。

**预期输出**：
```
Creating network "backend_zen-network" with driver "bridge"
Creating zen-mysql ... done
Creating zen-redis ... done
```

### 4.2 验证服务状态

```bash
# 查看容器运行状态
docker ps

# 预期输出：
# CONTAINER ID   IMAGE         STATUS         PORTS                    NAMES
# abc123def456   mysql:8.0     Up 2 minutes   0.0.0.0:3306->3306/tcp   zen-mysql
# def456abc123   redis:7.0     Up 2 minutes   0.0.0.0:6379->6379/tcp   zen-redis
```

### 4.3 测试 MySQL 连接

```bash
# 使用 docker exec 在容器内执行命令
docker exec -it zen-mysql mysql -uroot -proot123456

# 进入 MySQL 命令行后：
mysql> SHOW DATABASES;
# 预期输出包含 'barezen_base' 数据库

mysql> USE barezen_base;
mysql> SELECT 1;
# 验证数据库可正常操作

mysql> EXIT;
```

### 4.4 测试 Redis 连接

```bash
# 进入 Redis 容器并启动命令行
docker exec -it zen-redis redis-cli

# 进入 Redis 命令行后：
127.0.0.1:6379> PING
# 预期输出：PONG

127.0.0.1:6379> SET test "hello barebarezen_base"
127.0.0.1:6379> GET test
# 预期输出："hello barebarezen_base"

127.0.0.1:6379> EXIT
```

### 4.5 停止中间件环境

```bash
# 方式一：使用 Make 命令
make env-down

# 方式二：直接使用 docker-compose
docker-compose down

# 说明：
# down  ：停止并移除容器
# 数据库数据保留在 ./data/ 目录，下次启动会自动加载
```

如果希望同时删除数据卷（彻底清理数据），使用：

```bash
docker-compose down -v
# 注意：-v 会删除数据卷，数据库数据将丢失
```

---

## 5. Makefile 详解

BareZen_Base 项目提供 `Makefile` 封装常用命令，减少记忆负担。

文件路径：`backend/Makefile`

```makefile
# .PHONY 声明这些目标不是文件名，而是命令别名
.PHONY: env-up env-down dev-api clean

# 启动数据库和 Redis 环境
# 用法：make env-up
env-up:
	docker-compose up -d

# 停止并移除环境
# 用法：make env-down
env-down:
	docker-compose down

# 启动后端开发服务 (bz_web_app)
# 用法：make dev-api
dev-api:
	./gradlew :bz_web_app:bootRun

# 清理构建产物
# 用法：make clean
clean:
	./gradlew clean
```

关键点说明：
- `.PHONY`：告诉 Make 这些目标不是文件名，避免与同名文件冲突
- 命令前必须使用 Tab 缩进（不能使用空格）
- 使用方式极为简单：`make env-up` 即可

---

## 6. 数据持久化说明

### 6.1 数据存放位置

```
backend/
└── data/
    ├── mysql/       # MySQL 数据文件（表结构、数据行）
    │   ├── ibdata1
    │   ├── barezen_base/   # 数据库目录
    │   └── ...
    └── redis/       # Redis 数据文件
        ├── dump.rdb    # RDB 快照文件
        └── ...
```

`data/` 目录已在 `.gitignore` 中排除，不会被纳入版本控制。

### 6.2 数据备份

直接备份 `data/` 目录即可：

```bash
# 压缩备份
tar -czf data-backup-$(date +%Y%m%d).tar.gz data/

# 恢复时解压到原位置即可
tar -xzf data-backup-20260505.tar.gz
```

### 6.3 重置数据

如果需要将数据库恢复到初始状态：

```bash
# 停止并删除容器和卷
docker-compose down -v

# 删除宿主机数据目录
rm -rf data/mysql data/redis

# 重新启动（会自动创建空数据库）
docker-compose up -d
```

---

## 7. 常见问题排查

| 问题 | 原因 | 解决方案 |
| :--- | :--- | :--- |
| `docker: command not found` | Docker 未安装 | 安装 Docker（`sudo pacman -S docker docker-compose`） |
| `Cannot connect to the Docker daemon` | Docker 服务未启动 | `sudo systemctl start docker` |
| `port is already allocated` | 3306/6379 端口被占用 | 修改 `docker-compose.yml` 中的宿主机端口号，或停止占用进程 |
| MySQL 容器不断重启 | 数据目录权限问题 | `sudo chown -R 999:999 data/mysql/`（999 是 MySQL 容器内用户 ID） |
| 连接被拒绝 `Connection refused` | 容器未完全启动 | 等待约 30 秒让 MySQL 初始化完成后再连接 |
| `Access denied for user` | 密码错误 | 确认密码为 `root123456`，或检查环境变量配置 |
| Redis 数据不持久化 | Redis 默认不保存 | 已在 Volume 映射中处理，数据会自动写入 `/data/dump.rdb` |

---

## 8. 本章完成检查清单

完成本章后，请确认以下操作全部通过：

- [ ] `docker --version` 和 `docker-compose --version` 正常输出版本号
- [ ] `make env-up` 执行成功，`docker ps` 显示 mysql 和 redis 两个容器
- [ ] 能通过 `docker exec -it zen-mysql mysql -uroot -proot123456` 连接 MySQL
- [ ] 能通过 `docker exec -it zen-redis redis-cli PING` 返回 PONG
- [ ] `data/` 目录已自动生成并包含数据文件

---

*下一章：[03-Gradle多模块配置详解](./03-Gradle多模块配置详解.md)*