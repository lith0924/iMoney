package org.springblade.common.utils;

public class StringUtils {
	/**
	 * 将驼峰命名字段转换为下划线分隔的字段
	 *
	 * @param camelCase 驼峰命名字段（如 "userName"）
	 * @return 下划线分隔的字段（如 "user_name"）
	 */
	public static String camelToUnderline(String camelCase) {
		if (camelCase == null || camelCase.isEmpty()) {
			return camelCase;
		}

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < camelCase.length(); i++) {
			char currentChar = camelCase.charAt(i);
			if (Character.isUpperCase(currentChar)) {
				// 在大写字母前添加下划线，并将其转换为小写
				result.append('_');
				result.append(Character.toLowerCase(currentChar));
			} else {
				result.append(currentChar);
			}
		}
		return result.toString();
	}
}
