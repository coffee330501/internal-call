package io.github.coffee330501.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内部调用类
 * 声明为内部调用类后类中的所有方法都将变为内部调用方法，可不再添加@Internal
 */
@Controller
@ResponseBody
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalController {
    @AliasFor(
            annotation = Controller.class
    )
    String value() default "";
}
