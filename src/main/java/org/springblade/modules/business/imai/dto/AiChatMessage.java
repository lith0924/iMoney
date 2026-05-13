package org.springblade.modules.business.imai.dto;

import lombok.Data;

/**
 * Chat Completions 请求中的单条 message（独立类型，避免内部类在部分环境下 NoClassDefFoundError）。
 */
@Data
public class AiChatMessage {
	private String role;
	private String content;
}
