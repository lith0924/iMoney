package org.springblade.modules.business.iminvitecode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.iminvitecode.entity.ImInviteCode;
import org.springblade.modules.business.iminvitecode.mapper.ImInviteCodeMapper;
import org.springblade.modules.business.iminvitecode.service.IImInviteCodeService;
import org.springblade.modules.business.iminvitecodeuselog.entity.ImInviteCodeUseLog;
import org.springblade.modules.business.iminvitecodeuselog.service.IImInviteCodeUseLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.Date;

@Service
public class ImInviteCodeServiceImpl extends ServiceImpl<ImInviteCodeMapper, ImInviteCode> implements IImInviteCodeService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 5;
    private static final Random RANDOM = new Random();

    @Autowired
    private IImInviteCodeUseLogService inviteCodeUseLogService;

    @Override
    public ImInviteCode createInviteCode(Long groupId, Long inviterUserId, String inviteeAlias, Integer maxUse, Date expiresAt) {
        String inviteCode = generateUniqueInviteCode();
        
        // 创建邀请码对象
        ImInviteCode code = new ImInviteCode();
        code.setInviteCode(inviteCode);
        code.setGroupId(groupId);
        code.setInviterUserId(inviterUserId);
        code.setInviteeAlias(inviteeAlias);
        code.setMaxUse(maxUse == null || maxUse <= 0 ? 1 : maxUse);
        code.setUsedCount(0);
        code.setStatus(1); // 1可用
        code.setExpiresAt(expiresAt);
        code.setCreateTime(new Date());
        
        // 保存到数据库
        this.save(code);
        
        return code;
    }

    @Override
    @Transactional
    public ImInviteCode useInviteCode(String inviteCode, Long inviteeUserId) {
        ImInviteCode code = getAvailableInviteCode(inviteCode, inviteeUserId);
        markInviteCodeUsed(code, inviteeUserId);
        return code;
    }

    @Override
    public ImInviteCode getAvailableInviteCode(String inviteCode, Long inviteeUserId) {
        // 查询邀请码
        QueryWrapper<ImInviteCode> wrapper = new QueryWrapper<>();
        wrapper.eq("invite_code", inviteCode);
        ImInviteCode code = this.getOne(wrapper);
        
        if (code == null) {
            throw new RuntimeException("邀请码不存在");
        }

        // 禁止邀请人自己使用自己的邀请码
        if (inviteeUserId != null && code.getInviterUserId() != null && code.getInviterUserId().equals(inviteeUserId)) {
            recordUseLog(code.getId(), inviteCode, code.getGroupId(), inviteeUserId, "SELF_USE", "不可使用自己的邀请码");
            throw new RuntimeException("不可以使用自己的邀请码");
        }
        
        // 检查邀请码状态
        if (code.getStatus() == null || code.getStatus() != 1) {
            // 记录失败日志
            recordUseLog(code.getId(), inviteCode, code.getGroupId(), inviteeUserId, "INVALID", "邀请码已失效");
            throw new RuntimeException("邀请码已失效");
        }
        
        // 检查邀请码是否过期
        if (code.getExpiresAt() != null && new Date().after(code.getExpiresAt())) {
            // 记录失败日志
            recordUseLog(code.getId(), inviteCode, code.getGroupId(), inviteeUserId, "EXPIRED", "邀请码已过期");
            throw new RuntimeException("邀请码已过期");
        }
        
        // 检查是否达到最大使用次数
        int usedCount = code.getUsedCount() == null ? 0 : code.getUsedCount();
        int maxUse = code.getMaxUse() == null || code.getMaxUse() <= 0 ? 1 : code.getMaxUse();
        if (usedCount >= maxUse) {
            // 记录失败日志
            recordUseLog(code.getId(), inviteCode, code.getGroupId(), inviteeUserId, "USED_UP", "邀请码已达到最大使用次数");
            throw new RuntimeException("邀请码已达到最大使用次数");
        }

        return code;
    }

    @Override
    @Transactional
    public void markInviteCodeUsed(ImInviteCode code, Long inviteeUserId) {
        if (code == null || code.getId() == null) {
            throw new RuntimeException("邀请码不存在");
        }

        UpdateWrapper<ImInviteCode> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", code.getId());
        wrapper.eq("status", 1);
        wrapper.apply("used_count < max_use");
        wrapper.setSql("status = CASE WHEN used_count + 1 >= max_use THEN 0 ELSE status END");
        wrapper.setSql("used_count = used_count + 1");
        wrapper.set("used_at", new Date());

        if (!this.update(wrapper)) {
            recordUseLog(code.getId(), code.getInviteCode(), code.getGroupId(), inviteeUserId, "USED_UP", "邀请码已被使用或失效");
            throw new RuntimeException("邀请码已被使用或失效");
        }
        
        recordUseLog(code.getId(), code.getInviteCode(), code.getGroupId(), inviteeUserId, "OK", "邀请码使用成功");
    }

    /**
     * 记录邀请码使用日志
     */
    private void recordUseLog(Long inviteCodeId, String inviteCode, Long groupId, Long usedByUserId, String resultCode, String resultMsg) {
        ImInviteCodeUseLog log = new ImInviteCodeUseLog();
        log.setInviteCodeId(inviteCodeId);
        log.setInviteCode(inviteCode);
        log.setGroupId(groupId);
        log.setUsedByUserId(usedByUserId);
        log.setResultCode(resultCode);
        log.setResultMsg(resultMsg);
        log.setCreateTime(new Date());
        inviteCodeUseLogService.save(log);
    }

    /**
     * 生成随机邀请码
     * @return 邀请码
     */
    private String generateUniqueInviteCode() {
        for (int i = 0; i < 20; i++) {
            String code = generateInviteCode();
            QueryWrapper<ImInviteCode> wrapper = new QueryWrapper<>();
            wrapper.eq("invite_code", code);
            if (this.count(wrapper) == 0) {
                return code;
            }
        }
        throw new RuntimeException("邀请码生成失败，请重试");
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
