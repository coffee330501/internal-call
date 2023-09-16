package io.github.coffee330501.utils;

import cn.hutool.extra.spring.SpringUtil;

public class SpringContextUtil {
    public static <T> T getBean(Class<T> clazz) {
        try {
            return SpringUtil.getBean(clazz);
        } catch (Exception e) {
        }
        return null;
    }
}
