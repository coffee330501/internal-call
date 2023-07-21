package io.github.coffee330501.utils;

import io.github.coffee330501.SignatureResult;
import io.github.coffee330501.annotation.Internal;
import io.github.coffee330501.annotation.InternalController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

@Slf4j
public class SignatureUtil {
    private SignatureUtil() {
    }

    /**
     * 是否为内部调用接口
     *
     * @param handler handler
     * @return boolean
     */
    public static boolean isInternalCall(Object handler) {
        try {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Object bean = handlerMethod.getBean();

            // 检查注解InternalController是否存在
            Class<?> beanClass = bean.getClass();
            InternalController internalControllerAnno = beanClass.getAnnotation(InternalController.class);
            if (internalControllerAnno != null) return true;
            // 如果是spring代理的bean则使用方面的方法获取不到注解
            internalControllerAnno = AnnotationUtils.findAnnotation(beanClass, InternalController.class);
            if (internalControllerAnno != null) return true;

            // 检查注解Internal是否存在
            Method method = handlerMethod.getMethod();
            Internal internalAnno = method.getAnnotation(Internal.class);
            return internalAnno != null;
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }
    }

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @param <T>  返回数据类型
     * @return SignatureOutput
     */
    public static <T> SignatureResult<T> success(T data) {
        SignatureResult<T> signatureOutput = new SignatureResult<>();
        signatureOutput.setCode(200);
        signatureOutput.setData(data);
        return signatureOutput;
    }

    /**
     * 业务失败返回
     *
     * @param msg 失败信息
     * @return SignatureOutput
     */
    public static SignatureResult<Object> errorByBusiness(String msg) {
        SignatureResult<Object> signatureOutput = new SignatureResult<>();
        signatureOutput.setCode(500);
        signatureOutput.setMsg(msg);
        return signatureOutput;
    }

    /**
     * 系统失败返回
     *
     * @param msg 失败信息
     * @return SignatureOutput
     */
    public static SignatureResult<Object> errorBySystem(String msg) {
        SignatureResult<Object> signatureOutput = new SignatureResult<>();
        signatureOutput.setCode(501);
        signatureOutput.setMsg(msg);
        return signatureOutput;
    }

    /**
     * 客户端错误导致错误的response，此时msg用于展示错误原因
     *
     * @param msg 失败信息
     * @return SignatureOutput
     */
    public static SignatureResult<Object> errorByClient(String msg) {
        SignatureResult<Object> signatureOutput = new SignatureResult<>();
        signatureOutput.setCode(400);
        signatureOutput.setMsg(msg);
        return signatureOutput;
    }
}
