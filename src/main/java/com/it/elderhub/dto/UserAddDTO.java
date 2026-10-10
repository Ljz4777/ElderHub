package com.it.elderhub.dto;

import lombok.Data;

@Data
public class UserAddDTO {
    private String nickname;
    private String username;
    private String password;
    private Integer sex;
    private String email;
    private String phoneNumber;
    private Integer roleId;
}