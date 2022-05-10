package com.lif314.gulimall.product.exception;

import com.lif314.common.exception.BizCodeEnum;
import com.lif314.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j  // 日志记录
//@ControllerAdvice(basePackages = "com.lif314.gulimall.product.controller") // 统一处理异常
//@ResponseBody
@RestControllerAdvice(basePackages = "com.lif314.gulimall.product.controller") // 等同于上面两个
public class GulimallExceptionControllerAdvice {

    // 数据校验异常处理 -- 精确处理
    @ExceptionHandler(value = Exception.class)
//    @ResponseBody
    public R handleValidException(MethodArgumentNotValidException exception ){
        log.error("数据校验异常: {}, 异常类型： {}", exception.getMessage(), exception.getClass());

        // 获取异常具体信息
        Map<String,String> errorMap = new HashMap<>();
        BindingResult bindingResult = exception.getBindingResult();
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(), BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", errorMap);
    }

    // 公共处理异常
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){

        return R.error(BizCodeEnum.UNKNOW_EXEPTION.getCode(), BizCodeEnum.UNKNOW_EXEPTION.getMsg());
    }

}
