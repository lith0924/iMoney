package org.springblade.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class NotEmptyBean {
	/**
	 * 判断对象是否为空
	 *
	 * @param obj
	 * @return
	 */
	public static void isNotEmptyBean(Object obj, Class... classT) {
		if (null != obj) {
			int listSize = 0;
			int i = 0;
			do {
				Object localObj;
				//得到类对象
				Class<?> c = (Class<?>) obj.getClass();
				//判断是不是List类型
				if (obj instanceof List) {
					List objList = (List) obj;
					listSize = objList.size();
					c = objList.get(i).getClass();
					localObj = objList.get(i);
					i++;
				} else {
					localObj = obj;
					i = listSize;
				}
				//得到属性集合
				Field[] fs = c.getDeclaredFields();
				//得到方法体集合
				Method[] methods = c.getDeclaredMethods();
				//遍历属性
				for (Field f : fs) {
					//设置属性是可以访问的(私有的也可以)
					f.setAccessible(true);
					String fieldGetName = parGetName(f.getName());
					//判断属性是否存在get方法
					if (!checkGetMet(methods, fieldGetName)) {
						continue;
					}
					Object val = new Object();
					//得到此属性的值
					try {
						val = f.get(localObj);
					} catch (IllegalAccessException e) {
					}
					//只要有1个属性不为空,那么就不是所有的属性值都为空
					if ((!Verify.check(String.valueOf(val)) && f.isAnnotationPresent(NotNull.class)) || (String.valueOf(val).equals("null") && f.isAnnotationPresent(NotNull.class))) {
						NotNull notNull = f.getAnnotation(NotNull.class);
						for (Class<?> group : notNull.groups()) {
							for (Class aClass : classT) {
								if (group == aClass) {
									System.out.println(f.getName() + "传参不能为空!");
									throw new ServiceException(f.getName() + "传参不能为空!");
								}
							}

						}
					}
				}
			} while (i != listSize);
		}
	}

	/**
	 * 拼接某属性的 get方法
	 *
	 * @param fieldName
	 * @return String
	 */
	public static String parGetName(String fieldName) {
		if (null == fieldName || "".equals(fieldName)) {
			return null;
		}
		int startIndex = 0;
		if (fieldName.charAt(0) == '_')
			startIndex = 1;
		return "get"
			+ fieldName.substring(startIndex, startIndex + 1).toUpperCase()
			+ fieldName.substring(startIndex + 1);
	}

	/**
	 * 判断是否存在某属性的 get方法
	 *
	 * @param methods
	 * @param fieldGetMet
	 * @return boolean
	 */
	public static Boolean checkGetMet(Method[] methods, String fieldGetMet) {

		for (Method met : methods) {
			if (fieldGetMet.equals(met.getName())) {
				return true;
			}
		}
		return false;
	}
}
