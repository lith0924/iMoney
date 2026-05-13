package org.springblade.modules.business.imcommand.service;

import org.springblade.modules.business.imcommand.dto.ImCommandExecuteResponse;

public interface IImCommandReplyService {

	ImCommandExecuteResponse success(ImCommandExecuteResponse result, String reply);

	ImCommandExecuteResponse failure(ImCommandExecuteResponse result, String code, String reply);

	String formatStandardReply(String reply, boolean success);
}
