package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * <p>
 * 鍓嶇鎺у埗鍣?
 * </p>
 *
 * @author 铏庡摜
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 鏍规嵇id鏌ヨ鍟嗛摵淇℃伅
     * @param id 鍟嗛摵id
     * @return 鍟嗛摵璇︽儏鏁版嵁
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) throws InterruptedException {
        return shopService.queryById(id);
    }

    /**
     * 鏂板鍟嗛摵淇℃伅
     * @param shop 鍟嗛摵鏁版嵁
     * @return 鍟嗛摵id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 鍐欏叆鏁版嵁搴?
        shopService.save(shop);
        // 杩斿洖搴楅摵id
        return Result.ok(shop.getId());
    }

    /**
     * 鏇存柊鍟嗛摵淇℃伅
     * @param shop 鍟嗛摵鏁版嵁
     * @return 鏃?
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 鍐欏叆鏁版嵁搴?
        return shopService.update(shop);
    }

    /**
     * 根据店铺类型分页查询店铺信息
     * @param typeId 店铺类型
     * @param current 页码
     * @param x 经度
     * @param y 纬度
     * @return 店铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 鏍规嵁鍟嗛摵鍚嶇О鍏抽敭瀛楀垎椤垫煡璇㈠晢閾轰俊鎭?
     * @param name 鍟嗛摵鍚嶇О鍏抽敭瀛?
     * @param current 椤电爜
     * @return 鍟嗛摵鍒楄〃
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 鏍规嵁绫诲瀷鍒嗛〉鏌ヨ
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 杩斿洖鏁版嵁
        return Result.ok(page.getRecords());
    }
}
