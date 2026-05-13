package org.springblade.common.exception;

/**
 * 错误码枚举
 * 定义各种异常的错误码和消息
 */
public enum ErrorCodeEnum {

	// 成功
	SUCCESS(200, "操作成功"),

	// 权限相关 (1000-1999)
	PERMISSION_DENIED(1001, "权限不足"),
	UNAUTHORIZED(1002, "未授权访问"),
	TOKEN_EXPIRED(1003, "token已过期"),
	TOKEN_INVALID(1004, "token无效"),

	// 参数相关 (2000-2999)
	PARAM_ERROR(2001, "参数错误"),
	PARAM_MISSING(2002, "缺少必要参数"),
	PARAM_INVALID(2003, "参数格式错误"),

	// 用户相关 (3000-3999)
	USER_NOT_FOUND(3001, "用户不存在"),
	USER_DISABLED(3002, "用户已被禁用"),
	USER_FREE_COUNT_EXHAUSTED(3003, "免费次数已用完"),
	USER_VIP_EXPIRED(3004, "VIP已过期"),
	USER_LEVEL_INSUFFICIENT(3005, "用户等级不足"),

	// 业务相关 (4000-4999)
	BUSINESS_ERROR(4001, "请续费VIP"),
	FILE_NOT_FOUND(4002, "文件不存在"),
	FILE_UPLOAD_FAILED(4003, "文件上传失败"),
	FILE_PROCESS_FAILED(4004, "文件处理失败"),

	// 系统相关 (5000-5999)
	SYSTEM_ERROR(5001, "系统错误"),
	DATABASE_ERROR(5002, "数据库错误"),
	NETWORK_ERROR(5003, "网络错误"),
	SERVICE_UNAVAILABLE(5004, "服务不可用"),

	// 未知错误
	UNKNOWN_ERROR(9999, "未知错误");

	private final Integer code;
	private final String message;

	ErrorCodeEnum(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
