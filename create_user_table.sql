create table if not exists user
(
    id bigint auto_increment comment 'id' primary key,
    user_account varchar(256) not null comment '账号',
    user_password varchar(512) not null comment '密码',
    user_name varchar(256) not null comment '用户昵称',
    user_avatar varchar(1024) null comment '用户头像',
    user_introduction varchar(512) null comment '简介',
    user_type int default 0 not null comment '用户角色：0-普通用户 1-管理员',
    is_deleted tinyint default 0 not null comment '是否删除',
    edit_time datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    UNIQUE KEY uk_user_account (user_account),
    INDEX idx_user_name (user_name)

)comment '用户' collate = utf8mb4_unicode_ci;