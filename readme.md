# ElderHub 养老服务管理平台

## 项目简介
ElderHub是一套养老服务管理系统后端项目，使用SpringBoot开发，用于管理老人基础信息、健康档案、服务预约等业务。
本项目为小组协作开发项目，后端采用分层架构，配套MySQL数据库。

## 技术栈
- 后端框架：Spring Boot
- 持久层：MyBatis
- 数据库：MySQL 8.0
- 项目构建：Maven
- JDK版本：JDK 1.8

## 项目目录结构
ElderHub
├── docs                # 项目文档、需求、接口文档
├── sql                 # 数据库初始化脚本
├── src
│   └── main
│       ├── java/com/elderhub
│       │   ├── controller      # 控制器，接收前端请求
│       │   ├── service         # 业务逻辑层
│       │   │   └── impl        # Service实现类
│       │   ├── mapper          # MyBatis数据访问层
│       │   ├── entity          # 数据库实体类
│       │   ├── dto             # 数据传输对象
│       │   ├── vo              # 返回视图对象
│       │   ├── config          # 配置类
│       │   └── util            # 工具类
│       └── resources
│           ├── mapper          # MyBatis xml映射文件
│           └── application.yml # 项目配置文件
├── pom.xml             # Maven依赖配置
├── .gitignore          # Git忽略文件
└── readme.md           # 项目说明文档

## 功能模块
1. 用户管理：管理员、护工、老人账号管理，角色权限控制
2. 老人信息管理：老人基础档案、健康信息录入与维护
3. 护理服务管理：护理项目登记、护理计划安排、护理记录
4. 床位管理：床位信息、入住、退住管理
5. 健康监测：体检记录、慢病管理、健康档案
6. 系统管理：字典、公告、日志管理
7. 
## 开发规范
- 标准三层架构开发
- 统一返回结果封装
- Git 规范提交代码

## 团队成员
小组协作开发