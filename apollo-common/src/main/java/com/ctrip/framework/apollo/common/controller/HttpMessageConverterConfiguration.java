package com.ctrip.framework.apollo.common.controller;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;

import java.util.List;

/**
 * Created by Jason on 5/11/16.
 */
@Configuration
public class HttpMessageConverterConfiguration {

    @Bean
    public HttpMessageConverters messageConverters() {
        // 创建 GsonHttpMessageConverter 对象，用于对 JSON 数据转换
        GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create());
        // 创建 HttpMessageConverter 数组
        final List<HttpMessageConverter<?>> converters = Lists.newArrayList(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(), new AllEncompassingFormHttpMessageConverter(), gsonHttpMessageConverter);
        return new HttpMessageConverters() {
            @Override
            public List<HttpMessageConverter<?>> getConverters() {
                return converters;
            }
        };
    }

}