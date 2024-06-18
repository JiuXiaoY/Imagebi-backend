# 建表脚本

-- 创建库
create database if not exists image_bi;

-- 切换库
use image_bi;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

drop table chart;
-- 图表表
create table if not exists chart
(
    id          bigint auto_increment comment 'id' primary key,
    goal        text                               null comment '分析目标',
    relatedName varchar(128)                       null comment '名称',
    chartData   text                               null comment '图表数据',
    chartType   varchar(128)                       null comment '图表类型',
    genChart    text                               null comment '生成的图表数据',
    genResult   text                               null comment '生成的分析结论',
    execStatus  int      default 0                 null comment '执行状态',
    execMessage text                               null comment '执行信息',
    userId      bigint                             null comment '创建用户 id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;

# id
# 通知类型 系统通知 用户回复 私信
# 发送信息用户
# 接收信息用户
# 通知时间
# 图表名字
# 通知状态 已读，未读
# 通知内容
# 删除状态
create table if not exists chart_message
(
    `id`             bigint auto_increment comment 'id' primary key,
    `senderId`       bigint                             not null comment '发送信息用户',
    `receiverId`     bigint                             not null comment '接收信息用户',
    `chartId`        bigint                             not null comment '图表Id',
    `messageContent` text comment '通知内容',
    `messageType`    int                                not null comment '通知类型',
    `messageStatus`  int                                not null comment '通知状态',
    `messageTime`    datetime default CURRENT_TIMESTAMP not null comment '通知时间',
    `updateTime`     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete`       tinyint  default 0                 not null comment '删除状态'

) comment '消息通知表' collate = utf8mb4_unicode_ci;
