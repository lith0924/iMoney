package org.springblade.modules.business.imgroupmember.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.imgroupmember.entity.ImGroupMember;

public interface IImGroupMemberService extends IService<ImGroupMember> {
    /**
     * 通过邀请码加入群组
     * @param inviteCode 邀请码
     * @param inviteeUserId 被邀请人用户ID
     * @return 群组成员对象
     */
    ImGroupMember joinGroupByInviteCode(String inviteCode, Long inviteeUserId);

    /**
     * 退出群组
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean leaveGroup(Long groupId, Long userId);

    /**
     * 获取用户在群组中的角色
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 角色类型
     */
    Integer getMemberRole(Long groupId, Long userId);
}
