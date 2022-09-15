package com.ctrip.framework.apollo.build;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.internals.Injector;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;

/**
 * Apollo 注入器
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloInjector {

    /**
     * 注入器
     */
    private static volatile Injector s_injector;
    /**
     * 锁
     */
    private static final Object lock = new Object();

    /**
     * s_injector是单例，使用 JDK SPI 进行加载
     */
    private static Injector getInjector() {
        // 若 Injector 不存在，则初始化
        if (s_injector == null) {
            synchronized (lock) {
                if (s_injector == null) {
                    try {
                        // do 基于 JDK SPI 加载对应的 Injector 实现对象
                        s_injector = ServiceBootstrap.loadFirst(Injector.class);
                    } catch (Throwable ex) {
                        ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Injector!", ex);
                        Tracer.logError(exception);
                        throw exception;
                    }
                }
            }
        }
        return s_injector;
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            // getInjector() 通过SPI加载了 Injector 对象，最终底层调用还是基于 Guice 进行依赖注入对象的 getInstance()
            return getInjector().getInstance(clazz);
        } catch (Throwable ex) {
            Tracer.logError(ex);
            throw new ApolloConfigException(String.format("Unable to load instance for type %s!", clazz.getName()), ex);
        }
    }

    public static <T> T getInstance(Class<T> clazz, String name) {
        try {
            return getInjector().getInstance(clazz, name);
        } catch (Throwable ex) {
            Tracer.logError(ex);
            throw new ApolloConfigException(String.format("Unable to load instance for type %s and name %s !", clazz.getName(), name), ex);
        }
    }

}
