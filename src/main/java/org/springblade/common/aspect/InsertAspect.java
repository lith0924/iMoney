package org.springblade.common.aspect;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springblade.common.utils.IdUtils;
import org.springblade.common.utils.time.DateUtils;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @description: 切面，取消启动时打印Generating unique operation named
 * @create: 2021-01-15
 **/
@Slf4j
@Component
@Aspect
public class InsertAspect {
//	@Pointcut("execution(public * com.baomidou.mybatisplus.core.mapper.BaseMapper.insert*(..))")
//	public void allInsert() {
//	}

	//	@Before("allInsert()")
	public void beforeInsert(JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		// 遍历参数，找到id字段
		for (Object arg : args) {
			Class clazz = arg.getClass();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.getName().equals("id")) {
					field.setAccessible(true);
					try {
						field.set(arg, IdUtils.nextId());
					} catch (IllegalAccessException e) {
						log.error("设置id失败", e);
						throw new RuntimeException("设置id失败");
					}
				}
				if (field.getName().equals("createUser") || field.getName().equals("updateUser")) {
					field.setAccessible(true);
					try {
						field.set(arg, AuthUtil.getUserId());
					} catch (IllegalAccessException e) {
						log.error("设置createUser或updateUser失败", e);
						throw new RuntimeException("设置createUser或updateUser失败");
					}
				}
				if (field.getName().equals("createTime") || field.getName().equals("updateTime")) {
					field.setAccessible(true);
					try {
						field.set(arg, DateUtils.currentDate());
					} catch (IllegalAccessException e) {
						log.error("设置createTime或updateTime失败", e);
						throw new RuntimeException("设置createTime或updateTime失败");
					}
				}
			}
			// 将修改后的参数替换原参数
			joinPoint.getArgs()[0] = arg;
		}
		log.info("填入参数成功");
	}

}
