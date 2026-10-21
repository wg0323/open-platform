package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import com.bike.openapi.domain.BikePosition;

/**
 * 车辆位置 Mapper：位置上报按（appKey, sn, positionTime）幂等。
 */
public interface BikePositionMapper extends BaseMapper<BikePosition> {

    /**
     * 幂等写入：唯一键冲突时更新坐标与状态。
     *
     * @param position 位置记录
     * @return 影响行数
     */
    @Insert("INSERT INTO bike_position (app_key, area_code, sn, lng, lat, position_time, status, busi_status) "
            + "VALUES (#{item.appKey}, #{item.areaCode}, #{item.sn}, #{item.lng}, #{item.lat}, "
            + "#{item.positionTime}, #{item.status}, #{item.busiStatus}) AS incoming "
            + "ON DUPLICATE KEY UPDATE area_code = incoming.area_code, lng = incoming.lng, lat = incoming.lat, "
            + "status = incoming.status, busi_status = incoming.busi_status, update_time = NOW()")
    int upsert(@Param("item") BikePosition position);
}
