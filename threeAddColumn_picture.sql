use zhao_picture;
-- 添加新列
ALTER TABLE picture
    ADD COLUMN space_id  bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (space_id);
