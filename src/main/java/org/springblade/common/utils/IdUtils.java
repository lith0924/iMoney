package org.springblade.common.utils;

/**
 * @Description: id生成工具
 * @Company
 * @Auther:
 * @Date: 2020-03-16 12:38
 */
public class IdUtils {

	/**
	 * 业务id开始排列，根据业务需求按表名顺序递增。每个模块增加100
	 */
	public static final long BASE_CODING = 0;
	public static final long BASE_WEB_SOCKET_MSG = 1;//WEB_SOCKET


	/**
	 * 系统模块
	 */
	public static final long SYS_ORGANIZATION = 100;
	public static final long SYS_USER = 101;
	public static final long BS_ACTIVITY = 102;
	public static final long BS_LIKE_PLAYER = 103;
	public static final long BS_PLAYER = 104;
	public static final long BS_PRIZE = 105;
	public static final long FILE_NAME = 106;
	public static final long SYS_FILE = 107;
	public static final long WX_ORDER = 108;


	/**
	 * 利用雪花算法生成id
	 * @param workerId     业务id
	 * @return 19位的雪花算法id
	 */
	public static Long nextId(long workerId) {
		return SnowFlakeUtil.nextId(workerId, 1);
	}

	/**
	 * 利用雪花算法生成id
	 * @return 19位的雪花算法id
	 */
	public static Long nextId() {
		return SnowFlakeUtil.nextId(100L, 1);
	}
}
