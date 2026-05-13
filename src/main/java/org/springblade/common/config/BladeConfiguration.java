/**
 * Copyright (c) 2018-2028, Chill Zhuang 庄骞 (smallchill@163.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.common.config;


import org.springblade.core.secure.registry.SecureRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Blade配置
 *
 * @author Chill
 */
@Configuration(proxyBeanMethods = false)
public class BladeConfiguration implements WebMvcConfigurer {
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").
					allowedOriginPatterns("*").
					//allowedOrigins("*"). //允许跨域的域名，可以用*表示允许任何域名使用
						allowedMethods("*"). //允许任何方法（post、get等）
					allowedHeaders("*"). //允许任何请求头
					allowCredentials(true). //带上cookie信息
					exposedHeaders(HttpHeaders.SET_COOKIE).maxAge(3600L); //maxAge(3600)表明在3600秒内，不需要再发送预检验请求，可以缓存该结果
			}
		};
	}

	@Bean
	public SecureRegistry secureRegistry() {
		SecureRegistry secureRegistry = new SecureRegistry();
		secureRegistry.setEnabled(false);
		secureRegistry.excludePathPatterns("/blade-auth/**");
		secureRegistry.excludePathPatterns("/blade-system/menu/auth-routes");
		secureRegistry.excludePathPatterns("/blade-system/tenant/info");
		secureRegistry.excludePathPatterns("/doc.html");
		secureRegistry.excludePathPatterns("/ApiUse/buy");
		secureRegistry.excludePathPatterns("/js/**");
		secureRegistry.excludePathPatterns("/ocr/**");
		secureRegistry.excludePathPatterns("/webjars/**");
		secureRegistry.excludePathPatterns("/webjars/**");
		secureRegistry.excludePathPatterns("/wx/**");
		// ilink 扫码页匿名调用，避免鉴权返回 HTML 导致前端 res.json() 报错
		secureRegistry.excludePathPatterns("/blade-business/ilink/**");
		secureRegistry.excludePathPatterns("/ilink-qr/**");
		return secureRegistry;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
		registry.addResourceHandler("/index.html").addResourceLocations("classpath:/static/");
	}

}
