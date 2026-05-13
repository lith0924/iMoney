package org.springblade.test;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springblade.modules.business.imai.dto.AiMessageResponse;
import org.springblade.modules.business.imai.enums.EnumAiApiUrl;
import org.springblade.modules.business.imai.service.IAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

@SpringBootTest
public class AiChatServiceTest {

	@Autowired
	private IAiChatService aiChatService;

	@Test
	public void testZhipuChatCompletion() {
		String key = System.getenv("ZHIPU_API_KEY");
		Assumptions.assumeTrue(StringUtils.hasText(key), "未设置环境变量 ZHIPU_API_KEY，跳过真实调用测试");

		AiMessageResponse resp = aiChatService.chatCompletion(
			"你是一个简洁的助手。",
			"你好，请用一句话介绍你自己。",
			EnumAiApiUrl.ZHIPU_CHAT_COMPLETIONS,
			"glm-4-flash",
			key
		);

		System.out.println("success = " + resp.getSuccess());
		System.out.println("content = " + resp.getContent());
		System.out.println("requestId = " + resp.getRequestId());
		System.out.println("error = " + resp.getError());

		Assumptions.assumeTrue(Boolean.TRUE.equals(resp.getSuccess()), "调用失败: " + resp.getError());
		Assumptions.assumeTrue(StringUtils.hasText(resp.getContent()), "返回内容为空");
	}
}
