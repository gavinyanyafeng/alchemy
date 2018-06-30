package com.dfire.platform.alchemy.web.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author congbai
 * @date 06/03/2018
 */
@Component
public class SpringUtils implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return SpringUtils.applicationContext;
    }

    public static <T> T getBean(String name) {
        return (T)applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 上面两个方法，如果bean不存在，会抛出异常，所以需要先判断是否存在
     *
     * @param name
     * @param t
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> t) {
        if (applicationContext.containsBean(name)) {
            return applicationContext.getBean(name, t);
        }
        return null;
    }

    /**
     * 获取这个类型的所有bean
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> getBeanList(Class<T> clazz) {
        Map<String, T> beanMap = applicationContext.getBeansOfType(clazz);
        if (beanMap == null || beanMap.isEmpty()) {
            return null;
        }
        return new ArrayList<>(beanMap.values());
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
    }
}
