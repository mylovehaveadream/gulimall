package com.guigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserInfoTo {

    private Long userId;
    private String userKey; //临时用户，一定封装
    private boolean tempUser = false;    //cookie里面有临时用户，就改为true
}
