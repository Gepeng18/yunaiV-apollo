package com.ctrip.framework.apollo.common.controller;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter implements EmbeddedServletContainerCustomizer {

    // 增加分页参数解析器 https://blog.csdn.net/flowingflying/article/details/79515226
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 创建 PageableHandlerMethodArgumentResolver 对象
        PageableHandlerMethodArgumentResolver pageResolver = new PageableHandlerMethodArgumentResolver();
        pageResolver.setFallbackPageable(new PageRequest(0, 10)); // 默认第 0 页，大小为 10 。
        // 添加到 argumentResolvers 中
        argumentResolvers.add(pageResolver);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 禁用使用后缀方式进行内容协商 https://junq.io/spring-mvc%E5%AE%9E%E7%8E%B0http%E5%86%85%E5%AE%B9%E5%8D%8F%E5%95%86-content-negotiation.html
        configurer.favorPathExtension(false);
        // 默认为 application/json
        configurer.ignoreAcceptHeader(true).defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("html", "text/html;charset=utf-8");
        container.setMimeMappings(mappings);
    }

}