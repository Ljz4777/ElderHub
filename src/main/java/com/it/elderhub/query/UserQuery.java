package com.it.elderhub.query;

import lombok.Data;

@Data
public class UserQuery {
    private String username;
    private String nickname;
    private Integer roleId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
