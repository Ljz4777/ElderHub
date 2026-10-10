package com.it.elderhub.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private Integer id;
    private String nickname;
    private String email;
    private String phoneNumber;
    private Integer sex;
    private Integer roleId;
}