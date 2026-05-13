package org.springblade.common.aspect;

import com.google.gson.Gson;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @description: 切面，取消启动时打印Generating unique operation named
 * @create: 2021-01-15
 **/
@Aspect
@Component
public class WebLogAspect {
	private final static Logger logger = LoggerFactory.getLogger(WebLogAspect.class);

	@Pointcut("execution(public * com.zc.pre.routine.modules.system.controller..*.*(..))")
	public void sysController() {
	}

	/**
	 * 以 controller 包下定义的所有请求为切入点
	 */
	@Pointcut("execution(public * com.zc.pre.routine.modules.business.*.controller..*.*(..))")
	public void businessController() {
	}

	/**
	 * 在切点之前织入
	 *
	 * @param joinPoint
	 * @throws Throwable
	 */
//	@Before("sysController() || businessController()")
	public void doBefore(JoinPoint joinPoint) throws Throwable {
		// 开始打印请求日志
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();
		// 打印请求相关参数
		logger.info("========================================== Start ==========================================");
		// 打印请求 url
		logger.info("URL            : {}", request.getRequestURL().toString());
		// 打印 Http method
		logger.info("HTTP Method    : {}", request.getMethod());
		// 打印调用 controller 的全路径以及执行方法
		logger.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
		// 打印请求的 IP
		logger.info("IP             : {}", request.getRemoteAddr());
		// 打印请求入参
		logger.info("Request Args   : {}", new Gson().toJson(joinPoint.getArgs()));
	}

	/**
	 * 在切点之后织入
	 *
	 * @throws Throwable
	 */
//	@After("sysController() || businessController()")
	public void doAfter() throws Throwable {
		logger.info("=========================================== End ===========================================");
		// 每个请求之间空一行
		logger.info("");
	}

	/**
	 * 环绕
	 *
	 * @param proceedingJoinPoint
	 * @return
	 * @throws Throwable
	 */
//	@Around("sysController() || businessController()")
	public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		long startTime = System.currentTimeMillis();
		Object result = proceedingJoinPoint.proceed();
		// 打印出参
		logger.info("Response Args  : {}", new Gson().toJson(result));
		// 执行耗时
		logger.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);
		return result;
	}
}
