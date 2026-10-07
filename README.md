# 医院智能排班系统（GBSched）

为医院科室提供规则引擎排班、冲突检测、调班审批、班次统计和节假日策略管理的一体化系统。

## 快速启动（Docker Compose）

```bash
cp .env.example .env
docker compose up -d
docker compose ps
```

访问地址：

- 前端：http://localhost:18931
- 后端健康检查：http://localhost:19931/actuator/health
- API 示例：http://localhost:18931/api/dashboard

停止服务：

```bash
docker compose down
```

## 项目主要功能

- 科室与岗位管理：维护科室、岗位、编制人数和技能标签。
- 排班规则引擎：支持连续工作上限、周末轮循、节假日优先级、夜班后禁接白班。
- 可视化排班表：使用「人员 × 日期」矩阵展示白班、中班、夜班和休息。
  - **护士长可直接点击排班表上的任意格子，把某人的班次换成白班 / 中班 / 夜班 / 休息**，可填写调整原因。
  - 换班后自动重看该人前后几天的两类规则并实时刷新整张表：
    - **夜班后不能接白班**（前一天夜班、当天白班，日期相邻才判定）；
    - **连续上班超过 5 天**标出，休息打断连续且休息那天不算连续天数。
  - 冲突格子用描边标出（橙色=夜班接白班，红色=连续超 5 天），冲突提示、工时统计都跟随换过的结果重算。
  - 手动改过的格子带「改」标记并写入**手动调整记录**（谁、何时、原班次→新班次、原因），留痕可追溯。
- **一键生成策略（照此条执行）**：引擎重新铺原始基线后，按「人 × 日期」把每条调整记录套回——**保住手动改过的格子，重铺只盖没动过的人/格子**。因此一键生成后，同一天同一个人的班次必然与调整记录一致，不会出现班次和调整记录对不上的情况；未调整的格子则反映引擎最新结果。
- 调班与替班申请：展示申请人、替班人、日期、原因和审批状态。
- 出勤与工时统计：依据当前排班实时统计白班/中班/夜班数量、出勤天数、休息天数和加班小时（夜班按 4 小时折算）。
- 特殊日期策略：特殊日期自动标红提醒。

相关接口：`GET /api/dashboard`、`POST /api/schedule/adjust`（换班）、`POST /api/schedule/regenerate`（一键生成）。

## 本地开发方式

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus |
| 后端 | Spring Boot 3、Java 17、Actuator |
| 数据库 | MySQL 8.0 |
| 部署 | Docker Compose、Nginx |

## 项目目录结构

```text
.
├── backend/
│   ├── src/main/java/com/gb/sched/config/      # 常量配置
│   ├── src/main/java/com/gb/sched/controller/  # REST API
│   ├── src/main/java/com/gb/sched/model/       # 领域模型
│   └── src/main/java/com/gb/sched/service/     # 排班、调班、统计服务
├── database/
│   └── init.sql
├── frontend/
│   ├── src/api/
│   ├── src/components/
│   ├── src/constants/
│   ├── src/pages/
│   └── src/types/
├── docker-compose.yml
├── .env.example
└── README.md
```

## 环境变量说明

| 变量 | 说明 | 默认示例 |
| --- | --- | --- |
| `COMPOSE_PROJECT_NAME` | Compose 项目名 | `gbsched` |
| `DB_NAME` | MySQL 数据库名 | `gbsched` |
| `DB_USER` | MySQL 用户 | `gbsched_user` |
| `DB_PASSWORD` | MySQL 用户密码 | `change_me_strong_password` |
| `DB_ROOT_PASSWORD` | MySQL root 密码 | `change_me_root_password` |
| `JWT_SECRET` | JWT 签名密钥 | `change_me_to_a_long_random_secret` |
| `FRONTEND_PORT` | 前端宿主机端口 | `18931` |
| `BACKEND_PORT` | 后端宿主机端口 | `19931` |

## Docker 部署说明

- `docker-compose.yml` 不包含废弃的 `version` 字段，顶层声明 `name: gbsched`。
- 所有容器名带 `${COMPOSE_PROJECT_NAME:-gbsched}` 前缀，可在中文目录下直接运行。
- MySQL 使用命名卷 `db_data` 持久化。
- 前端 Nginx 通过 `/api/` 反向代理到 `http://backend:8080/`，前端代码不硬编码 `localhost`。
- 数据库和后端均配置健康检查，前端等待后端 healthy 后启动。

## License

MIT
