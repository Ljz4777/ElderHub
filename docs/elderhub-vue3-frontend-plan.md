# ElderHub 分离式 Vue3 前端 — 实施计划

## Context（背景）

ElderHub 颐养中心目前只有 Spring Boot 3 后端（`d:\ElderHub`，端口 8080，context-path `/ElderHub`）。用户需要一套**分离式 Vue3 管理后台前端**，放在 **D 盘独立目录 `d:\elderhub-frontend`**，UI 用 **Element Plus**，**全量覆盖已有后端接口**。用户偏好：浅色配色、核心功能可见、直观展示（统计卡片/进度条，不用复杂可视化）。

## 已核实的后端事实（前端设计的硬约束）

| 事实 | 影响 |
| --- | --- |
| 统一响应 `Result{code, message, data}`，成功 code=200（[Result.java](file:///d:/ElderHub/src/main/java/com/it/elderhub/common/Result.java)） | 拦截器读 `message` 字段 |
| JWT：`POST /auth/login` 返回 `{userId, username, token, role}`；请求头 `Authorization: Bearer <token>`；角色判断只认 `role === 'ADMIN'`，其余按管家处理 | 登录态与路由守卫 |
| **无客户分页接口**（CustomController 仅 save/update/remove/goOut/comeback/no-nurse） | 在住客户主表改用 `GET /admin/bed/details/list?status=1` |
| 实体直出字段是**下划线命名**（customer_name/bed_no/level_name），VO 是驼峰；`map-underscore-to-camel-case=false` | 表单提交键名按来源区分，需字段字典 |
| 分页两种格式：`IPage{records,total,...}` 与 `PageResult{records,total,pageNum,pageSize}` | api 层统一归一化为 `{list, total}` |
| 多个写接口用 `@RequestParam`（query 传参）：outward/back-down 审批、bed/details/update、nurse-level/update-status、assign-customer、nurse outward return | axios 一律走 `params` |
| 管家端接口需显式传 `userId` query 参数 | 从 user store 注入 |
| `LocalDateTime` 输出带 T 的 ISO；query 日期只收 `yyyy-MM-dd` | format.js 统一处理 |
| 性别字典两套：User.sex 0女1男；Customer.customer_sex 0男1女 | 两套常量，严禁共用 |
| Outward 后端缺陷：VO 无 id、customerName 恒 null、customerId 被记录 id 覆盖 | 审批用 `row.customerId` 传参，客户名显示"—"，留 TODO |
| 菜单表无初始数据，`GET /admin/menu/tree` 返回空 | 前端静态菜单兜底，按角色过滤 |
| 密码用 passwordEncoder（BCrypt），user/role 表无初始数据 | 联调前需 SQL 造管理员账号（BCrypt 哈希）+ 2 条角色 |
| customer 表代码中使用 `user_id`/`butler_id` 列，建表 SQL 中没有 | 联调前先 `DESC customer` 核对，缺列需补 |

## 技术栈

Vite + Vue 3（Composition API + `<script setup>`，JS 不用 TS）+ Vue Router + Pinia + Element Plus + @element-plus/icons-vue + Axios。本机 Node v24.15.0 / npm 11.17.0。

## 目录结构

```
d:\elderhub-frontend\
├─ index.html / package.json / vite.config.js / .env.development / .gitignore
└─ src\
   ├─ main.js                # pinia + router + Element Plus 全量引入 + 图标注册
   ├─ App.vue
   ├─ api\                   # request.js + 13 个模块 api（auth/user/role/customer/bed/
   │                         #   nurseAssign/nurseLevel/nurseContent/nurseRecord/
   │                         #   outward/backdown/nurseDaily）
   ├─ stores\user.js         # token/userId/username/role，localStorage 持久化，isAdmin getter
   ├─ router\index.js        # 静态路由表 + beforeEach 守卫
   ├─ config\menu.js         # 静态菜单元数据 + menusForRole(role) 过滤
   ├─ layout\                # index.vue（el-container）+ Sidebar.vue + Navbar.vue
   ├─ utils\                 # format.js（日期去T/空值—）+ constants.js（状态字典）
   ├─ components\            # Pagination / StatusTag / RoomBedPicker / CustomerPicker
   └─ views\
      ├─ login\index.vue             # 登录/注册卡片
      ├─ dashboard\index.vue         # 床位统计 4 卡片 + 入住率/外出率 el-progress
      ├─ bed\MapView.vue / Details.vue      # 床位图（状态着色+调床）+ 使用详情分页
      ├─ customer\index.vue          # 4 Tab：在住客户/入住登记/无床位客户/管家分配
      ├─ system\User.vue / Role.vue / Menu.vue
      ├─ nurse\Level.vue / Content.vue      # 护理级别（含配置项目抽屉）/ 护理项目
      ├─ business\OutwardAdmin.vue / BackdownAdmin.vue / RecordAdmin.vue
      ├─ nursework\Customers.vue / Records.vue / Outward.vue / Backdown.vue  # 管家四页
      ├─ placeholder\index.vue       # 餐饮/偏好占位（后端无 Controller）
      └─ error\404.vue
```

## 核心设计

### request.js（axios 封装）
- `baseURL = /api`；Vite proxy：`'/api' → http://localhost:8080`，rewrite `/api` → `/ElderHub`
- 请求拦截器：带 `Bearer token`（login/register 除外）
- 响应拦截器：HTTP/body 401 → 清 store 跳 `/login?redirect=...`；code=200 → 直接返回 `data`；否则 ElMessage 报错并 reject
- 导出 `normalizePage(data)`：返回 `{list: data.records||[], total: Number(data.total)||0}`

### 路由与菜单（静态声明，不做动态 addRoute）
| 路径 | 角色 | 页面 |
| --- | --- | --- |
| /login | 公开 | 登录 |
| /dashboard | 双方 | 仪表盘 |
| /bed/map /bed/details /customer | ADMIN | 床位图/详情、客户管理 |
| /system/user /system/role /system/menu | ADMIN | 系统管理 |
| /nurse/level /nurse/content | ADMIN | 护理级别/项目 |
| /outward /backdown /records | ADMIN | 外出/退住审批、护理记录 |
| /my/customers /my/records /my/outward /my/backdown | USER | 管家工作台 |
| /food /meal /preference | ADMIN | 占位页 |
| /:pathMatch(.*)* | 公开 | 404 |

守卫：无 token → `/login?redirect=`；已登录访问 /login → /dashboard；角色不匹配 → 跳 dashboard + warning。
侧边栏由 `config/menu.js` 的 `menusForRole(role)` 渲染（后端菜单树为空，静态兜底）。

### 关键页面要点
- **Dashboard**：`GET /admin/bed/statistics` → totalBeds/freeBeds/occupiedBeds/awayBeds 卡片 + 入住率/外出率进度条
- **客户管理 4 Tab**：在住客户（bed/details/list?status=1，行操作：外出/回院）；入住登记（Customer 下划线字段表单 + RoomBedPicker 联动房间→空闲床位，回填 room_no/building_no/bed_id）；无床位客户（no-nurse）；管家分配（左 nurse-list、右 no-nurse 池、assign-customer / remove-customer）
- **床位图**：`map?floor=` 房间卡 + 床位色块（1绿/2蓝/3橙）+ 图例；点击有人床位 → 调床弹窗（transfer body）；楼层选项来自 room/list
- **审批页**：auditStatus 字典标签；审批弹窗（通过1/拒绝2，auditPerson=当前用户名，query 传参）
- **管家四页**：所有请求带 `userId: store.userId`；录入护理记录弹窗（项目下拉按状态禁用 + nursingTime 转 ISO 带 T）

## 实施步骤（每步独立可验证）

1. **脚手架**：`npm create vite@latest elderhub-frontend -- --template vue` → 装依赖 → `npm run dev` 出默认页
2. **基础设施**：vite.config.js（别名+proxy）、.env、main.js、目录骨架 → 代理可达后端（TestController 探路）
3. **登录链路**：request.js + user store + api/auth.js + 登录页 → **先 SQL 造数**（role 2 条 + BCrypt 密码管理员账号）→ 登录成功写 localStorage
4. **路由/布局**：router + 守卫 + Sidebar/Navbar → 未登录访问被踢回、刷新不丢态、退出清态
5. **公共层**：constants/format、Pagination/StatusTag、Dashboard → 统计卡数字合理
6. **床位模块**：bed.js + MapView + Details + RoomBedPicker → 床位图渲染、调床/改日期生效
7. **客户模块**：customer.js + nurseAssign.js + 4 Tab → 入住登记后床位图变化、分配管家后池子减少
8. **系统管理**：user/role/menu → 用户新增出现在列表；menu 空态
9. **护理模块**：Level（含项目配置抽屉）+ Content → 级别配项目闭环
10. **审批+记录**：OutwardAdmin/BackdownAdmin/RecordAdmin → 审批后状态变化
11. **管家四页** → 管家账号登录只见管家菜单，录入记录成功
12. **收尾**：占位页、404、浅色主色微调（--el-color-primary）、全菜单回归点查

## 风险与注意点

- 后端 Outward VO 缺陷、customer 表可能缺 `user_id`/`butler_id` 列、register 接口 NOT NULL 缺陷 —— 前端留 TODO 注释，不改后端（本次任务仅前端）
- 表单提交 JSON 键名严格按来源：entity 用下划线、DTO 用驼峰
- query 型写接口必须 `params` 传参，不能放 body
- `Result.message` 与 NurseContent 的 `message`（项目描述）字段重名，注意区分

## 验证方式

- 后端先启动（8080），前端 `npm run dev`（默认 5173）
- 每步按上表验证；最终用浏览器自动化按「管理员/管家」两个角色走通：登录 → 各页面 CRUD → 审批 → 退出
