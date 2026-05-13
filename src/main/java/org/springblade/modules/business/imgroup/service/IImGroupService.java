package org.springblade.modules.business.imgroup.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.imgroup.entity.ImGroup;
import org.springblade.modules.business.imgroupmember.entity.ImGroupMember;

import java.util.List;

public interface IImGroupService extends IService<ImGroup> {
    /**
     * 创建分组
     * @param groupName 分组名称
     * @param ownerUserId 创建者用户ID
     * @return 创建的分组
     */
    ImGroup createGroup(String groupName, Long ownerUserId);

    /**
     * 获取用户加入的群组列表
     * @param userId 用户ID
     * @return 群组列表
     */
    List<ImGroup> getUserGroups(Long userId);

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 成员列表
     */
    List<ImGroupMember> getGroupMembers(Long groupId);

    /**
     * 解散群组
     * @param groupId 群组ID
     * @param ownerUserId 所有者用户ID
     * @return 是否成功
     */
    boolean dissolveGroup(Long groupId, Long ownerUserId);
}
