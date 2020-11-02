package com.guigu.gulimall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {

        //md5不能直接进行密码的加密存储
        String s = DigestUtils.md5Hex("123456");

        //盐值加密：加上随机数,加盐：$1$+8位字符
        //验证： 123456进行盐值(去数据库查，所以要在数据库中维护一个盐值的字段)加密
        String s1 = Md5Crypt.md5Crypt("123456".getBytes(),"$1$aaaaaa");

        //上面的不好用，有一个更好用的
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();//密码编码器
        //$2a$10$NR8hDe2iNxVY.R1bCLObme7mOZubrvfFFDcM6Y0vKxCm09HjOB4Km
        String encode = passwordEncoder.encode("123456");//每次加密的值不一样的

        //它的盐值存在了密文里面，密文字符串可以解析这个盐值，不需要额外的存储了
        boolean matches = passwordEncoder
                .matches("123456", "$2a$10$NR8hDe2iNxVY.R1bCLObme7mOZubrvfFFDcM6Y0vKxCm09HjOB4Km");
        System.out.println(encode + "=>" + matches);
    }

}
