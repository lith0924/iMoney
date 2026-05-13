package org.springblade.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class Verify {
	public static boolean check(String value) {
		if (value != null && !value.equals("")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean check(Integer value) {
		if (value != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean check(List value) {
		if (value != null && value.size() != 0) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean check(Object value) {
		if (value != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean notCheck(String value) {
		if (value != null && !value.equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean notCheck(Integer value) {
		if (value != null) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean notCheck(List value) {
		if (value != null && value.size() != 0) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean notCheck(Object value) {
		if (value != null) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isNull(String value) {
		if (value != null && !value.equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isNull(Integer value) {
		if (value != null) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isNull(List value) {
		if (value != null && value.size() != 0) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isNull(Object value) {
		if (value != null) {
			return false;
		} else {
			return true;
		}
	}

	//日期截取到年日
	public static String dateConversion(String value) {
		String dateTime = null;
		try {
			SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
			dateTime = date.format(date.parse(value));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateTime;
	}
}
