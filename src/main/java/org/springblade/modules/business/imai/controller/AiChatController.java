package org.springblade.modules.business.imai.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.imai.dto.AiMessageResponse;
import org.springblade.modules.business.imai.enums.EnumAiApiUrl;
import org.springblade.modules.business.imai.service.IAiChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blade-business/ai")
@Api(value = "AI对话", tags = "智谱AI对话接口")
public class AiChatController extends BladeController {

	private final IAiChatService aiChatService;

	@Value("${imai.zhipu.api-key:}")
	private String defaultApiKey;

	@PostMapping("/chat/completions")
	@ApiOperation(value = "智谱聊天补全", notes = "调用 https://open.bigmodel.cn/api/paas/v4/chat/completions")
	public R<AiMessageResponse> chatCompletion(@RequestBody ChatReq req) {
		String requestKey = StringUtils.hasText(req.getKey()) ? req.getKey() : defaultApiKey;
		AiMessageResponse response = aiChatService.chatCompletion(
			req.getSysMsg(),
			req.getMsg(),
			EnumAiApiUrl.ZHIPU_CHAT_COMPLETIONS,
			req.getModel(),
			requestKey
		);
		return R.data(response);
	}

	@Data
	public static class ChatReq {
		private String sysMsg;
		private String msg;
		private String model;
		private String key;
	}
}
