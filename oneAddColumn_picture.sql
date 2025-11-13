use zhao_picture;
alter table picture
    ADD column audit_status tinyint default 0 not null comment '审核状态 0-未审核，-1-审核拒绝，1-审核通过',
    ADD column audit_msg varchar(512) null comment '审核信息',
    ADD column auditor_id bigint null comment '审核人 id',
    ADD column audit_time DATETIME null comment '审核时间';
create index idx_audit_status
    on picture (audit_status);

