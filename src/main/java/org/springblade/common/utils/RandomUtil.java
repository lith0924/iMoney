package org.springblade.common.utils;

import java.util.Random;

public class RandomUtil {
	/**
	 * 生成账号（4 位随机英文 + 时间戳后 4 位）
	 *
	 * @return 8 位账号
	 */
	public static String generateAccountWithTimestamp() {
		// 生成 4 位随机英文
		String englishPart = generateRandomEnglish(4);
		// 获取时间戳后 4 位
		String timestampPart = getLast4DigitsOfTimestamp();
		// 拼接账号
		return englishPart + timestampPart;
	}

	/**
	 * 生成随机英文字母
	 *
	 * @param length 长度
	 * @return 随机英文字符串
	 */
	private static String generateRandomEnglish(int length) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = random.nextInt(characters.length());
			sb.append(characters.charAt(index));
		}
		return sb.toString();
	}

	/**
	 * 获取时间戳后 4 位
	 *
	 * @return 时间戳后 4 位字符串
	 */
	private static String getLast4DigitsOfTimestamp() {
		long timestamp = System.currentTimeMillis(); // 获取当前时间戳
		String timestampStr = Long.toString(timestamp);
		return timestampStr.substring(timestampStr.length() - 4); // 截取后 4 位
	}
}
