-- 插入管理员用户
use zhao_picture;
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 'https://example.com/avatars/admin.jpg', '系统管理员，负责用户管理和系统维护', 1),
                                                                                                         ('superadmin', 'e10adc3949ba59abbe56e057f20f883e', '超级管理员', 'https://example.com/avatars/superadmin.png', '拥有最高权限的管理员', 1);

-- 插入普通用户 - 技术爱好者系列
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('coder_li', 'e10adc3949ba59abbe56e057f20f883e', '编程小李', 'https://example.com/avatars/coder1.jpg', '全栈开发者，热爱开源技术', 0),
                                                                                                         ('java_master', 'e10adc3949ba59abbe56e057f20f883e', 'Java大师', 'https://example.com/avatars/java.jpg', '专注Java后端开发，Spring专家', 0),
                                                                                                         ('python_lover', 'e10adc3949ba59abbe56e057f20f883e', 'Python爱好者', 'https://example.com/avatars/python.png', 'AI和数据分析方向，Python重度用户', 0),
                                                                                                         ('frontend_wang', 'e10adc3949ba59abbe56e057f20f883e', '前端小王', 'https://example.com/avatars/frontend.jpg', 'Vue/React都精通，UI设计爱好者', 0),
                                                                                                         ('devops_zhang', 'e10adc3949ba59abbe56e057f20f883e', '运维老张', 'https://example.com/avatars/devops.png', '云原生、K8s、Docker实践者', 0);

-- 插入普通用户 - 日常用户系列
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('xiaoming123', 'e10adc3949ba59abbe56e057f20f883e', '小明同学', 'https://example.com/avatars/student1.jpg', '大学生，正在学习编程', 0),
                                                                                                         ('lily_flower', 'e10adc3949ba59abbe56e057f20f883e', '莉莉花开', 'https://example.com/avatars/lily.jpg', '喜欢摄影和旅行的设计师', 0),
                                                                                                         ('traveler_mike', 'e10adc3949ba59abbe56e057f20f883e', '旅行麦克', 'https://example.com/avatars/travel.jpg', '环游世界的背包客，分享旅行故事', 0),
                                                                                                         ('book_worm', 'e10adc3949ba59abbe56e057f20f883e', '书虫小张', 'https://example.com/avatars/book.jpg', '每周读一本书，文学爱好者', 0),
                                                                                                         ('music_fan', 'e10adc3949ba59abbe56e057f20f883e', '音乐狂热者', 'https://example.com/avatars/music.png', '钢琴10级，喜欢古典音乐', 0);

-- 插入普通用户 - 更多多样化用户
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('sports_man', 'e10adc3949ba59abbe56e057f20f883e', '运动达人', 'https://example.com/avatars/sports.jpg', '马拉松爱好者，健身教练', 0),
                                                                                                         ('food_lover', 'e10adc3949ba59abbe56e057f20f883e', '美食家', 'https://example.com/avatars/food.jpg', '探店美食博主，分享各地美食', 0),
                                                                                                         ('gamer_pro', 'e10adc3949ba59abbe56e057f20f883e', '游戏高手', 'https://example.com/avatars/gamer.jpg', '职业电竞选手，游戏直播', 0),
                                                                                                         ('art_designer', 'e10adc3949ba59abbe56e057f20f883e', '艺术设计师', 'https://example.com/avatars/art.jpg', '平面设计师，插画师', 0),
                                                                                                         ('tech_geek', 'e10adc3949ba59abbe56e057f20f883e', '科技极客', 'https://example.com/avatars/tech.jpg', '关注最新科技动态，数码产品评测', 0);

-- 插入一些简介为空的用户（测试默认值）
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('user001', 'e10adc3949ba59abbe56e057f20f883e', '用户001', 'https://example.com/avatars/default1.jpg', NULL, 0),
                                                                                                         ('user002', 'e10adc3949ba59abbe56e057f20f883e', '用户002', 'https://example.com/avatars/default2.jpg', NULL, 0),
                                                                                                         ('test_user', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', 'https://example.com/avatars/test.jpg', NULL, 0);

-- 插入一些昵称比较简单的用户
INSERT INTO user (user_account, user_password, user_name, user_avatar, user_introduction, user_type) VALUES
                                                                                                         ('simple1', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'https://example.com/avatars/simple1.jpg', '普通的上班族', 0),
                                                                                                         ('simple2', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'https://example.com/avatars/simple2.jpg', '喜欢看电影的程序员', 0),
                                                                                                         ('simple3', 'e10adc3949ba59abbe56e057f20f883e', '王五', 'https://example.com/avatars/simple3.jpg', '电商创业者', 0);