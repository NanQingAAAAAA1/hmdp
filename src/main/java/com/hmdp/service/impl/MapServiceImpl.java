package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IMapService;
import com.hmdp.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

/**
 * 地图服务实现类
 */
@Service
public class MapServiceImpl implements IMapService {

    @Resource
    private IShopService shopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getMapData() {
        // 返回地图基础数据，如默认中心点、缩放级别等
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("center", new double[]{116.397128, 39.916527}); // 默认北京天安门
        mapData.put("zoom", 12);
        mapData.put("minZoom", 8);
        mapData.put("maxZoom", 18);
        return Result.ok(mapData);
    }

    @Override
    public Result getShopsByLocation(Double x, Double y, Double radius) {
        // 获取所有店铺类型的地理位置数据
        List<Shop> allShops = new ArrayList<>();
        
        // 遍历所有店铺类型（这里简化处理，实际应该从数据库获取所有类型）
        for (int typeId = 1; typeId <= 10; typeId++) {
            String key = SHOP_GEO_KEY + typeId;
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                    .search(
                            key,
                            GeoReference.fromCoordinate(x, y),
                            new Distance(radius),
                            RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(50)
                    );
            
            if (results != null && !results.getContent().isEmpty()) {
                List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
                List<Long> ids = new ArrayList<>();
                Map<String, Double> distanceMap = new HashMap<>();
                
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : list) {
                    String shopIdStr = result.getContent().getName();
                    ids.add(Long.valueOf(shopIdStr));
                    distanceMap.put(shopIdStr, result.getDistance().getValue());
                }
                
                // 根据ID查询店铺详情
                if (!ids.isEmpty()) {
                    List<Shop> shops = shopService.listByIds(ids);
                    for (Shop shop : shops) {
                        shop.setDistance(distanceMap.get(shop.getId().toString()));
                    }
                    allShops.addAll(shops);
                }
            }
        }
        
        return Result.ok(allShops);
    }

    @Override
    public Result getNearbyShops(Double x, Double y, Double radius) {
        // 获取附近店铺，按距离排序
        return getShopsByLocation(x, y, radius);
    }
} 