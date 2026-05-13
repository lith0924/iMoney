package org.springblade.modules.business.imai.dto;

import lombok.Data;

@Data
public class AiMessageResponse {
	private Boolean success;
	private String content;
	private String requestId;
	private String rawResponse;
	private String error;
}
