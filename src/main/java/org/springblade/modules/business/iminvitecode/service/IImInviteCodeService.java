package org.springblade.modules.business.iminvitecode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.iminvitecode.entity.ImInviteCode;

public interface IImInviteCodeService extends IService<ImInviteCode> {
    /**
     * 创建邀请码
     * @param groupId 共享圈子ID
     * @param inviterUserId 邀请人用户ID
     * @param inviteeAlias 邀请时给对方的备注
     * @param maxUse 最大使用次数
     * @param expiresAt 过期时间
     * @return 生成的邀请码
     */
    ImInviteCode createInviteCode(Long groupId, Long inviterUserId, String inviteeAlias, Integer maxUse, java.util.Date expiresAt);

    /**
     * 校验并返回可用的邀请码，不消耗使用次数
     * @param inviteCode 邀请码
     * @param inviteeUserId 被邀请人用户ID
     * @return 可用的邀请码对象
     */
    ImInviteCode getAvailableInviteCode(String inviteCode, Long inviteeUserId);

    /**
     * 消耗邀请码使用次数
     * @param code 邀请码对象
     * @param inviteeUserId 被邀请人用户ID
     */
    void markInviteCodeUsed(ImInviteCode code, Long inviteeUserId);

    /**
     * 使用邀请码
     * @param inviteCode 邀请码
     * @param inviteeUserId 被邀请人用户ID
     * @return 邀请码对象
     */
    ImInviteCode useInviteCode(String inviteCode, Long inviteeUserId);
}
