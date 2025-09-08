package com.hmdp.service;

import com.hmdp.dto.Result;

/**
 * 地图服务接口
 */
public interface IMapService {

    /**
     * 获取地图基础数据
     */
    Result getMapData();

    /**
     * 根据位置获取店铺
     */
    Result getShopsByLocation(Double x, Double y, Double radius);

    /**
     * 获取附近店铺
     */
    Result getNearbyShops(Double x, Double y, Double radius);
} 