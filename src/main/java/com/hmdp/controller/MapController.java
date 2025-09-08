package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IMapService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 地图功能控制器
 */
@RestController
@RequestMapping("/map")
public class MapController {

    @Resource
    private IMapService mapService;

    /**
     * 获取地图基础数据
     */
    @GetMapping
    public Result getMapData() {
        return mapService.getMapData();
    }

    /**
     * 获取指定范围内的店铺
     */
    @GetMapping("/shops")
    public Result getShopsOnMap(
            @RequestParam Double x,
            @RequestParam Double y,
            @RequestParam(defaultValue = "5000") Double radius) {
        return mapService.getShopsByLocation(x, y, radius);
    }

    /**
     * 获取用户当前位置附近的店铺
     */
    @GetMapping("/nearby")
    public Result getNearbyShops(
            @RequestParam Double x,
            @RequestParam Double y,
            @RequestParam(defaultValue = "3000") Double radius) {
        return mapService.getNearbyShops(x, y, radius);
    }
} 