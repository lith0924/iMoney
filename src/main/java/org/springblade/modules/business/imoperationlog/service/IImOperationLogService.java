package org.springblade.modules.business.imoperationlog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.business.imoperationlog.entity.ImOperationLog;

public interface IImOperationLogService extends IService<ImOperationLog> {

	void recordOperation(Long userId, Integer scopeType, Long groupId, String opType, String refOpId, String requestText, String resultCode, String resultMsg);
}
