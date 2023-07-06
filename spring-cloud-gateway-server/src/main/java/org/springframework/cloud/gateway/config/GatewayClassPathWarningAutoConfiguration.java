/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.support.MvcFoundOnClasspathException;
import org.springframework.context.annotation.Configuration;

/**
 * 作用：检查项目中是否存在 spring-webmvc 依赖，如果存在则抛出异常
 * 启动：通过 ClassPath 目录下的 spring.factories 文件中定义的自动配置类，会加载该类，
 * 		GatewayClassPathWarningAutoConfiguration 会在 第一个加载，用于检查项目是否引入了合适的组件，如果没有则抛出异常阻止项目启动
 *
 *
 * 说明：spring-cloud-gateway 有两种web实现方式：
 * 	1、spring-cloud-gateway-mvc： 即 Spring MVC 模块，是建立在 servlet 之上得， 使用的是同步阻塞式 I/O 模型
 * 	2、spring-cloud-gateway-webflux： 是一个异步非阻塞式的 Web 框架，性能相较于 Spring MVC 更好，但是需要依赖于 Netty
 *
 * spring-cloud-gateway 默认使用 spring-webflux， 这里就需要排除 spring-webmvc模块，不能同时存在两个web模块
 * 如果需要使用 spring-webmvc 模块，需要导入 spring-cloud-starter-gateway-mvc 依赖
 *
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
public class GatewayClassPathWarningAutoConfiguration {

	private static final Log log = LogFactory.getLog(GatewayClassPathWarningAutoConfiguration.class);

	private static final String BORDER = "\n\n**********************************************************\n\n";


	/**
	 * 作用：检查项目中是否存在 spring-webmvc 依赖，如果存在则抛出异常
	 *
	 * @Configuration 注解表示该类是一个配置类，会被spring加载为组件
	 *	1、proxyBeanMethods = true时为：Full 全模式。 该模式下注入容器中的同一个组件无论被取出多少次都是同一个bean实例，即单实例对象，在该模式下SpringBoot每次启动都会判断检查容器中是否存在该组件
	 * 	2、proxyBeanMethods = false时为：Lite 轻量级模式。该模式下注入容器中的同一个组件无论被取出多少次都是不同的bean实例，即多实例对象，在该模式下SpringBoot每次启动会跳过检查容器中是否存在该组件
	 *
	 *  @ConditionalOnClass 注解的作用是当项目中存在某个类时才会使标有该注解的类或方法生效
	 *  这里表示 在 ClassPath 中存在 org.springframework.web.servlet.DispatcherServlet 类时, 会注册该类
	 *
	 *  @ConditionalOnWebApplication 注解表示 当前项目是一个web项目时 该类才会生效
	 *  这里指定了类型为 servlet， 所以只有当项目是一个 servlet 项目时，会注册该类
	 *
	 *  当满足上述条件之后，会注册该类， 又因该类只有一个构造函数，所以注册该类会执行该构造函数，最终会抛出异常 MvcFoundOnClasspathException
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class SpringMvcFoundOnClasspathConfiguration {

		public SpringMvcFoundOnClasspathConfiguration() {
			throw new MvcFoundOnClasspathException();
		}

	}

	/**
	 * @ConditionalOnMissingClass 注解的作用是判断当前项目中是否缺失某个类，与@ConditionalOnClass相关
	 * 	这里表示 当项目中不存在 org.springframework.web.reactive.DispatcherHandler 类时，会注册该类（执行构造函数会抛出警告）
	 *
	 * 	说明：DispatcherHandler 类似于 spring-webmvc 的 DispatcherServlet	，是求分发处理器
	 *
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.web.reactive.DispatcherHandler")
	protected static class WebfluxMissingFromClasspathConfiguration {

		public WebfluxMissingFromClasspathConfiguration() {
			log.warn(BORDER + "Spring Webflux is missing from the classpath, "
					+ "which is required for Spring Cloud Gateway at this time. "
					+ "Please add spring-boot-starter-webflux dependency." + BORDER);
		}

	}

}
