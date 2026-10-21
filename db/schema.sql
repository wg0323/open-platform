-- 共享自行车监管平台开放 API v3 建库脚本（业务表 + 管理后台 RBAC，已合并原有增量脚本）
-- 目标库：MySQL 8.0，字符集 utf8mb4
-- 幂等执行：建表统一用 CREATE TABLE IF NOT EXISTS，初始化数据统一用 INSERT IGNORE。
--   全新环境可一次建全；已有库重复执行只补缺失的表与数据，不会清空或覆盖既有数据。
--   注意：IF NOT EXISTS 不会给已存在的表补字段，后续表结构调整请另写变更脚本。

CREATE DATABASE IF NOT EXISTS `sbike_openapi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `sbike_openapi`;

CREATE TABLE IF NOT EXISTS `app_info` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_key`       varchar(64)     NOT NULL COMMENT '接入方标识，参与签名',
    `app_secret`    varchar(128)    NOT NULL COMMENT '签名密钥，生产环境必须加密存储',
    `company_code`  varchar(32)     NOT NULL COMMENT '企业编码，对应接口规范 companyCode',
    `company_name`  varchar(64)     NOT NULL COMMENT '企业名称',
    `ip_whitelist`  varchar(512)    DEFAULT NULL COMMENT 'IP 白名单，逗号分隔，支持 CIDR',
    `qps`           int             NOT NULL DEFAULT 10 COMMENT '每秒配额',
    `is_enabled`    tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否启用：1 是，0 否',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '接入方凭证';

CREATE TABLE IF NOT EXISTS `area` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          varchar(6)      NOT NULL COMMENT '行政区划编码',
    `name`          varchar(64)     NOT NULL COMMENT '区域名称',
    `parent_code`   varchar(6)      DEFAULT NULL COMMENT '上级区域编码',
    `fence_range`   varchar(2048)   DEFAULT NULL COMMENT '区域围栏经纬度 JSON 串',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '监管区域';

CREATE TABLE IF NOT EXISTS `fence` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `area_code`     varchar(6)      NOT NULL COMMENT '所属区域编码',
    `name`          varchar(64)     NOT NULL COMMENT '围栏名称',
    `fence`         varchar(2048)   NOT NULL COMMENT '真实围栏经纬度 JSON 串',
    `fence_logic`   varchar(2048)   DEFAULT NULL COMMENT '逻辑围栏经纬度 JSON 串',
    `limits`        int             DEFAULT NULL COMMENT '围栏自行车上限',
    `fence_type`    tinyint         NOT NULL DEFAULT 1 COMMENT '围栏类型：0 禁停，1 可停',
    `memo`          varchar(512)    DEFAULT NULL COMMENT '围栏备注',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_area_code` (`area_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '停车围栏';

CREATE TABLE IF NOT EXISTS `bike_order` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_key`       varchar(64)     NOT NULL COMMENT '接入方标识',
    `area_code`     varchar(6)      NOT NULL COMMENT '所属区域编码',
    `order_id`      varchar(32)     NOT NULL COMMENT '厂商订单号，幂等锚点',
    `sn`            varchar(32)     NOT NULL COMMENT '车辆编号',
    `start_time`    datetime        NOT NULL COMMENT '订单开始时间',
    `end_time`      datetime        NOT NULL COMMENT '订单结束时间',
    `amount`        decimal(10, 2)  DEFAULT NULL COMMENT '消费金额',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key_order_id` (`app_key`, `order_id`),
    KEY `idx_sn` (`sn`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '骑行订单';

CREATE TABLE IF NOT EXISTS `bike_position` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_key`       varchar(64)     NOT NULL COMMENT '接入方标识',
    `area_code`     varchar(6)      NOT NULL COMMENT '所属区域编码',
    `sn`            varchar(32)     NOT NULL COMMENT '车辆编号',
    `lng`           decimal(10, 7)  NOT NULL COMMENT '经度，高德坐标',
    `lat`           decimal(10, 7)  NOT NULL COMMENT '纬度，高德坐标',
    `position_time` datetime        NOT NULL COMMENT '定位时间',
    `status`        tinyint         NOT NULL COMMENT '车辆状态：0 开锁，1 锁止，2 锁止移动，3 正常',
    `busi_status`   tinyint         NOT NULL COMMENT '运营状态：0 空闲，1 运营中',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key_sn_position_time` (`app_key`, `sn`, `position_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车辆位置上报';

-- 管理后台 RBAC：用户、角色、菜单/按钮权限
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`             varchar(32)     NOT NULL COMMENT '登录账号',
    `password`             varchar(100)    NOT NULL COMMENT 'BCrypt 密文',
    `nickname`             varchar(64)     NOT NULL DEFAULT '' COMMENT '显示名',
    `enabled`              tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否启用：1 是，0 否',
    `must_change_password` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否需要强制改密：1 是，0 否',
    `token_version`        int             NOT NULL DEFAULT 0 COMMENT '改密后 +1，旧 token 失效',
    `last_login_time`      datetime        DEFAULT NULL COMMENT '最后登录时间',
    `create_time`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理后台用户';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_code`   varchar(64)     NOT NULL COMMENT '角色编码',
    `role_name`   varchar(64)     NOT NULL COMMENT '角色名称',
    `remark`      varchar(255)    DEFAULT NULL COMMENT '备注',
    `enabled`     tinyint unsigned NOT NULL DEFAULT 1 COMMENT '是否启用：1 是，0 否',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理后台角色';

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   bigint unsigned NOT NULL DEFAULT 0 COMMENT '上级菜单，0 为根',
    `menu_name`   varchar(64)     NOT NULL COMMENT '名称',
    `menu_type`   tinyint         NOT NULL COMMENT '类型：0 目录，1 菜单，2 按钮',
    `path`        varchar(128)    DEFAULT NULL COMMENT '前端路由',
    `component`   varchar(128)    DEFAULT NULL COMMENT '前端组件相对路径',
    `perms`       varchar(128)    DEFAULT NULL COMMENT '权限码',
    `icon`        varchar(64)     DEFAULT NULL COMMENT '图标名',
    `sort_no`     int             NOT NULL DEFAULT 0 COMMENT '同级排序',
    `visible`     tinyint unsigned NOT NULL DEFAULT 1 COMMENT '侧边栏是否显示：1 是，0 否',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理后台菜单与按钮权限';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`      bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint unsigned NOT NULL COMMENT '用户主键',
    `role_id` bigint unsigned NOT NULL COMMENT '角色主键',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id`      bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` bigint unsigned NOT NULL COMMENT '角色主键',
    `menu_id` bigint unsigned NOT NULL COMMENT '菜单主键',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联';

CREATE TABLE IF NOT EXISTS `api_metric_minute` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_key`         varchar(64)     NOT NULL COMMENT '接入方标识',
    `stat_minute`     datetime        NOT NULL COMMENT '统计分钟，精确到分',
    `requests`        bigint unsigned NOT NULL DEFAULT 0 COMMENT '请求数',
    `errors`          bigint unsigned NOT NULL DEFAULT 0 COMMENT '错误数',
    `limited`         bigint unsigned NOT NULL DEFAULT 0 COMMENT '限流拒绝数',
    `idempotent_hits` bigint unsigned NOT NULL DEFAULT 0 COMMENT '幂等命中数',
    `p99_millis`      int unsigned    NOT NULL DEFAULT 0 COMMENT 'P99 延迟（毫秒）',
    `create_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key_stat_minute` (`app_key`, `stat_minute`),
    KEY `idx_stat_minute` (`stat_minute`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '接口指标分钟快照';

-- 演示数据：一个接入方（appKey=sbike_demo，secret 可在管理台重置）
INSERT IGNORE INTO `app_info` (`id`, `app_key`, `app_secret`, `company_code`, `company_name`, `ip_whitelist`, `qps`, `is_enabled`)
VALUES (1, 'sbike_demo', 'SbikeDemoSecret2026x9f8e7d6c5b4a3210', '11011210', '演示单车企业', NULL, 10, 1);

-- 演示数据：北京市及两个区
INSERT IGNORE INTO `area` (`id`, `code`, `name`, `parent_code`, `fence_range`)
VALUES (1, '110100', '北京市', NULL, NULL),
       (2, '110101', '东城区', '110100', '[{"lng":116.406,"lat":39.928},{"lng":116.443,"lat":39.928},{"lng":116.443,"lat":39.905},{"lng":116.406,"lat":39.905}]'),
       (3, '110102', '西城区', '110100', '[{"lng":116.366,"lat":39.928},{"lng":116.406,"lat":39.928},{"lng":116.406,"lat":39.905},{"lng":116.366,"lat":39.905}]');

-- 演示数据：两个可停围栏
INSERT IGNORE INTO `fence` (`id`, `area_code`, `name`, `fence`, `fence_logic`, `limits`, `fence_type`, `memo`)
VALUES (1, '110101', '东单地铁站 A 口', '[{"lng":116.418893,"lat":39.908615},{"lng":116.419642,"lat":39.908589},{"lng":116.419667,"lat":39.907925},{"lng":116.418918,"lat":39.907951}]', '[{"lng":116.418861,"lat":39.908654},{"lng":116.419701,"lat":39.908641},{"lng":116.419719,"lat":39.907886},{"lng":116.418843,"lat":39.907899}]', 48, 1, '演示围栏'),
       (2, '110102', '西单文化广场', '[{"lng":116.373693,"lat":39.914593},{"lng":116.374452,"lat":39.914567},{"lng":116.374477,"lat":39.913903},{"lng":116.373718,"lat":39.913929}]', NULL, 60, 1, '演示围栏');

-- 演示数据：初始超级管理员 admin / Admin@123（首次登录强制改密）
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `enabled`, `must_change_password`, `token_version`)
VALUES (1, 'admin', '$2a$10$muH0alvjuQiLSss3iYXNJefgKF1xDJA7DtuDbNrUIcjIM93VS6pDi', '超级管理员', 1, 1, 0);

INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `remark`, `enabled`)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有全部权限，代码层特判', 1),
       (2, 'OPERATOR', '运营人员', '接入方管理与指标查看', 1);

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_no`, `visible`)
VALUES (1, 0, '接入方管理', 1, '/apps', 'app/index', 'app:list', 'Odometer', 1, 1),
       (2, 0, '指标看板', 1, '/metrics', 'metrics/index', 'metrics:view', 'DataLine', 2, 1),
       (3, 0, '系统管理', 0, '/system', NULL, NULL, 'Setting', 9, 1),
       (4, 3, '用户管理', 1, '/system/users', 'system/user/index', 'system:user:list', 'User', 1, 1),
       (5, 3, '角色管理', 1, '/system/roles', 'system/role/index', 'system:role:list', 'UserFilled', 2, 1),
       (6, 1, '新建接入方', 2, NULL, NULL, 'app:create', NULL, 1, 1),
       (7, 1, '编辑接入方', 2, NULL, NULL, 'app:update', NULL, 2, 1),
       (8, 1, '重置密钥', 2, NULL, NULL, 'app:reset-secret', NULL, 3, 1),
       (9, 1, '查看密钥', 2, NULL, NULL, 'app:reveal-secret', NULL, 4, 1),
       (10, 4, '新建用户', 2, NULL, NULL, 'system:user:create', NULL, 1, 1),
       (11, 4, '编辑用户', 2, NULL, NULL, 'system:user:update', NULL, 2, 1),
       (12, 4, '删除用户', 2, NULL, NULL, 'system:user:delete', NULL, 3, 1),
       (13, 4, '重置密码', 2, NULL, NULL, 'system:user:reset-password', NULL, 4, 1),
       (14, 4, '分配角色', 2, NULL, NULL, 'system:user:assign-role', NULL, 5, 1),
       (15, 5, '新建角色', 2, NULL, NULL, 'system:role:create', NULL, 1, 1),
       (16, 5, '编辑角色', 2, NULL, NULL, 'system:role:update', NULL, 2, 1),
       (17, 5, '删除角色', 2, NULL, NULL, 'system:role:delete', NULL, 3, 1),
       (18, 5, '分配权限', 2, NULL, NULL, 'system:role:assign-menu', NULL, 4, 1);

INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES (1, 1, 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
       (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18),
       (2, 1), (2, 2), (2, 6), (2, 7), (2, 8), (2, 9);
