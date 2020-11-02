package com.guigu.gulimall.member.exception;

public class UsernameExitException extends RuntimeException {

    public UsernameExitException() {
        super("用户名存在");
    }
}
