package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redisson;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listwithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // 2. 组装成父子的树形结构
        // 2.1 先找到一级分类
        List<CategoryEntity> level1Menu = categoryEntities.stream().filter((categoryEntity ->
                categoryEntity.getParentCid() == 0
        )).map(menu -> {
            menu.setChildren(getChildrens(menu,categoryEntities));
            return menu;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
        return level1Menu;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 逻辑删除

        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new LinkedList<>();
        findPath(catelogId, path);
        Collections.reverse(path);
        Long[] objects = path.toArray(new Long[path.size()]);
        return  objects;
    }

    @Override
    @Transactional
    public void updateCascae(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }


    public Map<String, List<Catelog2Vo>> getCatelogJsonBD() {
        /**
         * 优化:将数据库中的多次查询变为一次,存至缓存selectList,需要的数据从list取出,避免频繁的数据库交互
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有1级分类
        List<CategoryEntity> level1 = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1.查出1级分类中所有2级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    //2.封装上面的结果
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //查询当前2级分类的3级分类
                            List<CategoryEntity> level3 = getParent_cid(selectList, l2.getCatId());
                            if (level3 != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3.stream().map(l3 -> {
                                    //封装指定格式
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }
        ));
        return parent_cid;
    }


    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        //加入缓存逻辑,缓存中存的数据是JSON字符串
        //JSON跨语言,跨平台兼容
        //从缓存中取出的数据要逆转为能用的对象类型,序列化与发序列化
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            //缓存中没有数据,查询数据库
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();
            //查到的数据再放入缓存,将对象转为JSON放入缓存中
            String s = JSON.toJSONString(catelogJsonFromDb);
            stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return catelogJsonFromDb;
        }
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    //本地进程锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLocalLock() {
        synchronized (this) {
            String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
            if (StringUtils.isEmpty(catalogJSON)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
                return result;
            }
            /**
             * 优化:将数据库中的多次查询变为一次,存至缓存selectList,需要的数据从list取出,避免频繁的数据库交互
             */
            List<CategoryEntity> selectList = baseMapper.selectList(null);
            //1.查出所有1级分类
            List<CategoryEntity> level1 = getParent_cid(selectList, 0L);
            //2.封装数据
            Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                        //1.查出1级分类中所有2级分类
                        List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                        //2.封装上面的结果
                        List<Catelog2Vo> catelog2Vos = null;
                        if (categoryEntities != null) {
                            catelog2Vos = categoryEntities.stream().map(l2 -> {
                                Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                //查询当前2级分类的3级分类
                                List<CategoryEntity> level3 = getParent_cid(selectList, l2.getCatId());
                                if (level3 != null) {
                                    List<Catelog2Vo.Catelog3Vo> collect = level3.stream().map(l3 -> {
                                        //封装指定格式
                                        Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                        return catelog3Vo;
                                    }).collect(Collectors.toList());
                                    catelog2Vo.setCatalog3List(collect);
                                }
                                return catelog2Vo;
                            }).collect(Collectors.toList());
                        }
                        return catelog2Vos;
                    }
            ));
            String s = JSON.toJSONString(parent_cid);
            stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return parent_cid;
        }
    }

    //分布式程锁Redis
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        //占分布式锁,同时设置锁过期时间,必须和加锁同步原子操作
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", "111", 300, TimeUnit.SECONDS);
        if (lock) {
            //加锁成功,执行业务
            Map<String, List<Catelog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1])else return 0 end";
                //删除锁,Lua脚本
                Long lock1 = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock", uuid));
            }
            return dataFromDB;
        } else {
            //加锁失败,休眠2秒,重试
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getCatelogJsonFromDbWithRedisLock();//自旋
        }
    }

    //分布式锁Redisson
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        //加锁成功,执行业务
        Map<String, List<Catelog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }
        return dataFromDB;
    }


    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        /**
         * 优化:将数据库中的多次查询变为一次,存至缓存selectList,需要的数据从list取出,避免频繁的数据库交互
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有1级分类
        List<CategoryEntity> level1 = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1.查出1级分类中所有2级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    //2.封装上面的结果
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //查询当前2级分类的3级分类
                            List<CategoryEntity> level3 = getParent_cid(selectList, l2.getCatId());
                            if (level3 != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3.stream().map(l3 -> {
                                    //封装指定格式
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }
        ));
        String s = JSON.toJSONString(parent_cid);
        stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
    }

    private void findPath(Long categorygId, List<Long> path) {
        if (categorygId!=0){
            path.add(categorygId);
            CategoryEntity byId = getById(categorygId);
            findPath(byId.getParentCid(),path);
        }
    }

    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){
        return all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            // 找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
    }

}