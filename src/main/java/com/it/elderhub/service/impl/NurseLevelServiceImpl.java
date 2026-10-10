package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.entity.NurseContent;
import com.it.elderhub.entity.NurseLevel;
import com.it.elderhub.entity.NurseLevelItem;
import com.it.elderhub.mapper.NurseContentMapper;
import com.it.elderhub.mapper.NurseLevelItemMapper;
import com.it.elderhub.mapper.NurseLevelMapper;
import com.it.elderhub.service.NurseLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 护理级别Service实现类
 */
@Service
public class NurseLevelServiceImpl implements NurseLevelService {

    @Autowired
    private NurseLevelMapper nurseLevelMapper;

    @Autowired
    private NurseLevelItemMapper nurseLevelItemMapper;

    @Autowired
    private NurseContentMapper nurseContentMapper;

    @Override
    public IPage<NurseLevel> listLevels(Integer pageNum, Integer pageSize, Integer levelStatus) {
        // 构建查询条件
        LambdaQueryWrapper<NurseLevel> wrapper = new LambdaQueryWrapper<>();
        // 只查未删除的
        wrapper.eq(NurseLevel::getIs_deleted, 0);
        // 按状态筛选
        if (levelStatus != null) {
            wrapper.eq(NurseLevel::getLevel_status, levelStatus);
        }
        // 按ID倒序（表无 create_time 列）
        wrapper.orderByDesc(NurseLevel::getId);

        // 执行分页查询
        Page<NurseLevel> page = new Page<>(pageNum, pageSize);
        return nurseLevelMapper.selectPage(page, wrapper);
    }

    @Override
    public void saveLevel(NurseLevel level) {
        // 设置默认值
        if (level.getLevel_status() == null) {
            level.setLevel_status(1); // 默认启用
        }
        level.setIs_deleted(0);
        nurseLevelMapper.insert(level);
    }

    @Override
    public void updateLevelStatus(Integer id, Integer levelStatus) {
        NurseLevel level = new NurseLevel();
        level.setId(id);
        level.setLevel_status(levelStatus);
        nurseLevelMapper.updateById(level);
    }

    @Override
    public List<NurseContent> listLevelItems(Integer levelId) {
        // 1. 查询关联表，获取该级别下的所有项目ID
        LambdaQueryWrapper<NurseLevelItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(NurseLevelItem::getLevel_id, levelId);
        List<NurseLevelItem> levelItems = nurseLevelItemMapper.selectList(itemWrapper);

        // 如果没有配置项目，返回空列表
        if (levelItems == null || levelItems.isEmpty()) {
            return List.of();
        }

        // 2. 根据项目ID列表查询项目详情
        List<Integer> itemIds = levelItems.stream()
                .map(NurseLevelItem::getItem_id)
                .collect(Collectors.toList());

        LambdaQueryWrapper<NurseContent> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.in(NurseContent::getId, itemIds);
        contentWrapper.eq(NurseContent::getIs_deleted, 0);
        contentWrapper.orderByDesc(NurseContent::getId);

        return nurseContentMapper.selectList(contentWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLevelItem(Integer levelId, Integer itemId) {
        // 1. 校验护理项目是否存在且启用
        NurseContent content = nurseContentMapper.selectById(itemId);
        if (content == null || content.getIs_deleted() == 1) {
            throw new RuntimeException("护理项目不存在");
        }
        if (content.getStatus() != 1) {
            throw new RuntimeException("该护理项目已停用，不能添加");
        }

        // 2. 校验是否已经添加过（防止重复）
        LambdaQueryWrapper<NurseLevelItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NurseLevelItem::getLevel_id, levelId)
                .eq(NurseLevelItem::getItem_id, itemId);
        if (nurseLevelItemMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该项目已配置在此级别下，请勿重复添加");
        }

        // 3. 插入关联记录
        NurseLevelItem levelItem = new NurseLevelItem();
        levelItem.setLevel_id(levelId);
        levelItem.setItem_id(itemId);
        nurseLevelItemMapper.insert(levelItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLevelItem(Integer levelId, Integer itemId) {
        // 删除关联记录
        LambdaQueryWrapper<NurseLevelItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NurseLevelItem::getLevel_id, levelId)
                .eq(NurseLevelItem::getItem_id, itemId);
        nurseLevelItemMapper.delete(wrapper);
    }
}
