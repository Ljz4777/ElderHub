package com.it.elderhub.dto;

import lombok.Data;

@Data
public class UserQuery {
    private String nickname;
    private Integer roleId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}