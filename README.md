# 记账本后端 API

个人记账应用的后端服务（前后端分离，纯 API）。支持账单增删改查、分类/日期筛选、月度收支统计、分页查询、登录认证、密码加密。

## 技术栈

- Java 17 + Spring Boot 4.1.1 + Gradle
- MyBatis-Plus 3.5.17（含分页插件 `mybatis-plus-jsqlparser`）
- MySQL 8
- BCrypt 密码加密（spring-security-crypto）

## 功能

**用户模块**
- 注册：密码 BCrypt 加密后入库，返回 DTO（不含 password）
- 登录：验证通过后写入 Session，失败区分"用户不存在/密码错误"
- 用户 CRUD

**账单模块（Record）**
- 增删改查
- 按分类 / 日期区间筛选（QueryWrapper 动态条件）
- 月度收支统计（按月聚合：收入/支出分组 SUM）
- 分页查询（`record_date` + `id` 双降序，保证翻页顺序稳定）

**认证**
- `AuthInterceptor` 拦截 `/api/**`，未登录返回 401
- 放行路径：注册（`/api/user`）、登录（`/api/user/login`）
- Session 凭证（`userId`）读写

**安全**
- 密码不明文存储（BCrypt 单向哈希 + 随机盐）
- DTO 隔离敏感字段，password 不出现在任何接口返回中
- MyBatis-Plus 参数化查询（`?` 占位，防 SQL 注入）

## 快速启动

1. MySQL 建库建表（库名 `tally`，表 `user`/`record`，`user` 为保留字需加反引号）
2. 修改 `src/main/resources/application.properties` 中的数据库密码
3. 构建并启动：

```bash
./gradlew build -x test
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

4. 验证：

```bash
# 未登录被拦（401）
curl -i http://localhost:8080/api/record/list

# 注册
curl -X POST http://localhost:8080/api/user \
  -H "Content-Type: application/json" \
  -d '{"username":"test01","password":"123456","nickname":"测试一号"}'

# 登录（拿凭证）
curl -c cookies.txt -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test01","password":"123456"}'

# 带凭证访问
curl -b cookies.txt http://localhost:8080/api/record/list
```

## 项目结构

```
com.example.demo
├── DemoApplication.java          # 启动类（@MapperScan）
├── controller/
│   ├── UserController.java       # 注册/登录/用户 CRUD
│   └── RecordController.java     # 账单 CRUD/筛选/统计/分页
├── dto/
│   └── UserDTO.java              # 返回层对象（隐藏 password）
├── entity/
│   ├── User.java
│   └── Bill.java                 # 对应表 record（改名避开 JDK Record）
├── mapper/
│   ├── UserMapper.java           # BaseMapper 白送 CRUD
│   └── RecordMapper.java
├── config/
│   ├── MybatisPlusConfig.java    # 分页插件
│   └── WebConfig.java            # 拦截器注册
└── interceptor/
    └── AuthInterceptor.java      # 登录门卫
```

## 亮点与排坑

- **MyBatis-Plus v3.5.9 起分页插件被拆到独立模块**：`PaginationInnerInterceptor` 报"无法解析符号"，查官方文档确认需单独引入 `mybatis-plus-jsqlparser` 依赖。
- **分页翻页乱序**：仅按 `record_date` 排序时同日记录顺序不稳定，导致翻页重复/丢数据 → 加 `id` 主键做二级排序兜底。
- **拦截器认证死循环**：注册/登录接口若也被拦截，用户永远无法拿到凭证 → 必须 `excludePathPatterns` 放行。
- **密码长度坑**：BCrypt 密文 60 字符，`password` 列需 `VARCHAR(100)`，否则注册报 `Data too long`。
- **POST 返回 userId 为 null**：`Map` 里误用请求体中的 `user.getId()` → 应用数据库回查的 `dbUser.getId()`。

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/user | 注册（密码加密）|
| POST | /api/user/login | 登录（写 Session）|
| GET | /api/user/list | 用户列表（DTO）|
| GET | /api/record/list?category=&start=&end= | 账单筛选 |
| GET | /api/record/stats?year=&month= | 月度收支统计 |
| GET | /api/record/page?pageNum=&pageSize= | 分页 |
| POST/PUT/DELETE | /api/record、/api/record/{id} | 账单增改删 |

## 后续规划

- JWT 替代 Session（前后端分离标准凭证方案）
- 前端页面 / 小程序端
- Redis 缓存
