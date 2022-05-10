package com.lif314.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.gulimall.product.service.CategoryBrandRelationService;
import com.lif314.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.CategoryDao;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    // 注入dao来查询数据库表 -- 也可以使用泛型
    //    @Autowired
    //    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;


    @Autowired
    StringRedisTemplate redisTemplate;


    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // [1] 查出所有分类 -- 在dao中查询该表

        // 使用泛型 -- baseMapper即对应的dao
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);//查询所有 没有查询条件null

        // [2] 组装成父子树形结构
        // 找到一级分类 -- 父分类id为0

        return categoryEntities.stream().filter((categoryEntity) -> {
            // 过滤条件
            return categoryEntity.getParentCid() == 0L;
            // 一级分类收集为集合
        }).map((menu) -> {
            // 保存每一个菜单的子分类
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
    }

    /**
     * 删除菜单那
     *
     * @param asList id数组
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO：检查菜单是否被引用

        // 逻辑删除-show_status
        baseMapper.deleteBatchIds(asList);
    }

    // [2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        // 逆序转换
        Collections.reverse(paths);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新
     */
    @Caching(evict = {
            @CacheEvict(value = "category", key = "'getCatelogJson'"),
            @CacheEvict(value = "category", key = "'getLevel1Category'")
    })  // 组合操作，同时删除两个缓存数据
//    @CacheEvict(value = "category", allEntries = true)  // 删除该分区下所有数据
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        //级联更新
        if (!StringUtils.isEmpty(category.getName())) {
            categoryBrandRelationService.updateCategoryName(category.getCatId(), category.getName());

            // TODO:级联更新
        }
    }

    /**
     * 查询一级分类数据要放在哪个缓存分区中【最好按照业务类型划分】
     */

    @CacheEvict(value = "category", key = "#root.method.name") // 失效模式[数据库修改，清除缓存]：指定清除的缓存
    // 每一个需要缓存的数据我们来指定
    @Cacheable(value = {"category"}, key = "#root.method.name")// 代表当前方法的结果需要缓存。如果缓存中有，则不需要调用该方法；如果缓存中没有，则调用获取并放入缓存
    @Override
    public List<CategoryEntity> getLevel1Category() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }


    /**
     * 三级分类 -- SpringCache
     */
    @Cacheable(value = "category", key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        //  一次获取所有三级分类数据
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 查出所有的一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        /**
         * 封装数据
         *
         * key: 一级分类的id
         * Value: 二级分类的列表以及内部的子分类  List<Catelog2Vo>
         */
        Map<String, List<Catelog2Vo>> listMap = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 每一个一级分类-- 查询该分类的子分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
            // 封装二级分类列表
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map((l2) -> {
                    // 封装二级分类下的三级分类数据
                    List<CategoryEntity> level3Category = getParentCid(selectList, l2.getCatId());
                    List<Catelog2Vo.Category3Vo> category3Vos = null;
                    if (level3Category != null) {
                        // 封装Category3Vo
                        category3Vos = level3Category.stream().map((l3) -> {
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return category3Vo;
                        }).collect(Collectors.toList());
                    }

                    // 组装二级分类
                    return new Catelog2Vo(v.getCatId().toString(), category3Vos, l2.getCatId().toString(), l2.getName());
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return listMap;
    }


    /**
     * 带Redis的三级分类
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonCache() {
        // 给缓存中存放json字符串，拿出的json字符串，还要逆转为能用的对象类型
        // 【序列化与反序列化】

        /**
         * 1、空结果缓存 --- 缓存穿透
         * 2、设置过期时间(加随机值) -- 缓存雪崩
         * 3、加锁 ---- 缓存击穿
         */

        // 1、加入缓存逻辑，缓存中存json
        // JSON跨语言、跨平台兼容
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.isEmpty(catelogJson)) {
            // 2、缓存中没有，查询数据库  --- 加锁(将放入缓存也加入锁中)
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedisLock();
            // 返回数据
            return catelogJsonFromDb;
        }

        // 4、从缓存中拿到结果，转换为指定的对象
        return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }


    /**
     * 使用本地锁
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLocalLock() {

        /**
         * 在查询数据库的过程中加锁‘
         *
         * 只要是同一把锁，就能锁住需要这个锁的所有线程
         *
         * synchronized (this):SpringBoot所有组件在容器中都是单例的
         */
        // TODO 本地锁 synchronized JUC(lock)，只能锁住当前进程,在分布式情况下，要想锁住所有，必须使用分布式锁
        synchronized (this) {
            // 同步代码块
            // 得到锁以后，我们应该再去缓存中确定一次，如果没有才继续查询数据库
            String catelogJson = redisTemplate.opsForValue().get("catelogJson");
            if (!StringUtils.isEmpty(catelogJson)) {
                return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
            }

            //  一次获取所有三级分类数据
            List<CategoryEntity> selectList = baseMapper.selectList(null);

            // 查出所有的一级分类
            List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

            /**
             * 封装数据
             *
             * key: 一级分类的id
             * Value: 二级分类的列表以及内部的子分类  List<Catelog2Vo>
             */
            Map<String, List<Catelog2Vo>> listMap = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                // 每一个一级分类-- 查询该分类的子分类
                List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                // 封装二级分类列表
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map((l2) -> {
                        // 封装二级分类下的三级分类数据
                        List<CategoryEntity> level3Category = getParentCid(selectList, l2.getCatId());
                        List<Catelog2Vo.Category3Vo> category3Vos = null;
                        if (level3Category != null) {
                            // 封装Category3Vo
                            category3Vos = level3Category.stream().map((l3) -> {
                                Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return category3Vo;
                            }).collect(Collectors.toList());
                        }

                        // 组装二级分类
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), category3Vos, l2.getCatId().toString(), l2.getName());
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }

                return catelog2Vos;
            }));

            // 3、将查到的数据再放入再缓存中，将对象转为json放在缓存中
            String s = JSON.toJSONString(listMap);
            // 设置过期时间为1天 -- 解决缓存雪崩
            redisTemplate.opsForValue().set("catelogJson", s, 1, TimeUnit.DAYS);

            return listMap;
        }
    }


    /**
     * 使用Redis  SETNX
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {

        // 1、使用分布式锁，占坑 -- 去Redis中保存一个Key相同的东西
        // setIfAbsent() --- SETNX EX  占坑与设置过期时间--原子性
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            // 加锁成功 -- 执行业务
            // 设置过期时间 -- 防止死锁
            // redisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            // 设置过期时间必须和加锁是同步的，原子的
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = null;
            try {
                // 无论业务是否执行成功，我们都进行解锁
                catelogJsonFromDb = getCatelogJsonFromDb();
            } finally {
                /**
                 *      可能在传回值后一会儿锁过期了，等接收到的值确定是自己的锁，
                 *      但再去删除的时候已经是别人的锁
                 *     所以需要保证删除锁的过程也要是原子性的。
                 *
                 *     需要是使用lua脚本执行原子操作
                 */
                String luaScript = "if redis.call('get',KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call('del',KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                // 执行脚本原子删除锁  删成功返回1，失败返回0 Long类型
                redisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList("lock"), uuid);

            }

            return catelogJsonFromDb;
        } else {
            // 加锁失败.... 重试 synchronized () -- 自旋
            // 休眠后200ms重试
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }

            return getCatelogJsonFromDbWithRedisLock();
        }
    }


    /**
     * 使用Redisson
     * <p>
     * 缓存中的数据如何和数据库保持一致？ --- 缓存数据一致性问题
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        // 获取分布式锁 --- 注意锁的名字：涉及锁的粒度, 粒度越细越快
        RLock lock = redissonClient.getLock("catajson-lock");
        lock.lock(); // 加锁
        // 执行业务
        Map<String, List<Catelog2Vo>> catelogJsonFromDb;
        try {
            // 从数据库获取数据 -- 内含将数据加到缓存中
            catelogJsonFromDb = getCatelogJsonFromDb();
            return catelogJsonFromDb;
        } finally {
            // 解锁
            lock.unlock();
        }
    }

    /***
     * 从数据库获取三级分类json数据
     *
     * 一个原子操作
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {

        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.isEmpty(catelogJson)) {
            return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }

        //  一次获取所有三级分类数据
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 查出所有的一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        /**
         * 封装数据
         *
         * key: 一级分类的id
         * Value: 二级分类的列表以及内部的子分类  List<Catelog2Vo>
         */
        Map<String, List<Catelog2Vo>> listMap = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 每一个一级分类-- 查询该分类的子分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
            // 封装二级分类列表
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map((l2) -> {
                    // 封装二级分类下的三级分类数据
                    List<CategoryEntity> level3Category = getParentCid(selectList, l2.getCatId());
                    List<Catelog2Vo.Category3Vo> category3Vos = null;
                    if (level3Category != null) {
                        // 封装Category3Vo
                        category3Vos = level3Category.stream().map((l3) -> {
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return category3Vo;
                        }).collect(Collectors.toList());
                    }

                    // 组装二级分类
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), category3Vos, l2.getCatId().toString(), l2.getName());
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        // 3、将查到的数据再放入再缓存中，将对象转为json放在缓存中
        String s = JSON.toJSONString(listMap);
        // 设置过期时间为1天 -- 解决缓存雪崩
        redisTemplate.opsForValue().set("catelogJson", s, 1, TimeUnit.DAYS);

        return listMap;
    }

    /**
     * 从集合中挑选出Pcid的类别
     */
    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(category -> parent_cid.equals(category.getParentCid())).collect(Collectors.toList());
    }


    // 递归查询并收集路径信息 225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 获取当前分类的id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        // 如果存在父分类，则需要递归查询
        if (byId.getParentCid() != 0) {
            //递归查找父节点
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    /**
     * 递归查找所有菜单的子菜单
     *
     * @param root 当前菜单
     * @param all  所有菜单
     * @return 子菜单
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            // 过滤 当菜单的父id等于root菜单的id则为root菜单的子菜单
            return categoryEntity.getParentCid().longValue() == root.getCatId().longValue();  // 注意此处应该用longValue()来比较，否则会出先bug，因为parentCid和catId是long类型
        }).map(categoryEntity -> {
            // 1 找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
        return children;
    }

}
