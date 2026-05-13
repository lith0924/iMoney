package org.springblade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.common.constant.LauncherConstant;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 启动器
 *
 * @author Chill
 */
@EnableScheduling
@SpringBootApplication
@EnableAsync
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		// 定义 application 变量
		ConfigurableApplicationContext application = BladeApplication.run(LauncherConstant.APPLICATION_NAME, Application.class, args);

		// 获取环境对象
		Environment env = application.getEnvironment();
		// 使用日志框架的占位符格式化日志信息
		String address = "http://" + getHostAddress() + ":" + env.getProperty("server.port");
		LOGGER.info("\n[----------------------------------------------------------]\n\t" +
			"\t{}" +
			"\n[----------------------------------------------------------]", address);
	}

	private static String getHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			LOGGER.error("无法获取本地主机地址", e);
			return "未知地址";
		}
	}
}
