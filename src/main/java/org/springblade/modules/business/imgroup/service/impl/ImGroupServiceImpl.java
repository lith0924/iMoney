package org.springblade.modules.business.imgroup.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imgroup.entity.ImGroup;
import org.springblade.modules.business.imgroup.mapper.ImGroupMapper;
import org.springblade.modules.business.imgroup.service.IImGroupService;
import org.springblade.modules.business.imgroupmember.entity.ImGroupMember;
import org.springblade.modules.business.imgroupmember.service.IImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.Date;

@Service
public class ImGroupServiceImpl extends ServiceImpl<ImGroupMapper, ImGroup> implements IImGroupService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final Random RANDOM = new Random();

    @Autowired
    private IImGroupMemberService groupMemberService;

    @Override
    @Transactional
    public ImGroup createGroup(String groupName, Long ownerUserId) {
        // 生成唯一的分组编码
        String groupCode = generateUniqueGroupCode();
        
        // 创建分组对象
        ImGroup group = new ImGroup();
        group.setGroupCode(groupCode);
        group.setGroupName(groupName);
        group.setOwnerUserId(ownerUserId);
        group.setStatus(1); // 1启用
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());
        
        // 保存分组
        this.save(group);
        
        // 将创建者添加为群组成员
        ImGroupMember member = new ImGroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerUserId);
        member.setRoleType(1); // 1创建者
        member.setJoinedAt(new Date());
        member.setStatus(1); // 1在册
        groupMemberService.save(member);
        
        return group;
    }

    @Override
    public List<ImGroup> getUserGroups(Long userId) {
        // 先获取用户加入的所有群组ID
        QueryWrapper<ImGroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("user_id", userId);
        memberWrapper.eq("status", 1); // 1在册
        List<ImGroupMember> members = groupMemberService.list(memberWrapper);
        
        if (members.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 提取群组ID
        List<Long> groupIds = members.stream().map(ImGroupMember::getGroupId).collect(java.util.stream.Collectors.toList());
        
        // 查询群组信息
        QueryWrapper<ImGroup> groupWrapper = new QueryWrapper<>();
        groupWrapper.in("id", groupIds);
        groupWrapper.eq("status", 1); // 1启用
        
        return this.list(groupWrapper);
    }

    @Override
    public List<ImGroupMember> getGroupMembers(Long groupId) {
        // 检查群组是否存在
        ImGroup group = this.getById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new RuntimeException("群组不存在或已解散");
        }
        
        // 查询群组成员
        QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("status", 1); // 1在册
        
        return groupMemberService.list(wrapper);
    }

    @Override
    @Transactional
    public boolean dissolveGroup(Long groupId, Long ownerUserId) {
        // 检查群组是否存在
        ImGroup group = this.getById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new RuntimeException("群组不存在或已解散");
        }
        
        // 检查是否是创建者
        if (!group.getOwnerUserId().equals(ownerUserId)) {
            throw new RuntimeException("只有创建者可以解散群组");
        }
        
        // 更新群组状态为停用
        group.setStatus(0); // 0停用
        group.setUpdateTime(new Date());
        boolean updated = this.updateById(group);
        
        if (updated) {
            // 更新所有群组成员状态为已退出
            QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
            wrapper.eq("group_id", groupId);
            wrapper.eq("status", 1); // 1在册
            List<ImGroupMember> members = groupMemberService.list(wrapper);
            
            for (ImGroupMember member : members) {
                member.setStatus(0); // 0已退出
                member.setLeftAt(new Date());
                groupMemberService.updateById(member);
            }
        }
        
        return updated;
    }

    /**
     * 生成随机分组编码
     * @return 分组编码
     */
    private String generateUniqueGroupCode() {
        for (int i = 0; i < 20; i++) {
            String code = generateGroupCode();
            QueryWrapper<ImGroup> wrapper = new QueryWrapper<>();
            wrapper.eq("group_code", code);
            if (this.count(wrapper) == 0) {
                return code;
            }
        }
        throw new RuntimeException("群组编码生成失败，请重试");
    }

    private String generateGroupCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
