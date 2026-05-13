package org.springblade.modules.business.imai.enums;

public enum EnumAiApiUrl {
	ZHIPU_CHAT_COMPLETIONS("https://open.bigmodel.cn/api/paas/v4/chat/completions", "智谱AI");

	private final String url;
	private final String name;

	EnumAiApiUrl(String url, String name) {
		this.url = url;
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}
}
