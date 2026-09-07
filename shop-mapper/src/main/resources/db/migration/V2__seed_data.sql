-- ============================================================
-- V2 种子数据：分类 / 品牌 / 商品
-- ============================================================

-- 一级分类
INSERT INTO `pms_category` (`id`, `parent_id`, `name`, `sort`) VALUES
(1, 0, '手机数码', 1),
(2, 0, '电脑办公', 2),
(3, 0, '服饰鞋包', 3),
(4, 0, '食品生鲜', 4);

-- 二级分类
INSERT INTO `pms_category` (`id`, `parent_id`, `name`, `sort`) VALUES
(11, 1, '智能手机', 1),
(12, 1, '耳机音箱', 2),
(21, 2, '笔记本电脑', 1),
(22, 2, '显示器', 2),
(31, 3, '休闲鞋', 1),
(41, 4, '休闲零食', 1);

-- 品牌
INSERT INTO `pms_brand` (`id`, `name`, `story`) VALUES
(1, '星辰科技', '专注智能手机研发十余年，以极致体验为核心。'),
(2, '声界', '声学品牌，重新定义音乐聆听。'),
(3, '极算', '高性能计算设备品牌。'),
(4, '轻履', '舒适出行，从脚开始。'),
(5, '味觉工坊', '全球精选，美味直达。');

-- 商品（图片由文生图接口生成）
INSERT INTO `pms_product` (`category_id`, `brand_id`, `name`, `sub_title`, `main_image`, `price`, `original_price`, `available_stock`, `sale`, `detail_html`) VALUES
(11, 1, '星辰 X1 Pro 旗舰智能手机', '第二代骁龙8 | 2K曲面屏 | 100W快充',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=premium%20flagship%20smartphone%20product%20photo%2C%20black%20glass%20body%2C%20studio%20lighting%2C%20white%20background%2C%20commercial%20photography&image_size=square_hd',
 4999.00, 5499.00, 500, 1200, '<p>星辰 X1 Pro，性能与影像双旗舰。</p>'),
(11, 1, '星辰 X1 轻薄手机 5G', '轻薄机身 | 长续航 | 5000mAh',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sleek%20slim%20blue%20smartphone%20product%20shot%2C%20clean%20white%20background%2C%20soft%20studio%20light&image_size=square_hd',
 2699.00, 2999.00, 800, 3400, '<p>轻薄长续航，日常主力机。</p>'),
(12, 2, '声界 AirBuds 无线降噪耳机', '主动降噪 | 40小时续航 | HiFi音质',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20wireless%20earbuds%20with%20charging%20case%2C%20minimalist%20product%20photography%2C%20light%20gray%20background&image_size=square_hd',
 499.00, 599.00, 1500, 8600, '<p>口袋里的音乐厅。</p>'),
(21, 3, '极算 Book 14 轻薄笔记本', '标压R7 | 32G内存 | 2.8K屏',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=silver%20thin%20laptop%20product%20photo%20open%20screen%2C%20minimal%20white%20background%2C%20commercial%20style&image_size=square_hd',
 5699.00, 6299.00, 300, 560, '<p>生产力与轻薄兼得。</p>'),
(22, 3, '极算 27 英寸 4K 专业显示器', '4K IPS | 99%sRGB | Type-C 90W',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=27%20inch%204k%20computer%20monitor%20product%20photo%2C%20ultra%20thin%20bezel%2C%20white%20background%2C%20studio%20lighting&image_size=square_hd',
 2199.00, 2499.00, 260, 410, '<p>设计师的专业色彩伙伴。</p>'),
(31, 4, '轻履 云弹 缓震休闲跑步鞋', '轻量发泡 | 透气网面 | 全天舒适',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20running%20sneakers%20product%20photo%2C%20single%20shoe%20side%20view%2C%20clean%20light%20background%2C%20e-commerce%20style&image_size=square_hd',
 329.00, 429.00, 2000, 12600, '<p>像踩在云朵上一样轻盈。</p>'),
(41, 5, '味觉工坊 每日坚果 30袋礼盒', '科学配比 | 新鲜锁存 | 无添加',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=mixed%20nuts%20gift%20box%20product%20photo%2C%20small%20packets%20arranged%2C%20warm%20light%2C%20food%20commercial%20photography&image_size=square_hd',
 139.00, 169.00, 5000, 23000, '<p>每日一袋，营养均衡。</p>'),
(41, 5, '味觉工坊 海盐苏打饼干 500g', '低糖配方 | 酥脆可口 | 办公零食',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=soda%20crackers%20in%20packaging%20product%20photo%2C%20stack%20of%20biscuits%2C%20clean%20background%2C%20food%20photography&image_size=square_hd',
 19.90, 25.90, 10000, 52000, '<p>下午茶必备小饼干。</p>');
