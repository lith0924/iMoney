package org.springblade.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 用于拦截和处理自定义异常，返回统一的错误码和消息格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(CustomException.class)
	public R handleCustomException(CustomException e, HttpServletRequest request) {
		log.error("自定义异常 - URI: {}, 错误码: {}, 错误信息: {}",
			request.getRequestURI(), e.getCode(), e.getMessage());
		return R.fail(e.getCode(), e.getMessage());
	}
}
