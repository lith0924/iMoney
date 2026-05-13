package org.springblade.common.utils.http;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.springblade.core.tool.api.R;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;

/**
 * @Description: http过程的工具类 获取ip 及 客户端浏览器等信息
 * @Company
 * @Auther:
 * @Date: 2020-03-16 11:13
 */
public class HttpToolUtils {
	public static String INTRANET_IP = getIntranetIp(); // 内网IP
	public static String INTERNET_IP = getInternetIp(); // 外网IP

	/**
	 * 获取操作系统,浏览器及浏览器版本信息
	 *
	 * @param request
	 * @return
	 */
	public static Map<String, String> getOsAndBrowserInfo(HttpServletRequest request) {
		Map<String, String> map = Maps.newHashMap();
		String browserDetails = request.getHeader("User-Agent");
		String userAgent = browserDetails;
		String user = userAgent.toLowerCase();

		String os = "";
		String browser = "";

		//=================OS Info=======================
		if (userAgent.toLowerCase().contains("windows")) {
			os = "Windows";
		} else if (userAgent.toLowerCase().contains("mac")) {
			os = "Mac";
		} else if (userAgent.toLowerCase().contains("x11")) {
			os = "Unix";
		} else if (userAgent.toLowerCase().contains("android")) {
			os = "Android";
		} else if (userAgent.toLowerCase().contains("iphone")) {
			os = "IPhone";
		} else {
			os = "UnKnown, More-Info: " + userAgent;
		}
		//===============Browser===========================
		if (user.contains("edge")) {
			browser = (userAgent.substring(userAgent.indexOf("Edge")).split(" ")[0]).replace("/", "-");
		} else if (user.contains("msie")) {
			String substring = userAgent.substring(userAgent.indexOf("MSIE")).split(";")[0];
			browser = substring.split(" ")[0].replace("MSIE", "IE") + "-" + substring.split(" ")[1];
		} else if (user.contains("safari") && user.contains("version")) {
			browser = (userAgent.substring(userAgent.indexOf("Safari")).split(" ")[0]).split("/")[0]
				+ "-" + (userAgent.substring(userAgent.indexOf("Version")).split(" ")[0]).split("/")[1];
		} else if (user.contains("opr") || user.contains("opera")) {
			if (user.contains("opera")) {
				browser = (userAgent.substring(userAgent.indexOf("Opera")).split(" ")[0]).split("/")[0]
					+ "-" + (userAgent.substring(userAgent.indexOf("Version")).split(" ")[0]).split("/")[1];
			} else if (user.contains("opr")) {
				browser = ((userAgent.substring(userAgent.indexOf("OPR")).split(" ")[0]).replace("/", "-"))
					.replace("OPR", "Opera");
			}

		} else if (user.contains("chrome")) {
			browser = (userAgent.substring(userAgent.indexOf("Chrome")).split(" ")[0]).replace("/", "-");
		} else if ((user.contains("mozilla/7.0")) || (user.contains("netscape6")) ||
			(user.contains("mozilla/4.7")) || (user.contains("mozilla/4.78")) ||
			(user.contains("mozilla/4.08")) || (user.contains("mozilla/3"))) {
			browser = "Netscape-?";

		} else if (user.contains("firefox")) {
			browser = (userAgent.substring(userAgent.indexOf("Firefox")).split(" ")[0]).replace("/", "-");
		} else if (user.contains("rv")) {
			String IEVersion = (userAgent.substring(userAgent.indexOf("rv")).split(" ")[0]).replace("rv:", "-");
			browser = "IE" + IEVersion.substring(0, IEVersion.length() - 1);
		} else {
			browser = "UnKnown, More-Info: " + userAgent;
		}
		map.put("os", os);
		map.put("browser", browser);
		return map;
	}

	public static void writeJson(HttpServletResponse servletResponse, R result) {
		servletResponse.setCharacterEncoding("UTF-8");
		servletResponse.setContentType("application/json;charset=UTF-8");
		PrintWriter writer = null;
		try {
			writer = servletResponse.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.print(JSON.toJSONString(result));
		writer.flush();
		writer.close();
	}

	public static void writeJson(ServletResponse servletResponse, R result) {
		servletResponse.setCharacterEncoding("UTF-8");
		servletResponse.setContentType("application/json;charset=UTF-8");
		PrintWriter writer = null;
		try {
			writer = servletResponse.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.print(JSON.toJSONString(result));
		writer.flush();
		writer.close();
	}

	/**
	 * 获得内网IP
	 *
	 * @return 内网IP
	 */
	private static String getIntranetIp() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		System.out.println(INTRANET_IP);
		System.out.println(INTERNET_IP);
	}

	/**
	 * 获得外网IP
	 *
	 * @return 外网IP
	 */
	private static String getInternetIp() {
		try {
			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			Enumeration<InetAddress> addrs;
			while (networks.hasMoreElements()) {
				addrs = networks.nextElement().getInetAddresses();
				while (addrs.hasMoreElements()) {
					ip = addrs.nextElement();
					if (ip instanceof Inet4Address
						&& ip.isSiteLocalAddress()
						&& !ip.getHostAddress().equals(INTRANET_IP)) {
						return ip.getHostAddress();
					}
				}
			}
			// 如果没有外网IP，就返回内网IP
			return INTRANET_IP;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
