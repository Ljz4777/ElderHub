package com.it.elderhub.dto;

import lombok.Data;

@Data
public class UserAddDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer sex;
    private String phoneNumber;
    private String email;
    private Integer roleId;
}
