CREATE TABLE IF NOT EXISTS digital_twin_map (
    map_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地图ID',
    map_name VARCHAR(100) NOT NULL COMMENT '地图名称',
    building_id BIGINT NULL COMMENT '关联楼栋ID',
    floor_id BIGINT NULL COMMENT '关联楼层ID',
    building_name VARCHAR(100) NULL COMMENT '楼栋名称冗余',
    floor_no INT NULL COMMENT '楼层号冗余',
    map_image VARCHAR(255) NOT NULL COMMENT '平面图图片路径',
    width INT NOT NULL COMMENT '平面图设计宽度',
    height INT NOT NULL COMMENT '平面图设计高度',
    status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='数字孪生地图配置表';

INSERT INTO digital_twin_map (
    map_name,
    building_name,
    floor_no,
    map_image,
    width,
    height,
    status
)
SELECT
    '一号楼一层平面图',
    '一号楼',
    1,
    '/maps/floor1.png',
    1000,
    600,
    'enabled'
WHERE NOT EXISTS (
    SELECT 1
    FROM digital_twin_map
    WHERE status = 'enabled'
);
