package org.springblade.modules.business.imcommand.dto;

import org.springblade.modules.business.imcommand.enums.EnumImCommandRoute;

public class ImCommandExecuteResponse {

	private boolean success;
	private String code;
	private String reply;
	private EnumImCommandRoute route;
	private String filePath;
	private String fileName;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getReply() {
		return reply;
	}

	public void setReply(String reply) {
		this.reply = reply;
	}

	public EnumImCommandRoute getRoute() {
		return route;
	}

	public void setRoute(EnumImCommandRoute route) {
		this.route = route;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
