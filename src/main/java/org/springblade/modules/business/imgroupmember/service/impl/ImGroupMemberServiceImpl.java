package org.springblade.modules.business.imgroupmember.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imgroup.entity.ImGroup;
import org.springblade.modules.business.imgroup.mapper.ImGroupMapper;
import org.springblade.modules.business.imgroupmember.entity.ImGroupMember;
import org.springblade.modules.business.imgroupmember.mapper.ImGroupMemberMapper;
import org.springblade.modules.business.imgroupmember.service.IImGroupMemberService;
import org.springblade.modules.business.iminvitecode.entity.ImInviteCode;
import org.springblade.modules.business.iminvitecode.service.IImInviteCodeService;
import org.springblade.modules.business.immemberalias.entity.ImMemberAlias;
import org.springblade.modules.business.immemberalias.service.IImMemberAliasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
public class ImGroupMemberServiceImpl extends ServiceImpl<ImGroupMemberMapper, ImGroupMember> implements IImGroupMemberService {

    @Autowired
    private IImInviteCodeService inviteCodeService;

    @Autowired
    private ImGroupMapper groupMapper;
    @Autowired
    private IImMemberAliasService memberAliasService;

    @Override
    @Transactional
    public ImGroupMember joinGroupByInviteCode(String inviteCode, Long inviteeUserId) {
        // 先校验邀请码，确认可以加入后再消耗次数，避免重复加入误扣邀请码。
        ImInviteCode code = inviteCodeService.getAvailableInviteCode(inviteCode, inviteeUserId);
        ImGroup group = groupMapper.selectById(code.getGroupId());
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            throw new RuntimeException("账本不存在或已解散");
        }
        
        // 检查用户是否已经有群组成员记录
        QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", code.getGroupId());
        wrapper.eq("user_id", inviteeUserId);
        ImGroupMember existingMember = this.getOne(wrapper);
        
        if (existingMember != null && existingMember.getStatus() != null && existingMember.getStatus() == 1) {
            throw new RuntimeException("用户已经是群组成员");
        }

        ImGroupMember member;
        Date now = new Date();
        if (existingMember != null) {
            existingMember.setRoleType(2);
            existingMember.setJoinedAt(now);
            existingMember.setLeftAt(null);
            existingMember.setStatus(1);
            this.updateById(existingMember);
            member = existingMember;
        } else {
            // 创建新的群组成员记录
            member = new ImGroupMember();
            member.setGroupId(code.getGroupId());
            member.setUserId(inviteeUserId);
            member.setRoleType(2); // 2成员
            member.setJoinedAt(now);
            member.setStatus(1); // 1在册
            this.save(member);
        }
        
        inviteCodeService.markInviteCodeUsed(code, inviteeUserId);
        saveInviteAliasIfPresent(code, inviteeUserId, now);
        
        return member;
    }

    private void saveInviteAliasIfPresent(ImInviteCode code, Long inviteeUserId, Date now) {
        if (code == null
            || !StringUtils.hasText(code.getInviteeAlias())
            || code.getGroupId() == null
            || code.getInviterUserId() == null
            || inviteeUserId == null
            || code.getInviterUserId().equals(inviteeUserId)) {
            return;
        }
        QueryWrapper<ImMemberAlias> aliasWrapper = new QueryWrapper<>();
        aliasWrapper.eq("group_id", code.getGroupId());
        aliasWrapper.eq("owner_user_id", code.getInviterUserId());
        aliasWrapper.eq("target_user_id", inviteeUserId);
        ImMemberAlias alias = memberAliasService.getOne(aliasWrapper);
        if (alias == null) {
            alias = new ImMemberAlias();
            alias.setGroupId(code.getGroupId());
            alias.setOwnerUserId(code.getInviterUserId());
            alias.setTargetUserId(inviteeUserId);
            alias.setAliasName(code.getInviteeAlias().trim());
            alias.setCreateTime(now);
            alias.setUpdateTime(now);
            memberAliasService.save(alias);
            return;
        }
        alias.setAliasName(code.getInviteeAlias().trim());
        alias.setUpdateTime(now);
        memberAliasService.updateById(alias);
    }

    @Override
    @Transactional
    public boolean leaveGroup(Long groupId, Long userId) {
        // 查找用户在群组中的成员记录
        QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("user_id", userId);
        wrapper.eq("status", 1); // 1在册
        ImGroupMember member = this.getOne(wrapper);
        
        if (member == null) {
            throw new RuntimeException("用户不是群组成员");
        }
        
        // 检查是否是创建者
        if (member.getRoleType() == 1) {
            throw new RuntimeException("创建者不能退出群组");
        }
        
        // 更新成员状态
        member.setStatus(0); // 0已退出
        member.setLeftAt(new Date());
        
        // 保存更新
        return this.updateById(member);
    }

    @Override
    public Integer getMemberRole(Long groupId, Long userId) {
        // 查找用户在群组中的成员记录
        QueryWrapper<ImGroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId);
        wrapper.eq("user_id", userId);
        wrapper.eq("status", 1); // 1在册
        ImGroupMember member = this.getOne(wrapper);
        
        if (member == null) {
            return null;
        }
        
        return member.getRoleType();
    }
}
