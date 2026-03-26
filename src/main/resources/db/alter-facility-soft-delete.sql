-- facility 模块软删除字段（楼栋/楼层）
ALTER TABLE buildings
  ADD COLUMN deleted_at DATETIME NULL AFTER building_name;

ALTER TABLE floors
  ADD COLUMN deleted_at DATETIME NULL AFTER floor_name;

CREATE INDEX idx_buildings_deleted_at ON buildings (deleted_at);
CREATE INDEX idx_floors_deleted_at ON floors (deleted_at);
