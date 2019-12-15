package com.github.kaguya.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class User {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 昵称
     */
    private String username;

    /**
     * 邮箱(登录账号)
     */
    private String email;

    /**
     * 用户性别
     */
    private String sex;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 简介
     */
    private String description;

    /**
     * github
     */
    private String github;

    /**
     * twitter
     */
    private String twitter;

    /**
     * weibo
     */
    private String weibo;
}