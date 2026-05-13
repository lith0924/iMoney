package org.springblade.modules.business.imcommand.service;

import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;

public interface IImCommandService {


	ImCommandExecuteResponse execute(String wxUserId, String command);

	ImCommandExecuteResponse ensureUser(String wxUserId, String nickName);

	ImCommandExecuteResponse ensureUser(String wxUserId, String nickName, String botId, String botToken);
}

