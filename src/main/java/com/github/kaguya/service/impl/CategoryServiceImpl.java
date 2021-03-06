package com.github.kaguya.service.impl;

import com.github.kaguya.constant.CommonConstant;
import com.github.kaguya.dao.RedisDao;
import com.github.kaguya.dao.mapper.CategoryMapper;
import com.github.kaguya.exception.model.ResponseMsg;
import com.github.kaguya.model.Category;
import com.github.kaguya.service.CategoryService;
import com.github.kaguya.util.SnowFlake;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final static String KEY_PREFIX = CommonConstant.REDIS_KEY_PREFIX_KAGUYA_WEB + "categories:";
    private final static long EXPIRE = 3600 * 24L;

    @Resource
    private RedisDao redisDao;
    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listCategories() {
        String key = KEY_PREFIX + "list";
        List<Category> categoryList = (List<Category>) (List) redisDao.lGet(key, 0, -1);
        if (CollectionUtils.isEmpty(categoryList)) {
            categoryList = categoryMapper.selectAll();
            // 存储到redis，这里需要类型转换
            if (categoryList != null && categoryList.size() > 0) {
                redisDao.lSet(key, (List<Object>) (List) categoryList, EXPIRE);
            }
        }
        return categoryList;
    }

    @Override
    public ResponseMsg add(String name) {
        Category category = new Category()
                .setCategoryId(SnowFlake.generateId())
                .setName(name)
                .setOrderId(getNextOrderId());
        int result = categoryMapper.insert(category);
        if (result > 0) {
            // 清除redis缓存
            String key = KEY_PREFIX + "list";
            redisDao.del(key);
            return ResponseMsg.buildSuccessResult();
        } else {
            return ResponseMsg.buildFailResult();
        }
    }

    /**
     * 获取下一个orderId
     */
    public Integer getNextOrderId() {
        // 获取最大的orderId
        Integer orderId = categoryMapper.getMaxOrderIdBy();
        if (null == orderId) {
            return 0;
        } else {
            orderId += 1;
        }
        return orderId;
    }
}
