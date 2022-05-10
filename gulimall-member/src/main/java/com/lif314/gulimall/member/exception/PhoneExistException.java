package com.lif314.gulimall.member.exception;

public class PhoneExistException extends RuntimeException{

    public PhoneExistException(){
        super("手机号已存在");
    }
}
