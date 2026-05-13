package org.springblade.modules.business.imoperationlog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.business.imoperationlog.entity.ImOperationLog;
import org.springblade.modules.business.imoperationlog.mapper.ImOperationLogMapper;
import org.springblade.modules.business.imoperationlog.service.IImOperationLogService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ImOperationLogServiceImpl extends ServiceImpl<ImOperationLogMapper, ImOperationLog> implements IImOperationLogService {

	private static final int SCOPE_PRIVATE = 1;
	private static final int SCOPE_SHARED = 2;
	private static final int REQUEST_TEXT_MAX_LEN = 500;
	private static final int RESULT_MSG_MAX_LEN = 255;
	/** 与常见库表 varchar(255) 对齐，避免异常栈写入 result_msg 超长 */
	private static final int MAX_RESULT_MSG_LEN = 240;
	private static final int MAX_REQUEST_TEXT_LEN = 2000;

	private static String limitDbText(String value, int maxLen) {
		if (value == null) {
			return null;
		}
		if (maxLen <= 0) {
			return "";
		}
		if (value.length() <= maxLen) {
			return value;
		}
		int keep = Math.max(0, maxLen - 3);
		return value.substring(0, keep) + "...";
	}

	@Override
	public void recordOperation(Long userId, Integer scopeType, Long groupId, String opType, String refOpId, String requestText, String resultCode, String resultMsg) {
		ImOperationLog log = new ImOperationLog();
		log.setOpId(buildLogOpId());
		log.setRefOpId(refOpId);
		log.setUserId(userId);
		Integer logScopeType = scopeType != null && scopeType.equals(SCOPE_SHARED) && groupId != null ? SCOPE_SHARED : SCOPE_PRIVATE;
		log.setScopeType(logScopeType);
		log.setGroupId(logScopeType.equals(SCOPE_SHARED) ? groupId : null);
		log.setOpType(opType);
		log.setRequestText(limitLength(requestText, REQUEST_TEXT_MAX_LEN));
		log.setRequestText(limitDbText(requestText, MAX_REQUEST_TEXT_LEN));
		log.setResultCode(resultCode);
		log.setResultMsg(limitLength(resultMsg, RESULT_MSG_MAX_LEN));
		log.setResultMsg(limitDbText(resultMsg, MAX_RESULT_MSG_LEN));
		log.setCreateTime(new Date());
		this.save(log);
	}

	private String buildLogOpId() {
		return new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
	}

	private String limitLength(String value, int maxLen) {
		if (value == null || value.length() <= maxLen) {
			return value;
		}
		return value.substring(0, maxLen);
	}
}
