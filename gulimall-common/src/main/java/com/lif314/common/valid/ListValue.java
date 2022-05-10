package com.lif314.common.valid;



import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
/**
 * 指明校验器
 *
 * 自定义校验器
 *
 * 关联校验器
 */
@Constraint(validatedBy = { ListValueConstraintValidator.class})
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface ListValue {

    /**
     * 一般默认值为全类名：在配置文件中去除配置作为返回的消息
     * 配置文件：搜索ValidationMessages.properties
     *
     * 创建配置文件
     */
    String message() default "{com.lif314.common.valid.ListValue.message}";


    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default {};

    int[] vals() default {};
}
