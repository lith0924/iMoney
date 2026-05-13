package org.springblade.modules.business.imai.service;

import org.springblade.modules.business.imai.dto.AiMessageResponse;
import org.springblade.modules.business.imai.enums.EnumAiApiUrl;

public interface IAiChatService {

	AiMessageResponse chatCompletion(String sysMsg, String msg, EnumAiApiUrl apiUrl, String model, String key);
}
