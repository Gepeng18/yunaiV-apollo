package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.property.AutoUpdateConfigChangeListener;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apollo Property Sources processor for Spring Annotation Based Application. <br /> <br />
 * <p>
 * The reason why PropertySourcesProcessor implements {@link BeanFactoryPostProcessor} instead of
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} is that lower versions of
 * Spring (e.g. 3.1.1) doesn't support registering BeanDefinitionRegistryPostProcessor in ImportBeanDefinitionRegistrar
 * - {@link com.ctrip.framework.apollo.spring.annotation.ApolloConfigRegistrar}
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    /**
     * Namespace 名字集合
     *
     * KEY：优先级
     * VALUE：Namespace 名字集合
     */
    private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();
    /**
     * 是否初始化的标识
     */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector.getInstance(ConfigPropertySourceFactory.class);
    private final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    /**
     * Spring ConfigurableEnvironment 对象
     */
    private ConfigurableEnvironment environment;

    /**
     * 添加到 `NAMESPACE_NAMES` 中
     *
     * @param namespaces Namespace 名字
     * @param order 优先级
     * @return 是否添加成功
     */
    public static boolean addNamespaces(Collection<String> namespaces, int order) {
        return NAMESPACE_NAMES.putAll(order, namespaces);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (INITIALIZED.compareAndSet(false, true)) {
            // 初始化 PropertySource 们
            initializePropertySources();
            // 初始化 AutoUpdateConfigChangeListener 对象，实现属性的自动更新
            initializeAutoUpdatePropertiesFeature(beanFactory);
        }
    }

    private void initializePropertySources() {
        // 若 `environment` 已经有 APOLLO_PROPERTY_SOURCE_NAME 属性源，说明已经初始化，直接返回。
        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME)) {
            // already initialized
            return;
        }
        // 创建 CompositePropertySource 对象，组合多个 Namespace 的 ConfigPropertySource 。
        CompositePropertySource composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);
        // 按照优先级，顺序遍历 Namespace
        // sort by order asc
        ImmutableSortedSet<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
        for (Integer order : orders) {
            for (String namespace : NAMESPACE_NAMES.get(order)) {
                // 创建 Apollo Config 对象
                Config config = ConfigService.getConfig(namespace);
                // 创建 Namespace 对应的 ConfigPropertySource 对象
                // 添加到 `composite` 中。
                composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
            }
        }

        // 若有 APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME 属性源，添加到其后
        // add after the bootstrap property source or to the first
        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME, composite);
        // 若没 APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME 属性源，添加到首个
        } else {
            environment.getPropertySources().addFirst(composite);
        }
    }

    private void initializeAutoUpdatePropertiesFeature(ConfigurableListableBeanFactory beanFactory) {
        // 若未开启属性的自动更新功能，直接返回
        if (!configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
            return;
        }
        // 创建 AutoUpdateConfigChangeListener 对象
        AutoUpdateConfigChangeListener autoUpdateConfigChangeListener = new AutoUpdateConfigChangeListener(environment, beanFactory);
        // 循环，向 ConfigPropertySource 注册配置变更器
        List<ConfigPropertySource> configPropertySources = configPropertySourceFactory.getAllConfigPropertySources();
        for (ConfigPropertySource configPropertySource : configPropertySources) {
            configPropertySource.addChangeListener(autoUpdateConfigChangeListener);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        //it is safe enough to cast as all known environment is derived from ConfigurableEnvironment
        this.environment = (ConfigurableEnvironment) environment;
    }

    //only for test
    private static void reset() {
        NAMESPACE_NAMES.clear();
        INITIALIZED.set(false);
    }

    @Override
    public int getOrder() {
        // make it as early as possible
        return Ordered.HIGHEST_PRECEDENCE; // 最高优先级
    }

}
