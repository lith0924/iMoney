package org.springblade.modules.business.imai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.business.imai.dto.AiChatMessage;
import org.springblade.modules.business.imai.dto.AiMessageRequest;
import org.springblade.modules.business.imai.dto.AiMessageResponse;
import org.springblade.modules.business.imai.enums.EnumAiApiUrl;
import org.springblade.modules.business.imai.service.IAiChatService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

	private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public AiMessageResponse chatCompletion(String sysMsg, String msg, EnumAiApiUrl apiUrl, String model, String key) {
		AiMessageRequest request = buildAiRequest(msg, sysMsg, model);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", key);

		HttpEntity<AiMessageRequest> entity = new HttpEntity<>(request, headers);
		AiMessageResponse result = new AiMessageResponse();

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				apiUrl.getUrl(),
				HttpMethod.POST,
				entity,
				String.class
			);
			String raw = response.getBody();
			result.setRawResponse(raw);
			result.setSuccess(response.getStatusCode().is2xxSuccessful());
			if (!StringUtils.hasText(raw)) {
				result.setContent("");
				return result;
			}
			JsonNode root = objectMapper.readTree(raw);
			result.setRequestId(root.path("id").asText(""));
			String content = root.path("choices").path(0).path("message").path("content").asText("");
			result.setContent(content);
			if (!response.getStatusCode().is2xxSuccessful()) {
				result.setError(raw);
			}
			return result;
		} catch (Exception e) {
			log.error("zhipu chatCompletion failed", e);
			result.setSuccess(Boolean.FALSE);
			result.setError(e.getMessage());
			result.setContent("");
			return result;
		}
	}

	private AiMessageRequest buildAiRequest(String msg, String sysMsg, String model) {
		AiMessageRequest request = new AiMessageRequest();
		request.setModel(StringUtils.hasText(model) ? model : "glm-4-flash");
		request.setStream(Boolean.FALSE);

		List<AiChatMessage> messages = new ArrayList<>();
		if (StringUtils.hasText(sysMsg)) {
			AiChatMessage system = new AiChatMessage();
			system.setRole("system");
			system.setContent(sysMsg);
			messages.add(system);
		}
		AiChatMessage user = new AiChatMessage();
		user.setRole("user");
		user.setContent(msg == null ? "" : msg);
		messages.add(user);
		request.setMessages(messages);
		return request;
	}
}
