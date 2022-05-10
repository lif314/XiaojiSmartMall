package com.lif314.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 * ListValue校验器
 *
 * 必须实现接口  ConstraintValidator<ListValue, Integer>
 *     - 第一个参数是校验的注解
 *
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {

    private Set<Integer> set = new HashSet<>();

    // 初始化方法：获取注解上详细信息 {0，1}
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }
    }

    /**
     * 判断是否校验成功
     * @param value 提交的值，判断改值知否在constraintAnnotation.vals();范围中
     * @param context 校验的上下文环境信息
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return set.contains(value);
    }
}
