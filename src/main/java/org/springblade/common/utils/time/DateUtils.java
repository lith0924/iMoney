package org.springblade.common.utils.time;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @Description: 日期工具类
 * @Company
 * @Auther:
 * @Date: 2020-03-16 11:15
 */
@Log4j2
public class DateUtils {


	public final static SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");

	public final static SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");

	public final static SimpleDateFormat sdfDays = new SimpleDateFormat("yyyyMMdd");

	public final static SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 获取当前日期
	 *
	 * @return
	 */
	public static Date currentDate() {
		return Calendar.getInstance().getTime();
	}

	/**
	 * 转时间戳
	 *
	 * @param dateStr
	 * @return
	 */
	public static Date getTimestamp(String dateStr) {
		return Calendar.getInstance().getTime();
	}


	/**
	 * 获取YYYY-MM-DD hh:mm:ss格式
	 *
	 * @return
	 */
	public static String getTime() {
		return sdfTime.format(new Date());
	}

	/**
	 * 获取YYYY-MM-DD hh:mm:ss格式
	 *
	 * @return
	 */
	public static String getSdfDayTime() {
		return sdfDay.format(new Date());
	}

	public static String localDateStr(String format) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return LocalDate.now().format(formatter);
	}

	public static String addHoursToDateTime(String dateTimeStr, int hours) {
		// 定义日期时间格式
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		// 解析日期时间字符串
		LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

		// 增加小时
		LocalDateTime newDateTime = dateTime.plusHours(hours);

		// 格式化新的日期时间字符串
		return newDateTime.format(formatter);
	}

	/**
	 * 获取当前时间的前 n 天
	 *
	 * @param days 天数
	 * @return 前 n 天的日期
	 */
	public static Date getDateBeforeDays(int days) {
		Calendar calendar = Calendar.getInstance(); // 获取当前时间
		calendar.add(Calendar.DAY_OF_YEAR, -days); // 减去指定的天数
		return calendar.getTime(); // 返回计算后的日期
	}

	/**
	 * 获取当前时间过去指定天数的时间
	 *
	 * @param daysPast 过去的天数
	 * @param format   日期格式
	 * @return 格式化后的日期字符串
	 */
	public static String getDatePastDays(int daysPast, String format) {
		// 获取当前日期
		LocalDate currentDate = LocalDate.now();

		// 获取过去指定天数的日期
		LocalDate pastDate = currentDate.minusDays(daysPast);

		// 格式化日期输出
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return pastDate.format(formatter);
	}

	/**
	 * 获取当天任意的时间
	 * 获取任意时间
	 * format:时:分:秒
	 *
	 * @param format 例如：yyyy-MM-dd 00:00:00
	 * @return
	 */
	public static String getWeeHours(String format) {
		return DateFormatUtils.format(currentDate(), format);
	}

	public static String format(Date date, String format) {
		return DateFormatUtils.format(date, format);
	}

	public static Date format(String date) {
		try {
			return sdfTime.parse(date);
		} catch (Exception e) {
			log.error("时间转化失败");
		}
		return null;
	}

	public static Date format(String date, SimpleDateFormat sdf) {
		try {
			return sdf.parse(date);
		} catch (Exception e) {
			log.error("时间转化失败");
		}
		return null;
	}

	public static String formatNormal(Date date) {
		return DateFormatUtils.format(date, "yyyy-MM-dd hh:mm:ss");
	}

	public static String yearLater(Date date, Integer num) {
		LocalDate currentDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		// 几年后的日期
		LocalDate oneYearLater = currentDate.plusYears(num);

		// 格式化日期以仅包含年月日
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate = oneYearLater.format(formatter);

		// 打印结果
		return formattedDate;
	}

	/**
	 * 获取时间+几天后的日期
	 */
	public static Date getDateAfterDays(String date, int days) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date date1 = sdf.parse(date);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date1);
			calendar.add(Calendar.DAY_OF_YEAR, days);
			return calendar.getTime();
		} catch (Exception e) {
			log.error("时间转化失败");
		}
		return null;

	}
}
