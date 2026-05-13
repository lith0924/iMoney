package org.springblade.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @program:
 * @description: 切面，取消启动时打印Generating unique operation named
 * @author:
 * @create: 2021-01-15
 **/
@Slf4j
@Component
@Aspect
public class CachingOperationNameGeneratorAspect {
	private Map<String, Integer> generated = newHashMap();


	@Pointcut("execution(* springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator.startingWith(String))")
	public void c() {
	}


	@Around("c()")
	public Object a(ProceedingJoinPoint point) {
		Object[] args = point.getArgs();
		return startingWith(String.valueOf(args[0]));
	}

	private String startingWith(String prefix) {
		if (generated.containsKey(prefix)) {
			generated.put(prefix, generated.get(prefix) + 1);
			String nextUniqueOperationName = String.format("%s_%s", prefix, generated.get(prefix));
//			log.warn("组件中存在相同的方法名称，自动生成组件方法唯一名称进行替换: {}", nextUniqueOperationName);
			return nextUniqueOperationName;
		} else {
			generated.put(prefix, 0);
			return prefix;
		}
	}
}
