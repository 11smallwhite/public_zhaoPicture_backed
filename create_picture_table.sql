-- 图片
-- auto-generated definition
create table picture
(
    id             bigint auto_increment comment 'id'
        primary key,
    p_url          varchar(512)                       not null comment '图片 url',
    p_name         varchar(128)                       not null comment '图片名称',
    p_introduction varchar(512)                       null comment '简介',
    p_category     varchar(64)                        null comment '分类',
    p_tags         varchar(512)                       null comment '标签（JSON 数组）',
    p_size         bigint                             null comment '图片体积',
    p_width        int                                null comment '图片宽度',
    p_height       int                                null comment '图片高度',
    p_scale        double                             null comment '图片宽高比例',
    p_format       varchar(32)                        null comment '图片格式',
    user_id        bigint                             not null comment '创建用户 id',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint  default 0                 not null comment '是否删除'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_p_category
    on picture (p_category);

create index idx_p_introduction
    on picture (p_introduction);

create index idx_p_name
    on picture (p_name);

create index idx_p_tags
    on picture (p_tags);

create index idx_userId
    on picture (user_id);