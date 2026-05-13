package org.springblade.modules.business.imai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiMessageRequest {
	private String model;
	private Boolean stream = Boolean.FALSE;
	private List<AiChatMessage> messages = new ArrayList<>();
}
