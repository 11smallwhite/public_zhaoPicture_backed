use zhao_picture;
ALTER TABLE picture
    ADD column thumbnail_url varchar(512) null comment '缩略图 url';