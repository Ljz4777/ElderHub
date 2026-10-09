package com.it.elderhub.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private Integer id;
    private String nickname;
    private Integer sex;
    private String phoneNumber;
    private String email;
    private Integer roleId;
}
